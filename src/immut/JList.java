package immut;

import java.util.*;
import java.util.concurrent.atomic.*;

@SuppressWarnings({"NonAtomicOperationOnVolatileField", "UnnecessaryLocalVariable"})
public class JList <T> extends AbstractList<T> implements List<T> {

    private volatile Finger<T> f = Finger.Empty.empty();

    @Override
    public int size() {
        return f.size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        var local_f = f;
        var s = f.size();
        for (int i = 0; i < s; i += 1) {
            var li = Utils.indexGet(local_f, i);
            if (Objects.equals(li, o)) {
                return true;
            }
        }
        return false;
    }

    static class IteratorJList <T> implements Iterator<T> {
        private final Finger<T> f2;
        private int i;
        public IteratorJList(JList<T> l) {
            f2 = l.f;
            i = 0;
        }

        @Override
        public boolean hasNext() {
            return i < f2.size();
        }

        @Override
        public T next() {
            if (i >= f2.size()) {
                throw new NoSuchElementException();
            }
            // System.out.printf("i = %d, f2:size = %d. \n", i, f2.size());
            var n = Utils.indexGet(f2, i);
            i += 1;
            return n;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new IteratorJList<>(this);
    }

    @Override
    public Object[] toArray() {
        var l = Utils.toList(f);
        return l.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        var l = Utils.toList(f);
        return l.toArray(a);
    }

    @Override
    public boolean add(T t) {
        f = Utils.pushRight(f, t);
        return true;
    }

    @Override
    public T get(int index) {
        var local_f = f;
        if (index < 0 || index >= local_f.size()) {
            throw new NoSuchElementException();
        }
        return Utils.indexGet(local_f, index);
    }

    @Override
    public boolean remove(Object o) {
        var local_f = f;
        var s = f.size();
        for (int i = 0; i < s; i += 1) {
            var li = Utils.indexGet(local_f, i);
            if (Objects.equals(li, o)) {
                var pair = Utils.split(local_f, i);
                var l = pair.left();
                var r = Utils.popLeft(pair.right(), _ -> {});
                var ans = Utils.merge(l, r);
                f = ans;
                return true;
            }
        }
        return false;
    }

    @Override
    public T set(int index, T element) {
        var local_f = f;
        if (index < 0 || index > local_f.size()) {
            throw new IllegalArgumentException();
        }
        var ans = new AtomicReference<T>();
        var sp = Utils.split(local_f, index);
        var l = sp.left();
        var r = sp.right();
        if (index != local_f.size()) {
            r = Utils.popLeft(r, ans::set);
        }
        r = Utils.pushLeft(r, element);
        var rst = Utils.merge(l, r);
        f = rst;
        return ans.get();
    }

    @Override
    public void add(int index, T element) {
        var local_f = f;
        if (index < 0 || index > local_f.size()) {
            throw new IllegalArgumentException();
        }
        var sp = Utils.split(local_f, index);
        var l = sp.left();
        var r = sp.right();
        r = Utils.pushLeft(r, element);
        var rst = Utils.merge(l, r);
        f = rst;
    }

    @Override
    public T remove(int index) {
        var local_f = f;
        if (index < 0 || index >= local_f.size()) {
            throw new IllegalArgumentException();
        }
        var ans = new AtomicReference<T>();
        var sp = Utils.split(local_f, index);
        var l = sp.left();
        var r = sp.right();
        r = Utils.popLeft(r, ans::set);
        var rst = Utils.merge(l, r);
        f = rst;
        return ans.get();
    }

    @Override
    public void clear() {
        f = Finger.Empty.empty();
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        var current_f = f;
        if (c instanceof JList<? extends T> c2) {
            if (index < 0 || index >= current_f.size()) {
                throw new IllegalArgumentException("index: Out of bounds");
            }
            @SuppressWarnings("unchecked") var c3 = (JList<T> ) c2;
            if (c3.size() == 0) {
                return false;
            }
            var f2 = c3.f;
            var sp = Utils.split(current_f, index);
            var l = sp.left();
            l = Utils.merge(l, f2);
            l = Utils.merge(l, sp.right());
            f = l;
            return true;
        } else {
            return super.addAll(index, c);
        }
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        // super.removeRange(fromIndex, toIndex);
        var local_f = f;
        if (toIndex < 0 || toIndex >= local_f.size()) {
            throw new IllegalArgumentException();
        }
        if (fromIndex < 0 || fromIndex >= local_f.size()) {
            throw new IllegalArgumentException();
        }
        var sp = Utils.split(local_f, toIndex);
        var sp2 = Utils.split(sp.left(), fromIndex);
        var ans = Utils.merge(sp2.left(), sp.right());
        f = ans;
    }

}
