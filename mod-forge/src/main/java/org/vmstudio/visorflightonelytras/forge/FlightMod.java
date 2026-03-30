package org.vmstudio.visorflightonelytras.forge;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visorflightonelytras.core.client.FlightAddonClient;
import org.vmstudio.visorflightonelytras.core.common.AddonNetworking;
import org.vmstudio.visorflightonelytras.core.common.VisorFlight;
import org.vmstudio.visorflightonelytras.core.network.NetworkHelper;
import org.vmstudio.visorflightonelytras.core.server.FlightAddonServer;
import org.vmstudio.visorflightonelytras.forge.network.ForgeNetworkChannel;

@Mod(VisorFlight.MOD_ID)
public class FlightMod {
    public FlightMod() {
        NetworkHelper.setChannel(new ForgeNetworkChannel(new ResourceLocation(VisorFlight.MOD_ID, "network")));
        AddonNetworking.initCommon();

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
