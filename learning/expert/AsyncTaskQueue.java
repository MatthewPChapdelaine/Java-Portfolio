import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Production-ready Async Task Queue System
 * Features: Priority queues, persistence, worker pool, retry logic, monitoring, dead-letter queue
 */
public class AsyncTaskQueue {
    
    // ============ TASK DEFINITION ============
    enum TaskStatus {
        PENDING, RUNNING, COMPLETED, FAILED, DEAD_LETTER
    }
    
    enum Priority {
        LOW(0), NORMAL(1), HIGH(2), CRITICAL(3);
        
        final int value;
        Priority(int value) { this.value = value; }
    }
    
    static class Task implements Serializable {
        private static final long serialVersionUID = 1L;
        
        final String id;
        final String name;
        final Map<String, Object> payload;
        final Priority priority;
        final int maxRetries;
        
        TaskStatus status;
        int retryCount;
        String errorMessage;
        Instant createdAt;
        Instant startedAt;
        Instant completedAt;
        long executionTimeMs;
        
        Task(String name, Map<String, Object> payload, Priority priority, int maxRetries) {
            this.id = UUID.randomUUID().toString();
            this.name = name;
            this.payload = payload;
            this.priority = priority;
            this.maxRetries = maxRetries;
            this.status = TaskStatus.PENDING;
            this.retryCount = 0;
            this.createdAt = Instant.now();
        }
        
        @Override
        public String toString() {
            return String.format("Task[id=%s, name=%s, priority=%s, status=%s, retries=%d/%d]",
                id.substring(0, 8), name, priority, status, retryCount, maxRetries);
        }
    }
    
    // ============ TASK HANDLER ============
    @FunctionalInterface
    interface TaskHandler {
        void handle(Task task) throws Exception;
    }
    
    // ============ WORKER ============
    static class Worker implements Runnable {
        private final String workerId;
        private final TaskQueue queue;
        private final Map<String, TaskHandler> handlers;
        private final AtomicInteger processedCount = new AtomicInteger(0);
        private volatile boolean running = true;
        
        Worker(String workerId, TaskQueue queue, Map<String, TaskHandler> handlers) {
            this.workerId = workerId;
            this.queue = queue;
            this.handlers = handlers;
        }
        
        @Override
        public void run() {
            System.out.printf("[Worker %s] Started\n", workerId);
            
            while (running) {
                try {
                    Task task = queue.dequeue(1000);
                    if (task != null) {
                        processTask(task);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.printf("[Worker %s] Error: %s\n", workerId, e.getMessage());
                }
            }
            
            System.out.printf("[Worker %s] Stopped (processed %d tasks)\n", 
                workerId, processedCount.get());
        }
        
        private void processTask(Task task) {
            System.out.printf("[Worker %s] Processing %s\n", workerId, task);
            
            task.status = TaskStatus.RUNNING;
            task.startedAt = Instant.now();
            long startTime = System.currentTimeMillis();
            
            try {
                TaskHandler handler = handlers.get(task.name);
                if (handler == null) {
                    throw new IllegalArgumentException("No handler for task: " + task.name);
                }
                
                handler.handle(task);
                
                task.status = TaskStatus.COMPLETED;
                task.completedAt = Instant.now();
                task.executionTimeMs = System.currentTimeMillis() - startTime;
                
                queue.markCompleted(task);
                processedCount.incrementAndGet();
                
                System.out.printf("[Worker %s] Completed %s in %dms\n", 
                    workerId, task.id.substring(0, 8), task.executionTimeMs);
                
            } catch (Exception e) {
                task.executionTimeMs = System.currentTimeMillis() - startTime;
                task.errorMessage = e.getMessage();
                
                if (task.retryCount < task.maxRetries) {
                    task.retryCount++;
                    task.status = TaskStatus.PENDING;
                    queue.retry(task);
                    System.err.printf("[Worker %s] Task %s failed, retrying (%d/%d): %s\n",
                        workerId, task.id.substring(0, 8), task.retryCount, task.maxRetries, 
                        e.getMessage());
                } else {
                    task.status = TaskStatus.FAILED;
                    queue.moveToDLQ(task);
                    System.err.printf("[Worker %s] Task %s failed permanently: %s\n",
                        workerId, task.id.substring(0, 8), e.getMessage());
                }
            }
        }
        
        void shutdown() {
            running = false;
        }
    }
    
    // ============ TASK QUEUE ============
    static class TaskQueue {
        private final PriorityBlockingQueue<Task> queue;
        private final Map<String, Task> taskRegistry = new ConcurrentHashMap<>();
        private final Queue<Task> deadLetterQueue = new ConcurrentLinkedQueue<>();
        private final Path persistencePath;
        private final AtomicLong totalEnqueued = new AtomicLong(0);
        private final AtomicLong totalCompleted = new AtomicLong(0);
        private final AtomicLong totalFailed = new AtomicLong(0);
        
