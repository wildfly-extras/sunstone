package org.wildfly.extras.sunstone.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;

/**
 * This class is just a wrapper around list of {@link Node nodes} and implementing AutoCloseable interface.
 */
public class CreatedNodes implements AutoCloseable, List<Node> {

    private final List<Node> nodes;

    public CreatedNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    /**
     * Takes care about closing of all nodes in the list
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        final List<Exception> closeExceptions = new ArrayList<>();
        final List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Node node : nodes) {
            futures.add(CompletableFuture.runAsync(node::close));
        }
        futures.forEach(f -> {
            try {
                f.join();
            } catch (Exception e) {
                closeExceptions.add(e);
            }
        });

        if (!closeExceptions.isEmpty()) {
            final Exception firstException = closeExceptions.get(0);
            closeExceptions.subList(1, closeExceptions.size()).forEach(e -> firstException.addSuppressed(e));
            throw firstException;
        }
    }

    @Override
    public int size() {
        return nodes.size();
    }

    @Override
    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return nodes.contains(o);
    }

    @Override
    public Iterator<Node> iterator() {
        return nodes.iterator();
    }

    @Override
    public Object[] toArray() {
        return nodes.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return nodes.toArray(a);
    }

    @Override
    public boolean add(Node node) {
        return nodes.add(node);
    }

    @Override
    public boolean remove(Object o) {
        return nodes.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return nodes.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends Node> c) {
        return nodes.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends Node> c) {
        return nodes.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return nodes.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return nodes.retainAll(c);
    }

    @Override
    public void clear() {
        nodes.clear();
    }

    @Override
    public Node get(int index) {
        return nodes.get(index);
    }

    @Override
    public Node set(int index, Node element) {
        return nodes.set(index, element);
    }

    @Override
    public void add(int index, Node element) {
        nodes.add(index, element);
    }

    @Override
    public Node remove(int index) {
        return nodes.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return nodes.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return nodes.lastIndexOf(o);
    }

    @Override
    public ListIterator<Node> listIterator() {
        return nodes.listIterator();
    }

    @Override
    public ListIterator<Node> listIterator(int index) {
        return nodes.listIterator(index);
    }

    @Override
    public List<Node> subList(int fromIndex, int toIndex) {
        return nodes.subList(fromIndex, toIndex);
    }
}
