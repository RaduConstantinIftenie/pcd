package com.fii.pcd.hw01.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class TCPClient {
    private final int port;
    private final String serverAddress;
    private final int messageSize;
    
    public void sendFileTransferRequst(String filePath) {
        try (Socket socket = new Socket(serverAddress, port);
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
            Instant startTime = Instant.now();
            log.info("TCP Client is sending the file = {} to the TCP Server = {} on port = {} with message size = {}",
                filePath, serverAddress, port, messageSize);
            
            long totalNumberOfMessages = 0;
            long totalNumberOfBytes = 0;

            String fileName = Paths.get(filePath).getFileName().toString();
            File file = new File(filePath);
            
            // Send the file name.
            dataOutputStream.writeUTF(fileName);
            dataOutputStream.flush();
            totalNumberOfMessages++;
            
            // Send the file size.
            dataOutputStream.writeLong(file.length());
            dataOutputStream.flush();
            totalNumberOfMessages++;
            
            // Send the file content.
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                int sentBytes;
                byte[] buffer = new byte[messageSize];
                while ((sentBytes = fileInputStream.read(buffer)) != -1) {
                    dataOutputStream.write(buffer, 0, sentBytes);
                    dataOutputStream.flush();
                    totalNumberOfMessages++;
                    totalNumberOfBytes += sentBytes;
                }
            } catch (Exception e) {
                throw e;
            }
            
            String transferStatus = dataInputStream.readUTF();
            
            Instant endTime = Instant.now();
            Duration executionTime = Duration.between(startTime, endTime);
            log.info("The file transfer request was completed " +
                "with execution time = {} and " +
                "with total number of messages = {} and " +
                "with total number of bytes = {} and " +
                "with status = {}",
                executionTime, totalNumberOfMessages, totalNumberOfBytes, transferStatus);
        } catch (Exception e) {
            log.error("The file transfer request sent to the TCP Server failed.", e);
        }
    }
}