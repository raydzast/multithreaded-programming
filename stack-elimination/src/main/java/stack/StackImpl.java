package stack;

import kotlinx.atomicfu.AtomicIntArray;
import kotlinx.atomicfu.AtomicRef;

public class StackImpl implements Stack {
    private static final int ELIMINATION_ARRAY_SIZE = 512;
    private static final int PUSH_WAIT = 256;
    private static final int PUSH_TRIES = 8;
    private static final int POP_TRIES = 8;

    private static class Node {
        final AtomicRef<Node> next;
        final int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }

    // head pointer
    private final AtomicRef<Node> head = new AtomicRef<>(null);
    private final AtomicIntArray values = new AtomicIntArray(ELIMINATION_ARRAY_SIZE);

    public StackImpl() {
        for (int i = 0; i < ELIMINATION_ARRAY_SIZE; i++) {
            values.get(i).setValue(Integer.MIN_VALUE);
        }
    }

    private static int random(int border) {
        assert border > 0;
        return (int) (Math.random() * border);
    }

    @Override
    public void push(int x) {
        int index = random(ELIMINATION_ARRAY_SIZE);
        boolean found = false;

        for (int i = 0; i < PUSH_TRIES && i + index < ELIMINATION_ARRAY_SIZE; i++) {
            if (values.get(index + i).compareAndSet(Integer.MIN_VALUE, x)) {
                index = index + i;
                found = true;
                break;
            }
        }

        if (found) {
            for (int i = 0; i < PUSH_WAIT; i++) {
                if (values.get(index).compareAndSet(Integer.MAX_VALUE, Integer.MIN_VALUE)) {
                    return;
                }
            }
            if (values.get(index).getAndSet(Integer.MIN_VALUE) == Integer.MAX_VALUE) {
                return;
            }
        }

        fallbackPush(x);
    }

    private void fallbackPush(int x) {
        while (true) {
            Node curHead = head.getValue();
            Node newHead = new Node(x, curHead);
            if (head.compareAndSet(curHead, newHead)) {
                return;
            }
        }
    }

    @Override
    public int pop() {
        int index = random(ELIMINATION_ARRAY_SIZE);
        for (int i = 0; i < POP_TRIES && index + i < ELIMINATION_ARRAY_SIZE; i++) {
            int value = values.get(index + i).getValue();
            if (value == Integer.MIN_VALUE || value == Integer.MAX_VALUE) continue;
            if (values.get(index + i).compareAndSet(value, Integer.MAX_VALUE)) {
                return value;
            }
        }

        return fallbackPop();
    }

    private int fallbackPop() {
        while (true) {
            Node curHead = head.getValue();
            if (curHead == null) return Integer.MIN_VALUE;
            if (head.compareAndSet(curHead, curHead.next.getValue())) {
                return curHead.x;
            }
        }
    }
}