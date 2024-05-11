package immut;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

public sealed interface Finger<T> {

    int size();

    final class Empty<T> implements Finger<T> {

        public final static Empty<?> EMPTY = new Empty<>();

        public static <Any> Empty<Any> empty() {
            return (Empty<Any>) EMPTY;
        }

        @Override
        public int hashCode() {
            return "Empty".hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Empty;
        }

        @Override
        public int size() {
            return 0;
        }

    }

    final class Digit<T> {
        public int size;
        public Object[] values;

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Finger.Digit<?> o) {
                if (size != o.size) {
                    return false;
                }
                return Objects.deepEquals(values, o.values);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(values);
        }
    }

    final class Single<T> implements Finger<T> {

        public T inner;

        @Override
        public int size() {
            if (inner instanceof Finger.Digit<?> digitInner) {
                return digitInner.size;
            } else {
                return 1;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(inner);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Finger.Single<?> other) {
                return Objects.equals(inner, other.inner);
            }
            return false;
        }
    }

    final class Deep<T> implements Finger<T> {
        public Digit<T> left;
        public Finger<Digit<T>> deep;
        public Digit<T> right;
        public int size;

        public void initSize() {
            size = left.size + deep.size() + right.size;
        }

        @Override
        public int size() {
            return size;
        }
    }

}

class Utils {

    public static <T> int calcObjSize(T obj) {
        if (obj instanceof Finger.Digit<?> d) {
            return d.size;
        } else {
            return 1;
        }
    }

    public static void updateDigitSize(Finger.Digit<?> digit) {
        int calc = 0;
        for (Object value : digit.values) {
            calc += calcObjSize(value);
        }
        digit.size = calc;
    }

    public static <T, O extends T> Finger<T> pushLeft(Finger<T> finger, O obj) {
        switch (finger) {
            case Finger.Deep<T> v -> {
                int len = v.left.values.length;
                if (len == 4) {
                    var newLeft = new Finger.Digit<T>();
                    newLeft.values = new Object[2];
                    System.arraycopy(v.left.values, 0, newLeft.values, 1, 1);
                    newLeft.values[0] = obj;
                    updateDigitSize(newLeft);

                    var nxtPushLeft = new Finger.Digit<T>();
                    nxtPushLeft.values = new Object[3];
                    System.arraycopy(v.left.values, 1, nxtPushLeft.values, 0, 3);
                    updateDigitSize(nxtPushLeft);

                    var newDeep = pushLeft(v.deep, nxtPushLeft);
                    var newOut = new Finger.Deep<T>();
                    newOut.left = newLeft;
                    newOut.deep = newDeep;
                    newOut.right = v.right;
                    newOut.initSize();
                    return newOut;
                } else {
                    var newLeft = new Finger.Digit<T>();
                    newLeft.values = new Object[len + 1];
                    System.arraycopy(v.left.values, 0, newLeft.values, 1, len);
                    newLeft.values[0] = obj;
                    updateDigitSize(newLeft);

                    var newOut = new Finger.Deep<T>();
                    newOut.left = newLeft;
                    newOut.deep = v.deep;
                    newOut.right = v.right;
                    newOut.initSize();
                    return newOut;
                }
            }
            case Finger.Empty<T> v -> {
                var single = new Finger.Single<T>();
                single.inner = obj;
                return single;
            }
            case Finger.Single<T> v -> {
                var out = new Finger.Deep<T>();
                var left = new Finger.Digit<T>();
                left.values = new Object[] { obj };
                updateDigitSize(left);

                var right = new Finger.Digit<T>();
                right.values = new Object[] { v.inner };
                updateDigitSize(right);

                out.left = left;
                out.right = right;
                out.deep = Finger.Empty.empty();
                out.initSize();
                return out;
            }
        }
    }

