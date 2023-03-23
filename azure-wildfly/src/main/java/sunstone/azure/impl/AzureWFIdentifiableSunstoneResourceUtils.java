package sunstone.azure.impl;


import sunstone.annotation.WildFly;
import sunstone.azure.annotation.AzureVirtualMachine;
import com.azure.resourcemanager.appservice.models.WebApp;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import sunstone.annotation.OperatingMode;
import sunstone.annotation.inject.Hostname;
import sunstone.core.CreaperUtils;
import sunstone.core.exceptions.SunstoneException;
import sunstone.core.exceptions.UnsupportedSunstoneOperationException;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static sunstone.azure.impl.AzureWFIdentifiableSunstoneResource.VM_INSTANCE;

public class AzureWFIdentifiableSunstoneResourceUtils {

    static Hostname resolveHostname(AzureWFIdentifiableSunstoneResource.Identification identification, AzureSunstoneStore store) throws SunstoneException {
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

    static OnlineManagementClient resolveOnlineManagementClient(AzureWFIdentifiableSunstoneResource.Identification identification, WildFly wildfly, AzureSunstoneStore store) throws SunstoneException {
        try {
            if (identification.type == VM_INSTANCE) {
                AzureVirtualMachine annotation = (AzureVirtualMachine) identification.identification;

                if (wildfly.mode() == OperatingMode.STANDALONE) {
                    return CreaperUtils.createStandaloneManagementClient(resolveHostname(identification, store).get(), wildfly.standalone());
                } else {
                    throw new UnsupportedSunstoneOperationException("Only standalone mode is supported for injecting OnlineManagementClient.");
                }
            } else {
                throw new UnsupportedSunstoneOperationException("Only Azure VM instance is supported for injecting OnlineManagementClient.");
            }
        } catch (IOException e) {
            throw new SunstoneException(e);
        }
//        return null;
    }

    static WildFly getWFAnnotation(Annotation[] allAnns) {
        List<Annotation> wfAnns = Arrays.stream(allAnns).filter(a -> WildFly.class.isAssignableFrom(a.annotationType())).collect(Collectors.toList());
        WildFly wfAnn;
        if (!wfAnns.isEmpty()) {
            if (wfAnns.size() > 1) {
                // todo
            }
            wfAnn = (WildFly) wfAnns.get(0);
        } else  {
            wfAnn = new WildFly.WildFlyDefault();
        }
        return wfAnn;
    }
}
