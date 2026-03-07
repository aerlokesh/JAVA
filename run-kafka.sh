#!/bin/bash
# Kafka Practice Runner Script

cd "$(dirname "$0")"

echo "📨 Starting Kafka Practice..."
echo ""

echo "⚠️  NOTE: Kafka server must be running separately"
echo "   If not running:"
echo "   1. brew install kafka"
echo "   2. zookeeper-server-start /opt/homebrew/etc/kafka/zookeeper.properties &"
echo "   3. kafka-server-start /opt/homebrew/etc/kafka/server.properties &"
echo ""

echo "📦 Compiling KafkaPractice.java..."

# Compile with all dependencies
javac -cp kafka-clients-3.6.1.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar:. KafkaPractice.java

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful"
    echo "🚀 Running tests..."
    echo ""
    java -cp kafka-clients-3.6.1.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar:. KafkaPractice
else
    echo "❌ Compilation failed"
    exit 1
fi
