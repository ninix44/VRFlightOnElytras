package org.vmstudio.visorflightonelytras.core.client.tasks;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseType;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseClient;
import org.vmstudio.visor.api.client.tasks.RegisterVisorTask;
import org.vmstudio.visor.api.client.tasks.TaskType;
import org.vmstudio.visor.api.client.tasks.VisorTask;
import org.vmstudio.visor.api.common.HandType;
import org.vmstudio.visor.api.common.addon.VisorAddon;
import org.vmstudio.visorflightonelytras.core.common.AddonNetworking;
import org.vmstudio.visorflightonelytras.core.common.AddonUtils;
import org.vmstudio.visorflightonelytras.core.network.NetworkHelper;

@RegisterVisorTask
public class TaskElytraFlight extends VisorTask {
    public static final String ID = "elytra_flight";

    private int takeoffPoseTicks;
    private int takeoffCooldown;
    private int steerPacketCooldown;
    private float lastSentSteering;
    private boolean sentNeutralSteering;

    public TaskElytraFlight(@NotNull VisorAddon owner) {
        super(owner);
    }

    @Override
    protected void onRun(@Nullable LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == null || player.level() == null || minecraft.isPaused()) {
            return;
        }

        if (takeoffCooldown > 0) {
            takeoffCooldown--;
        }
        if (steerPacketCooldown > 0) {
            steerPacketCooldown--;
        }

        if (!AddonUtils.hasUsableElytra(player)) {
            resetState();
            return;
        }

        PlayerPoseClient pose = VisorAPI.client().getVRLocalPlayer().getPoseData(PlayerPoseType.TICK);
        if (player.isFallFlying()) {
            takeoffPoseTicks = 0;
            tickSteering(player, pose);
            return;
        }

        sentNeutralSteering = false;
        lastSentSteering = 0.0F;
        tickTakeoff(player, pose);
    }

    private void tickTakeoff(LocalPlayer player, PlayerPoseClient pose) {
        if (!AddonUtils.canAttemptTakeoff(player) || !AddonUtils.isTPose(pose)) {
            takeoffPoseTicks = Math.max(0, takeoffPoseTicks - 1);
            return;
        }

        takeoffPoseTicks++;
        if (takeoffPoseTicks == AddonUtils.REQUIRED_T_POSE_TICKS - 2) {
            pulseBothHands(0.035F);
        }

        if (takeoffPoseTicks < AddonUtils.REQUIRED_T_POSE_TICKS || takeoffCooldown > 0) {
            return;
        }

        sendStartFlightPacket();
        pulseBothHands(0.12F);
        spawnTakeoffEffects(player, pose);
        takeoffCooldown = AddonUtils.TAKEOFF_COOLDOWN_TICKS;
        takeoffPoseTicks = 0;
    }

    private void tickSteering(LocalPlayer player, PlayerPoseClient pose) {
        float steering = AddonUtils.computeSteering(pose);
        spawnSteeringEffects(player, pose, steering);

        boolean shouldSendSteering = Math.abs(steering) >= AddonUtils.MIN_STEERING_TO_SEND;
        boolean changedEnough = Math.abs(steering - lastSentSteering) >= AddonUtils.STEERING_CHANGE_TO_RESEND;

        if (shouldSendSteering && (steerPacketCooldown <= 0 || changedEnough)) {
            sendSteeringPacket(steering);
            lastSentSteering = steering;
            steerPacketCooldown = AddonUtils.STEER_PACKET_INTERVAL_TICKS;
            sentNeutralSteering = false;
            pulseSteeringHand(steering);
            return;
        }

        if (!shouldSendSteering && !sentNeutralSteering && (Math.abs(lastSentSteering) >= AddonUtils.MIN_STEERING_TO_SEND || changedEnough)) {
            sendSteeringPacket(0.0F);
            lastSentSteering = 0.0F;
            sentNeutralSteering = true;
            steerPacketCooldown = AddonUtils.STEER_PACKET_INTERVAL_TICKS;
        }
    }

    private void spawnTakeoffEffects(LocalPlayer player, PlayerPoseClient pose) {
        AddonUtils.WingState wingState = AddonUtils.getWingState(pose);
        if (wingState == null) {
            return;
        }

        for (int i = 0; i < 6; i++) {
            spawnWingParticle(player, wingState.left().position(), 0.02D);
            spawnWingParticle(player, wingState.right().position(), 0.02D);
        }
        player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.45F, 1.15F, false);
    }

    private void spawnSteeringEffects(LocalPlayer player, PlayerPoseClient pose, float steering) {
        if (Math.abs(steering) < 0.25F || player.level().random.nextInt(3) != 0) {
            return;
        }

        AddonUtils.WingState wingState = AddonUtils.getWingState(pose);
        if (wingState == null) {
            return;
        }

        Vec3 targetWing = steering > 0.0F ? wingState.right().position() : wingState.left().position();
        spawnWingParticle(player, targetWing, 0.01D + Math.abs(steering) * 0.015D);
    }

    private void spawnWingParticle(LocalPlayer player, Vec3 position, double speed) {
        player.level().addParticle(
                ParticleTypes.CLOUD,
                position.x,
                position.y,
                position.z,
                (player.level().random.nextDouble() - 0.5D) * speed,
                player.level().random.nextDouble() * speed,
                (player.level().random.nextDouble() - 0.5D) * speed
        );
    }

    private void pulseBothHands(float strength) {
        VisorAPI.client().getInputManager().triggerHapticPulse(HandType.MAIN, strength);
        VisorAPI.client().getInputManager().triggerHapticPulse(HandType.OFFHAND, strength);
    }

    private void pulseSteeringHand(float steering) {
        HandType handType = steering > 0.0F ? HandType.OFFHAND : HandType.MAIN;
        VisorAPI.client().getInputManager().triggerHapticPulse(handType, 0.03F + Math.abs(steering) * 0.03F);
    }

    private void sendStartFlightPacket() {
        NetworkHelper.sendToServer(AddonNetworking.START_ELYTRA_FLIGHT_C2S, new FriendlyByteBuf(Unpooled.buffer()));
    }

    private void sendSteeringPacket(float steering) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeFloat(Mth.clamp(steering, -1.0F, 1.0F));
        NetworkHelper.sendToServer(AddonNetworking.STEER_ELYTRA_FLIGHT_C2S, buf);
    }

    @Override
    protected void onClear(@Nullable LocalPlayer player) {
        resetState();
    }

    private void resetState() {
        takeoffPoseTicks = 0;
        takeoffCooldown = 0;
        steerPacketCooldown = 0;
        lastSentSteering = 0.0F;
        sentNeutralSteering = false;
    }

    @Override
    public boolean isActive(@Nullable LocalPlayer player) {
        return player != null && VisorAPI.clientState().stateMode().isActive();
    }

    @Override
    public @NotNull TaskType getType() {
        return TaskType.VR_PLAYER_TICK;
    }

    @Override
    public @NotNull String getId() {
        return ID;
    }
}