    public static <T> Finger<T> popLeft(Finger<T> finger, Consumer<? super T> consume) {
        switch (finger) {
            case Finger.Deep<T> v -> {
                var leftOne = (T ) v.left.values[0];
                consume.accept(leftOne);

                if (v.left.values.length == 1) {
                    // if deep is empty ...
                    if (Objects.equals(v.deep, Finger.Empty.empty())) {
                        // change to single or take a value from right ...
                        if (v.right.values.length == 1) {
                            var newSingle = new Finger.Single<T>();
                            newSingle.inner = (T ) v.right.values[0];
                            return newSingle;
                        } else {
                            var newLeft = new Finger.Digit<T>();
                            newLeft.values = new Object[] { v.right.values[0] };
                            updateDigitSize(newLeft);

                            var newRight = new Finger.Digit<T>();
                            newRight.values = new Object[v.right.values.length - 1];
                            System.arraycopy(v.right.values, 1, newRight.values, 0, newRight.values.length);
                            updateDigitSize(newRight);

                            var newOut = new Finger.Deep<T>();
                            newOut.left = newLeft;
                            newOut.deep = Finger.Empty.empty();
                            newOut.right = newRight;
                            newOut.initSize();
                            return newOut;
                        }
                    } else {
                        // just pop from deep
                        var getADigit = new AtomicReference<Finger.Digit<T>>();
                        Consumer<Finger.Digit<T>> f = getADigit::set;
                        var newDeep = popLeft(v.deep, f);
                        var newLeft = getADigit.get();

                        var newOut = new Finger.Deep<T>();
                        newOut.left = newLeft;
                        newOut.deep = newDeep;
                        newOut.right = v.right;
                        newOut.initSize();
                        return newOut;
                    }
                } else {
                    var newLeft = new Finger.Digit<T>();
                    newLeft.values = new Object[v.left.values.length - 1];
                    System.arraycopy(v.left.values, 1, newLeft.values, 0, newLeft.values.length);
                    updateDigitSize(newLeft);

                    var newOut = new Finger.Deep<T>();
                    newOut.left = newLeft;
                    newOut.deep = v.deep;
                    newOut.right = v.right;
                    newOut.initSize();
                    return newOut;
                }
            }
            case Finger.Empty<T> v -> {
                throw new RuntimeException();
            }
            case Finger.Single<T> v -> {
                consume.accept(v.inner);
                return Finger.Empty.empty();
            }
        }
    }

    public static <T, O extends T> Finger<T> pushRight(Finger<T> finger, O obj) {
        switch (finger) {
            case Finger.Deep<T> v -> {
                int len = v.right.values.length;
                if (len == 4) {
                    var newRight = new Finger.Digit<T>();
                    newRight.values = new Object[2];
                    System.arraycopy(v.right.values, 3, newRight.values, 0, 1);
                    newRight.values[1] = obj;
                    updateDigitSize(newRight);

                    var nxtPushRight = new Finger.Digit<T>();
                    nxtPushRight.values = new Object[3];
                    System.arraycopy(v.right.values, 0, nxtPushRight.values, 0, 3);
                    updateDigitSize(nxtPushRight);

                    var newDeep = pushRight(v.deep, nxtPushRight);
                    var newOut = new Finger.Deep<T>();
                    newOut.left = v.left;
                    newOut.deep = newDeep;
                    newOut.right = newRight;
                    newOut.initSize();
                    return newOut;
                } else {
                    var newRight = new Finger.Digit<T>();
                    newRight.values = new Object[len + 1];
                    System.arraycopy(v.right.values, 0, newRight.values, 0, len);
                    newRight.values[newRight.values.length - 1] = obj;
                    updateDigitSize(newRight);

                    var newOut = new Finger.Deep<T>();
                    newOut.left = v.left;
                    newOut.deep = v.deep;
                    newOut.right = newRight;
                    newOut.initSize();
                    return newOut;
                }
            }
            case Finger.Empty<T> v -> {
                var single = new Finger.Single<T>();
                single.inner = obj;
                return single;
            }
            case Finger.Single<T> v -> {
                var left = new Finger.Digit<T>();
                left.values = new Object[] { v.inner };
                updateDigitSize(left);

                var right = new Finger.Digit<T>();
                right.values = new Object[] { obj };
                updateDigitSize(right);

                var out = new Finger.Deep<T>();
                out.left = left;
                out.right = right;
                out.deep = Finger.Empty.empty();
                out.initSize();
                return out;
            }
        }
    }


