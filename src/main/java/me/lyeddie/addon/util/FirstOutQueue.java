package me.lyeddie.addon.util;

import com.google.common.collect.ForwardingQueue;
import com.google.common.collect.Iterables;
import org.jetbrains.annotations.NotNull;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Collection;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class FirstOutQueue<E> extends ForwardingQueue<E> implements Serializable {

    final int maxSize;
    private final ArrayDeque<E> delegate;

    public FirstOutQueue(int maxSize) {
        checkArgument(maxSize >= 0, "maxSize (%s) must >= 0", maxSize);
        this.delegate = new ArrayDeque<>(maxSize);
        this.maxSize = maxSize;
    }

    @Override
    protected @NotNull ArrayDeque<E> delegate() {
        return delegate;
    }

    @Override
    public boolean offer(E e) {
        return add(e);
    }

    @Override
    public boolean add(E e) {
        checkNotNull(e);
        if (maxSize == 0) {
            return true;
        }
        if (size() == maxSize) {
            delegate.remove();
        }
        delegate.add(e);
        return true;
    }

    public E addFirst(E e) {
        checkNotNull(e);
        if (maxSize == 0) {
            return null;
        }
        E removed = null;
        if (size() == maxSize) {
            removed = delegate.remove();
        }
        delegate.addFirst(e);
        return removed;
    }

    public E getFirst() {
        return delegate.getFirst();
    }

    public E getLast() {
        return delegate.getLast();
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        int size = collection.size();
        if (size >= maxSize) {
            clear();
            return Iterables.addAll(this, Iterables.skip(collection, size - maxSize));
        }
        return standardAddAll(collection);
    }

    @Override
    public Object[] toArray() {
        return super.toArray();
    }
}
