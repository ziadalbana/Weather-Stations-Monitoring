package com.example.Central.station;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;


@Configuration
@EnableKafkaStreams
public class Streamer {

    @Bean
    public KStream<String, String> process(StreamsBuilder streamsBuilder) {
        KStream<String, String> input = streamsBuilder.stream("station");

        KStream<String, String>[] branches = input
                .branch(
                        (key, value) -> someFilterCondition(key, value),
                        (key, value) -> true
                );

        branches[0].to("alert");
//        branches[1].to("station");

        return input;
    }

    private boolean someFilterCondition(String key, String value) {
        // Define your filtering logic here
        // Return true to keep the message, or false to drop it
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            StationData stationData = objectMapper.readValue(value, StationData.class);
            int humidity=stationData.getWeather().getHumidity();
            if (humidity>70) return true;
            else return false;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}


