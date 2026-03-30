package org.vmstudio.visorflightonelytras.fabric;

import net.fabricmc.api.ModInitializer;
import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visorflightonelytras.core.client.FlightAddonClient;
import org.vmstudio.visorflightonelytras.core.common.AddonNetworking;
import org.vmstudio.visorflightonelytras.core.network.NetworkHelper;
import org.vmstudio.visorflightonelytras.core.server.FlightAddonServer;
import org.vmstudio.visorflightonelytras.fabric.network.FabricNetworkChannel;

public class FlightMod implements ModInitializer {
    @Override
    public void onInitialize() {
        NetworkHelper.setChannel(new FabricNetworkChannel());
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
