package sunstone.core;

import java.util.Map;

/**
 * Manager of cloud resources. Handles deploy operation, registering resources and deleting them. This is meant to be used
 * for deploying parametrized templates. Parameters are supposed to be defined in an annotation as a String.
 *
 * Used by {@link SunstoneCloudDeploy}.
 */
interface TemplateCloudDeploymentManager extends CloudDeploymentRegistry {
    void deployAndRegister(String templateContent, Map<String, String> parameters) throws Exception;
}
