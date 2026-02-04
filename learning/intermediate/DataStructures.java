/*
 * Data Structures - Linked list, BST, hash map with full implementations
 * 
 * Compile: javac DataStructures.java
 * Run: java DataStructures
 */

public class DataStructures {
    
    // ============ Singly Linked List ============
    static class LinkedList<T> {
        private class Node {
            T data;
            Node next;
            
            Node(T data) {
                this.data = data;
                this.next = null;
            }
        }
        
        private Node head;
        private int size;
        
        public LinkedList() {
            this.head = null;
            this.size = 0;
        }
        
        // Add element to the end
        public void add(T data) {
            Node newNode = new Node(data);
            if (head == null) {
                head = newNode;
            } else {
                Node current = head;
                while (current.next != null) {
                    current = current.next;
                }
                current.next = newNode;
            }
            size++;
        }
        
        // Add element at beginning
        public void addFirst(T data) {
            Node newNode = new Node(data);
            newNode.next = head;
            head = newNode;
            size++;
        }
        
        // Remove element by value
        public boolean remove(T data) {
            if (head == null) return false;
            
            if (head.data.equals(data)) {
                head = head.next;
                size--;
                return true;
            }
            
            Node current = head;
            while (current.next != null) {
                if (current.next.data.equals(data)) {
                    current.next = current.next.next;
                    size--;
                    return true;
                }
                current = current.next;
            }
            return false;
        }
        
        // Check if list contains element
        public boolean contains(T data) {
            Node current = head;
            while (current != null) {
                if (current.data.equals(data)) {
                    return true;
                }
                current = current.next;
            }
            return false;
        }
        
        public int size() {
            return size;
        }
        
        public void print() {
            Node current = head;
            System.out.print("[");
            while (current != null) {
                System.out.print(current.data);
                if (current.next != null) System.out.print(" -> ");
                current = current.next;
            }
            System.out.println("]");
        }
    }
    
    // ============ Binary Search Tree ============
    static class BST<T extends Comparable<T>> {
        private class Node {
            T data;
            Node left, right;
            
            Node(T data) {
                this.data = data;
                this.left = this.right = null;
            }
        }
        
        private Node root;
        
        public BST() {
            this.root = null;
        }
        
        // Insert element
        public void insert(T data) {
            root = insertRec(root, data);
        }
        
        private Node insertRec(Node node, T data) {
            if (node == null) {
                return new Node(data);
            }
            
            if (data.compareTo(node.data) < 0) {
                node.left = insertRec(node.left, data);
            } else if (data.compareTo(node.data) > 0) {
                node.right = insertRec(node.right, data);
            }
            
            return node;
        }
        
        // Search for element
        public boolean search(T data) {
            return searchRec(root, data);
        }
        
        private boolean searchRec(Node node, T data) {
            if (node == null) return false;
            
            if (data.equals(node.data)) return true;
            
            if (data.compareTo(node.data) < 0) {
                return searchRec(node.left, data);
            } else {
                return searchRec(node.right, data);
            }
        }
        
        // Delete element
        public void delete(T data) {
            root = deleteRec(root, data);
        }
        
        private Node deleteRec(Node node, T data) {
            if (node == null) return null;
            
            if (data.compareTo(node.data) < 0) {
                node.left = deleteRec(node.left, data);
            } else if (data.compareTo(node.data) > 0) {
                node.right = deleteRec(node.right, data);
            } else {
                // Node with one child or no child
                if (node.left == null) return node.right;
                if (node.right == null) return node.left;
                
                // Node with two children: get inorder successor
                node.data = minValue(node.right);
                node.right = deleteRec(node.right, node.data);
            }
            
            return node;
        }
        
        private T minValue(Node node) {
            T minv = node.data;
            while (node.left != null) {
                minv = node.left.data;
                node = node.left;
            }
            return minv;
        }
        
        // In-order traversal
        public void inOrder() {
            inOrderRec(root);
            System.out.println();
        }
        
        private void inOrderRec(Node node) {
            if (node != null) {
                inOrderRec(node.left);
                System.out.print(node.data + " ");
                inOrderRec(node.right);
            }
        }
        
        // Calculate height
        public int height() {
            return heightRec(root);
        }
        
        private int heightRec(Node node) {
            if (node == null) return 0;
            return 1 + Math.max(heightRec(node.left), heightRec(node.right));
        }
    }
    
