package sunstone.core.setup.suitetests;


import sunstone.api.AbstractSetupTask;

class RegularClassTask extends AbstractSetupTask {
    static boolean setupCalled = false;
    static boolean cleanupCalled = false;

    @Override
    public void setup() throws Exception {
        setupCalled = true;
    }

    @Override
    public void cleanup() throws Exception {
        cleanupCalled = true;
    }

    public static void reset() {
        setupCalled = false;
        cleanupCalled = false;
    }
}
