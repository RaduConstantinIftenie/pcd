package com.fii.pcd.hw01.udp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.fii.pcd.hw01.udp.UDPUtils.byteArrayToLong;
import static com.fii.pcd.hw01.udp.UDPUtils.longToByteArray;

@AllArgsConstructor
@Slf4j
public class UDPServer {
    private final int port;
    private final int messageSize;

    public void start() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            Instant startTime = Instant.now();
            log.info("UDP Server is starting on port = {} with message size = {}",
                port, messageSize);
            
            // Create the directory where all received files will be stored.
            File file = new File("./FilesReceived/");
            file.mkdirs();
            
            long totalNumberOfMessages = 0;
            long totalNumberOfBytes = 0;
            
            // Read the file name.
            byte[] fileNameBuffer = new byte[messageSize];
            DatagramPacket fileNamePacket = new DatagramPacket(fileNameBuffer, fileNameBuffer.length);
            socket.receive(fileNamePacket);
            byte[] data = fileNamePacket.getData();
            String fileName = new String(data, 0, fileNamePacket.getLength());
            totalNumberOfMessages++;
            log.info("Received an UDP Client request to transfer the file = {}", fileName);
            
            // Read the file content.
            file = new File("./FilesReceived/" + fileName);
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                boolean isEof;
                long seqNumber;
                long lastAckSeq = 0;
                
                while (true) {
                    byte[] message = new byte[messageSize];
                    byte[] fileData = new byte[messageSize - (Long.BYTES + 1)];
                    byte[] seqNumberBytes = new byte[Long.BYTES];
                    
                    DatagramPacket fileDataPacket = new DatagramPacket(message, message.length);
                    socket.receive(fileDataPacket);
                    message = fileDataPacket.getData();
                    
                    // Read the port and the address for sending acknowledgment to the UDP Client.
                    InetAddress clientAddress = fileDataPacket.getAddress();
                    int clientPort = fileDataPacket.getPort();
                    
                    // Read the sequence number.
                    System.arraycopy(message, 0, seqNumberBytes, 0, seqNumberBytes.length);
                    seqNumber = byteArrayToLong(seqNumberBytes);
                    // Read the end of file flag.
                    isEof = (message[8] & 0xff) == 1;
                    totalNumberOfMessages++;
                    totalNumberOfBytes += message.length;
                    
                    if (seqNumber == (lastAckSeq + 1)) {
                        lastAckSeq = seqNumber;
                        
                        if (!isEof) {
                            // Read the file data from the message.
                            System.arraycopy(message, Long.BYTES + 1, fileData, 0, fileData.length);
                            fileOutputStream.write(fileData);
                        }                        
                        
                        acknowledgeMessageReceived(lastAckSeq, socket, clientAddress, clientPort);
                        
                        if (isEof) {
                            break;
                        }
                    } else {
                        log.info("Message discarded! Expected the sequence number = {} " +
                            "but received the sequence number = {}", (lastAckSeq + 1), seqNumber);
                        
                        // Resend the last acknowledge sequence number.
                        acknowledgeMessageReceived(lastAckSeq, socket, clientAddress, clientPort);
                    }
                }
            } catch (Exception e) {
                throw e;
            }
            
            Instant endTime = Instant.now();
            Duration executionTime = Duration.between(startTime, endTime);
            log.info("UDP Client request processed successfully " +
                "with execution time = {} and " +
                "with total number of messages received = {} and " +
                "with total number of bytes received = {}",
                executionTime, totalNumberOfMessages, totalNumberOfBytes);
        } catch (Exception e) {
            log.error("UDP Server failed to listen on port = {} or to receive message packets.", port, e);
        }   
    }    
    
    private void acknowledgeMessageReceived(long lastAckSeq, DatagramSocket socket,
            InetAddress clientAddress, int clientPort) throws IOException {
        byte[] ackData = longToByteArray(lastAckSeq);
        
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, clientAddress, clientPort);
        socket.send(ackPacket);
        
        // log.info("UDP Server acknowledged the receiving of the sequence number = {}", lastAckSeq);
    }
}