    // ============ Hash Map (Simple Implementation) ============
    static class HashMap<K, V> {
        private class Entry {
            K key;
            V value;
            Entry next;
            
            Entry(K key, V value) {
                this.key = key;
                this.value = value;
                this.next = null;
            }
        }
        
        private Entry[] buckets;
        private int capacity;
        private int size;
        
        @SuppressWarnings("unchecked")
        public HashMap(int capacity) {
            this.capacity = capacity;
            this.buckets = new Entry[capacity];
            this.size = 0;
        }
        
        public HashMap() {
            this(16);
        }
        
        // Hash function
        private int hash(K key) {
            return Math.abs(key.hashCode() % capacity);
        }
        
        // Put key-value pair
        public void put(K key, V value) {
            int index = hash(key);
            Entry newEntry = new Entry(key, value);
            
            if (buckets[index] == null) {
                buckets[index] = newEntry;
                size++;
            } else {
                Entry current = buckets[index];
                Entry prev = null;
                
                while (current != null) {
                    if (current.key.equals(key)) {
                        current.value = value; // Update existing
                        return;
                    }
                    prev = current;
                    current = current.next;
                }
                
                prev.next = newEntry;
                size++;
            }
        }
        
        // Get value by key
        public V get(K key) {
            int index = hash(key);
            Entry current = buckets[index];
            
            while (current != null) {
                if (current.key.equals(key)) {
                    return current.value;
                }
                current = current.next;
            }
            
            return null;
        }
        
        // Remove key-value pair
        public boolean remove(K key) {
            int index = hash(key);
            Entry current = buckets[index];
            Entry prev = null;
            
            while (current != null) {
                if (current.key.equals(key)) {
                    if (prev == null) {
                        buckets[index] = current.next;
                    } else {
                        prev.next = current.next;
                    }
                    size--;
                    return true;
                }
                prev = current;
                current = current.next;
            }
            
            return false;
        }
        
        // Check if key exists
        public boolean containsKey(K key) {
            return get(key) != null;
        }
        
        public int size() {
            return size;
        }
        
        public void print() {
            System.out.println("HashMap contents:");
            for (int i = 0; i < capacity; i++) {
                if (buckets[i] != null) {
                    Entry current = buckets[i];
                    System.out.print("Bucket " + i + ": ");
                    while (current != null) {
                        System.out.print(current.key + "=" + current.value);
                        if (current.next != null) System.out.print(", ");
                        current = current.next;
                    }
                    System.out.println();
                }
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Data Structures Demo ===\n");
        
        // ========== Linked List Demo ==========
        System.out.println("1. LINKED LIST");
        System.out.println("-".repeat(40));
        
        LinkedList<Integer> list = new LinkedList<>();
        list.add(10);
        list.add(20);
        list.add(30);
        list.addFirst(5);
        
        System.out.print("List: ");
        list.print();
        System.out.println("Size: " + list.size());
        System.out.println("Contains 20: " + list.contains(20));
        
        list.remove(20);
        System.out.print("After removing 20: ");
        list.print();
        
        // ========== BST Demo ==========
        System.out.println("\n2. BINARY SEARCH TREE");
        System.out.println("-".repeat(40));
        
        BST<Integer> bst = new BST<>();
        bst.insert(50);
        bst.insert(30);
        bst.insert(70);
        bst.insert(20);
        bst.insert(40);
        bst.insert(60);
        bst.insert(80);
        
        System.out.print("In-order traversal: ");
        bst.inOrder();
        System.out.println("Height: " + bst.height());
        System.out.println("Search 40: " + bst.search(40));
        System.out.println("Search 100: " + bst.search(100));
        
        bst.delete(30);
        System.out.print("After deleting 30: ");
        bst.inOrder();
        
        // ========== HashMap Demo ==========
        System.out.println("\n3. HASH MAP");
        System.out.println("-".repeat(40));
        
        HashMap<String, Integer> map = new HashMap<>();
        map.put("Alice", 25);
        map.put("Bob", 30);
        map.put("Charlie", 35);
        map.put("David", 28);
        
        map.print();
        System.out.println("\nSize: " + map.size());
        System.out.println("Get 'Bob': " + map.get("Bob"));
        System.out.println("Contains 'Alice': " + map.containsKey("Alice"));
        
        map.remove("Bob");
        System.out.println("\nAfter removing 'Bob':");
        map.print();
        
        System.out.println("\n=== Demo Complete ===");
    }
}
