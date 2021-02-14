package linked_list_set;

import java.util.concurrent.atomic.AtomicReference;

public class SetImpl implements Set {
    private interface NodeOrFlag {
    }

    private static class Node implements NodeOrFlag {
        final int key;
        final AtomicReference<NodeOrFlag> ref;

        Node(int key, Node next) {
            this.key = key;
            this.ref = new AtomicReference<NodeOrFlag>(next);
        }
    }

    private static class Flag implements NodeOrFlag {
        final Node node;

        Flag(Node node) {
            this.node = node;
        }
    }

    private static class Window {
        Node left, right;
    }

    private final Node head = new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE, null));

    private Window findWindow(int key) {
        retry:
        while (true) {
            Window w = new Window();

            w.left = head;
            w.right = (Node) w.left.ref.get();

            while (w.right.key < key) {
                NodeOrFlag x = w.right.ref.get();
                if (x instanceof Flag) {
                    if (!w.left.ref.compareAndSet(w.right, ((Flag) x).node)) {
                        continue retry;
                    }
                    w.right = ((Flag) x).node;
                } else {
                    w.left = w.right;
                    w.right = (Node) x;
                }
            }

            NodeOrFlag x = w.right.ref.get();

            if (!(x instanceof Flag)) {
                return w;
            }

            w.left.ref.compareAndSet(w.right, ((Flag) x).node);
            w.right = ((Flag) x).node;
        }
    }

    @Override
    public boolean add(int key) {
        while (true) {
            Window w = findWindow(key);

            if (w.right.key == key) {
                return false;
            }

            if (w.left.ref.compareAndSet(w.right, new Node(key, w.right))) {
                return true;
            }
        }
    }

    @Override
    public boolean remove(int key) {
        while (true) {
            Window w = findWindow(key + 1);
            NodeOrFlag x = w.left.ref.get();

            if (w.left.key != key || x instanceof Flag) {
                return false;
            }

            if (w.left.ref.compareAndSet(w.right, new Flag(w.right))) {
                return true;
            }
        }
    }

    @Override
    public boolean contains(int key) {
        Window w = findWindow(key);
        return w.right.key == key;
    }
}