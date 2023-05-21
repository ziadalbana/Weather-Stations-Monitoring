package com.example.Central.station;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Service
public class Consumer {

    List<StationData> records = new LinkedList<>();
    Parquet parquet = new Parquet();

   @KafkaListener(topics = "station",
           groupId = "springboot",
           properties = {
           "bootstrap.servers=127.0.0.1:9092"
           }
   )
    public void consume(String msg){
       System.out.println("message :"+ msg);

       try {
           ObjectMapper objectMapper = new ObjectMapper();
           StationData stationData = objectMapper.readValue(msg, StationData.class);


           System.out.println(stationData.getStationId());
           System.out.println(stationData.getSNo());
           System.out.println(stationData.getBatteryStatus());
           System.out.println(stationData.getStatusTimestamp());
           System.out.println(stationData.getWeather().getHumidity());
           System.out.println(stationData.getWeather().getTemperature());
           System.out.println(stationData.getWeather().getWindSpeed());

           records.add(stationData);

           if (records.size() == 10){
               parquet.write("file.parquet", records);
           }


       } catch (Exception e) {
           e.printStackTrace();
       }

   }
}
