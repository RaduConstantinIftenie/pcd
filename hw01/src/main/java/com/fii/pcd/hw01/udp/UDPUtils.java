package com.fii.pcd.hw01.udp;

import java.nio.ByteBuffer;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UDPUtils {
    public static byte[] longToByteArray(long number) {
        return new byte[] {
            (byte) ((number >> 56) & 0xff),
            (byte) ((number >> 48) & 0xff),
            (byte) ((number >> 40) & 0xff),
            (byte) ((number >> 32) & 0xff),
            (byte) ((number >> 24) & 0xff),
            (byte) ((number >> 16) & 0xff),
            (byte) ((number >> 8) & 0xff),
            (byte) (number & 0xff)
        };
    }
    
    public static long byteArrayToLong(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES);
        byteBuffer.put(bytes);
        byteBuffer.flip();
        
        return byteBuffer.getLong();
    }
    
    public static void setUDPControlData(byte[] message, long sequenceNumber, boolean eof) {
        // Copy the sequence number on the first 8 bytes.
        System.arraycopy(longToByteArray(sequenceNumber), 0, message, 0, Long.BYTES);
        
        // The 9th byte will flag if it's the end of the file.
        if (eof) {
            message[8] = (byte) (1);
        } else {
            message[8] = (byte) (0);
        }
    }
}