package com.github.paopaoyue.mesh.dictionary_application.service;

import com.github.paopaoyue.mesh.dictionary_application.config.Properties;
import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import jakarta.annotation.PreDestroy;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.io.output.WriterOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;


@Component
public class LSMTree {

    private static final Logger logger = LoggerFactory.getLogger(LSMTree.class);
    private static final int MAX_DATA_FILE_NUM = 1000;
    private static final String TOMB_VALUE = "";
    private final Properties prop;
    private final Map<String, String> memoryTable;
    private final List<BufferedReader> diskFileReaderList;
    private BufferedWriter logFileWriter;
    private BloomFilter<String> bloomFilter;

    public LSMTree(Properties prop) {
        this.prop = prop;

        this.memoryTable = new TreeMap<>();
        this.diskFileReaderList = new ArrayList<>();

        // Load data from disk to memory table during initialization
        loadDataFromDisk();
    }

    @PreDestroy
    public void shutdown() {
        try {
            logFileWriter.close();
        } catch (IOException e) {
            logger.error("Error closing log file writer", e);
        }
        for (BufferedReader br : diskFileReaderList) {
            try {
                br.close();
            } catch (IOException e) {
                logger.error("Error closing disk file reader", e);
            }
        }
    }

    public synchronized void add(String key, String value) {
        if (query(key) != null) {
            throw new IllegalArgumentException("Key already exists");
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
    }

    public synchronized void update(String key, String value) {
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
        }
    }

    public synchronized void remove(String key) {
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
        }
    }

    public synchronized String query(String key) {
        String value = memoryTable.get(key);
        if (value == null) {
            for (int i = diskFileReaderList.size() - 1; i >= 0; i--) {
                BufferedReader br = diskFileReaderList.get(i);
                try {
                    BloomFilter bloomFilter = BloomFilter.readFrom(new ReaderInputStream(br), Funnels.stringFunnel(Charsets.UTF_8));
                    if (!bloomFilter.mightContain(key)) continue;
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split("=");
                        if (Objects.equals(parts[0], key) && parts.length == 2) {
                            value = parts[1];
                            break;
                        }
                    }
                    br.close();
                    if (value != null) {
                        break;
                    }
                } catch (IOException e) {
                    logger.error("Error reading disk file", e);
                }
            }
        }
        if (value == null) {
            throw new IllegalArgumentException("Key not found");
        }
        return value;
    }

    private synchronized void rotationToDisk() {
        try {
            int i = diskFileReaderList.size();
            BufferedWriter br = new BufferedWriter(new FileWriter(getDiskFileName(i)));
            BloomFilter bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8), prop.getLogfileRotationSize(), 0.01);
            for (Map.Entry<String, String> entry : memoryTable.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    bloomFilter.put(entry.getKey());
                }
            }
            bloomFilter.writeTo(new WriterOutputStream(br));
            for (Map.Entry<String, String> entry : memoryTable.entrySet()) {
                br.write(entry.getKey() + "=" + entry.getValue() + System.lineSeparator());
            }
            br.close();
            memoryTable.clear();
            new FileWriter(getLogFileName(), false).close();
            diskFileReaderList.add(new BufferedReader(new FileReader(getDiskFileName(i))));
        } catch (IOException e) {
            logger.error("Error rotating disk file", e);
        }
    }

    private synchronized void loadDataFromDisk() {
        try {
            File logFile = new File(getLogFileName());
            if (logFile.exists()) {
                this.logFileWriter = new BufferedWriter(new FileWriter(logFile, true));
                BufferedReader br = new BufferedReader(new FileReader(logFile));
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
                this.logFileWriter = new BufferedWriter(new FileWriter(logFile));
            }
        } catch (IOException e) {
            logger.error("Error loading log data from disk", e);
        }
        for (int i = 0; i < MAX_DATA_FILE_NUM; i++) {
            File diskFile = new File(getDiskFileName(i));
            if (diskFile.exists()) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(diskFile));
                    diskFileReaderList.add(br);
                } catch (IOException e) {
                    logger.error("Error loading disk data from disk", e);
                }
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