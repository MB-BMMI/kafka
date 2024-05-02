package io.conductor.demos.kafka;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemoWithKeys {

    private static final Logger log = LoggerFactory.getLogger(ProducerDemoWithKeys.class.getSimpleName());

    public static void main(String[] args) throws InterruptedException {
        log.info("I'm a Kafka Producer!");

        // create Producer properties
        Properties properties = new Properties();

        // connect to LocalHost
        properties.setProperty("bootstrap.servers", "[::1]:9092");

        // set Producer properties
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", StringSerializer.class.getName());

        //properties.setProperty("batch.size", "400");

        // create the Producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        for (int j = 0; j < 2; j++){

            for (int i = 0; i < 10; i++) {

                String topic = "demo_java";
                String key = "id_" + i;
                String value = "hello world " + i;

                // create Producer record -> record that would be sent to Kafka
                /* to check options click CTR + p */
                ProducerRecord<String, String> producerRecord =
                        new ProducerRecord<>(topic, key, value);

                // send data
                producer.send(producerRecord, new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                        // execute every time a record successfully sent or an exception is thrown
                        if (e == null) {
                            // the record successfully sent
                            log.info("Key: " + key + " | Partition: " + recordMetadata.partition());
                        } else {
                            log.error("Error while producing" + e);
                        }
                    }
                });
            }
        }

        // tell the Producer to send all data and block until done --synchronous
        producer.flush();

        // flush and close the Producer
        producer.flush();
    }
}

