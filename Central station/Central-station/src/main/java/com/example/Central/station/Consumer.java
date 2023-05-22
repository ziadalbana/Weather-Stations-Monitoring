package com.example.Central.station;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
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

           if(station_id < 1 || station_id > 10){ // wrong station_id
               return;
           }

           bitcask.put(String.valueOf(stationData.getStationId()), msg);

           if(stations_records.containsKey(station_id)){
               stations_records.get(station_id).add(stationData);
           }else{
               stations_records.put(station_id, new LinkedList<>());
               stations_records.get(station_id).add(stationData);
           }

           this.number_of_msg++;

           if(this.number_of_msg < 10_000)
               return;

           // write to parquet
           for (int i = 1; i <= 10; i++) {
               if (!stations_records.containsKey(i)) continue;

               List<StationData> records = stations_records.get(i);
               stations_records.get(i).clear();

               if (records.size() == 1) {
                   parquet.write(getFileNameFromTimestamp(records.get(0).getStatusTimestamp()) + "_" + i, records);
               }

               records.sort(Comparator.comparingLong(StationData::getStatusTimestamp));

               int l = 0, r = 0;

               String start = getFileNameFromTimestamp(records.get(0).getStatusTimestamp());

               while (r < records.size()) {
                   String cur = getFileNameFromTimestamp(records.get(r).getStatusTimestamp());
                   if (start.equals(cur)) {
                       r++;
                   } else {
                       parquet.write(start + "_" + i, records.subList(l, r));
                       l = r;
                       start = getFileNameFromTimestamp(records.get(l).getStatusTimestamp());
                       r++;
                   }
               }

               if (l != records.size() - 1) {
                   parquet.write(start + "_" + i, records.subList(l, r));
               }
           }





       } catch (Exception e) {
           e.printStackTrace();
       }

   }

    public String getFileNameFromTimestamp(long timestamp) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-HH");
        Date date = new Date(timestamp);
        return dateFormatter.format(date);
    }
}
