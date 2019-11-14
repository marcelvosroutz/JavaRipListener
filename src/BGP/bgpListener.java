package BGP;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.LinkedBlockingQueue;

public class bgpListener extends Thread  {

    Thread runner;
    LinkedBlockingQueue routeHandler;

    // define BGP command TYPE's
    public static final byte BGP_OPEN = 0x01, BGP_UPDATE = 0x02, BGP_NOTIFICATION = 0x03, BGP_KEEPALIVE = 0x04;

    // define BGP command tags
    private int bgpCommand;

    private ServerSocket conn;
    private DataInputStream is;
    private BufferedOutputStream os;

    private Socket recvSock, sendSock;

    public bgpListener(LinkedBlockingQueue routeHandler) {
        runner = new Thread(this, "bgpListener Thread");
        this.routeHandler = routeHandler;
    }

    public void run() {

        // if Session gets closed.. start a new one instantly to listen for a new session
        while (true) {
            try {
                // show start message
                System.out.println("Tread: Waiting for peering request on TCP/179 (BGP) traffic");

                // construct our socket
                conn = new ServerSocket(179);
                recvSock = conn.accept();
                is = new DataInputStream(recvSock.getInputStream());

                // bgp header is 19 bytes
                byte[] bgpHeader = new byte[19];
                byte[] bgpPayload;

                while (conn != null) {
                    // reads the first 19 bytes into BGP header
                    is.readFully(bgpHeader);

                    // parse the BGP header
                    System.out.println("BGP HEADER: " + byteArrayToHex(bgpHeader));
                    byte[] bgpMarker = new byte[16];
                    byte[] bgpLength = new byte[2];
                    byte[] bgpType = new byte[1];

                    // copy bytes from bgpHeader into marker, length and type
                    System.arraycopy(bgpHeader, 0, bgpMarker, 0, 16);
                    System.arraycopy(bgpHeader, 16, bgpLength, 0, 2);
                    System.arraycopy(bgpHeader, 18, bgpType, 0, 1);

                    // determine payLoad size (length field - header size)
                    int messageSize = (bgpLength[0] & 0xFF) + (bgpLength[1] & 0xFF) - 19;

                    bgpPayload = new byte[messageSize];

                    // read the BGP payload
                    is.readFully(bgpPayload);

                    switch (bgpType[0]) {
                        case BGP_OPEN:
                            System.out.println("Received BGP_OPEN");
                            System.out.println("PAYLOAD: " + byteArrayToHex(bgpPayload));
                            break;
                        case BGP_UPDATE:
                            System.out.println("Received BGP_UPDATE");
                            System.out.println("PAYLOAD: " + byteArrayToHex(bgpPayload));
                            break;
                        case BGP_NOTIFICATION:
                            System.out.println("Received BGP_NOTIFICATION");
                            System.out.println("PAYLOAD: " + byteArrayToHex(bgpPayload));
                            break;
                        case BGP_KEEPALIVE:
                            System.out.println("Received BGP_KEEPALIVE");
                            System.out.println("PAYLOAD: " + byteArrayToHex(bgpPayload));
                            break;
                        default:
                            System.out.print("Received UNKNOWN bgp type: " + byteArrayToHex(bgpType));
                            System.out.println("PAYLOAD: " + byteArrayToHex(bgpPayload));
                            break;
                    }
                }

            } catch (IOException errorMessage) {
                System.out.println("TCP Session closed");
            }
        }

    }

    // stolen somewhere online for easy debugging packet data; displays byte array as hex string
    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x ", b));
        return sb.toString();
    }
}