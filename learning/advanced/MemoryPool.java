import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MemoryPool - Object pool implementation with performance benchmarks.
 * 
 * Features:
 * - Generic object pooling
 * - Thread-safe operations
 * - Automatic object validation and cleanup
 * - Pool size limits (min/max)
 * - Performance benchmarking vs direct allocation
 * - Statistics tracking
 * 
 * Compile: javac MemoryPool.java
 * Run: java MemoryPool
 * 
 * @author Advanced Java Learning
 * @version 1.0
 */
public class MemoryPool {
    
    /**
     * Factory interface for creating pool objects.
     */
    @FunctionalInterface
    interface ObjectFactory<T> {
        T create();
    }
    
    /**
     * Validator interface for checking object validity.
     */
    @FunctionalInterface
    interface ObjectValidator<T> {
        boolean validate(T object);
    }
    
    /**
     * Generic object pool implementation.
     * 
     * @param <T> Type of objects in the pool
     */
    static class ObjectPool<T> {
        private final BlockingQueue<T> available;
        private final Set<T> inUse;
        private final ObjectFactory<T> factory;
        private final ObjectValidator<T> validator;
        private final int minSize;
        private final int maxSize;
        private final AtomicInteger created = new AtomicInteger(0);
        private final AtomicInteger acquired = new AtomicInteger(0);
        private final AtomicInteger released = new AtomicInteger(0);
        private final AtomicInteger validated = new AtomicInteger(0);
        private final AtomicInteger evicted = new AtomicInteger(0);
        
        /**
         * Constructs an object pool.
         * 
         * @param factory Factory for creating objects
         * @param validator Validator for checking object validity
         * @param minSize Minimum pool size
         * @param maxSize Maximum pool size
         */
        public ObjectPool(ObjectFactory<T> factory, ObjectValidator<T> validator,
                         int minSize, int maxSize) {
            if (minSize < 0 || maxSize < minSize) {
                throw new IllegalArgumentException("Invalid pool size parameters");
            }
            
            this.factory = factory;
            this.validator = validator;
            this.minSize = minSize;
            this.maxSize = maxSize;
            this.available = new LinkedBlockingQueue<>(maxSize);
            this.inUse = Collections.synchronizedSet(new HashSet<>());
            
            // Pre-populate pool to minimum size
            for (int i = 0; i < minSize; i++) {
                available.offer(createObject());
            }
        }
        
        /**
         * Creates a new object using the factory.
         */
        private T createObject() {
            T object = factory.create();
            created.incrementAndGet();
            return object;
        }
        
        /**
         * Acquires an object from the pool.
         * 
         * @return An object from the pool
         * @throws InterruptedException if interrupted while waiting
         */
        public T acquire() throws InterruptedException {
            T object = available.poll();
            
            // If no available object and we haven't reached max size, create new one
            if (object == null && (created.get() < maxSize)) {
                object = createObject();
            }
            
            // If still null, wait for an object to become available
            if (object == null) {
                object = available.take();
            }
            
            // Validate object
            validated.incrementAndGet();
            if (!validator.validate(object)) {
                evicted.incrementAndGet();
                object = createObject();
            }
            
            inUse.add(object);
            acquired.incrementAndGet();
            return object;
        }
        
        /**
         * Acquires an object with timeout.
         * 
         * @param timeout Timeout value
         * @param unit Time unit
         * @return An object or null if timeout
         */
        public T acquire(long timeout, TimeUnit unit) throws InterruptedException {
            T object = available.poll(timeout, unit);
            
            if (object == null && (created.get() < maxSize)) {
                object = createObject();
            }
            
            if (object != null) {
                validated.incrementAndGet();
                if (!validator.validate(object)) {
                    evicted.incrementAndGet();
                    object = createObject();
                }
                inUse.add(object);
                acquired.incrementAndGet();
            }
            
            return object;
        }
        
        /**
         * Releases an object back to the pool.
         * 
         * @param object The object to release
         */
        public void release(T object) {
            if (object == null) {
                return;
            }
            
            if (!inUse.remove(object)) {
                throw new IllegalArgumentException("Object not acquired from this pool");
            }
            
            // Validate before returning to pool
            if (validator.validate(object)) {
                available.offer(object);
                released.incrementAndGet();
            } else {
                evicted.incrementAndGet();
                // Don't return invalid object to pool
            }
        }
        
