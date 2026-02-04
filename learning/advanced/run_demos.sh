#!/bin/bash
# run_demos.sh - Run demonstrations of all programs

echo "=========================================="
echo "Advanced Java Programs - Demo Runner"
echo "=========================================="
echo ""
echo "This script will run demonstrations of all programs."
echo "Some programs may run indefinitely (servers) - use Ctrl+C to stop."
echo ""

# Function to run a demo
run_demo() {
    local name=$1
    local class=$2
    local args=$3
    
    echo ""
    echo "=========================================="
    echo "Running: $name"
    echo "=========================================="
    echo ""
    
    if [ -n "$args" ]; then
        java $class $args
    else
        java $class
    fi
    
    echo ""
    read -p "Press Enter to continue to next demo..."
}

# Check if compiled
if [ ! -f "DesignPatterns.class" ]; then
    echo "Programs not compiled yet. Running compile_all.sh..."
    ./compile_all.sh
    echo ""
    read -p "Press Enter to continue to demos..."
fi

# Run demos
run_demo "Design Patterns" "DesignPatterns"
run_demo "Graph Algorithms" "GraphAlgorithms"
run_demo "Compression Tool" "CompressionTool" "demo"
run_demo "Memory Pool" "MemoryPool"
run_demo "Lexer Parser" "LexerParser"
run_demo "Database ORM" "DatabaseORM"

echo ""
echo "=========================================="
echo "Interactive/Server Programs"
echo "=========================================="
echo ""
echo "The following programs are interactive or run as servers:"
echo ""
echo "1. MultiThreadedServer - TCP server (use Ctrl+C to stop)"
echo "   Run: java MultiThreadedServer"
echo "   Test: telnet localhost 8080"
echo ""
echo "2. WebFramework - HTTP server (use Ctrl+C to stop)"
echo "   Run: java WebFramework"
echo "   Test: curl http://localhost:3000/"
echo ""
echo "Run these manually when needed."
echo ""
echo "=========================================="
echo "All Demos Complete!"
echo "=========================================="
