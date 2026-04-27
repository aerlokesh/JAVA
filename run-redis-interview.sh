#!/bin/bash
# Redis Interview Practice Runner Script

cd "$(dirname "$0")"

echo "🎯 Starting Redis Interview Practice (HLD Use Cases)..."
echo ""

# Check if Redis is running
if ! redis-cli ping > /dev/null 2>&1; then
    echo "Starting Redis server..."
    redis-server --daemonize yes
    sleep 1
fi

echo "✅ Redis server is running"
echo "📦 Compiling RedisInterviewPractice.java..."

# Compile with all dependencies
javac -cp jedis-5.1.0.jar:slf4j-api-2.0.9.jar:commons-pool2-2.12.0.jar:. RedisInterviewPractice.java

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful"
    echo "🚀 Running HLD use case tests..."
    echo ""
    java -cp jedis-5.1.0.jar:slf4j-api-2.0.9.jar:commons-pool2-2.12.0.jar:slf4j-simple-2.0.9.jar:. RedisInterviewPractice
else
    echo "❌ Compilation failed"
    exit 1
fi
