package org.vmstudio.visorflightonelytras.core.client;

import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visor.api.common.addon.VisorAddon;
import org.vmstudio.visorflightonelytras.core.client.overlays.VROverlayFlight;
import org.vmstudio.visorflightonelytras.core.common.VisorFlight;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FlightAddonClient implements VisorAddon {
    @Override
    public void onAddonLoad() {
        VisorAPI.addonManager().getRegistries()
                .overlays()
                .registerComponents(
                        List.of(
                                new VROverlayFlight(
                                        this,
                                        VROverlayFlight.ID
                                )
                        )
                );
    }

    @Override
    public @Nullable String getAddonPackagePath() {
        return "org.vmstudio.visorflightonelytras.core.client";
    }

    @Override
    public @NotNull String getAddonId() {
        return VisorFlight.MOD_ID;
    }

    @Override
    public @NotNull Component getAddonName() {
        return Component.literal(VisorFlight.MOD_NAME);
    }

    @Override
    public String getModId() {
        return VisorFlight.MOD_ID;
    }
}
