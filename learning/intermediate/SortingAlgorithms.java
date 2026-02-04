/*
 * Sorting Algorithms - Quicksort, mergesort, bubblesort with performance comparison
 * 
 * Compile: javac SortingAlgorithms.java
 * Run: java SortingAlgorithms
 */

import java.util.*;

public class SortingAlgorithms {
    
    // ============ Bubble Sort ============
    // Time: O(n²), Space: O(1)
    public static void bubbleSort(int[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - i - 1; j++) {
                if (arr[j] > arr[j + 1]) {
                    // Swap
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                    swapped = true;
                }
            }
            // If no swap occurred, array is sorted
            if (!swapped) break;
        }
    }
    
    // ============ Quick Sort ============
    // Time: O(n log n) average, O(n²) worst, Space: O(log n)
    public static void quickSort(int[] arr) {
        quickSortHelper(arr, 0, arr.length - 1);
    }
    
    private static void quickSortHelper(int[] arr, int low, int high) {
        if (low < high) {
            int pivotIndex = partition(arr, low, high);
            quickSortHelper(arr, low, pivotIndex - 1);
            quickSortHelper(arr, pivotIndex + 1, high);
        }
    }
    
    private static int partition(int[] arr, int low, int high) {
        // Choose rightmost element as pivot
        int pivot = arr[high];
        int i = low - 1;
        
        for (int j = low; j < high; j++) {
            if (arr[j] <= pivot) {
                i++;
                // Swap arr[i] and arr[j]
                int temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }
        
        // Swap arr[i+1] and arr[high] (pivot)
        int temp = arr[i + 1];
        arr[i + 1] = arr[high];
        arr[high] = temp;
        
        return i + 1;
    }
    
    // ============ Merge Sort ============
    // Time: O(n log n), Space: O(n)
    public static void mergeSort(int[] arr) {
        if (arr.length < 2) return;
        mergeSortHelper(arr, 0, arr.length - 1);
    }
    
    private static void mergeSortHelper(int[] arr, int left, int right) {
        if (left < right) {
            int mid = left + (right - left) / 2;
            
            mergeSortHelper(arr, left, mid);
            mergeSortHelper(arr, mid + 1, right);
            merge(arr, left, mid, right);
        }
    }
    
    private static void merge(int[] arr, int left, int mid, int right) {
        // Create temp arrays
        int n1 = mid - left + 1;
        int n2 = right - mid;
        
        int[] L = new int[n1];
        int[] R = new int[n2];
        
        // Copy data to temp arrays
        for (int i = 0; i < n1; i++)
            L[i] = arr[left + i];
        for (int j = 0; j < n2; j++)
            R[j] = arr[mid + 1 + j];
        
        // Merge the temp arrays back
        int i = 0, j = 0, k = left;
        
        while (i < n1 && j < n2) {
            if (L[i] <= R[j]) {
                arr[k++] = L[i++];
            } else {
                arr[k++] = R[j++];
            }
        }
        
        // Copy remaining elements
        while (i < n1) {
            arr[k++] = L[i++];
        }
        while (j < n2) {
            arr[k++] = R[j++];
        }
    }
    
    // ============ Selection Sort ============
    // Time: O(n²), Space: O(1)
    public static void selectionSort(int[] arr) {
        int n = arr.length;
        
        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < n; j++) {
                if (arr[j] < arr[minIdx]) {
                    minIdx = j;
                }
            }
            
            // Swap minimum element with first element
            int temp = arr[minIdx];
            arr[minIdx] = arr[i];
            arr[i] = temp;
        }
    }
    
    // ============ Insertion Sort ============
    // Time: O(n²), Space: O(1)
    public static void insertionSort(int[] arr) {
        int n = arr.length;
        
        for (int i = 1; i < n; i++) {
            int key = arr[i];
            int j = i - 1;
            
            // Move elements greater than key one position ahead
            while (j >= 0 && arr[j] > key) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = key;
        }
    }
    
    // Helper methods
    private static int[] copyArray(int[] arr) {
        return Arrays.copyOf(arr, arr.length);
    }
    
    private static void printArray(int[] arr, int limit) {
        System.out.print("[");
        for (int i = 0; i < Math.min(limit, arr.length); i++) {
            System.out.print(arr[i]);
            if (i < Math.min(limit, arr.length) - 1) System.out.print(", ");
        }
        if (arr.length > limit) System.out.print("...");
        System.out.println("]");
    }
    
    private static boolean isSorted(int[] arr) {
        for (int i = 0; i < arr.length - 1; i++) {
            if (arr[i] > arr[i + 1]) return false;
        }
        return true;
    }
    
    private static long measureSort(String name, int[] arr, Runnable sortFunc) {
        System.out.println("\n" + name + ":");
        System.out.print("  Before: ");
        printArray(arr, 10);
        
        long startTime = System.nanoTime();
        sortFunc.run();
        long endTime = System.nanoTime();
        
        System.out.print("  After:  ");
        printArray(arr, 10);
        
        long duration = (endTime - startTime) / 1000; // Convert to microseconds
        System.out.println("  Time: " + duration + " μs");
        System.out.println("  Sorted: " + isSorted(arr));
        
        return duration;
    }
    
    public static void main(String[] args) {
        System.out.println("=== Sorting Algorithms Demo ===\n");
        
        // Test with small array first
        System.out.println("Test 1: Small Array (10 elements)");
        System.out.println("=".repeat(50));
        
        int[] smallArray = {64, 34, 25, 12, 22, 11, 90, 88, 45, 50};
        
        int[] arr1 = copyArray(smallArray);
        measureSort("Bubble Sort", arr1, () -> bubbleSort(arr1));
        
        int[] arr2 = copyArray(smallArray);
        measureSort("Selection Sort", arr2, () -> selectionSort(arr2));
        
        int[] arr3 = copyArray(smallArray);
        measureSort("Insertion Sort", arr3, () -> insertionSort(arr3));
        
        int[] arr4 = copyArray(smallArray);
        measureSort("Quick Sort", arr4, () -> quickSort(arr4));
        
        int[] arr5 = copyArray(smallArray);
        measureSort("Merge Sort", arr5, () -> mergeSort(arr5));
        
        // Performance comparison with larger array
        System.out.println("\n\nTest 2: Performance Comparison (1000 elements)");
        System.out.println("=".repeat(50));
        
        Random rand = new Random(42);
        int[] largeArray = new int[1000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = rand.nextInt(1000);
        }
        
        Map<String, Long> results = new TreeMap<>();
        
        int[] test1 = copyArray(largeArray);
        results.put("Bubble Sort", measureSort("Bubble Sort", test1, () -> bubbleSort(test1)));
        
        int[] test2 = copyArray(largeArray);
        results.put("Selection Sort", measureSort("Selection Sort", test2, () -> selectionSort(test2)));
        
        int[] test3 = copyArray(largeArray);
        results.put("Insertion Sort", measureSort("Insertion Sort", test3, () -> insertionSort(test3)));
        
        int[] test4 = copyArray(largeArray);
        results.put("Quick Sort", measureSort("Quick Sort", test4, () -> quickSort(test4)));
        
        int[] test5 = copyArray(largeArray);
        results.put("Merge Sort", measureSort("Merge Sort", test5, () -> mergeSort(test5)));
        
        // Summary
        System.out.println("\n\nPerformance Summary (1000 elements):");
        System.out.println("=".repeat(50));
        
        List<Map.Entry<String, Long>> sortedResults = new ArrayList<>(results.entrySet());
        sortedResults.sort(Map.Entry.comparingByValue());
        
        for (int i = 0; i < sortedResults.size(); i++) {
            Map.Entry<String, Long> entry = sortedResults.get(i);
            System.out.printf("%d. %-20s %,10d μs\n", i + 1, entry.getKey(), entry.getValue());
        }
        
        // Edge cases
        System.out.println("\n\nTest 3: Edge Cases");
        System.out.println("=".repeat(50));
        
        // Already sorted
        int[] sorted = {1, 2, 3, 4, 5};
        int[] sortedTest = copyArray(sorted);
        measureSort("Quick Sort (already sorted)", sortedTest, () -> quickSort(sortedTest));
        
        // Reverse sorted
        int[] reverse = {5, 4, 3, 2, 1};
        int[] reverseTest = copyArray(reverse);
        measureSort("Quick Sort (reverse sorted)", reverseTest, () -> quickSort(reverseTest));
        
        // All same elements
        int[] same = {7, 7, 7, 7, 7};
        int[] sameTest = copyArray(same);
        measureSort("Quick Sort (all same)", sameTest, () -> quickSort(sameTest));
        
        System.out.println("\n=== Demo Complete ===");
    }
}
