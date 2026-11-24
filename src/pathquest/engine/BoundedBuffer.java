package pathquest.engine;

import java.util.ArrayDeque;
import java.util.Queue;

public class BoundedBuffer<T> {
    private final Queue<T> items = new ArrayDeque<>();
    private final int capacity;
    private boolean closed;
    private boolean trace;

    public BoundedBuffer(int capacity) {
        this.capacity = capacity;
    }

    public synchronized boolean put(T item) {
        while (items.size() == capacity && !closed) {
            if (trace) {
                System.out.println("[!] buffer full, producer waiting");
            }
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        if (closed) {
            return false;
        }
        items.add(item);
        if (trace) {
            System.out.println("[+] produced " + item + " (" + items.size() + "/" + capacity + ")");
        }
        notifyAll();
        return true;
    }

    public synchronized T take() {
        while (items.isEmpty() && !closed) {
            if (trace) {
                System.out.println("[!] buffer empty, consumer waiting");
            }
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        if (items.isEmpty()) {
            return null;
        }
        T item = items.remove();
        if (trace) {
            System.out.println("[-] consumed " + item + " (" + items.size() + "/" + capacity + ")");
        }
        notifyAll();
        return item;
    }

    public synchronized void close() {
        closed = true;
        notifyAll();
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    public synchronized void setTrace(boolean trace) {
        this.trace = trace;
    }
}
