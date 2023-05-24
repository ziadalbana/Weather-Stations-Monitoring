package com.example.Central.station;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class Consumer {

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-HH");

    private final Map<Integer, List<StationData>> stations_records = new HashMap<>();

    int number_of_msg = 0;

    Parquet parquet = new Parquet("./archive");
    Bitcask bitcask = new Bitcask("./store");

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

           int station_id = stationData.getStationId();


           bitcask.put(String.valueOf(stationData.getStationId()), msg);

           List<StationData> records = stations_records.getOrDefault(station_id, new LinkedList<>());
           records.add(stationData);
           stations_records.put(station_id, records);

           this.number_of_msg++;

           if(this.number_of_msg < 30)
               return;

           // write to parquet
           for (Integer id : stations_records.keySet()) {
               //if (!stations_records.containsKey(id)) continue;

               records = stations_records.get(id);
               if(records.isEmpty())
                   continue;

               if (records.size() == 1) {
                   parquet.write(getFileNameFromTimestamp(records.get(0).getStatusTimestamp()) + "_" + id, records);
               }

               records.sort(Comparator.comparingLong(StationData::getStatusTimestamp));

               int l = 0, r = 0;

               String start = getFileNameFromTimestamp(records.get(0).getStatusTimestamp());

               while (r < records.size()) {
                   String cur = getFileNameFromTimestamp(records.get(r).getStatusTimestamp());
                   if (start.equals(cur)) {
                       r++;
                   } else {
                       parquet.write(start + "_" + id, records.subList(l, r));
                       l = r;
                       start = getFileNameFromTimestamp(records.get(l).getStatusTimestamp());
                       r++;
                   }
               }

               if (l != records.size() - 1) {
                   parquet.write(start + "_" + id, records.subList(l, r));
               }

               stations_records.get(id).clear();
           }

           this.number_of_msg = 0;


       } catch (Exception e) {
           e.printStackTrace();
       }

   }

    public String getFileNameFromTimestamp(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");
        //System.out.println(formattedDateTime);
        return dateTime.format(formatter);
    }

}
