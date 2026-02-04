import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Real-Time Stream Processing System
 * Features: Windowing (tumbling, sliding, session), aggregations, transformations, 
 * backpressure handling, event-time processing, watermarks
 */
public class RealTimeSystem {
    
    // ============ EVENT ============
    static class Event<T> {
        final T value;
        final Instant timestamp;
        final String key;
        
        Event(T value, Instant timestamp, String key) {
            this.value = value;
            this.timestamp = timestamp;
            this.key = key;
        }
        
        Event(T value) {
            this(value, Instant.now(), null);
        }
        
        @Override
        public String toString() {
            return String.format("Event[%s, ts=%s, key=%s]", value, timestamp, key);
        }
    }
    
    // ============ WINDOW TYPES ============
    interface Window {
        long getStart();
        long getEnd();
        boolean contains(Instant timestamp);
    }
    
    static class TumblingWindow implements Window {
        final long start;
        final long end;
        
        TumblingWindow(long start, long duration) {
            this.start = start;
            this.end = start + duration;
        }
        
        @Override
        public long getStart() { return start; }
        
        @Override
        public long getEnd() { return end; }
        
        @Override
        public boolean contains(Instant timestamp) {
            long ts = timestamp.toEpochMilli();
            return ts >= start && ts < end;
        }
        
        @Override
        public String toString() {
            return String.format("TumblingWindow[%s-%s]", 
                Instant.ofEpochMilli(start), Instant.ofEpochMilli(end));
        }
    }
    
    static class SlidingWindow implements Window {
        final long start;
        final long end;
        
        SlidingWindow(long start, long size) {
            this.start = start;
            this.end = start + size;
        }
        
        @Override
        public long getStart() { return start; }
        
        @Override
        public long getEnd() { return end; }
        
        @Override
        public boolean contains(Instant timestamp) {
            long ts = timestamp.toEpochMilli();
            return ts >= start && ts < end;
        }
        
        @Override
        public String toString() {
            return String.format("SlidingWindow[%s-%s]", 
                Instant.ofEpochMilli(start), Instant.ofEpochMilli(end));
        }
    }
    
    static class SessionWindow implements Window {
        long start;
        long end;
        final long gap;
        
        SessionWindow(long start, long gap) {
            this.start = start;
            this.end = start;
            this.gap = gap;
        }
        
        void extend(long timestamp) {
            if (timestamp < start) start = timestamp;
            if (timestamp > end) end = timestamp;
        }
        
        boolean canMerge(long timestamp) {
            return timestamp <= end + gap;
        }
        
        @Override
        public long getStart() { return start; }
        
        @Override
        public long getEnd() { return end; }
        
        @Override
        public boolean contains(Instant timestamp) {
            long ts = timestamp.toEpochMilli();
            return ts >= start && ts <= end;
        }
        
        @Override
        public String toString() {
            return String.format("SessionWindow[%s-%s]", 
                Instant.ofEpochMilli(start), Instant.ofEpochMilli(end));
        }
    }
    
    // ============ STREAM ============
    static class Stream<T> {
        private final BlockingQueue<Event<T>> queue;
        private final List<Function<Event<T>, Event<?>>> transformations = new ArrayList<>();
        private final List<Consumer<Event<?>>> consumers = new ArrayList<>();
        private final ExecutorService executor = Executors.newCachedThreadPool();
        private volatile boolean running = true;
        
        static class Consumer<T> {
            final Function<Event<T>, Boolean> predicate;
            final java.util.function.Consumer<Event<T>> handler;
            
            Consumer(Function<Event<T>, Boolean> predicate, 
                    java.util.function.Consumer<Event<T>> handler) {
                this.predicate = predicate;
                this.handler = handler;
            }
        }
        
        Stream(int capacity) {
            this.queue = new ArrayBlockingQueue<>(capacity);
            startProcessing();
        }
        
        void emit(T value) throws InterruptedException {
            emit(new Event<>(value));
        }
        
        void emit(Event<T> event) throws InterruptedException {
            if (!queue.offer(event, 100, TimeUnit.MILLISECONDS)) {
                // Backpressure: drop oldest or block
                System.err.println("[Backpressure] Queue full, applying backpressure");
                queue.put(event); // Block until space available
            }
        }
        
