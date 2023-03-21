package com.fii.pcd.hw01.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class TCPServer {
    private final int port;
    private final int messageSize;
    
    public void start() {
        ExecutorService executor = null;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("TCP Server is starting on port = {} with message size = {}",
                port, messageSize);
            
            // Create the directory where all received files will be stored.
            File file = new File("./FilesReceived/");
            file.mkdirs();
            
            executor = Executors.newFixedThreadPool(10);
            log.info("TCP Server is waiting for client requests.");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Runnable clientRequestHandler = new TCPClientRequestHandler(clientSocket, messageSize);
                executor.execute(clientRequestHandler);
            }
        } catch (Exception e) {
            log.error("TCP Server failed to listen on port = {} or to accept connections.", port, e);
        } finally {
            if (executor != null) {
                executor.shutdown();
            }
        }
    }
    
    @AllArgsConstructor
    @Slf4j
    public static class TCPClientRequestHandler implements Runnable {
        private final Socket clientSocket;
        private final int messageSize;
        
        @Override
        public void run() {
            Instant startTime = Instant.now();
            String threadName = Thread.currentThread().getName();
            log.info("TCP Client request is handled in thread = {}", threadName);
            
            long totalNumberOfMessages = 0;
            long totalNumberOfBytes = 0;
            try (DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream())) {
                // Read the file name.
                String fileName = dataInputStream.readUTF();
                totalNumberOfMessages++;
                
                // Read the file size.
                long fileSize = dataInputStream.readLong();
                totalNumberOfMessages++;
                
                log.info("TCP Client request handled in thread = {} " +
                    "for transfering the file = {} with file size = {}",
                    threadName, fileName, fileSize);
                
                // Read the file content.
                File file = new File("./FilesReceived/" + fileName);
                try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    int receivedBytes;
                    byte[] buffer = new byte[messageSize];
                    while (fileSize > 0 && (receivedBytes =
                        dataInputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                        fileOutputStream.write(buffer, 0, receivedBytes);
                        fileSize -= receivedBytes;
                        totalNumberOfMessages++;
                        totalNumberOfBytes += receivedBytes;
                    }
                } catch (Exception e) {
                    throw e;
                }
                
                // Send the transfer status.
                dataOutputStream.writeUTF("\"" + fileName + "\" was transfered successfully to the TCP Server.");
                
                Instant endTime = Instant.now();
                Duration executionTime = Duration.between(startTime, endTime);
                log.info("TCP Client request processed successfully in thread = {} " +
                    "with execution time = {} and " +
                    "with total number of messages received = {} and " +
                    "with total number of bytes received = {}",
                    threadName, executionTime, totalNumberOfMessages, totalNumberOfBytes);
            } catch (Exception e) {
                log.error("TCP Client request failed to be processed in thread = {}", threadName, e);
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    log.error("TCP Client socket cannot be closed in thread = {}", threadName, e);
                }
            }
        }
    }
}