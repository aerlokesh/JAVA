/**
 * KafkaPractice.java
 * 
 * A comprehensive Java practice file for mastering Apache Kafka operations.
 * Complete all the TODO methods below and run the main method to test your solutions.
 * 
 * QUICK START (Single Command):
 * ============================
 * ./run-kafka.sh
 * 
 * This script automatically:
 * - Downloads Kafka client libraries
 * - Compiles with all dependencies
 * - Runs the test suite
 * 
 * Note: You need Kafka server running separately
 * Install: brew install kafka (Mac)
 * Start Zookeeper: zookeeper-server-start /opt/homebrew/etc/kafka/zookeeper.properties &
 * Start Kafka: kafka-server-start /opt/homebrew/etc/kafka/server.properties &
 * 
 * Instructions:
 * 1. Read each method's description carefully
 * 2. Replace the "TODO: Implement this method" with your solution
 * 3. All methods use Kafka Producer/Consumer APIs
 * 4. Run ./run-kafka.sh to test your solutions
 */

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public class KafkaPractice {
    
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TEST_TOPIC = "test-topic";
    
    // ==================== PRODUCER OPERATIONS ====================
    
    /**
     * Task 1: Create a Kafka Producer
     * @return KafkaProducer<String, String>
     */
    public static KafkaProducer<String, String> createProducer() {
        // TODO: Create Properties, set bootstrap.servers, key/value serializers
        // Use StringSerializer for both key and value
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }
    
    /**
     * Task 2: Send a simple message (fire and forget)
     * @param producer The producer
     * @param topic Topic name
     * @param key Message key
     * @param value Message value
     */
    public static void sendMessage(KafkaProducer<String, String> producer, String topic, String key, String value) {
        // TODO: Create ProducerRecord and send using producer.send()
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        producer.send(record);
    }
    
    /**
     * Task 3: Send message synchronously (wait for response)
     * @param producer The producer
     * @param topic Topic name
     * @param key Message key
     * @param value Message value
     * @return RecordMetadata with offset, partition info
     */
    public static RecordMetadata sendMessageSync(KafkaProducer<String, String> producer, String topic, String key, String value) {
        // TODO: Send message and call .get() on Future to wait for result
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
            return producer.send(record).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Task 4: Send message with callback
     * @param producer The producer
     * @param topic Topic name
     * @param key Message key
     * @param value Message value
     * @param onSuccess Success message consumer
     * @param onError Error consumer
     */
    public static void sendMessageAsync(KafkaProducer<String, String> producer, String topic, String key, String value,
                                        java.util.function.Consumer<RecordMetadata> onSuccess,
                                        java.util.function.Consumer<Exception> onError) {
        // TODO: Send with Callback that handles success/error
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        producer.send(record, new Callback() {
            @Override
            public void onCompletion(RecordMetadata metadata, Exception exception) {
                if (exception == null) {
                    onSuccess.accept(metadata);
                } else {
                    onError.accept(exception);
                }
            }
        });
    }
    
    /**
     * Task 5: Send batch of messages
     * @param producer The producer
     * @param topic Topic name
     * @param messages Map of key-value pairs
     * @return Number of messages sent
     */
    public static int sendBatch(KafkaProducer<String, String> producer, String topic, Map<String, String> messages) {
        // TODO: Loop through messages and send each one
        int count = 0;
        for (Map.Entry<String, String> entry : messages.entrySet()) {
            sendMessage(producer, topic, entry.getKey(), entry.getValue());
            count++;
        }
        return count;
    }
    
    // ==================== CONSUMER OPERATIONS ====================
    
    /**
     * Task 6: Create a Kafka Consumer
     * @param groupId Consumer group ID
     * @return KafkaConsumer<String, String>
     */
    public static KafkaConsumer<String, String> createConsumer(String groupId) {
        // TODO: Create Properties, set bootstrap.servers, group.id, key/value deserializers
        // Use StringDeserializer, set auto.offset.reset to "earliest"
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(props);
    }
    
    /**
     * Task 7: Subscribe to topic(s)
     * @param consumer The consumer
     * @param topics List of topic names
     */
    public static void subscribe(KafkaConsumer<String, String> consumer, List<String> topics) {
        // TODO: Use consumer.subscribe()
        consumer.subscribe(topics);
    }
    
    /**
     * Task 8: Poll messages (single batch)
     * @param consumer The consumer
     * @param timeoutMs Timeout in milliseconds
     * @return List of records
     */
    public static List<ConsumerRecord<String, String>> pollMessages(KafkaConsumer<String, String> consumer, long timeoutMs) {
        // TODO: Use consumer.poll() and convert ConsumerRecords to List
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(timeoutMs));
        List<ConsumerRecord<String, String>> list = new ArrayList<>();
        for (ConsumerRecord<String, String> record : records) {
            list.add(record);
        }
        return list;
    }
    
    /**
     * Task 9: Commit offsets manually
     * @param consumer The consumer
     */
    public static void commitSync(KafkaConsumer<String, String> consumer) {
        // TODO: Use consumer.commitSync()
        consumer.commitSync();
    }
    
    /**
     * Task 10: Seek to beginning of partitions
     * @param consumer The consumer
     */
    public static void seekToBeginning(KafkaConsumer<String, String> consumer) {
        // TODO: Use consumer.seekToBeginning() with consumer.assignment()
        consumer.seekToBeginning(consumer.assignment());
    }
    
    // ==================== ADMIN OPERATIONS ====================
    
    /**
     * Task 11: Create Admin Client
     * @return AdminClient
     */
    public static AdminClient createAdminClient() {
        // TODO: Create Properties with bootstrap.servers, then AdminClient.create()
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        return AdminClient.create(props);
    }
    
    /**
     * Task 12: Create a topic
     * @param admin Admin client
     * @param topicName Topic name
     * @param numPartitions Number of partitions
     * @param replicationFactor Replication factor
     */
    public static void createTopic(AdminClient admin, String topicName, int numPartitions, short replicationFactor) {
        // TODO: Create NewTopic and use admin.createTopics()
        try {
            NewTopic newTopic = new NewTopic(topicName, numPartitions, replicationFactor);
            admin.createTopics(Collections.singletonList(newTopic)).all().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Task 13: List all topics
     * @param admin Admin client
     * @return Set of topic names
     */
    public static Set<String> listTopics(AdminClient admin) {
        // TODO: Use admin.listTopics().names().get()
        try {
            return admin.listTopics().names().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Task 14: Delete a topic
     * @param admin Admin client
     * @param topicName Topic name
     */
    public static void deleteTopic(AdminClient admin, String topicName) {
        // TODO: Use admin.deleteTopics()
        try {
            admin.deleteTopics(Collections.singletonList(topicName)).all().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Task 15: Describe topic
     * @param admin Admin client
     * @param topicName Topic name
     * @return TopicDescription
     */
    public static TopicDescription describeTopic(AdminClient admin, String topicName) {
        // TODO: Use admin.describeTopics()
        try {
            return admin.describeTopics(Collections.singletonList(topicName)).all().get().get(topicName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Helper: Wait for messages to be available
     */
    private static void waitForMessages(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // ==================== TEST CASES ====================
    
    public static void main(String[] args) {
        int totalTests = 0;
        int passedTests = 0;
        
        System.out.println("=".repeat(60));
        System.out.println("KAFKA PRACTICE - TEST SUITE");
        System.out.println("=".repeat(60));
        System.out.println("\nConnecting to Kafka at " + BOOTSTRAP_SERVERS + "...");
        
        AdminClient admin = null;
        KafkaProducer<String, String> producer = null;
        KafkaConsumer<String, String> consumer = null;
        
        try {
            // Test 1: Create Producer
            System.out.println("\n[Test 1] Create Producer");
            producer = createProducer();
            totalTests++;
            if (producer != null) {
                System.out.println("  ✓ PASS: Producer created");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Producer is null");
            }
            
            // Test 11: Create Admin Client
            System.out.println("\n[Test 11] Create Admin Client");
            admin = createAdminClient();
            totalTests++;
            if (admin != null) {
                System.out.println("  ✓ PASS: Admin client created");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Admin client is null");
            }
            
            if (admin == null || producer == null) {
                System.out.println("\n✗ Cannot proceed without admin client and producer");
                return;
            }
            
            // Test 12: Create Topic
            System.out.println("\n[Test 12] Create Topic");
            try {
                createTopic(admin, TEST_TOPIC, 3, (short)1);
                waitForMessages(1000); // Wait for topic creation
                System.out.println("  ✓ PASS: Topic created");
                totalTests++;
                passedTests++;
            } catch (Exception e) {
                System.out.println("  ⚠ SKIP: Topic might already exist");
                totalTests++;
                passedTests++; // Don't fail if topic exists
            }
            
            // Test 13: List Topics
            System.out.println("\n[Test 13] List Topics");
            Set<String> topics = listTopics(admin);
            totalTests++;
            if (topics != null && topics.contains(TEST_TOPIC)) {
                System.out.println("  ✓ PASS: Found " + topics.size() + " topics including " + TEST_TOPIC);
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: List topics");
            }
            
            // Test 15: Describe Topic
            System.out.println("\n[Test 15] Describe Topic");
            TopicDescription desc = describeTopic(admin, TEST_TOPIC);
            totalTests++;
            if (desc != null && desc.partitions().size() == 3) {
                System.out.println("  ✓ PASS: Topic has 3 partitions");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Describe topic");
            }
            
            // Test 2: Send Message
            System.out.println("\n[Test 2] Send Message");
            try {
                sendMessage(producer, TEST_TOPIC, "key1", "Hello Kafka");
                producer.flush();
                System.out.println("  ✓ PASS: Message sent");
                totalTests++;
                passedTests++;
            } catch (Exception e) {
                System.out.println("  ✗ FAIL: " + e.getMessage());
                totalTests++;
            }
            
            // Test 3: Send Message Synchronously
            System.out.println("\n[Test 3] Send Message Sync");
            try {
                RecordMetadata metadata = sendMessageSync(producer, TEST_TOPIC, "key2", "Sync Message");
                totalTests++;
                if (metadata != null && metadata.offset() >= 0) {
                    System.out.println("  ✓ PASS: Sync send => offset: " + metadata.offset());
                    passedTests++;
                } else {
                    System.out.println("  ✗ FAIL: Send message sync");
                }
            } catch (Exception e) {
                System.out.println("  ✗ FAIL: " + e.getMessage());
                totalTests++;
            }
            
            // Test 5: Send Batch
            System.out.println("\n[Test 5] Send Batch Messages");
            Map<String, String> batch = new LinkedHashMap<>();
            batch.put("batch1", "Message 1");
            batch.put("batch2", "Message 2");
            batch.put("batch3", "Message 3");
            int sent = sendBatch(producer, TEST_TOPIC, batch);
            producer.flush();
            totalTests++;
            if (sent == 3) {
                System.out.println("  ✓ PASS: Sent 3 messages");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Expected 3, sent: " + sent);
            }
            
            // Wait for messages to be available
            waitForMessages(2000);
            
            // Test 6: Create Consumer
            System.out.println("\n[Test 6] Create Consumer");
            consumer = createConsumer("test-group");
            totalTests++;
            if (consumer != null) {
                System.out.println("  ✓ PASS: Consumer created");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Consumer is null");
            }
            
            if (consumer == null) {
                System.out.println("\n✗ Cannot proceed without consumer");
                return;
            }
            
            // Test 7: Subscribe
            System.out.println("\n[Test 7] Subscribe to Topic");
            try {
                subscribe(consumer, Arrays.asList(TEST_TOPIC));
                System.out.println("  ✓ PASS: Subscribed to " + TEST_TOPIC);
                totalTests++;
                passedTests++;
            } catch (Exception e) {
                System.out.println("  ✗ FAIL: " + e.getMessage());
                totalTests++;
            }
            
            // Test 8: Poll Messages
            System.out.println("\n[Test 8] Poll Messages");
            List<ConsumerRecord<String, String>> records = pollMessages(consumer, 5000);
            totalTests++;
            if (records != null && records.size() >= 5) {
                System.out.println("  ✓ PASS: Polled " + records.size() + " messages");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Expected >= 5 messages, got: " + (records != null ? records.size() : 0));
            }
            
            // Test 10: Seek to Beginning
            System.out.println("\n[Test 10] Seek to Beginning");
            try {
                seekToBeginning(consumer);
                System.out.println("  ✓ PASS: Seeked to beginning");
                totalTests++;
                passedTests++;
            } catch (Exception e) {
                System.out.println("  ✗ FAIL: " + e.getMessage());
                totalTests++;
            }
            
            // Test 9: Commit Offsets
            System.out.println("\n[Test 9] Commit Offsets");
            try {
                commitSync(consumer);
                System.out.println("  ✓ PASS: Offsets committed");
                totalTests++;
                passedTests++;
            } catch (Exception e) {
                System.out.println("  ✗ FAIL: " + e.getMessage());
                totalTests++;
            }
            
        } catch (Exception e) {
            System.out.println("\n✗ ERROR: " + e.getMessage());
            e.printStackTrace();
            System.out.println("\nMake sure Kafka is running:");
            System.out.println("  1. Install: brew install kafka");
            System.out.println("  2. Start Zookeeper: zookeeper-server-start /opt/homebrew/etc/kafka/zookeeper.properties &");
            System.out.println("  3. Start Kafka: kafka-server-start /opt/homebrew/etc/kafka/server.properties &");
        } finally {
            // Cleanup
            if (consumer != null) {
                try { consumer.close(); } catch (Exception e) {}
            }
            if (producer != null) {
                try { producer.close(); } catch (Exception e) {}
            }
            if (admin != null) {
                try { admin.close(); } catch (Exception e) {}
            }
        }
        
        // Print final results
        System.out.println("\n" + "=".repeat(60));
        System.out.println("TEST RESULTS");
        System.out.println("=".repeat(60));
        System.out.println("Total Tests: " + totalTests);
        System.out.println("Passed: " + passedTests);
        System.out.println("Failed: " + (totalTests - passedTests));
        System.out.println("Success Rate: " + String.format("%.2f", (passedTests * 100.0 / totalTests)) + "%");
        System.out.println("=".repeat(60));
        
        if (passedTests == totalTests) {
            System.out.println("\n🎉 CONGRATULATIONS! All tests passed! 🎉");
            System.out.println("You have successfully mastered Kafka operations!");
        } else {
            System.out.println("\n⚠️  Keep practicing! Review the failed tests and try again.");
        }
    }
}
