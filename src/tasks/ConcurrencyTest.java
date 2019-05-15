package tasks;

public class ConcurrencyTest {
    public static long test = 0;
    public static void main(String[] args) throws InterruptedException {
        Thread[] threads = new Thread[4];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (long j = 0; j < 1000000000l; j++) {
                    test += j;
                }
                System.out.println(test);
            });
        }
        for (int i = 0; i < threads.length; i++) {
            System.out.println("started " + i);
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
    }
}
