package com.example.Central.station;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class Consumer {

   @KafkaListener(topics = "station",
           groupId = "springboot",
           properties = {
           "bootstrap.servers=kafka-service:9092"
           }
   )
    public void consume(String msg){
       System.out.println("message :"+ msg);
   }
}
