package azure.core;


import azure.core.identification.AzureVirtualMachine;
import com.azure.resourcemanager.appservice.models.WebApp;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.api.EapMode;
import sunstone.api.inject.Hostname;
import sunstone.core.CreaperUtils;
import sunstone.core.exceptions.SunstoneException;
import sunstone.core.exceptions.UnsupportedSunstoneOperationException;

import java.io.IOException;

import static azure.core.AzureIdentifiableSunstoneResource.VM_INSTANCE;

public class AzureIdentifiableSunstoneResourceUtils {

    static Hostname resolveHostname(AzureIdentifiableSunstoneResource.Identification identification, AzureSunstoneStore store) throws SunstoneException {
        switch (identification.type) {
            case VM_INSTANCE:
                VirtualMachine vm = identification.get(store, VirtualMachine.class);
                return vm.getPrimaryPublicIPAddress()::ipAddress;
            case WEB_APP:
                WebApp app = identification.get(store, WebApp.class);
                return app::defaultHostname;
            default:
                throw new UnsupportedSunstoneOperationException("Unsupported type for getting hostname: " + identification.type);
        }
    }

    static OnlineManagementClient resolveOnlineManagementClient(AzureIdentifiableSunstoneResource.Identification identification, AzureSunstoneStore store) throws SunstoneException {
        try {
            if (identification.type == VM_INSTANCE) {
                AzureVirtualMachine annotation = (AzureVirtualMachine) identification.identification;
                if (annotation.mode() == EapMode.STANDALONE) {
                    return CreaperUtils.createStandaloneManagementClient(resolveHostname(identification, store).get(), annotation.standalone());
                } else {
                    throw new UnsupportedSunstoneOperationException("Only standalone mode is supported for injecting OnlineManagementClient.");
                }
            } else {
                throw new UnsupportedSunstoneOperationException("Only Azure VM instance is supported for injecting OnlineManagementClient.");
            }
        } catch (IOException e) {
            throw new SunstoneException(e);
        }
    }
}