        TaskQueue(String persistencePath) {
            this.persistencePath = Paths.get(persistencePath);
            this.queue = new PriorityBlockingQueue<>(100, 
                Comparator.comparingInt((Task t) -> -t.priority.value)
                          .thenComparing(t -> t.createdAt));
            
            try {
                Files.createDirectories(this.persistencePath);
                loadPersistedTasks();
            } catch (IOException e) {
                System.err.println("Failed to initialize persistence: " + e.getMessage());
            }
        }
        
        void enqueue(Task task) {
            queue.offer(task);
            taskRegistry.put(task.id, task);
            totalEnqueued.incrementAndGet();
            persistTask(task);
            System.out.printf("Enqueued: %s\n", task);
        }
        
        Task dequeue(long timeoutMs) throws InterruptedException {
            return queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        }
        
        void retry(Task task) {
            queue.offer(task);
            persistTask(task);
        }
        
        void markCompleted(Task task) {
            totalCompleted.incrementAndGet();
            deletePersistedTask(task);
        }
        
        void moveToDLQ(Task task) {
            task.status = TaskStatus.DEAD_LETTER;
            deadLetterQueue.offer(task);
            totalFailed.incrementAndGet();
            deletePersistedTask(task);
            persistDLQTask(task);
        }
        
        Task getTask(String taskId) {
            return taskRegistry.get(taskId);
        }
        
        List<Task> getPendingTasks() {
            return new ArrayList<>(queue);
        }
        
        List<Task> getDeadLetterTasks() {
            return new ArrayList<>(deadLetterQueue);
        }
        
        QueueStats getStats() {
            return new QueueStats(
                totalEnqueued.get(),
                totalCompleted.get(),
                totalFailed.get(),
                queue.size(),
                deadLetterQueue.size()
            );
        }
        
        // ============ PERSISTENCE ============
        private void persistTask(Task task) {
            try {
                Path taskFile = persistencePath.resolve(task.id + ".task");
                try (ObjectOutputStream oos = new ObjectOutputStream(
                        Files.newOutputStream(taskFile))) {
                    oos.writeObject(task);
                }
            } catch (IOException e) {
                System.err.println("Failed to persist task: " + e.getMessage());
            }
        }
        
        private void deletePersistedTask(Task task) {
            try {
                Path taskFile = persistencePath.resolve(task.id + ".task");
                Files.deleteIfExists(taskFile);
            } catch (IOException e) {
                System.err.println("Failed to delete persisted task: " + e.getMessage());
            }
        }
        
        private void persistDLQTask(Task task) {
            try {
                Path dlqFile = persistencePath.resolve(task.id + ".dlq");
                try (ObjectOutputStream oos = new ObjectOutputStream(
                        Files.newOutputStream(dlqFile))) {
                    oos.writeObject(task);
                }
            } catch (IOException e) {
                System.err.println("Failed to persist DLQ task: " + e.getMessage());
            }
        }
        
        private void loadPersistedTasks() throws IOException {
            Files.list(persistencePath)
                .filter(p -> p.toString().endsWith(".task"))
                .forEach(path -> {
                    try (ObjectInputStream ois = new ObjectInputStream(
                            Files.newInputStream(path))) {
                        Task task = (Task) ois.readObject();
                        task.status = TaskStatus.PENDING;
                        queue.offer(task);
                        taskRegistry.put(task.id, task);
                        System.out.println("Recovered task: " + task);
                    } catch (Exception e) {
                        System.err.println("Failed to load task: " + e.getMessage());
                    }
                });
            
            Files.list(persistencePath)
                .filter(p -> p.toString().endsWith(".dlq"))
                .forEach(path -> {
                    try (ObjectInputStream ois = new ObjectInputStream(
                            Files.newInputStream(path))) {
                        Task task = (Task) ois.readObject();
                        deadLetterQueue.offer(task);
                        System.out.println("Recovered DLQ task: " + task);
                    } catch (Exception e) {
                        System.err.println("Failed to load DLQ task: " + e.getMessage());
                    }
                });
        }
    }
    
    // ============ STATISTICS ============
    static class QueueStats {
        final long totalEnqueued;
        final long totalCompleted;
        final long totalFailed;
        final int pendingCount;
        final int dlqCount;
        
        QueueStats(long totalEnqueued, long totalCompleted, long totalFailed, 
                  int pendingCount, int dlqCount) {
            this.totalEnqueued = totalEnqueued;
            this.totalCompleted = totalCompleted;
            this.totalFailed = totalFailed;
            this.pendingCount = pendingCount;
            this.dlqCount = dlqCount;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Stats[enqueued=%d, completed=%d, failed=%d, pending=%d, dlq=%d]",
                totalEnqueued, totalCompleted, totalFailed, pendingCount, dlqCount);
        }
    }
    
    // ============ TASK QUEUE MANAGER ============
    static class TaskQueueManager {
        private final TaskQueue queue;
        private final ExecutorService workerPool;
        private final List<Worker> workers = new ArrayList<>();
        private final Map<String, TaskHandler> handlers = new ConcurrentHashMap<>();
        private final ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor();
        