    public static <T> Finger<T> popRight(Finger<T> finger, Consumer<? super T> consume) {
        switch (finger) {
            case Finger.Deep<T> v -> {
                var rightOne = (T ) v.right.values[v.right.values.length - 1];
                consume.accept(rightOne);

                if (v.right.values.length == 1) {
                    // if deep is empty ...
                    if (Objects.equals(v.deep, Finger.Empty.empty())) {
                        // change to single or take a value from right ...
                        if (v.left.values.length == 1) {
                            var newSingle = new Finger.Single<T>();
                            newSingle.inner = (T ) v.left.values[0];
                            return newSingle;
                        } else {
                            var newRight = new Finger.Digit<T>();
                            newRight.values = new Object[] { v.left.values[v.left.values.length - 1] };
                            updateDigitSize(newRight);

                            var newLeft = new Finger.Digit<T>();
                            newLeft.values = new Object[v.left.values.length - 1];
                            System.arraycopy(v.left.values, 0, newLeft.values, 0, newLeft.values.length);
                            updateDigitSize(newLeft);

                            var newOut = new Finger.Deep<T>();
                            newOut.left = newLeft;
                            newOut.deep = Finger.Empty.empty();
                            newOut.right = newRight;
                            newOut.initSize();
                            return newOut;
                        }
                    } else {
                        // just pop from deep
                        var getADigit = new AtomicReference<Finger.Digit<T>>();
                        Consumer<Finger.Digit<T>> f = getADigit::set;
                        var newDeep = popRight(v.deep, f);
                        var newRight = getADigit.get();

                        var newOut = new Finger.Deep<T>();
                        newOut.left = v.left;
                        newOut.deep = newDeep;
                        newOut.right = newRight;
                        newOut.initSize();
                        return newOut;
                    }
                } else {
                    var newRight = new Finger.Digit<T>();
                    newRight.values = new Object[v.right.values.length - 1];
                    System.arraycopy(v.right.values, 0, newRight.values, 0, newRight.values.length);
                    updateDigitSize(newRight);

                    var newOut = new Finger.Deep<T>();
                    newOut.left = v.left;
                    newOut.deep = v.deep;
                    newOut.right = newRight;
                    newOut.initSize();
                    return newOut;
                }
            }
            case Finger.Empty<T> v -> {
                throw new RuntimeException();
            }
            case Finger.Single<T> v -> {
                consume.accept(v.inner);
                return Finger.Empty.empty();
            }
        }
    }

    record EPair<T> (T left, T right) {}

    public static <T> Finger<T> merge(Finger<T> leftFinger, Finger<T> rightFinger) {
        switch (leftFinger) {
            case Finger.Deep<T> lv -> {
                switch (rightFinger) {
                    case Finger.Deep<T> rv -> {
                        var lInner = lv.deep;
                        var lc = lv.right.values.length;
                        var rc = rv.left.values.length;
                        var sum = lc + rc;
                        var buf = new Object[sum];
                        System.arraycopy(lv.right.values, 0, buf, 0, lc);
                        System.arraycopy(rv.left.values, 0, buf, lc, rc);
                        int[] bufNum;
                        // sum: 2, 3, 4, 5, 6, 7, 8
                        switch (sum) {
                            case 2, 3 -> {
                                bufNum = new int[] { sum };
                            }
                            case 4, 5 -> {
                                bufNum = new int[] { 2, sum - 2 };
                            }
                            case 6 -> {
                                bufNum = new int[] { 3, 3 };
                            }
                            case 7, 8 -> {
                                bufNum = new int[] { 2, sum - 5, 3 };
                            }
                            default -> {
                                throw new AssertionError();
                            }
                        }
                        int start = 0;
                        for (int b : bufNum) {
                            var tmpBuf = new Object[b];
                            System.arraycopy(buf, start, tmpBuf, 0, b);
                            var newDigit = new Finger.Digit<T>();
                            newDigit.values = tmpBuf;
                            updateDigitSize(newDigit);

                            lInner = pushRight(lInner, newDigit);
                        }
                        lInner = merge(lInner, rv.deep);

                        var newDeep = lInner;
                        var newOut = new Finger.Deep<T>();
                        newOut.left = lv.left;
                        newOut.right = rv.right;
                        newOut.deep = newDeep;
                        newOut.initSize();
                        return newOut;
                    }
                    case Finger.Empty<T> v1 -> {
                        return leftFinger;
                    }
                    case Finger.Single<T> v1 -> {
                        return pushRight(leftFinger, v1.inner);
                    }
                }
            }
            case Finger.Empty<T> v -> {
                return rightFinger;
            }
            case Finger.Single<T> v -> {
                return pushLeft(rightFinger, v.inner);
            }
        }
    }

