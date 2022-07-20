package sunstone.core;

import java.util.Map;

public interface TemplateDeploymentManager extends DeploymentRegistry {
    public void deployAndRegister(String templateContent, Map<String, String> parameters) throws Exception;
}
