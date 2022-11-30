import java.net.*;
import java.io.*;
import java.util.*;

public class NTPServer {

    public class NTPClientHandler implements Runnable {
        DatagramSocket ntpSocket;
        DatagramPacket ntpPacket;
        byte[] ntpData;
        long cstarttime;
        long cendtime;

        private long seventyOffset; // offset (in ms) between 1900 and 1970
        private long transmitMillis;

        // Offsets in NTPData for each timestamp
        private final byte referenceOffset = 16;
        private final byte originateOffset = 24;
        private final byte receiveOffset = 32;
        private final byte transmitOffset = 40;
        private long transmitTimestamp;

        public NTPClientHandler(DatagramSocket ntpSocket, DatagramPacket ntpPacket, byte[] ntpData, long cstarttime) {
            this.ntpSocket = ntpSocket;
            this.cstarttime = cstarttime;
            this.ntpData = ntpData;
            this.ntpPacket = ntpPacket;
        }

        public void run() {
            System.out.println("Server started!");

            seventyOffset = 70 * 365; // days in 70 years
            seventyOffset += 17; // add days for leap years between 1900 and 1970
            seventyOffset *= 24; // hours in a day
            seventyOffset *= 60; // minutes in an hour
            seventyOffset *= 60; // seconds in a minute
            seventyOffset *= 1000; // milliseconds in a second

            try {
                initPacket();
                String threadName = Thread.currentThread().getName();

                String message = "in HandleClient";
                System.out.format("%s: %s%n", threadName, message); // in thread run()

                DatagramPacket sendPacket = new DatagramPacket(ntpData, ntpData.length, ntpPacket.getAddress(),
                        ntpPacket.getPort());
                ntpSocket.send(sendPacket);

                System.out.println("in thread after send data to client");

                System.out.println("IPAddress=" + ntpPacket.getAddress() + " port=" + ntpPacket.getPort());

                long cendtime = System.currentTimeMillis();
                System.out.println("time=" + (cendtime - cstarttime));
            } catch (SocketException e) {
                System.out.println("Socket exception in run()");
            } catch (IOException e) {
            }
        }

        private void setOrigTime() {
            toBytes(transmitTimestamp, originateOffset);
        }

        private void setReferenceTime() {
            toBytes(transmitTimestamp, referenceOffset);
        }

        private void setReceiveTime() {
            GregorianCalendar startCal = new GregorianCalendar();
            long startMillis = startCal.getTimeInMillis();
            transmitMillis = startMillis + seventyOffset;
            toBytes(transmitMillis, receiveOffset);
        }

        private void setTransmitTime() {
            GregorianCalendar startCal = new GregorianCalendar();
            long startMillis = startCal.getTimeInMillis();
            transmitMillis = startMillis + seventyOffset;
            toBytes(transmitMillis, transmitOffset);
        }

        public void toBytes(long n, int offset) {
            long intPart = 0;
            long fracPart = 0;
            intPart = n / 1000;
            fracPart = ((n % 1000) / 1000) * 0X100000000L;

            ntpData[offset + 0] = (byte) (intPart >>> 24);
            ntpData[offset + 1] = (byte) (intPart >>> 16);
            ntpData[offset + 2] = (byte) (intPart >>> 8);
            ntpData[offset + 3] = (byte) (intPart);

            ntpData[offset + 4] = (byte) (fracPart >>> 24);
            ntpData[offset + 5] = (byte) (fracPart >>> 16);
            ntpData[offset + 6] = (byte) (fracPart >>> 8);
            ntpData[offset + 7] = (byte) (fracPart);

        }

        public long toLong(int offset) {

            long intPart = ((((long) ntpData[offset + 3]) & 0xFF)) +
                    ((((long) ntpData[offset + 2]) & 0xFF) << 8) +
                    ((((long) ntpData[offset + 1]) & 0xFF) << 16) +
                    ((((long) ntpData[offset + 0]) & 0xFF) << 24);

            long fracPart = ((((long) ntpData[offset + 7]) & 0xFF)) +
                    ((((long) ntpData[offset + 6]) & 0xFF) << 8) +
                    ((((long) ntpData[offset + 5]) & 0xFF) << 16) +
                    ((((long) ntpData[offset + 4]) & 0xFF) << 24);
            long millisLong = (intPart * 1000) + (fracPart * 1000) / 0X100000000L;

            return millisLong;
        }

        private void initPacket() {
            try {
                ntpData[0] = 0x1C;
                for (int i = 1; i < 16; i++) {
                    ntpData[i] = 0;
                }

                setReferenceTime();
                setOrigTime();
                setReceiveTime();
                setTransmitTime();
            } catch (Exception e) {
            }

        }
    }

    // Threading
    public void nonStatic(DatagramSocket ntpSocket, DatagramPacket ntpPacket, byte[] ntpData, long stime) {
        Thread t = new Thread(new NTPClientHandler(ntpSocket, ntpPacket, ntpData, stime));
        t.start();
    }

    public static void main(String[] args) {
        NTPServer ntpserver = new NTPServer();
        try {

            // Port number should be 123 for server and 1023 for client, but 123 only works
            // when I'm on different wifi ¯\_(ツ)_/¯
            DatagramSocket ntpSocket = new DatagramSocket(1023);
            byte[] receiveData = new byte[48];

            while (true) {

                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                ntpSocket.receive(receivePacket);
                long cstarttime = System.currentTimeMillis();

                receiveData = receivePacket.getData();

                ntpserver.nonStatic(ntpSocket, receivePacket, receiveData, cstarttime);
            }
        } catch (SocketException e) {
            System.out.println("Socket excpetion in main");
        } catch (IOException e) {
        }
    }
}