    public static <T> Finger<T> fromDigit(Finger.Digit<T> digit) {
        int len = digit.values.length;
        switch (len) {
            case 1 -> {
                var newOut = new Finger.Single<T>();
                newOut.inner = (T ) digit.values[0];
                return newOut;
            }
            case 2, 3, 4 -> {
                var newOut = new Finger.Deep<T>();
                var splitIdx = len / 2;
                var newLeft = new Finger.Digit<T>();
                newLeft.values = new Object[splitIdx];
                System.arraycopy(digit.values, 0, newLeft.values, 0, newLeft.values.length);
                updateDigitSize(newLeft);

                var newRight = new Finger.Digit<T>();
                newRight.values = new Object[len - splitIdx];
                System.arraycopy(digit.values, splitIdx, newRight.values, 0, newRight.values.length);
                updateDigitSize(newRight);

                newOut.left = newLeft;
                newOut.right = newRight;
                newOut.deep = Finger.Empty.empty();
                newOut.initSize();
                return newOut;
            }
            default -> {
                throw new AssertionError();
            }
        }
    }

    public static <T> EPair<Finger<T>> split(Finger<T> finger, int index) {
        return splitImpl(finger, index, (i, d) -> {
            int len = d.values.length;
            if (i < 0 || i > len) {
                throw new AssertionError();
            }
            // notice here, the update of size is far away!
            var leftDigit = new Finger.Digit<T>();
            var rightDigit = new Finger.Digit<T>();
            leftDigit.values = new Object[i];
            System.arraycopy(d.values, 0, leftDigit.values, 0, leftDigit.values.length);

            rightDigit.values = new Object[len - i];
            System.arraycopy(d.values, i, rightDigit.values, 0, rightDigit.values.length);

            Optional<Finger.Digit<T>> leftOut, rightOut;
            if (leftDigit.values.length == 0) {
                leftOut = Optional.empty();
            } else {
                updateDigitSize(leftDigit);
                leftOut = Optional.of(leftDigit);
            }
            if (rightDigit.values.length == 0) {
                rightOut = Optional.empty();
            } else {
                updateDigitSize(rightDigit);
                rightOut = Optional.of(rightDigit);
            }
            return new EPair<>(leftOut, rightOut);
        });
    }

