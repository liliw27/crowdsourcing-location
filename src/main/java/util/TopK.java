package util;



import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 8/31/21 11:26 AM
 */
public class TopK<E extends Comparable> {

    /**
     * 返回前K大的元素
     *
     * @param maxSize
     * @param list
     * @return
     */
    public PriorityQueue<E> topK(int maxSize, List<E> list) {
        if (maxSize <= 0) {
            return null;
        }
        PriorityQueue<E> queue = new PriorityQueue<>(maxSize + 1, new Comparator<E>() {
            @Override
            public int compare(E o1, E o2) {
                // 最大堆用o2 - o1，最小堆用o1 - o2
                int a = o1.compareTo(o2);
                return a;
            }
        });
        for (E e : list) {
            queue.add(e);
            if (queue.size() > maxSize) {
                queue.poll();
            }
        }
        return queue;
    }


    /**
     * 返回前K小的元素
     *
     * @param maxSize
     * @param list
     * @return
     */
    public PriorityQueue<E> bottomK(int maxSize, List<E> list) {
        if (maxSize <= 0) {
            return null;
        }
        PriorityQueue<E> queue = new PriorityQueue<>(maxSize + 1, new Comparator<E>() {
            @Override
            public int compare(E o1, E o2) {
                // 最大堆用o2 - o1，最小堆用o1 - o2
                int a = o2.compareTo(o1);
                return a;
            }
        });
        for (E e : list) {
            queue.add(e);
            if (queue.size() > maxSize) {
                queue.poll();
            }
        }
        return queue;
    }
//    public static PriorityQueue<JobReduced> bottomK(int maxSize, Set<JobReduced> array, double gap) {
//        if (maxSize <= 0) {
//            return null;
//        }
//        PriorityQueue<JobReduced> queue = new PriorityQueue<>(maxSize + 1, new Comparator<JobReduced>() {
//            @Override
//            public int compare(JobReduced o1, JobReduced o2) {
//                // 最大堆用o2 - o1，最小堆用o1 - o2
//                int a = o2.compareTo(o1);
//                return a;
//            }
//        });
//        for (JobReduced e : array) {
//            if (e.getReducedCost() < gap) {
//                queue.add(e);
//            }
//            if (queue.size() > maxSize) {
//                queue.poll();
//            }
//        }
//        return queue;
//    }
}