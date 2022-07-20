package sunstone.core;


public interface DeploymentRegistry extends AutoCloseable {

    /**
     * Register deployment for future undeploy operation.
     */
    public void register(String id);

    /**
     * Undeploy registered deployment.
     */
    public void undeploy(String id);


    /**
     * Undeploy all registered deployments.
     */
    public void undeplyAll();

    /**
     * Close whatever client is used.
     */
    public void close();
}
