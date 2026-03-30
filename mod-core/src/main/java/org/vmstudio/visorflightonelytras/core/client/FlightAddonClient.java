package org.vmstudio.visorflightonelytras.core.client;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vmstudio.visor.api.common.addon.VisorAddon;
import org.vmstudio.visorflightonelytras.core.common.AddonNetworking;
import org.vmstudio.visorflightonelytras.core.common.VisorFlight;

public class FlightAddonClient implements VisorAddon {
    @Override
    public void onAddonLoad() {
        AddonNetworking.initCommon();
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