    public static <T> EPair<Finger<T>> splitImpl(Finger<T> finger, int index, BiFunction<Integer, Finger.Digit<T>, EPair<Optional<Finger.Digit<T>>>> splitOp) {
        if (index < 0) {
            throw new AssertionError();
        }
        if (index == 0) {
            return new EPair<>(Finger.Empty.empty(), finger);
        }
        if (index > finger.size()) {
            throw new AssertionError();
        }
        if (index == finger.size()) {
            return new EPair<>(finger, Finger.Empty.empty());
        }
        switch (finger) {
            case Finger.Deep<T> v -> {
                var leftSize = v.left.size;
                if (index <= leftSize) {
                    // handle here
                    var newLeftEpair = splitOp.apply(index, v.left);
                    var leftFinger = fromDigit(newLeftEpair.left.get());

                    if (newLeftEpair.right.isPresent()) {
                        // great, just remove the left guys all

                        var newLeftInRightFinger = newLeftEpair.right.get();
                        var rightFinger = new Finger.Deep<T>();
                        rightFinger.left = newLeftInRightFinger;
                        rightFinger.deep = v.deep;
                        rightFinger.right = v.right;
                        rightFinger.initSize();

                        return new EPair<>(leftFinger, rightFinger);
                    } else {
                        // bad situation, should get some value from deep ()

                        var newLeftInRightFingerHolder = new AtomicReference<Finger.Digit<T>>();
                        if (Objects.equals(v.deep, Finger.Empty.empty())) {
                            var rightFinger = fromDigit(v.right);
                            return new EPair<>(leftFinger, rightFinger);
                        }
                        var newRightFingerDeep = popLeft(v.deep, newLeftInRightFingerHolder::set);
                        var newLeftInRightFinger = newLeftInRightFingerHolder.get();

                        var rightFinger = new Finger.Deep<T>();
                        rightFinger.left = newLeftInRightFinger;
                        rightFinger.deep = newRightFingerDeep;
                        rightFinger.right = v.right;
                        rightFinger.initSize();
                        return new EPair<>(leftFinger, rightFinger);
                    }
                }
                var deepSize = v.deep.size();
                if (index <= leftSize + deepSize) {
                    // handle here

                    BiFunction<Integer, Finger.Digit<Finger.Digit<T>>, EPair<Optional<Finger.Digit<Finger.Digit<T>>>>>
                            bf = (i, d) -> {
                        // Array[Digit[T]]:
                        var dvalues = d.values;
                        int less = i;
                        var newLeft = new ArrayList<Finger.Digit<T>>();
                        var newRight = new ArrayList<Finger.Digit<T>>();
                        for (int i0 = 0; i0 < dvalues.length; i0++) {
                            var di = (Finger.Digit<T> )dvalues[i0];
                            var i0Size = di.size;
                            if (less >= i0Size) {
                                newLeft.add(di);
                                less -= i0Size;
                            } else if (less == 0) {
                                newRight.add(di);
                            } else {
                                // less > 0 but less < i0Size !
                                var splitRst = splitOp.apply(less, di);
                                var splitLeft = splitRst.left.get();
                                var splitRight = splitRst.right.get();
                                newLeft.add(splitLeft);
                                newRight.add(splitRight);
                                less = 0;
                            }
                        }
                        Optional<Finger.Digit<Finger.Digit<T>>> leftOut, rightOut;
                        if (newLeft.isEmpty()) {
                            leftOut = Optional.empty();
                        } else {
                            var leftAr = newLeft.toArray();
                            if (leftAr.length > 4) throw new AssertionError();
                            var digit = new Finger.Digit<Finger.Digit<T>>();
                            digit.values = leftAr;
                            updateDigitSize(digit);
                            leftOut = Optional.of(digit);
                        }
                        if (newRight.isEmpty()) {
                            rightOut = Optional.empty();
                        } else {
                            var rightAr = newRight.toArray();
                            if (rightAr.length > 4) throw new AssertionError();
                            var digit = new Finger.Digit<Finger.Digit<T>>();
                            digit.values = rightAr;
                            updateDigitSize(digit);
                            rightOut = Optional.of(digit);
                        }
                        return new EPair<>(leftOut, rightOut);
                    };

                    var splitDeep = splitImpl(v.deep, index - leftSize, bf);
                    var rightDigitHolderInLeftFinger = new AtomicReference<Finger.Digit<T>>();
                    // if successful ~
                    var leftFinger = new Finger.Deep<T>();
                    leftFinger.left = v.left;
                    leftFinger.deep = popRight(splitDeep.left, rightDigitHolderInLeftFinger::set);
                    leftFinger.right = rightDigitHolderInLeftFinger.get();
                    leftFinger.initSize();

                    if (Objects.equals(splitDeep.right, Finger.Empty.empty())) {
                        var rightFinger = fromDigit(v.right);
                        return new EPair<>(leftFinger, rightFinger);
                    }
                    var rightFinger = new Finger.Deep<T>();
                    var leftDigitHolderInRightFinger = new AtomicReference<Finger.Digit<T>>();
                    rightFinger.right = v.right;
                    rightFinger.deep = popLeft(splitDeep.right, leftDigitHolderInRightFinger::set);
                    rightFinger.left = leftDigitHolderInRightFinger.get();
                    rightFinger.initSize();

                    return new EPair<>(leftFinger, rightFinger);
                }
                // ending here
                var less = index - leftSize - deepSize;
                {
                    var newRightSplit = splitOp.apply(less, v.right);
                    if (newRightSplit.right.isEmpty()) {
                        // check why ?!?!?!
                        System.out.printf("less: %d\nv.right.size: %d\n", less, v.right.size);
                    }
                    var rightFinger = fromDigit(newRightSplit.right.get());

                    if (newRightSplit.left.isPresent()) {
                        // great, just remove the left guys all

                        var newRightInLeftFinger = newRightSplit.left.get();
                        var leftFinger = new Finger.Deep<T>();
                        leftFinger.left = v.left;
                        leftFinger.deep = v.deep;
                        leftFinger.right = newRightInLeftFinger;
                        leftFinger.initSize();

                        return new EPair<>(leftFinger, rightFinger);
                    } else {
                        // bad situation, should get some value from deep ()
                        // impossible to meet this situation ...

                        if (true) {
                            throw new AssertionError();
                        }

                        var newRightInLeftFingerHolder = new AtomicReference<Finger.Digit<T>>();
                        var newLeftFingerDeep = popRight(v.deep, newRightInLeftFingerHolder::set);
                        var newRightInLeftFinger = newRightInLeftFingerHolder.get();

                        var leftFinger = new Finger.Deep<T>();
                        leftFinger.left = v.left;
                        leftFinger.deep = newLeftFingerDeep;
                        leftFinger.right = newRightInLeftFinger;
                        leftFinger.initSize();
                        return new EPair<>(leftFinger, rightFinger);
                    }
                }

            }
            case Finger.Empty<T> v -> {
                throw new AssertionError();
            }
            case Finger.Single<T> v -> {
                var asDigit = new Finger.Digit<T>();
                asDigit.values = new Object[] { v.inner };
                updateDigitSize(asDigit);
                var spliter = splitOp.apply(index, asDigit);
                var l = spliter.left.get();
                var r = spliter.right.get();
                var leftFinger = fromDigit(l);
                var rightFinger = fromDigit(r);
                return new EPair<>(leftFinger, rightFinger);
            }
        }
    }

