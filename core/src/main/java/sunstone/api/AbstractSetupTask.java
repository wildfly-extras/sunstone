package sunstone.api;



public abstract class AbstractSetupTask {
    public abstract void setup() throws Exception;
    public abstract void teardown() throws Exception;
}
