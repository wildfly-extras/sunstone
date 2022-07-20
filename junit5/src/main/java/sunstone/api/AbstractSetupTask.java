package sunstone.api;


public abstract class AbstractSetupTask {
    private AbstractSetupTask(){

    }
    abstract void beforeAll();
    abstract void afterAll();
}
