//package org.eventa.core.config;
//
//import jakarta.annotation.PostConstruct;
//import org.apache.kafka.clients.admin.AdminClient;
//import org.apache.kafka.clients.admin.ListTopicsOptions;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
//import org.springframework.kafka.listener.MessageListenerContainer;
//import org.springframework.stereotype.Service;
//
//import java.util.concurrent.ExecutionException;
//
//@Service
//public class KafkaTopicMonitorService {
//
//    @Autowired
//    private AdminClient adminClient;
//
//    @Autowired
//    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;
//
//    @PostConstruct
//    public void monitorTopics() {
//        // You can use a scheduled task or a loop to continuously monitor topics
//        new Thread(() -> {
//            while (true) {
//                try {
//                    var topics = adminClient.listTopics(new ListTopicsOptions().listInternal(false)).names().get();
//                    // Check for topic changes and handle accordingly
//                    System.out.println("Available topics: " + topics);
//
//                    // Example: Restart listener container if needed
//                    for (MessageListenerContainer listenerContainer : kafkaListenerEndpointRegistry.getListenerContainers()) {
//                        // Restart logic based on topic changes
//                        listenerContainer.stop();
//                        listenerContainer.start();
//                    }
//
//                    // Sleep or wait for a certain interval before the next check
//                    Thread.sleep(10000); // Check every 10 seconds
//
//                } catch (InterruptedException | ExecutionException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//    }
//}
