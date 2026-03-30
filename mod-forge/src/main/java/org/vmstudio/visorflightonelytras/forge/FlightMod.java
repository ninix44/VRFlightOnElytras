package org.vmstudio.visorflightonelytras.forge;

import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visorflightonelytras.core.client.FlightAddonClient;
import org.vmstudio.visorflightonelytras.core.common.VisorFlight;
import org.vmstudio.visorflightonelytras.core.server.FlightAddonServer;
import net.minecraftforge.fml.common.Mod;

@Mod(VisorFlight.MOD_ID)
public class FlightMod {
    public FlightMod() {
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
