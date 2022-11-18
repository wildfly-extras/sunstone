package sunstone.core;

/**
 * Registry that handles registering and un-deploying resources from a cloud - e.g. registering stack where
 * CloudFormation template is deployed to and deleting the whole stack.
 *
 * Used by {@link SunstoneCloudDeploy}.
 */
interface CloudDeploymentRegistry extends AutoCloseable {

    /**
     * Register deployment for future undeploy operation.
     */
     void register(String id);

    /**
     * Undeploy registered deployment.
     */
    void undeploy(String id);


    /**
     * Undeploy all registered deployments.
     */
    void undeployAll();

    /**
     * Close whatever client is being used.
     */
    void close();
}
