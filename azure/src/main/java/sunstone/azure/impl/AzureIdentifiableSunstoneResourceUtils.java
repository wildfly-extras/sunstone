package sunstone.azure.impl;


import com.azure.resourcemanager.appservice.models.WebApp;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import sunstone.inject.Hostname;
import sunstone.core.exceptions.SunstoneException;
import sunstone.core.exceptions.UnsupportedSunstoneOperationException;


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
}
