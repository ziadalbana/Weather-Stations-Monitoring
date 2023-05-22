package com.example.Central.station;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class Bitcask {

    private final String directory;
    private Map<String, Map.Entry<String, Long>> index;
    private long fileNumber = 0;
    private File currentFile;
    private RandomAccessFile currentFileStream;

    public Bitcask(String directory) {
        this.directory = directory;
        File dir = new File(directory);
        dir.mkdirs();
        this.index = new HashMap<>();
        this.fileNumber = 0;
        if(recovery())
            fileNumber++;
        initialize();
    }

    private void initialize() {
        currentFile = new File(directory + "/" + fileNumber + ".bitcask");
        try {
            currentFile.createNewFile();
            currentFileStream = new RandomAccessFile(currentFile, "rw");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private boolean recovery() {
        List<String> bitcaskFileNames = this.getSortedFileNames(this.directory, ".bitcask");
        if (bitcaskFileNames.isEmpty())
            return false;
        List<String> hintFileNames = this.getSortedFileNames(this.directory, ".hint");

        try {  // successful recovery
            String lastHintFile = hintFileNames.get(hintFileNames.size() - 1);
            this.index = loadHintFiles(lastHintFile);

            if (bitcaskFileNames.size() == hintFileNames.size()) {
                this.fileNumber = this.extractFileNumber(lastHintFile);
                return true;
            } else {
                String latestBitcaskFile = bitcaskFileNames.get(bitcaskFileNames.size() - 1);
                File file = new File(latestBitcaskFile);
                RandomAccessFile fileStream = new RandomAccessFile(file, "r");

                while (fileStream.getFilePointer() < fileStream.length()) {
                    long offset = fileStream.getFilePointer();
                    int keyLength = fileStream.readInt();
                    byte[] keyBytes = new byte[keyLength];
                    fileStream.readFully(keyBytes);
                    int valueLength = fileStream.readInt();
                    byte[] valueBytes = new byte[valueLength];
                    fileStream.readFully(valueBytes);

                    index.put(new String(keyBytes, StandardCharsets.UTF_8),
                            new AbstractMap.SimpleEntry<>(file.getCanonicalPath(), offset));
                }
                fileStream.close();
                this.fileNumber = this.extractFileNumber(latestBitcaskFile);
            }
        } catch (IOException e) {  // unsuccessful recovery
            return false;
        }

        return true;
    }





    public void put(String key, String value) {
        try {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.allocate(8 + keyBytes.length + valueBytes.length);

            buffer.putInt(keyBytes.length);
            buffer.put(keyBytes);
            buffer.putInt(valueBytes.length);
            buffer.put(valueBytes);

            long offset = currentFileStream.length();
            currentFileStream.seek(offset);
            currentFileStream.write(buffer.array());
            index.put(key, new AbstractMap.SimpleEntry<>(currentFile.getCanonicalPath(), offset));
            if (currentFileStream.length() > 10_000) {
                rotateFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public String get(String key) {

        if(!index.containsKey(key))
            return null;

        try {
            Map.Entry<String, Long> value= index.get(key);

            String filePath = value.getKey();
            Long offset = value.getValue();

            if (currentFile.getCanonicalPath().equals(filePath)) {
                currentFileStream.seek(offset);
                int keyLength = currentFileStream.readInt();
                byte[] keyBytes = new byte[keyLength];
                currentFileStream.readFully(keyBytes);
                int valueLength = currentFileStream.readInt();
                byte[] valueBytes = new byte[valueLength];
                currentFileStream.readFully(valueBytes);
                return new String(valueBytes, StandardCharsets.UTF_8);
            }else{
                RandomAccessFile tempFileStream = new RandomAccessFile(new File(filePath), "r");
                tempFileStream.seek(offset);
                int keyLength = tempFileStream.readInt();
                byte[] keyBytes = new byte[keyLength];
                tempFileStream.readFully(keyBytes);
                int valueLength = tempFileStream.readInt();
                byte[] valueBytes = new byte[valueLength];
                tempFileStream.readFully(valueBytes);
                tempFileStream.close();
                return new String(valueBytes, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private void rotateFile() {
        try {
            currentFileStream.close();
            createHintFiles(this.index, currentFile.getName());
            if(fileNumber % 3 == 0){
                compact();
            }else{
                fileNumber++;
                currentFile = new File(directory + "/" + fileNumber + ".bitcask");
                currentFile.createNewFile();
                currentFileStream = new RandomAccessFile(currentFile, "rw");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void compact() {
        try {
            this.fileNumber++;
            long newFileNumber = fileNumber;
            File newFile = new File(directory + "/" + newFileNumber + "_compact.bitcask");
            newFile.createNewFile();
            RandomAccessFile newFileStream = new RandomAccessFile(newFile, "rw");
            Map<String, Map.Entry<String, Long>> newIndex = new HashMap<>();

            for (Map.Entry<String, Map.Entry<String, Long>> entry : index.entrySet()) {
                String key = entry.getKey();
                String filePath = entry.getValue().getKey();
                Long offset = entry.getValue().getValue();

                RandomAccessFile fileStream = new RandomAccessFile(new File(filePath), "r");
                fileStream.seek(offset);
                int keyLength = fileStream.readInt();
                byte[] keyBytes = new byte[keyLength];
                fileStream.readFully(keyBytes);
                int valueLength = fileStream.readInt();
                byte[] valueBytes = new byte[valueLength];
                fileStream.readFully(valueBytes);
                fileStream.close();

                byte[] keyBytesNew = keyBytes;
                byte[] valueBytesNew = valueBytes;
                long offsetNew = newFileStream.length();

                ByteBuffer buffer = ByteBuffer.allocate(8 + keyBytesNew.length + valueBytesNew.length);
                buffer.putInt(keyBytesNew.length);
                buffer.put(keyBytesNew);
                buffer.putInt(valueBytesNew.length);
                buffer.put(valueBytesNew);

                newFileStream.seek(offsetNew);
                newFileStream.write(buffer.array());

                newIndex.put(key, new AbstractMap.SimpleEntry<>(newFile.getCanonicalPath(), offsetNew));
            }

            currentFileStream.close();
            //currentFile.delete();

            currentFile = newFile;
            currentFileStream = newFileStream;
            index.clear();
            index.putAll(newIndex);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createHintFiles(Map<String, Map.Entry<String, Long>> index, String fileName) {
        try (FileOutputStream fileOut = new FileOutputStream(directory + "/" + fileName + ".hint");
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(index);
            System.out.println("Serialized data is saved in " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Map.Entry<String, Long>> loadHintFiles(String fileName) {
        Map<String, Map.Entry<String, Long>> newIndex = null;
        try (FileInputStream fileIn = new FileInputStream(directory + "/" + fileName);
             ObjectInputStream in = new ObjectInputStream(fileIn)) {
            newIndex = (Map<String, Map.Entry<String, Long>>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return newIndex;
    }

    public List<String> getSortedFileNames(String directory, String suffix){
        List<String> previousFileNames = Arrays.stream(Objects.requireNonNull(new File(directory).listFiles()))
                .filter(f -> f.getName().endsWith(suffix))
                .sorted(Comparator.comparing(File::getName))
                .map(File::getName)
                .collect(Collectors.toList());

        //Collections.reverse(previousFileNames);

        return previousFileNames;
    }

    public int extractFileNumber(String fileName) {
        int endIndex = fileName.indexOf('_'); // Find the index of the underscore
        String fileNumberStr;
        if (endIndex != -1) {
            fileNumberStr = fileName.substring(0, endIndex);
        } else {
            // Handle file names without an underscore
            fileNumberStr = fileName.substring(0, fileName.lastIndexOf('.'));
        }
        return Integer.parseInt(fileNumberStr);
    }

    public void setCompactionInProgress(boolean b) {

    }
}
