package com.fii.pcd.hw01.udp;

import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.fii.pcd.hw01.udp.UDPUtils.byteArrayToLong;
import static com.fii.pcd.hw01.udp.UDPUtils.setUDPControlData;
import java.io.IOException;
import java.time.Duration;

@AllArgsConstructor
@Slf4j
public class UDPClient {
    private final int port;
    private final String serverAddress;
    private final int messageSize;
    
    public void sendFileTransferRequst(String filePath) {
        try (DatagramSocket socket = new DatagramSocket()) {
            Instant startTime = Instant.now();
            log.info("UDP Client is sending the file = {} to the UDP Server = {} on port = {} with message size = {}",
                filePath, serverAddress, port, messageSize);
            
            InetAddress address = InetAddress.getByName(serverAddress);
            long totalNumberOfMessages = 0;
            long totalNumberOfBytes = 0;
            
            String fileName = Paths.get(filePath).getFileName().toString();
            File file = new File(filePath);
            
            // Send the file name.
            byte[] fileNameBytes = fileName.getBytes();
            DatagramPacket packet = new DatagramPacket(fileNameBytes, fileNameBytes.length, address, port);
            socket.send(packet);
            totalNumberOfMessages++;
            
            // Send the file content.
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                long seqNumber = 1;
                
                byte[] message = new byte[messageSize];
                // First 9 bytes of data are for control (sequence number and end of file flag)
                int maxReadLength = message.length - (Long.BYTES + 1);
                int readBytes;
                long numberOfMessagesResent;
                while ((readBytes = fileInputStream.read(message, 9, maxReadLength)) != -1) {
                    setUDPControlData(message, seqNumber, false);        
                    packet = new DatagramPacket(message, message.length, address, port);
                    socket.send(packet);
                    
                    numberOfMessagesResent =
                        acknowledgeMessageSent(seqNumber, packet, socket);
                    
                    totalNumberOfMessages += (numberOfMessagesResent + 1);
                    totalNumberOfBytes += ((numberOfMessagesResent + 1) * message.length);
                    
                    seqNumber++;
                    message = new byte[messageSize];
                }
                
                setUDPControlData(message, seqNumber, true);
                packet = new DatagramPacket(message, message.length, address, port);
                socket.send(packet);
                
                numberOfMessagesResent =
                    acknowledgeMessageSent(seqNumber, packet, socket);
                
                totalNumberOfMessages += (numberOfMessagesResent + 1);
                totalNumberOfBytes += ((numberOfMessagesResent + 1) * message.length);
            } catch (Exception e) {
                throw e;
            }
            
            Instant endTime = Instant.now();
            Duration executionTime = Duration.between(startTime, endTime);
            log.info("The file transfer request was completed " +
                "with execution time = {} and " +
                "with total number of messages = {} and " +
                "with total number of bytes = {}",
                executionTime, totalNumberOfMessages, totalNumberOfBytes);
        } catch (Exception e) {
            log.error("The file transfer request sent to the UDP Server failed.", e);
        }
    }
    
    private long acknowledgeMessageSent(long sequenceNumber, DatagramPacket messagePacket, DatagramSocket socket)
            throws IOException {
        long numberOfMessagesResent = 0;
        
        while (true) {
            long ackSeqNumber = -1;
            byte[] ack = new byte[Long.BYTES];
            DatagramPacket ackPacket = new DatagramPacket(ack, ack.length);
            
            try {
                socket.setSoTimeout(150);
                socket.receive(ackPacket);
                ackSeqNumber = byteArrayToLong(ack);
            } catch (IOException e) {
                log.error("UDP Client timed out waiting for acknowledging sequence numer = {}",
                    sequenceNumber, e);
            }
            
            if (ackSeqNumber == sequenceNumber) {
                // log.info("UDP Client received the acknowledge for the sequence number = {}", sequenceNumber);
                break;
            } else {
                // The package was not received, so it must be resent.
                socket.send(messagePacket);
                log.info("UDP Client is resending the message for the sequence number = {}", sequenceNumber);
                numberOfMessagesResent++;
            }
        }
        
        return numberOfMessagesResent;
    }
}