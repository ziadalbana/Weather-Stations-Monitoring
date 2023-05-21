package com.example.Central.station;



import org.apache.avro.reflect.ReflectData;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.ParquetWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.parquet.hadoop.ParquetFileWriter.Mode;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
public class Parquet {

    public  void write(String fileName, List<StationData> records) {

        try (ParquetWriter<StationData> writer = AvroParquetWriter.<StationData>builder(new Path(fileName))
                .withSchema(ReflectData.AllowNull.get().getSchema(StationData.class))
                .withDataModel(ReflectData.get())
                .withConf(new Configuration())
                .withCompressionCodec(CompressionCodecName.SNAPPY)
                .withWriteMode(Mode.OVERWRITE)
                .build()) {

            for (StationData s : records)
                writer.write(s);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}