        TaskQueueManager(int numWorkers, String persistencePath) {
            this.queue = new TaskQueue(persistencePath);
            this.workerPool = Executors.newFixedThreadPool(numWorkers);
            
            for (int i = 0; i < numWorkers; i++) {
                Worker worker = new Worker("W" + i, queue, handlers);
                workers.add(worker);
                workerPool.submit(worker);
            }
        }
        
        void registerHandler(String taskName, TaskHandler handler) {
            handlers.put(taskName, handler);
        }
        
        String submitTask(String name, Map<String, Object> payload, Priority priority, int maxRetries) {
            Task task = new Task(name, payload, priority, maxRetries);
            queue.enqueue(task);
            return task.id;
        }
        
        Task getTask(String taskId) {
            return queue.getTask(taskId);
        }
        
        QueueStats getStats() {
            return queue.getStats();
        }
        
        void startMonitoring(long intervalMs) {
            monitor.scheduleAtFixedRate(() -> {
                System.out.println("\n=== Queue Monitoring ===");
                System.out.println(queue.getStats());
                System.out.println("Workers active: " + workers.size());
                System.out.println();
            }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        }
        
        void shutdown() {
            System.out.println("Shutting down task queue manager...");
            
            workers.forEach(Worker::shutdown);
            workerPool.shutdown();
            monitor.shutdown();
            
            try {
                if (!workerPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    workerPool.shutdownNow();
                }
                monitor.shutdownNow();
            } catch (InterruptedException e) {
                workerPool.shutdownNow();
                monitor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            System.out.println("Shutdown complete.");
        }
    }
    
    // ============ DEMO ============
    public static void main(String[] args) throws Exception {
        System.out.println("=== Async Task Queue Demo ===\n");
        
        TaskQueueManager manager = new TaskQueueManager(4, "/tmp/task-queue");
        
        // Register task handlers
        manager.registerHandler("email", task -> {
            String recipient = (String) task.payload.get("recipient");
            String subject = (String) task.payload.get("subject");
            Thread.sleep(100); // Simulate email sending
            System.out.printf("  Sent email to %s: %s\n", recipient, subject);
        });
        
        manager.registerHandler("process-data", task -> {
            Integer dataSize = (Integer) task.payload.get("size");
            Thread.sleep(50 * dataSize); // Simulate processing
            System.out.printf("  Processed %d records\n", dataSize);
        });
        
        manager.registerHandler("generate-report", task -> {
            String reportType = (String) task.payload.get("type");
            Thread.sleep(200);
            System.out.printf("  Generated %s report\n", reportType);
        });
        
        manager.registerHandler("flaky-task", task -> {
            if (task.retryCount < 2) {
                throw new RuntimeException("Simulated failure");
            }
            System.out.println("  Flaky task succeeded after retries!");
        });
        
        manager.registerHandler("failing-task", task -> {
            throw new RuntimeException("This task always fails");
        });
        
        // Start monitoring
        manager.startMonitoring(2000);
        
        // Submit various tasks with different priorities
        System.out.println("=== Submitting Tasks ===\n");
        
        List<String> taskIds = new ArrayList<>();
        
        // Critical priority tasks
        taskIds.add(manager.submitTask("email", 
            Map.of("recipient", "admin@example.com", "subject", "System Alert"),
            Priority.CRITICAL, 3));
        
        // High priority tasks
        taskIds.add(manager.submitTask("generate-report",
            Map.of("type", "Financial"),
            Priority.HIGH, 2));
        
        // Normal priority tasks
        for (int i = 0; i < 5; i++) {
            taskIds.add(manager.submitTask("email",
                Map.of("recipient", "user" + i + "@example.com", "subject", "Newsletter"),
                Priority.NORMAL, 3));
        }
        
        // Low priority tasks
        for (int i = 0; i < 3; i++) {
            taskIds.add(manager.submitTask("process-data",
                Map.of("size", i + 1),
                Priority.LOW, 2));
        }
        
        // Flaky task (will retry)
        taskIds.add(manager.submitTask("flaky-task",
            Map.of("attempt", "test"),
            Priority.NORMAL, 3));
        
        // Failing task (will go to DLQ)
        taskIds.add(manager.submitTask("failing-task",
            Map.of("doomed", true),
            Priority.NORMAL, 2));
        
        // Wait for processing
        System.out.println("\nWaiting for tasks to complete...\n");
        Thread.sleep(3000);
        
        // Check status
        System.out.println("=== Task Status ===");
        for (String id : taskIds.subList(0, Math.min(5, taskIds.size()))) {
            Task task = manager.getTask(id);
            if (task != null) {
                System.out.printf("%s - Status: %s, Execution: %dms\n",
                    task.name, task.status, task.executionTimeMs);
            }
        }
        
        // Final statistics
        Thread.sleep(2000);
        System.out.println("\n=== Final Statistics ===");
        System.out.println(manager.getStats());
        
        // Shutdown
        manager.shutdown();
        
        System.out.println("\n=== Demo Complete ===");
    }
}
