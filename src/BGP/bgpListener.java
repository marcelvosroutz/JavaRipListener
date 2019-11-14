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

    // BGP ERROR CODES
    private String[] bgpNotificationCodes = new String[] {"", "MESSAGE_HEADER_ERROR", "OPEN_MESSAGE_ERROR", "UPDATE_MESSAGE_ERROR", "HOLD_TIME_EXPIRED", "FINITE_STATE_ERROR","CEASE"};
    private String[] bgpNotificationSubCodes = new String[] {"", "Connection Not Synchronized", "Bad Message Length", "Bad Message Type"};

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

        try {
            conn = new ServerSocket(179);

            // if Session gets closed.. start a new one instantly to listen for a new session
            while (true) {
                try {
                    // show start message
                    System.out.println("Tread: Waiting for peering request on TCP/179 (BGP) traffic");

                    // construct our socket
                    recvSock = conn.accept();
                    is = new DataInputStream(recvSock.getInputStream());

                    // bgp header is 19 bytes
                    byte[] bgpHeader = new byte[19];
                    byte[] bgpPayload;

                    while (conn != null) {
                        // reads the first 19 bytes into BGP header
                        is.readFully(bgpHeader);

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

                        // parse the BGP header
                        System.out.println("--");
                        System.out.println("Received BGP message from " + recvSock.getInetAddress().toString());
                        System.out.println("BGP HEADER: " + byteArrayToHex(bgpHeader));
                        System.out.println("BGP PAYLOAD: " + byteArrayToHex(bgpPayload));

                        switch (bgpType[0]) {
                            case BGP_OPEN:
                                //       0                   1                   2                   3
                                //       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
                                //       +-+-+-+-+-+-+-+-+
                                //       |    Version    |
                                //       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                                //       |     My Autonomous System      |
                                //       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                                //       |           Hold Time           |
                                //       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                                //       |                         BGP Identifier                        |
                                //       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                                //       | Opt Parm Len  |
                                //       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                                //       |                                                               |
                                //       |             Optional Parameters (variable)                    |
                                //       |                                                               |
                                //       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                                System.out.println("bgpType: BGP_OPEN");

                                byte[] bgpPeerVersion = new byte[1];
                                byte[] bgpPeerAutonomousSystem = new byte[2];
                                byte[] bgpPeerHoldTime = new byte[2];
                                byte[] bgpPeerIdentifier = new byte[4];

                                System.arraycopy(bgpPayload, 0, bgpPeerVersion, 0, 1);
                                System.arraycopy(bgpPayload, 1, bgpPeerAutonomousSystem, 0, 2);
                                System.arraycopy(bgpPayload, 3, bgpPeerHoldTime, 0, 2);
                                System.arraycopy(bgpPayload, 5, bgpPeerIdentifier, 0, 4);

                                System.out.println("BGP_OPEN -> Version: " + (bgpPeerVersion[0] & 0xFF));
                                System.out.println("BGP_OPEN -> Peer AS Number: " + (((bgpPeerAutonomousSystem[0] & 0xFF ) << 8 ) | (bgpPeerAutonomousSystem[1]  & 0xFF)));
                                System.out.println("BGP_OPEN -> Peer Hold Time: " + (((bgpPeerHoldTime[0] & 0xFF ) << 8 ) | (bgpPeerHoldTime[1]  & 0xFF)));
                                System.out.println("BGP_OPEN -> Identifier: " + ((bgpPeerIdentifier[0] & 0xFF) + "." + (bgpPeerIdentifier[1] & 0xFF) + "." + (bgpPeerIdentifier[2] & 0xFF) + "." + (bgpPeerIdentifier[3] & 0xFF)));

                                if (bgpPayload.length>9) {
                                    // this is not it.. check for Optional parameters...
                                }



                                break;
                            case BGP_UPDATE:
                                System.out.println("bgpType: BGP_UPDATE");
                                break;
                            case BGP_NOTIFICATION:
                                // BGP_NOTIFICATION message is sent when an error condiction is detected. connection is closed afterwards
                                // 0                   1                   2                   3
                                // 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
                                //     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                                //     | Error code    | Error subcode |   Data (variable)             |
                                //     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                                System.out.println("bgpType: BGP_NOTIFICATION");

                                // parse NotificationMessage
                                byte[] bgpErrorCode = new byte[1];
                                byte[] bgpErrorSubCode = new byte[1];

                                System.arraycopy(bgpPayload, 0, bgpErrorCode, 0, 1);
                                System.arraycopy(bgpPayload, 1, bgpErrorSubCode, 0, 1);

                                // print the Error Code value from lookup Table
                                System.out.println("BGP_NOTIFICATION -> Error Code: " + bgpNotificationCodes[bgpErrorCode[0]]);

                                // print the Error SubCode value from lookup Table
                                System.out.println("BGP_NOTIFICATION -> Error SubCode: " + bgpNotificationSubCodes[bgpErrorSubCode[0]]);

                                break;
                            case BGP_KEEPALIVE:
                                System.out.println("bgpType: BGP_KEEPALIVE");
                                break;
                            default:
                                System.out.print("bgpType: UNKNOWN bgp type: " + byteArrayToHex(bgpType));
                                break;
                        }
                    }

                } catch (IOException errorMessage) {
                    System.out.println("TCP Session closed");
                }
            }
        } catch (Exception e) {}
    }

    // stolen somewhere online for easy debugging packet data; displays byte array as hex string
    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x ", b));
        return sb.toString();
    }
}