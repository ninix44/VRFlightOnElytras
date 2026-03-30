package org.vmstudio.visorflightonelytras.core.server;

import org.vmstudio.visor.api.common.addon.VisorAddon;
import org.vmstudio.visorflightonelytras.core.common.VisorFlight;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FlightAddonServer implements VisorAddon {
    @Override
    public void onAddonLoad() {

    }

    @Override
    public @Nullable String getAddonPackagePath() {
        return "org.vmstudio.visorflightonelytras.core.server";
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