    public static <T> T indexGet(Finger<T> finger, int index) {
        var ans = new AtomicReference<T>();
        indexGetImpl(finger, index, (i, d) -> {
            var di = (T ) d.values[i];
            ans.set(di);
        });
        return ans.get();
    }

    public static <T> void indexGetImpl(Finger<T> finger, int idx, BiConsumer<Integer, Finger.Digit<T>> consume) {
        if (idx < 0 || idx > finger.size()) {
            throw new AssertionError();
        }
        switch (finger) {
            case Finger.Deep<T> v -> {
                if (idx < v.left.size) {
                    consume.accept(idx, v.left);
                } else if (idx < v.left.size + v.deep.size()) {
                    int l = idx - v.left.size;
                    // ...
                    BiConsumer<Integer, Finger.Digit<Finger.Digit<T>>> c = (i, d) -> {
                        if (i < 0 || i >= d.size) {
                            throw new AssertionError();
                        }
                        var dv = d.values;
                        for (int i0 = 0; i0 < dv.length; i0++) {
                            var di = (Finger.Digit<T> ) dv[i0];
                            if (i < di.size) {
                                consume.accept(i, di);
                                break ;
                            }
                            i -= di.size;
                        }
                    };
                    indexGetImpl(v.deep, l, c);
                } else {
                    int l = idx - v.left.size - v.deep.size();
                    consume.accept(l, v.right);
                }
            }
            case Finger.Single<T> v -> {
                var selfDigit = new Finger.Digit<T>();
                selfDigit.values = new Object[] { v.inner };
                updateDigitSize(selfDigit);
                consume.accept(idx, selfDigit);
            }
            default -> {
                throw new AssertionError();
            }
        }

    }

    public static <T> void orderlyMethod(Finger<T> finger, Consumer<T> handle) {
        switch (finger) {
            case Finger.Deep<T> v -> {
                for (Object value : v.left.values) {
                    handle.accept((T ) value);
                }
                // deep inner ~
                Consumer<Finger.Digit<T>> handleUp = d -> {
                    for (Object value : d.values) {
                        handle.accept((T ) value);
                    }
                };
                orderlyMethod(v.deep, handleUp);
                for (Object value : v.right.values) {
                    handle.accept((T ) value);
                }
            }
            case Finger.Empty<T> v -> {
            }
            case Finger.Single<T> v -> {
                handle.accept(v.inner);
            }
        }
    }

    public static <T> List<T> toList(Finger<T> finger) {
        var l = new ArrayList<T>();
        l.ensureCapacity(finger.size());
        orderlyMethod(finger, l::add);
        return l;
    }

    public static <T> String toString(Finger<T> finger) {
        var l = toList(finger);
        var ans = l.stream().map(Objects::toString).collect(Collectors.joining(","));
        return ans;
    }

}