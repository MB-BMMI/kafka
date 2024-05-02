package io.conductor.demos.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.datatransfer.StringSelection;
import java.util.Properties;

public class ProducerDemo {

    private static final Logger log = LoggerFactory.getLogger(ProducerDemo.class.getSimpleName());

    public static void main(String[] args) {
        log.info("I'm a Kafka Producer!");

        // create Producer properties
        Properties properties = new Properties();

        // connect to LocalHost
        properties.setProperty("bootstrap.servers", "[::1]:9092");

        // set Producer properties
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", StringSerializer.class.getName());

        // create the Producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        // create Producer record -> record that would be sent to Kafka
        /* to check options click CTR + p */
        ProducerRecord<String, String> producerRecord =
                new ProducerRecord<>( "demo_java", "hello world");

        // send data
        producer.send(producerRecord);

        // tell the Producer to send all data and block until done --synchronous
        producer.flush();

        // flush and close the Producer
        producer.flush();
    }
}

