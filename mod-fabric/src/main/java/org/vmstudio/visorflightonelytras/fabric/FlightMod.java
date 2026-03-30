package org.vmstudio.visorflightonelytras.fabric;

import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visorflightonelytras.core.client.FlightAddonClient;
import org.vmstudio.visorflightonelytras.core.server.FlightAddonServer;
import net.fabricmc.api.ModInitializer;

public class FlightMod implements ModInitializer {
    @Override
    public void onInitialize() {
        if (ModLoader.get().isDedicatedServer()) {
            VisorAPI.registerAddon(
                    new FlightAddonServer()
            );
        } else {
            VisorAPI.registerAddon(
                    new FlightAddonClient()
            );
        }
    }
}
