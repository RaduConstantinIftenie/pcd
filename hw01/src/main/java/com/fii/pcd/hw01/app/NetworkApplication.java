package com.fii.pcd.hw01.app;

import com.fii.pcd.hw01.tcp.TCPClient;
import com.fii.pcd.hw01.tcp.TCPServer;
import com.fii.pcd.hw01.udp.UDPClient;
import com.fii.pcd.hw01.udp.UDPServer;
import java.util.Properties;

public class NetworkApplication {
    public static void main(String[] args) {
        if (args == null || args.length == 0 || args.length % 2 != 0) {
            throw new IllegalArgumentException( "Invalid input parameters were given for the Network Application!");
        }
        
        Properties appInputParams = new Properties();
        for (int i = 0; i < args.length; i += 2) {
            appInputParams.setProperty(args[i], args[i + 1]);
        }
        
        NetworkAppType type = null;
        try {
            type = NetworkAppType.valueOf(appInputParams.getProperty("--type").toUpperCase());
        } catch(Exception e) {
            throw new IllegalArgumentException( "Invalid network application type!", e);
        }
        
        Protocol protocol = null;
        try {
            protocol = Protocol.valueOf(appInputParams.getProperty("--protocol").toUpperCase());
        } catch(Exception e) {
            throw new IllegalArgumentException( "Invalid protocol!", e);
        }
        
        int port = Integer.parseInt(appInputParams.getProperty("--port"));
        if (port <= 1024) {
            throw new IllegalArgumentException( "Invalid port!");
        }
        
        int messageSize = Integer.parseInt(appInputParams.getProperty("--messageSize"));
        if ((messageSize < 1) || (messageSize > 65535)) {
            throw new IllegalArgumentException( "Invalid message size!");
        }
        
        switch (type) {
            case SERVER:
                initServer(protocol, port, messageSize);
                break;
            case CLIENT:
                initClient(protocol, port, messageSize, appInputParams);
                break;
            default:
                throw new IllegalArgumentException( "Invalid network application type!");
        }
    }
    
    private static void initServer(Protocol protocol, int port, int messageSize) {
        switch (protocol) {
            case TCP:
                var tcpServer = new TCPServer(port, messageSize);
                tcpServer.start();
                break;
            case UDP:
                var udpServer = new UDPServer(port, messageSize);
                udpServer.start();
                break;
            default:
                throw new IllegalArgumentException( "Invalid protocol for server!");
        }
    }
    
    private static void initClient(Protocol protocol, int port, int messageSize, Properties appInputParams) {        
        String serverAddress = appInputParams.getProperty("--serverAddress");
        if (serverAddress == null || serverAddress.isEmpty()) {
            throw new IllegalArgumentException( "Invalid server address!");
        }
        
        String filePath = appInputParams.getProperty("--filePath");
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException( "Invalid file path!");
        }
        
        switch (protocol) {
            case TCP:
                var tcpClient = new TCPClient(port, serverAddress, messageSize);
                tcpClient.sendFileTransferRequst(filePath);
                break;
            case UDP:
                var udpClient = new UDPClient(port, serverAddress, messageSize);
                udpClient.sendFileTransferRequst(filePath);
                break;
            default:
                throw new IllegalArgumentException( "Invalid protocol for client!");
        }
    }
    
    public static enum NetworkAppType {
        SERVER,
        CLIENT
    }
    
    public static enum Protocol {
        TCP,
        UDP
    }
}