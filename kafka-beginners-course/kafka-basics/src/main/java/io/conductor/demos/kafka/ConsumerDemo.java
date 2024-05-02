package io.conductor.demos.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerDemo {

    private static final Logger log = LoggerFactory.getLogger(ConsumerDemo.class.getSimpleName());

    public static void main(String[] args) {
        log.info("I'm a Kafka Producer!");

        String groupId = "my-java-application";
        String topic = "demo_java";

        // create Producer properties
        Properties properties = new Properties();

        // connect to LocalHost
        properties.setProperty("bootstrap.servers", "[::1]:9092");

        // set Consumer properties
        properties.setProperty("key.deserializer", StringDeserializer.class.getName());
        properties.setProperty("value.deserializer", StringDeserializer.class.getName());

        properties.setProperty("group.id", groupId);

        // there are none/earliest/latest
        properties.setProperty("auto.offset.reset", "earliest");

        // create a consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        // subscribe to a topic
        consumer.subscribe(Arrays.asList(topic));

        // poll for data
        while (true) {

            log.info("Polling");

            ConsumerRecords<String, String> records =
                    consumer.poll(Duration.ofMillis(1000));

            for (ConsumerRecord<String, String> record: records) {
                log.info("Key: " + record.key() + ", Value " + record.value());
                log.info("Partition: " + record.partition() + ", Offset " + record.offset());
            }

        }

    }
}

