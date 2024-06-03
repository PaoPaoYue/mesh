package com.github.paopaoyue.mesh.dictionary_application.service;

import com.github.paopaoyue.mesh.dictionary_application.config.Properties;
import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;


@Component
@ConditionalOnProperty(prefix = "mesh.rpc", name = "server-enabled", havingValue = "true")
public class LSMTree {

    private static final Logger logger = LoggerFactory.getLogger(LSMTree.class);
    private static final int MAX_DATA_FILE_NUM = 1000;
    private static final String TOMB_VALUE = "";
    private final Properties prop;
    private final Map<String, String> memoryTable;
    private final List<File> diskFileList;
    private BufferedWriter logFileWriter;

    public LSMTree(Properties prop) {
        this.prop = prop;

        this.memoryTable = new TreeMap<>();
        this.diskFileList = new ArrayList<>();
    }

    @PreDestroy
    public void shutdown() {
        try {
            logFileWriter.close();
        } catch (IOException e) {
            logger.error("Error closing log file writer", e);
        }
    }

    public synchronized boolean add(String key, String value) {
        if (query(key) != null) {
            return false;
        }
        memoryTable.put(key, value);
        try {
            logFileWriter.write(key + "=" + value + System.lineSeparator());
        } catch (IOException e) {
            logger.error("Error writing to log file", e);
        }
        if (memoryTable.size() >= prop.getLogfileRotationSize()) {
            rotationToDisk();
        }
        return true;
    }

    public synchronized boolean update(String key, String value) {
        if (query(key) != null) {
            memoryTable.put(key, value);
            try {
                logFileWriter.write(key + "=" + value + System.lineSeparator());
            } catch (IOException e) {
                logger.error("Error writing to log file", e);
            }
            if (memoryTable.size() >= prop.getLogfileRotationSize()) {
                rotationToDisk();
            }
            return true;
        }
        return false;
    }

    public synchronized boolean remove(String key) {
        if (query(key) != null) {
            memoryTable.put(key, TOMB_VALUE);
            try {
                logFileWriter.write(key + "=" + TOMB_VALUE + System.lineSeparator());
            } catch (IOException e) {
                logger.error("Error writing to log file", e);
            }
            if (memoryTable.size() >= prop.getLogfileRotationSize()) {
                rotationToDisk();
            }
            return true;
        }
        return false;
    }

    public synchronized String query(String key) {
        String value = memoryTable.get(key);
        if (value == null) {
            for (int i = diskFileList.size() - 1; i >= 0; i--) {
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new FileReader(diskFileList.get(i), Charsets.UTF_8));
                    String line;
                    if ((line = br.readLine()) != null) {
                        byte[] bytes = Base64.getDecoder().decode(line);
                        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
                        BloomFilter bloomFilter = (BloomFilter) ois.readObject();
                        ois.close();
                        if (!bloomFilter.mightContain(key)) continue;
                    } else {
                        continue;
                    }
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split("=");
                        if (Objects.equals(parts[0], key)) {
                            if (parts.length == 2) {
                                value = parts[1];
                            } else {
                                value = TOMB_VALUE;
                            }
                            break;
                        }
                    }
                    if (value != null) {
                        break;
                    }
                } catch (IOException | ClassNotFoundException e) {
                    logger.error("Error reading disk file", e);
                } finally {
                    try {
                        if (br != null) br.close();
                    } catch (IOException e) {
                        logger.error("Error closing disk file reader", e);

                    }
                }
            }
        }
        return (value != null && !value.isEmpty()) ? value : null;
    }

    private synchronized void rotationToDisk() {
        try {
            int i = diskFileList.size();
            File diskFile = new File(getDiskFileName(i));
            BloomFilter bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8), prop.getLogfileRotationSize(), 0.01);
            memoryTable.keySet().stream().forEach(bloomFilter::put);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(bloomFilter);
            BufferedWriter br = new BufferedWriter(new FileWriter(diskFile, Charsets.UTF_8, true));
            br.write(new String(Base64.getEncoder().encode(bos.toByteArray())) + System.lineSeparator());
            for (Map.Entry<String, String> entry : memoryTable.entrySet()) {
                br.write(entry.getKey() + "=" + entry.getValue() + System.lineSeparator());
            }
            oos.close();
            br.close();
            memoryTable.clear();
            // refresh log file
            logFileWriter.close();
            new FileWriter(getLogFileName(), false).close();
            logFileWriter = new BufferedWriter(new FileWriter(getLogFileName(), Charsets.UTF_8, false));

            diskFileList.add(diskFile);
        } catch (IOException e) {
            logger.error("Error rotating disk file", e);
        }
    }

    @PostConstruct
    private synchronized void loadDataFromDisk() {
        try {
            File logFile = new File(getLogFileName());
            if (logFile.exists()) {
                this.logFileWriter = new BufferedWriter(new FileWriter(logFile, Charsets.UTF_8, true));
                BufferedReader br = new BufferedReader(new FileReader(logFile, Charsets.UTF_8));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("=");
                    if (parts.length == 2) {
                        memoryTable.put(parts[0], parts[1]);
                    } else {
                        memoryTable.put(parts[0], TOMB_VALUE);
                    }
                }
                br.close();
            } else {
                this.logFileWriter = new BufferedWriter(new FileWriter(logFile, Charsets.UTF_8, false));
            }
        } catch (IOException e) {
            logger.error("Error loading log data from disk", e);
        }
        for (int i = 0; i < MAX_DATA_FILE_NUM; i++) {
            File diskFile = new File(getDiskFileName(i));
            if (diskFile.exists()) {
                diskFileList.add(diskFile);
            }
        }
    }

    private String getDiskFileName(int i) {
        return prop.getDataFolderPath() + File.separator + prop.getDiskFileBaseName() + "-" + i + ".dat";
    }

    private String getLogFileName() {
        return prop.getDataFolderPath() + File.separator + prop.getLogFileBaseName() + ".dat";
    }

}