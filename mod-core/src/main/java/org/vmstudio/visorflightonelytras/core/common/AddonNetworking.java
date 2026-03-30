package org.vmstudio.visorflightonelytras.core.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.vmstudio.visorflightonelytras.core.network.NetworkHelper;

public final class AddonNetworking {
    public static final ResourceLocation START_ELYTRA_FLIGHT_C2S = AddonUtils.id("start_elytra_flight");
    public static final ResourceLocation STEER_ELYTRA_FLIGHT_C2S = AddonUtils.id("steer_elytra_flight");

    private static boolean initialized;

    private AddonNetworking() {
    }

    public static void initCommon() {
        if (initialized) {
            return;
        }
        initialized = true;

        NetworkHelper.registerServerReceiver(START_ELYTRA_FLIGHT_C2S, (buf, player) -> {
            if (!AddonUtils.canAttemptTakeoff(player)) {
                return;
            }

            player.startFallFlying();
        });

        NetworkHelper.registerServerReceiver(STEER_ELYTRA_FLIGHT_C2S, (buf, player) -> {
            float steering = Mth.clamp(buf.readFloat(), -1.0F, 1.0F);
            if (!canSteer(player)) {
                return;
            }

            player.setYRot(player.getYRot() + steering * AddonUtils.STEERING_YAW_DEGREES);
            player.setYHeadRot(player.getYRot());
            player.yBodyRot = player.getYRot();
            player.setDeltaMovement(AddonUtils.applySteering(player.getDeltaMovement(), player.getLookAngle(), steering));
            player.hurtMarked = true;
        });
    }

    private static boolean canSteer(Player player) {
        return player != null
                && player.isAlive()
                && player.isFallFlying()
                && AddonUtils.hasUsableElytra(player)
                && !player.isPassenger();
    }
}
