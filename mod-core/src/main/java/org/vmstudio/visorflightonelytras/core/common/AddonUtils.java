package org.vmstudio.visorflightonelytras.core.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseClient;
import org.vmstudio.visor.api.common.player.VRPose;

public final class AddonUtils {
    public static final int REQUIRED_T_POSE_TICKS = 8;
    public static final int TAKEOFF_COOLDOWN_TICKS = 20;
    public static final int STEER_PACKET_INTERVAL_TICKS = 2;

    public static final double MIN_DESCENT_FOR_TAKEOFF = -0.08D;
    public static final double MIN_T_POSE_SIDE_OFFSET = 0.38D;
    public static final double MAX_T_POSE_FORWARD_OFFSET = 0.38D;
    public static final double MIN_T_POSE_VERTICAL_OFFSET = -0.70D;
    public static final double MAX_T_POSE_VERTICAL_OFFSET = 0.18D;
    public static final double MAX_T_POSE_HAND_HEIGHT_DELTA = 0.22D;
    public static final double MIN_T_POSE_SPAN = 1.00D;

    public static final double MIN_STEERING_SPAN = 0.85D;
    public static final double MAX_STEERING_FORWARD_OFFSET = 0.50D;
    public static final double MAX_STEERING_HAND_HEIGHT_DELTA = 0.65D;
    public static final double WING_HEIGHT_RANGE = 0.42D;
    public static final float MIN_STEERING_TO_SEND = 0.08F;
    public static final float STEERING_CHANGE_TO_RESEND = 0.05F;
    public static final float STEERING_YAW_DEGREES = 12.5F;
    public static final double STEERING_SIDE_ACCELERATION = 0.165D;
    public static final double STEERING_FORWARD_ACCELERATION = 0.028D;
    public static final double MAX_PLAYER_SPEED = 2.8D;

    private AddonUtils() {
    }

    public static boolean hasUsableElytra(Player player) {
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        return chestStack.getItem() instanceof ElytraItem && ElytraItem.isFlyEnabled(chestStack);
    }

    public static boolean canAttemptTakeoff(Player player) {
        return hasUsableElytra(player)
                && !player.onGround()
                && !player.isFallFlying()
                && !player.isPassenger()
                && !player.isInWater()
                && !player.getAbilities().flying
                && player.getDeltaMovement().y < MIN_DESCENT_FOR_TAKEOFF;
    }

    public static @Nullable WingState getWingState(PlayerPoseClient pose) {
        Vec3 head = pose.getHmd().getPositionVec3();
        Vec3 forward = horizontalDirection(pose.getHmd(), pose.getMcPlayer().getLookAngle());
        if (forward.lengthSqr() < 1.0E-5D) {
            return null;
        }

        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        HandSample main = sampleHand(pose.getGripMainHand(), head, forward, right);
        HandSample off = sampleHand(pose.getGripOffhand(), head, forward, right);

        HandSample left = main.sideOffset <= off.sideOffset ? main : off;
        HandSample rightHand = left == main ? off : main;
        return new WingState(left, rightHand, head, forward, right);
    }

    public static boolean isTPose(PlayerPoseClient pose) {
        WingState wingState = getWingState(pose);
        if (wingState == null) {
            return false;
        }

        return wingState.left().sideOffset() <= -MIN_T_POSE_SIDE_OFFSET
                && wingState.right().sideOffset() >= MIN_T_POSE_SIDE_OFFSET
                && Math.abs(wingState.left().forwardOffset()) <= MAX_T_POSE_FORWARD_OFFSET
                && Math.abs(wingState.right().forwardOffset()) <= MAX_T_POSE_FORWARD_OFFSET
                && wingState.left().verticalOffset() >= MIN_T_POSE_VERTICAL_OFFSET
                && wingState.left().verticalOffset() <= MAX_T_POSE_VERTICAL_OFFSET
                && wingState.right().verticalOffset() >= MIN_T_POSE_VERTICAL_OFFSET
                && wingState.right().verticalOffset() <= MAX_T_POSE_VERTICAL_OFFSET
                && Math.abs(wingState.left().verticalOffset() - wingState.right().verticalOffset()) <= MAX_T_POSE_HAND_HEIGHT_DELTA
                && wingState.span() >= MIN_T_POSE_SPAN;
    }

    public static float computeSteering(PlayerPoseClient pose) {
        WingState wingState = getWingState(pose);
        if (wingState == null) {
            return 0.0F;
        }

        if (wingState.span() < MIN_STEERING_SPAN) {
            return 0.0F;
        }

        if (Math.abs(wingState.left().forwardOffset()) > MAX_STEERING_FORWARD_OFFSET
                || Math.abs(wingState.right().forwardOffset()) > MAX_STEERING_FORWARD_OFFSET) {
            return 0.0F;
        }

        double heightDelta = Mth.clamp(
                wingState.left().verticalOffset() - wingState.right().verticalOffset(),
                -MAX_STEERING_HAND_HEIGHT_DELTA,
                MAX_STEERING_HAND_HEIGHT_DELTA
        );

        return Mth.clamp((float) (heightDelta / WING_HEIGHT_RANGE), -1.0F, 1.0F);
    }

    public static Vec3 applySteering(Vec3 currentVelocity, Vec3 lookDirection, float steering) {
        if (Math.abs(steering) < 1.0E-4F) {
            return currentVelocity;
        }

        Vec3 horizontalLook = new Vec3(lookDirection.x, 0.0D, lookDirection.z);
        if (horizontalLook.lengthSqr() < 1.0E-5D) {
            return currentVelocity;
        }

        horizontalLook = horizontalLook.normalize();
        Vec3 right = new Vec3(-horizontalLook.z, 0.0D, horizontalLook.x);
        Vec3 steered = currentVelocity
                .add(right.scale(steering * STEERING_SIDE_ACCELERATION))
                .add(horizontalLook.scale(STEERING_FORWARD_ACCELERATION));

        double speed = steered.length();
        if (speed > MAX_PLAYER_SPEED) {
            steered = steered.scale(MAX_PLAYER_SPEED / speed);
        }
        return steered;
    }

    private static Vec3 horizontalDirection(VRPose referencePose, Vec3 fallback) {
        Vec3 fromPose = referencePose.getDirectionVec3();
        Vec3 horizontal = new Vec3(fromPose.x, 0.0D, fromPose.z);
        if (horizontal.lengthSqr() >= 1.0E-5D) {
            return horizontal.normalize();
        }

        Vec3 fallbackHorizontal = new Vec3(fallback.x, 0.0D, fallback.z);
        if (fallbackHorizontal.lengthSqr() >= 1.0E-5D) {
            return fallbackHorizontal.normalize();
        }
        return Vec3.ZERO;
    }

    private static HandSample sampleHand(VRPose pose, Vec3 head, Vec3 forward, Vec3 right) {
        Vec3 position = pose.getPositionVec3();
        Vec3 delta = position.subtract(head);
        double sideOffset = delta.dot(right);
        double forwardOffset = delta.dot(forward);
        double verticalOffset = delta.y;
        return new HandSample(position, sideOffset, forwardOffset, verticalOffset);
    }

    public record HandSample(Vec3 position, double sideOffset, double forwardOffset, double verticalOffset) {
    }

    public record WingState(HandSample left, HandSample right, Vec3 head, Vec3 forward, Vec3 rightAxis) {
        public double span() {
            return right.position().distanceTo(left.position());
        }
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(VisorFlight.MOD_ID, path);
    }
}



