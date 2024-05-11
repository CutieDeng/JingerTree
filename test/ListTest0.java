import immut.*;

import java.util.*;

public class ListTest0 {
    public static void main(String[] args) {
        List<String> list = new JList<>();
        list.add("apple");
        list.add("pear");
        list.add("apple");
        System.out.println(list.size());
        System.out.println(list);
    }
}

class ListTest1 {
    public static void main(String[] args) {
        List<String> list = new JList<>();
        list.add("apple"); // size=1
        list.add(null); // size=2
        list.add("pear"); // size=3
        String second = list.get(1); // null
        System.out.println(second);
        System.out.println(list);
    }
}

class ListTest2 {
    public static void main(String[] args) {
        // 构造从start到end的序列：
        final int start = 10;
        final int end = 20;
        List<Integer> list = new JList<>();
        for (int i = start; i <= end; i++) {
            list.add(i);
        }
        // 随机删除List中的一个元素:
        int removed = list.remove((int) (Math.random() * list.size()));
        int found = findMissingNumber(start, end, list);
        System.out.println(list.toString());
        System.out.println("missing number: " + found);
        System.out.println("actual missing: " + removed);
        System.out.println(removed == found ? "测试成功" : "测试失败");
    }
    static int findMissingNumber(int start, int end, List<Integer> list) {
        end += 1;
        long s = list.stream().mapToInt(i -> i).sum();
        long should = (long) (end - start) * (end + start - 1) / 2;
        return (int ) (should - s);
    }
}

class BenchMark {

    int len;

    public static void main(String[] args) {
        BenchMark m = new BenchMark();
        m.len = 950000;
        m.test1();
    }

    public void test0() {
        var arr = listItemsAdd(new ArrayList<>());
        var link = listItemsAdd(new LinkedList<>());
        var myL = listItemsAdd(new JList<>());
        System.out.printf("Add %d itmes bench with 10 times. \n", len);
        System.out.printf("ArrayList: %f s\n", arr);
        System.out.printf("LinkedList: %f s\n", link);
        System.out.printf("JList: %f s\n", myL);
    }

    public void test1() {
        var arr = listOrderFetch(new ArrayList<>());
        //var link = listOrderFetch(new LinkedList<>());
        var link = 0.0;
        var myL = listOrderFetch(new JList<>());
        System.out.printf("Fetch %d itmes bench with 10 times. \n", len);
        System.out.printf("ArrayList: %f s\n", arr);
        System.out.printf("LinkedList: %f s\n", link);
        System.out.printf("JList: %f s\n", myL);
    }

    public double listItemsAdd(List<Integer> list) {
        list.clear();
        for (int i = 0; i < len; i += 1) {
            list.add(i);
        }
        long start = System.nanoTime();
        for (int j = 0; j < 10; j += 1) {
            list.clear();
            for (int i = 0; i < len; i += 1) {
                list.add(i);
            }
        }
        long end = System.nanoTime();
        return (end - start) / 1e9;
    }

    public double listOrderFetch(List<Integer> list) {
        list.clear();
        for (int i = 0; i < len; i += 1) {
            list.add(i);
        }
        long start = System.nanoTime();
        for (int j = 0; j < 10; j += 1) {
            for (int i = 0; i < len; i += 1) {
                var _ = list.get(i);
            }
        }
        long end = System.nanoTime();
        return (end - start) / 1e9;
    }

}