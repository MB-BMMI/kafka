package io.conductor.demos.kafka;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemoWithCallback {

    private static final Logger log = LoggerFactory.getLogger(ProducerDemoWithCallback.class.getSimpleName());

    public static void main(String[] args) throws InterruptedException {
        log.info("I'm a Kafka Producer!");

        // create Producer properties
        Properties properties = new Properties();

        // connect to LocalHost
        properties.setProperty("bootstrap.servers", "[::1]:9092");

        // set Producer properties
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", StringSerializer.class.getName());

        properties.setProperty("batch.size", "400");

        // create the Producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        for (int j = 0; j < 10; j++) {

            for (int i = 0; i < 30; i++) {
                // create Producer record -> record that would be sent to Kafka
                /* to check options click CTR + p */
                ProducerRecord<String, String> producerRecord =
                        new ProducerRecord<>("demo_java", "hello world " + i + " " + j);

                // send data
                producer.send(producerRecord, new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                        // execute every time a record successfully sent or an exception is thrown
                        if (e == null) {
                            // the record successfully sent
                            log.info("Received new metadata \n" +
                                    "Topic " + recordMetadata.topic() + "\n" +
                                    "Partition " + recordMetadata.partition() + "\n" +
                                    "Offsets " + recordMetadata.offset() + "\n" +
                                    "Timestamp " + recordMetadata.timestamp() + "\n"
                            );
                        } else {
                            log.error("Error while producing" + e);
                        }
                    }
                });
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // tell the Producer to send all data and block until done --synchronous
        producer.flush();

        // flush and close the Producer
        producer.flush();
    }
}

