package io.conduktor.demos.kafka.opensearch;

import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.Configurable;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.lucene.index.IndexReader;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.xcontent.XContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

public class OpenSearchConsumer {

    public static RestHighLevelClient createOpenSearchClient() {
       // String connString = "http://localhost:9200";
        String connString = "https://qw921nrkgx:uqhfcil7sl@test-search-1752884673.eu-central-1.bonsaisearch.net:443";

        // we build a URI from the connection string
        RestHighLevelClient restHighLevelClient;
        URI connUri = URI.create(connString);
        // extract login information if it exists
        String userInfo = connUri.getUserInfo();

        if (userInfo == null) {
            // REST client without security
            restHighLevelClient = new RestHighLevelClient(RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), "http")));

        } else {
            // REST client with security
            String[] auth = userInfo.split(":");

            CredentialsProvider cp = new BasicCredentialsProvider();
            cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(auth[0], auth[1]));

            restHighLevelClient = new RestHighLevelClient(
                    RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), connUri.getScheme()))
                            .setHttpClientConfigCallback(
                                    httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(cp)
                                            .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())));


        }

        return restHighLevelClient;
    }

    private static KafkaConsumer<String, String> createKafkaConsumer(){
        String groupId = "consumer-opensearch-demo";
        String topic = "wikimedia.recentchange";

        // create Producer properties
        Properties properties = new Properties();

        // connect to LocalHost
        properties.setProperty("bootstrap.servers", "[::1]:9092");

        // set Consumer properties
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        // there are none/earliest/latest
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        // create a consumer
        return new KafkaConsumer<>(properties);
    }

    private static String extractId(String json){
        // gson library
        return JsonParser.parseString(json)
                .getAsJsonObject()
                .get("meta")
                .getAsJsonObject()
                .get("id")
                .getAsString();
    }


    public static void main(String[] args) throws IOException {

        Logger log = LoggerFactory.getLogger(OpenSearchConsumer.class.getSimpleName());

        // first create an OpenSearch Client
        RestHighLevelClient openSearchClient = createOpenSearchClient();

        // create a Kafka Client
        KafkaConsumer<String, String> consumer = createKafkaConsumer();

        // we need to create the index on OpenSearch if it doesn't exist already
        try(openSearchClient ; consumer /* This peace of code closes both clients
                                           nevertheless if the code in try will be
                                           successfully executed or not*/) {

            boolean indexExist =  openSearchClient.indices().exists(new GetIndexRequest("wikimedia"), RequestOptions.DEFAULT);

            if (!indexExist) {
                CreateIndexRequest createIndexRequest = new CreateIndexRequest("wikimedia");
                openSearchClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
                log.info("The Wikimedia Index has been created");
            } else {
                log.info("The Wikimedia Index already created");
            }


            // we subscribe the consumer
            consumer.subscribe(Collections.singleton("wikimedia.recentchange"));

            // create a shutdown hook
            // get a reference to the main thread
            final Thread mainThread = Thread.currentThread();

            // add the shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    log.info("Detected a shutdown, let's exit by calling consumer.wakeup()");
                    consumer.wakeup();

                    // join the main thread to allow the execution of the code in the main thread
                    try {
                        mainThread.join();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            try {

                // main code logic
                while (true) {

                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(3000));

                    int recordCount = records.count();
                    log.info("Received " + recordCount + " Records(s)");

                    BulkRequest bulkRequest = new BulkRequest();

                    for (ConsumerRecord<String, String> record : records) {

                        // strategy 1
                        // define an ID using Kafka Record coordinate
//                        String id = record.topic() + "_" + record.partition() + "_" + record.offset();

                        try {
                            // send the record into OpenSearch

                            // strategy 2
                            // we extract the ID from the JSON value
                            String id = extractId(record.value());

                            IndexRequest indexRequest = new IndexRequest("wikimedia")
                                    .source(record.value(), XContentType.JSON);

//                        IndexResponse response = openSearchClient.index(indexRequest, RequestOptions.DEFAULT);
//                        if (response.getId() != null)
//                            log.info(response.getId());

                            bulkRequest.add(indexRequest);

                        } catch (Exception e) {

                        }

                    }

                    if (bulkRequest.numberOfActions() > 0) {
                        BulkResponse bulkResponse = openSearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
                        log.info("Inserted " + bulkResponse.getItems().length + " record(s).");

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        // commit offsets after the batch is consumed
                        if (recordCount != 0) {
                            consumer.commitAsync();
                            log.info("Offsets have been commited~");
                        }
                    }

                }
            } catch (WakeupException e) {
                log.info("Consumer is starting to shut down");
            } catch (Exception e) {
                log.error("Unexpected exception in the consumer", e);
            } finally {
                consumer.close(); // close the consumer, this will also commit the offsets
                log.info("The consumer is now gracefully shut down");
            }

            // close thing
        }
        }
}