        /**
         * Gets pool statistics.
         */
        public PoolStatistics getStatistics() {
            return new PoolStatistics(
                available.size(),
                inUse.size(),
                created.get(),
                acquired.get(),
                released.get(),
                validated.get(),
                evicted.get()
            );
        }
        
        /**
         * Clears the pool.
         */
        public void clear() {
            available.clear();
            inUse.clear();
        }
    }
    
    /**
     * Pool statistics.
     */
    static class PoolStatistics {
        public final int available;
        public final int inUse;
        public final int totalCreated;
        public final int totalAcquired;
        public final int totalReleased;
        public final int totalValidated;
        public final int totalEvicted;
        
        public PoolStatistics(int available, int inUse, int totalCreated,
                            int totalAcquired, int totalReleased,
                            int totalValidated, int totalEvicted) {
            this.available = available;
            this.inUse = inUse;
            this.totalCreated = totalCreated;
            this.totalAcquired = totalAcquired;
            this.totalReleased = totalReleased;
            this.totalValidated = totalValidated;
            this.totalEvicted = totalEvicted;
        }
        
        @Override
        public String toString() {
            return String.format(
                "PoolStatistics[available=%d, inUse=%d, created=%d, " +
                "acquired=%d, released=%d, validated=%d, evicted=%d]",
                available, inUse, totalCreated, totalAcquired, 
                totalReleased, totalValidated, totalEvicted
            );
        }
    }
    
    // ============================================================
    // EXAMPLE: EXPENSIVE OBJECT
    // ============================================================
    
    /**
     * Example of an expensive object to pool.
     */
    static class ExpensiveObject {
        private static final AtomicInteger idGenerator = new AtomicInteger(0);
        private final int id;
        private byte[] data;
        private boolean valid;
        
