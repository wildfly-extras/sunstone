package sunstone.core;


import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

import java.io.Closeable;
import java.util.Deque;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * An abstraction over JUnit5 Extension store providing methods to set commonly used resources.
 */
public class SunstoneStore {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create("sunstone", "core", "SunstoneStore");
    private static final String CLOSABLES = "closables";

    private static final String SUITE_LEVEL_DEPLOYMENTS = "sunstoneSuiteLevelDeployments";

    private final ExtensionContext context;

    protected SunstoneStore(ExtensionContext ctx) {
        this.context = ctx;
    }

    static SunstoneStore get(ExtensionContext ctx) {
        return new SunstoneStore(ctx);
    }

    protected Store getStore() {
        return getContext().getStore(NAMESPACE);
    }


    /**
     * Add {@link ExtensionContext.Store.CloseableResource} to the root global store.
     */
    public void addSuiteLevelClosable(AutoCloseable closable) {
        Store store = context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        store.put(UUID.randomUUID().toString(), (ExtensionContext.Store.CloseableResource) closable::close);
    }
    /**
     * Add {@link ExtensionContext.Store.CloseableResource} to the root global store.
     */
    public void addSuiteLevelClosable(Closeable closable) {
        Store store = getContext().getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        store.put(UUID.randomUUID().toString(), (ExtensionContext.Store.CloseableResource) closable::close);
    }

    /**
     * Add sum to the root global store.
     */
    public void addSuiteLevelDeployment(String checkSum) {
        Store store = getContext().getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        Set<String> checkSums = (Set<String>) store.getOrComputeIfAbsent(SUITE_LEVEL_DEPLOYMENTS, s -> new ConcurrentSkipListSet<String>());
        checkSums.add(checkSum);
    }

    /**
     * Check if sum is present in root global store.
     */
    public boolean suiteLevelDeploymentExists(String checkSum) {
        Store store = getContext().getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        Set<String> checkSums = (Set<String>) store.getOrComputeIfAbsent(SUITE_LEVEL_DEPLOYMENTS, s -> new ConcurrentSkipListSet<String>());
        return checkSums.contains(checkSum);
    }

    Deque<Closeable> getClosablesOrCreate() {
        return getStore().getOrComputeIfAbsent(CLOSABLES, k -> new ConcurrentLinkedDeque<Closeable>(), Deque.class);
    }

    public void addClosable(Closeable closable) {
        getClosablesOrCreate().push(closable);
    }

    protected ExtensionContext getContext() {
        return context;
    }
}
