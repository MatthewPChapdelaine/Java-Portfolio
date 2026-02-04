/*
 * File Processor - CSV processing, data aggregation, statistics, report generation
 * 
 * Compile: javac FileProcessor.java
 * Run: java FileProcessor
 */

import java.io.*;
import java.util.*;

public class FileProcessor {
    
    // CSV Row representation
    static class CsvRow {
        Map<String, String> data;
        
        CsvRow(String[] headers, String[] values) {
            data = new LinkedHashMap<>();
            for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                data.put(headers[i], values[i]);
            }
        }
        
        String get(String column) {
            return data.get(column);
        }
        
        double getDouble(String column) {
            try {
                return Double.parseDouble(data.get(column));
            } catch (Exception e) {
                return 0.0;
            }
        }
        
        int getInt(String column) {
            try {
                return Integer.parseInt(data.get(column));
            } catch (Exception e) {
                return 0;
            }
        }
    }
    
    // CSV Reader
    static class CsvReader {
        private String[] headers;
        private List<CsvRow> rows;
        
        public CsvReader(String filename) throws IOException {
            rows = new ArrayList<>();
            
            try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
                String line = br.readLine();
                if (line != null) {
                    headers = parseCsvLine(line);
                }
                
                while ((line = br.readLine()) != null) {
                    String[] values = parseCsvLine(line);
                    rows.add(new CsvRow(headers, values));
                }
            }
        }
        
        private String[] parseCsvLine(String line) {
            List<String> result = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inQuotes = false;
            
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                
                if (c == '"') {
                    inQuotes = !inQuotes;
                } else if (c == ',' && !inQuotes) {
                    result.add(current.toString().trim());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
            result.add(current.toString().trim());
            
            return result.toArray(new String[0]);
        }
        
        public String[] getHeaders() {
            return headers;
        }
        
        public List<CsvRow> getRows() {
            return rows;
        }
        
        public int getRowCount() {
            return rows.size();
        }
    }
    
    // Statistics Calculator
    static class Statistics {
        private List<Double> values;
        
        public Statistics(List<Double> values) {
            this.values = new ArrayList<>(values);
            Collections.sort(this.values);
        }
        
        public double mean() {
            if (values.isEmpty()) return 0;
            double sum = 0;
            for (double v : values) sum += v;
            return sum / values.size();
        }
        
        public double median() {
            if (values.isEmpty()) return 0;
            int n = values.size();
            if (n % 2 == 0) {
                return (values.get(n/2 - 1) + values.get(n/2)) / 2.0;
            } else {
                return values.get(n/2);
            }
        }
        
        public double min() {
            return values.isEmpty() ? 0 : values.get(0);
        }
        
        public double max() {
            return values.isEmpty() ? 0 : values.get(values.size() - 1);
        }
        
        public double sum() {
            double total = 0;
            for (double v : values) total += v;
            return total;
        }
        
        public double stdDev() {
            if (values.size() < 2) return 0;
            double avg = mean();
            double sumSquaredDiff = 0;
            for (double v : values) {
                double diff = v - avg;
                sumSquaredDiff += diff * diff;
            }
            return Math.sqrt(sumSquaredDiff / values.size());
        }
    }
    
    // Report Generator
    static class ReportGenerator {
        private StringBuilder report;
        
        public ReportGenerator(String title) {
            report = new StringBuilder();
            report.append("=".repeat(60)).append("\n");
            report.append(title).append("\n");
            report.append("=".repeat(60)).append("\n\n");
        }
        
        public void addSection(String title) {
            report.append("\n").append(title).append("\n");
            report.append("-".repeat(40)).append("\n");
        }
        
        public void addLine(String line) {
            report.append(line).append("\n");
        }
        
        public void addKeyValue(String key, Object value) {
            report.append(String.format("%-25s: %s\n", key, value));
        }
        
        public void addTable(String[] headers, List<String[]> rows) {
            // Calculate column widths
            int[] widths = new int[headers.length];
            for (int i = 0; i < headers.length; i++) {
                widths[i] = headers[i].length();
            }
            for (String[] row : rows) {
                for (int i = 0; i < Math.min(row.length, widths.length); i++) {
                    widths[i] = Math.max(widths[i], row[i].length());
                }
            }
            
            // Print headers
            for (int i = 0; i < headers.length; i++) {
                report.append(String.format("%-" + (widths[i] + 2) + "s", headers[i]));
            }
            report.append("\n");
            
            // Print separator
            for (int i = 0; i < headers.length; i++) {
                report.append("-".repeat(widths[i] + 2));
            }
            report.append("\n");
            
            // Print rows
            for (String[] row : rows) {
                for (int i = 0; i < Math.min(row.length, widths.length); i++) {
                    report.append(String.format("%-" + (widths[i] + 2) + "s", row[i]));
                }
                report.append("\n");
            }
        }
        
        public String getReport() {
            return report.toString();
        }
        
        public void saveToFile(String filename) throws IOException {
            try (PrintWriter writer = new PrintWriter(filename)) {
                writer.print(report.toString());
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== File Processor Demo ===\n");
        
        try {
            // Create sample CSV file
            String csvFile = "sales_data.csv";
            System.out.println("Creating sample CSV file: " + csvFile);
            
            try (PrintWriter writer = new PrintWriter(csvFile)) {
                writer.println("Date,Product,Category,Quantity,Price,Total");
                writer.println("2024-01-01,Laptop,Electronics,5,999.99,4999.95");
                writer.println("2024-01-02,Mouse,Electronics,20,29.99,599.80");
                writer.println("2024-01-03,Keyboard,Electronics,15,79.99,1199.85");
                writer.println("2024-01-04,Monitor,Electronics,8,299.99,2399.92");
                writer.println("2024-01-05,Desk,Furniture,10,399.99,3999.90");
                writer.println("2024-01-06,Chair,Furniture,12,249.99,2999.88");
                writer.println("2024-01-07,Lamp,Furniture,25,49.99,1249.75");
                writer.println("2024-01-08,Webcam,Electronics,18,89.99,1619.82");
                writer.println("2024-01-09,Headset,Electronics,30,59.99,1799.70");
                writer.println("2024-01-10,Notebook,Office,100,4.99,499.00");
            }
            
            // Read CSV file
            System.out.println("Reading CSV file...\n");
            CsvReader reader = new CsvReader(csvFile);
            
            System.out.println("Loaded " + reader.getRowCount() + " rows");
            System.out.println("Columns: " + String.join(", ", reader.getHeaders()));
            
            // Process data and calculate statistics
            System.out.println("\nProcessing data...");
            
            List<CsvRow> rows = reader.getRows();
            
            // Aggregate by category
            Map<String, Double> categoryTotals = new HashMap<>();
            Map<String, Integer> categoryCount = new HashMap<>();
            List<Double> allTotals = new ArrayList<>();
            
            for (CsvRow row : rows) {
                String category = row.get("Category");
                double total = row.getDouble("Total");
                
                categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + total);
                categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
                allTotals.add(total);
            }
            
            // Calculate statistics
            Statistics stats = new Statistics(allTotals);
            
            // Generate report
            ReportGenerator report = new ReportGenerator("SALES DATA ANALYSIS REPORT");
            
            report.addSection("Overall Statistics");
            report.addKeyValue("Total Records", rows.size());
            report.addKeyValue("Total Revenue", String.format("$%.2f", stats.sum()));
            report.addKeyValue("Average Sale", String.format("$%.2f", stats.mean()));
            report.addKeyValue("Median Sale", String.format("$%.2f", stats.median()));
            report.addKeyValue("Minimum Sale", String.format("$%.2f", stats.min()));
            report.addKeyValue("Maximum Sale", String.format("$%.2f", stats.max()));
            report.addKeyValue("Std Deviation", String.format("$%.2f", stats.stdDev()));
            
            report.addSection("Sales by Category");
            List<String[]> categoryTable = new ArrayList<>();
            for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
                String category = entry.getKey();
                double total = entry.getValue();
                int count = categoryCount.get(category);
                double avg = total / count;
                
                categoryTable.add(new String[] {
                    category,
                    String.valueOf(count),
                    String.format("$%.2f", total),
                    String.format("$%.2f", avg)
                });
            }
            report.addTable(
                new String[] {"Category", "Count", "Total", "Average"},
                categoryTable
            );
            
            report.addSection("Top 5 Sales");
            List<String[]> topSales = new ArrayList<>();
            rows.sort((a, b) -> Double.compare(b.getDouble("Total"), a.getDouble("Total")));
            for (int i = 0; i < Math.min(5, rows.size()); i++) {
                CsvRow row = rows.get(i);
                topSales.add(new String[] {
                    row.get("Date"),
                    row.get("Product"),
                    row.get("Quantity"),
                    String.format("$%.2f", row.getDouble("Total"))
                });
            }
            report.addTable(
                new String[] {"Date", "Product", "Qty", "Total"},
                topSales
            );
            
            // Display report
            System.out.println("\n" + report.getReport());
            
            // Save report to file
            String reportFile = "sales_report.txt";
            report.saveToFile(reportFile);
            System.out.println("Report saved to: " + reportFile);
            
            System.out.println("\n=== Demo Complete ===");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