        public ExpensiveObject() {
            this.id = idGenerator.incrementAndGet();
            this.data = new byte[1024 * 100]; // 100KB
            this.valid = true;
            
            // Simulate expensive initialization
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        public void use() {
            if (!valid) {
                throw new IllegalStateException("Object is invalid");
            }
            // Simulate some work
            Arrays.fill(data, (byte) (id % 256));
        }
        
        public void reset() {
            Arrays.fill(data, (byte) 0);
        }
        
        public void invalidate() {
            valid = false;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public int getId() {
            return id;
        }
        
        @Override
        public String toString() {
            return "ExpensiveObject[id=" + id + ", valid=" + valid + "]";
        }
    }
    
    // ============================================================
    // BENCHMARKS
    // ============================================================
    
    /**
     * Benchmarks pool performance vs direct allocation.
     */
    static class PoolBenchmark {
        
        /**
         * Runs benchmark with pool.
         */
        public static long benchmarkWithPool(int iterations, int threads) throws InterruptedException {
            ObjectPool<ExpensiveObject> pool = new ObjectPool<>(
                ExpensiveObject::new,
                ExpensiveObject::isValid,
                10,
                50
            );
            
            CountDownLatch latch = new CountDownLatch(threads);
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            
            long startTime = System.nanoTime();
            
            for (int t = 0; t < threads; t++) {
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < iterations; i++) {
                            ExpensiveObject obj = pool.acquire();
                            obj.use();
                            pool.release(obj);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
            long endTime = System.nanoTime();
            
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            
            System.out.println("Pool Statistics: " + pool.getStatistics());
            
            return endTime - startTime;
        }
        
        /**
         * Runs benchmark without pool (direct allocation).
         */
        public static long benchmarkWithoutPool(int iterations, int threads) throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(threads);
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            
            long startTime = System.nanoTime();
            
            for (int t = 0; t < threads; t++) {
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < iterations; i++) {
                            ExpensiveObject obj = new ExpensiveObject();
                            obj.use();
                            // Object will be garbage collected
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
            long endTime = System.nanoTime();
            
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            
            return endTime - startTime;
        }
        
        /**
         * Runs comparison benchmark.
         */
        public static void runComparison() {
            System.out.println("\n=== PERFORMANCE BENCHMARK ===");
            
            int[] iterationCounts = {100, 500, 1000};
            int[] threadCounts = {1, 4, 8};
            
            for (int threads : threadCounts) {
                for (int iterations : iterationCounts) {
                    System.out.println("\n--- " + threads + " threads, " + 
                                     iterations + " iterations per thread ---");
                    
                    try {
                        // Warmup
                        benchmarkWithPool(10, threads);
                        benchmarkWithoutPool(10, threads);
                        System.gc();
                        Thread.sleep(100);
                        
                        // With pool
                        long poolTime = benchmarkWithPool(iterations, threads);
                        System.gc();
                        Thread.sleep(100);
                        
                        // Without pool
                        long directTime = benchmarkWithoutPool(iterations, threads);
                        
                        // Results
                        double poolMs = poolTime / 1_000_000.0;
                        double directMs = directTime / 1_000_000.0;
                        double speedup = (double) directTime / poolTime;
                        
                        System.out.println("\nResults:");
                        System.out.println("  With pool:    " + String.format("%.2f ms", poolMs));
                        System.out.println("  Without pool: " + String.format("%.2f ms", directMs));
                        System.out.println("  Speedup:      " + String.format("%.2fx", speedup));
                        
                    } catch (InterruptedException e) {
                        System.err.println("Benchmark interrupted");
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }
    
    // ============================================================
    // DEMO
    // ============================================================
    
    /**
     * Demonstrates basic pool usage.
     */
    private static void demoBasicUsage() {
        System.out.println("\n=== BASIC USAGE DEMO ===");
        
        ObjectPool<ExpensiveObject> pool = new ObjectPool<>(
            ExpensiveObject::new,
            ExpensiveObject::isValid,
            5,  // min size
            10  // max size
        );
        
        System.out.println("Initial statistics: " + pool.getStatistics());
        
        try {
            // Acquire and use objects
            System.out.println("\nAcquiring 3 objects...");
            ExpensiveObject obj1 = pool.acquire();
            ExpensiveObject obj2 = pool.acquire();
            ExpensiveObject obj3 = pool.acquire();
            
            System.out.println("  Acquired: " + obj1);
            System.out.println("  Acquired: " + obj2);
            System.out.println("  Acquired: " + obj3);
            System.out.println("Statistics: " + pool.getStatistics());
            
            // Use objects
            System.out.println("\nUsing objects...");
            obj1.use();
            obj2.use();
            obj3.use();
            
            // Release objects
            System.out.println("\nReleasing objects...");
            pool.release(obj1);
            pool.release(obj2);
            pool.release(obj3);
            System.out.println("Statistics: " + pool.getStatistics());
            
            // Test validation
            System.out.println("\nTesting validation...");
            ExpensiveObject obj4 = pool.acquire();
            obj4.invalidate();
            pool.release(obj4);
            System.out.println("Statistics: " + pool.getStatistics());
            
        } catch (InterruptedException e) {
            System.err.println("Demo interrupted");
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Demonstrates concurrent usage.
     */
    private static void demoConcurrentUsage() throws InterruptedException {
        System.out.println("\n=== CONCURRENT USAGE DEMO ===");
        
        ObjectPool<ExpensiveObject> pool = new ObjectPool<>(
            ExpensiveObject::new,
            ExpensiveObject::isValid,
            3,
            10
        );
        
        int numThreads = 5;
        int iterationsPerThread = 20;
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        System.out.println("Starting " + numThreads + " threads, " + 
                         iterationsPerThread + " iterations each...");
        
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < iterationsPerThread; i++) {
                        ExpensiveObject obj = pool.acquire();
                        obj.use();
                        Thread.sleep(10); // Simulate work
                        pool.release(obj);
                    }
                    System.out.println("  Thread " + threadId + " completed");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        System.out.println("\nFinal statistics: " + pool.getStatistics());
    }
    
    /**
     * Main method - Runs demos and benchmarks.
     */
    public static void main(String[] args) {
        System.out.println("Object Pool Implementation & Benchmarks");
        System.out.println("=======================================");
        
        try {
            demoBasicUsage();
            demoConcurrentUsage();
            PoolBenchmark.runComparison();
            
            System.out.println("\n=== ALL DEMOS COMPLETED ===");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
