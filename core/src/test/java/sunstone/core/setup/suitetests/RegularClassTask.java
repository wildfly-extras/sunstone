package sunstone.core.setup.suitetests;


import sunstone.api.AbstractSetupTask;

class RegularClassTask extends AbstractSetupTask {
    static boolean setupCalled = false;
    static boolean teardownCalled = false;

    @Override
    public void setup() throws Exception {
        setupCalled = true;
    }

    @Override
    public void teardown() throws Exception {
        teardownCalled = true;
    }

    public static void reset() {
        setupCalled = false;
        teardownCalled = false;
    }
}