        private void startProcessing() {
            executor.submit(() -> {
                while (running) {
                    try {
                        Event<T> event = queue.poll(100, TimeUnit.MILLISECONDS);
                        if (event != null) {
                            processEvent(event);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
        
        @SuppressWarnings("unchecked")
        private void processEvent(Event<T> event) {
            Event<?> current = event;
            
            // Apply transformations
            for (Function<Event<T>, Event<?>> transformation : transformations) {
                current = transformation.apply((Event<T>) current);
                if (current == null) return; // Filtered out
            }
            
            // Dispatch to consumers
            Event<?> finalEvent = current;
            for (Consumer consumer : consumers) {
                if (consumer.predicate.apply(finalEvent)) {
                    consumer.handler.accept(finalEvent);
                }
            }
        }
        
        <R> Stream<R> map(Function<T, R> mapper) {
            Stream<R> newStream = new Stream<>(queue.remainingCapacity());
            
            transformations.add(event -> {
                R result = mapper.apply(event.value);
                Event<R> newEvent = new Event<>(result, event.timestamp, event.key);
                try {
                    newStream.emit(newEvent);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return newEvent;
            });
            
            return newStream;
        }
        
        Stream<T> filter(Function<T, Boolean> predicate) {
            Stream<T> newStream = new Stream<>(queue.remainingCapacity());
            
            transformations.add(event -> {
                if (predicate.apply(event.value)) {
                    try {
                        newStream.emit(event);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return event;
                }
                return null;
            });
            
            return newStream;
        }
        
        void forEach(java.util.function.Consumer<Event<T>> handler) {
            consumers.add(new Consumer<>(e -> true, handler));
        }
        
        void shutdown() {
            running = false;
            executor.shutdown();
        }
    }
    
    // ============ WINDOWED STREAM ============
    static class WindowedStream<T> {
        private final Stream<T> source;
        private final Map<String, List<Event<T>>> windows = new ConcurrentHashMap<>();
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        WindowedStream(Stream<T> source) {
            this.source = source;
        }
        
        static <T> WindowedStream<T> tumbling(Stream<T> source, Duration windowSize) {
            WindowedStream<T> windowed = new WindowedStream<>(source);
            
            source.forEach(event -> {
                long windowStart = (event.timestamp.toEpochMilli() / windowSize.toMillis()) 
                    * windowSize.toMillis();
                String windowKey = "window-" + windowStart;
                
                windowed.windows.computeIfAbsent(windowKey, k -> new CopyOnWriteArrayList<>())
                    .add(event);
            });
            
            // Trigger window computation periodically
            windowed.scheduler.scheduleAtFixedRate(() -> {
                long now = Instant.now().toEpochMilli();
                List<String> expiredWindows = new ArrayList<>();
                
                for (String key : windowed.windows.keySet()) {
                    long windowStart = Long.parseLong(key.replace("window-", ""));
                    if (now >= windowStart + windowSize.toMillis()) {
                        expiredWindows.add(key);
                    }
                }
                
                for (String key : expiredWindows) {
                    List<Event<T>> events = windowed.windows.remove(key);
                    if (events != null && !events.isEmpty()) {
                        long windowStart = Long.parseLong(key.replace("window-", ""));
                        TumblingWindow window = new TumblingWindow(windowStart, windowSize.toMillis());
                        windowed.triggerWindow(window, events);
                    }
                }
            }, windowSize.toMillis(), windowSize.toMillis() / 2, TimeUnit.MILLISECONDS);
            
            return windowed;
        }
        
        static <T> WindowedStream<T> sliding(Stream<T> source, Duration windowSize, Duration slide) {
            WindowedStream<T> windowed = new WindowedStream<>(source);
            
            source.forEach(event -> {
                long timestamp = event.timestamp.toEpochMilli();
                long slideMs = slide.toMillis();
                long sizeMs = windowSize.toMillis();
                
                // Determine which windows this event belongs to
                long firstWindowStart = ((timestamp - sizeMs) / slideMs) * slideMs;
                
                for (long start = firstWindowStart; start <= timestamp; start += slideMs) {
                    if (timestamp >= start && timestamp < start + sizeMs) {
                        String windowKey = "window-" + start;
                        windowed.windows.computeIfAbsent(windowKey, k -> new CopyOnWriteArrayList<>())
                            .add(event);
                    }
                }
            });
            
            return windowed;
        }
        
        private final List<WindowAggregator<T, ?>> aggregators = new ArrayList<>();
        
        interface WindowAggregator<T, R> {
            R aggregate(Window window, List<Event<T>> events);
        }
        
        private void triggerWindow(Window window, List<Event<T>> events) {
            for (WindowAggregator<T, ?> aggregator : aggregators) {
                Object result = aggregator.aggregate(window, events);
                System.out.printf("[Window] %s -> %s\n", window, result);
            }
        }
        
        <R> void aggregate(WindowAggregator<T, R> aggregator) {
            aggregators.add(aggregator);
        }
        
        void shutdown() {
            scheduler.shutdown();
        }
    }
    
    // ============ AGGREGATORS ============
    static class Aggregators {
        static <T> WindowedStream.WindowAggregator<T, Long> count() {
            return (window, events) -> (long) events.size();
        }
        
        static WindowedStream.WindowAggregator<Number, Double> sum() {
            return (window, events) -> events.stream()
                .mapToDouble(e -> e.value.doubleValue())
                .sum();
        }
        
        static WindowedStream.WindowAggregator<Number, Double> average() {
            return (window, events) -> events.stream()
                .mapToDouble(e -> e.value.doubleValue())
                .average()
                .orElse(0.0);
        }
        
        static WindowedStream.WindowAggregator<Number, Double> max() {
            return (window, events) -> events.stream()
                .mapToDouble(e -> e.value.doubleValue())
                .max()
                .orElse(Double.MIN_VALUE);
        }
        
        static WindowedStream.WindowAggregator<Number, Double> min() {
            return (window, events) -> events.stream()
                .mapToDouble(e -> e.value.doubleValue())
                .min()
                .orElse(Double.MAX_VALUE);
        }
        
        static <T, K> WindowedStream.WindowAggregator<T, Map<K, Long>> groupCount(
                Function<T, K> keyExtractor) {
            return (window, events) -> events.stream()
                .collect(Collectors.groupingBy(
                    e -> keyExtractor.apply(e.value),
                    Collectors.counting()
                ));
        }
    }
    
    // ============ WATERMARK ============
    static class Watermark {
        private final AtomicLong watermark = new AtomicLong(0);
        private final Duration maxOutOfOrderness;
        
        Watermark(Duration maxOutOfOrderness) {
            this.maxOutOfOrderness = maxOutOfOrderness;
        }
        
        void update(Instant eventTime) {
            long newWatermark = eventTime.toEpochMilli() - maxOutOfOrderness.toMillis();
            watermark.updateAndGet(current -> Math.max(current, newWatermark));
        }
        
        long get() {
            return watermark.get();
        }
        
        boolean isLate(Instant eventTime) {
            return eventTime.toEpochMilli() < watermark.get();
        }
    }
    
    // ============ STREAM PROCESSOR ============
    static class StreamProcessor {
        private final Map<String, Stream<?>> streams = new ConcurrentHashMap<>();
        
        <T> Stream<T> createStream(String name, int capacity) {
            Stream<T> stream = new Stream<>(capacity);
            streams.put(name, stream);
            return stream;
        }
        
        void shutdown() {
            streams.values().forEach(Stream::shutdown);
        }
    }
    
    // ============ DEMO ============
    public static void main(String[] args) throws Exception {
        System.out.println("=== Real-Time Stream Processing System Demo ===\n");
        
        StreamProcessor processor = new StreamProcessor();
        
        // ============ BASIC TRANSFORMATIONS ============
        System.out.println("=== Basic Transformations ===\n");
        
        Stream<Integer> numbers = processor.createStream("numbers", 100);
        
        Stream<Integer> evenNumbers = numbers.filter(n -> n % 2 == 0);
        Stream<Integer> doubled = evenNumbers.map(n -> n * 2);
        
        doubled.forEach(event -> 
            System.out.printf("Processed: %d (doubled from even number)\n", event.value)
        );
        
        for (int i = 1; i <= 10; i++) {
            numbers.emit(i);
            Thread.sleep(50);
        }
        
        Thread.sleep(500);
        
        // ============ TUMBLING WINDOWS ============
        System.out.println("\n=== Tumbling Window Aggregation (2 second windows) ===\n");
        
        Stream<Double> sensorData = processor.createStream("sensors", 100);
        WindowedStream<Double> tumblingWindow = WindowedStream.tumbling(
            sensorData, Duration.ofSeconds(2)
        );
        
        tumblingWindow.aggregate(Aggregators.count());
        tumblingWindow.aggregate(Aggregators.average());
        tumblingWindow.aggregate(Aggregators.max());
        tumblingWindow.aggregate(Aggregators.min());
        
        // Simulate sensor readings
        Random random = new Random();
        ScheduledExecutorService sensorSimulator = Executors.newScheduledThreadPool(1);
        
        sensorSimulator.scheduleAtFixedRate(() -> {
            try {
                double temp = 20 + random.nextDouble() * 10;
                sensorData.emit(temp);
                System.out.printf("Sensor reading: %.2f°C\n", temp);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 200, TimeUnit.MILLISECONDS);
        
        Thread.sleep(6000);
        sensorSimulator.shutdown();
        
        // ============ KEY-BASED AGGREGATION ============
        System.out.println("\n=== Key-Based Aggregation ===\n");
        
        Stream<String> clicks = processor.createStream("clicks", 100);
        WindowedStream<String> clickWindow = WindowedStream.tumbling(
            clicks, Duration.ofSeconds(2)
        );
        
        clickWindow.aggregate(Aggregators.groupCount(page -> page));
        
        String[] pages = {"home", "about", "products", "contact"};
        for (int i = 0; i < 20; i++) {
            String page = pages[random.nextInt(pages.length)];
            clicks.emit(page);
            System.out.printf("Click on: %s\n", page);
            Thread.sleep(300);
        }
        
        Thread.sleep(3000);
        
        // ============ COMPLEX PIPELINE ============
        System.out.println("\n=== Complex Processing Pipeline ===\n");
        
        Stream<String> events = processor.createStream("events", 100);
        
        events
            .filter(s -> s.length() > 3)
            .map(String::toUpperCase)
            .map(s -> s + "!")
            .forEach(event -> 
                System.out.printf("Pipeline output: %s\n", event.value)
            );
        
        String[] testEvents = {"hi", "hello", "world", "foo", "test", "ok", "great"};
        for (String event : testEvents) {
            events.emit(event);
            Thread.sleep(100);
        }
        
        Thread.sleep(500);
        
        // ============ BACKPRESSURE TEST ============
        System.out.println("\n=== Backpressure Test (small queue) ===\n");
        
        Stream<Integer> smallQueue = new Stream<>(5);
        smallQueue.forEach(event -> {
            try {
                Thread.sleep(100); // Slow consumer
                System.out.printf("Slow consumer processed: %d\n", event.value);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        for (int i = 0; i < 10; i++) {
            System.out.printf("Emitting: %d\n", i);
            smallQueue.emit(i);
        }
        
        Thread.sleep(2000);
        
        // ============ WATERMARK TEST ============
        System.out.println("\n=== Watermark & Late Events ===\n");
        
        Watermark watermark = new Watermark(Duration.ofSeconds(2));
        
        Instant now = Instant.now();
        Instant[] timestamps = {
            now,
            now.minusSeconds(1),
            now.minusSeconds(3), // Late event
            now,
            now.minusSeconds(5)  // Very late event
        };
        
        for (Instant ts : timestamps) {
            watermark.update(now);
            boolean isLate = watermark.isLate(ts);
            System.out.printf("Event at %s is %s (watermark: %s)\n", 
                ts, isLate ? "LATE" : "ON-TIME", 
                Instant.ofEpochMilli(watermark.get()));
        }
        
        // Cleanup
        Thread.sleep(1000);
        System.out.println("\n=== Shutting Down ===");
        
        tumblingWindow.shutdown();
        clickWindow.shutdown();
        processor.shutdown();
        smallQueue.shutdown();
        
        System.out.println("\n=== Demo Complete ===");
    }
}
