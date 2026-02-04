#!/bin/bash
# compile_all.sh - Compile all Java programs

echo "=========================================="
echo "Compiling All Advanced Java Programs"
echo "=========================================="
echo ""

# Array of Java files
files=(
    "MultiThreadedServer.java"
    "DesignPatterns.java"
    "WebFramework.java"
    "DatabaseORM.java"
    "GraphAlgorithms.java"
    "CompressionTool.java"
    "MemoryPool.java"
    "LexerParser.java"
)

compiled=0
failed=0

for file in "${files[@]}"; do
    echo -n "Compiling $file... "
    if javac "$file" 2>/dev/null; then
        echo "✓ SUCCESS"
        ((compiled++))
    else
        echo "✗ FAILED"
        ((failed++))
    fi
done

echo ""
echo "=========================================="
echo "Compilation Summary"
echo "=========================================="
echo "Successfully compiled: $compiled"
echo "Failed: $failed"
echo ""

if [ $failed -eq 0 ]; then
    echo "All programs compiled successfully!"
    echo ""
    echo "You can now run:"
    echo "  java DesignPatterns"
    echo "  java GraphAlgorithms"
    echo "  java CompressionTool demo"
    echo "  java MemoryPool"
    echo "  java LexerParser"
    echo "  java MultiThreadedServer"
    echo "  java WebFramework"
    echo "  java DatabaseORM"
else
    echo "Some programs failed to compile."
    echo "Please check for errors."
fi
