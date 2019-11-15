package BGP;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

public class bgpListener extends Thread  {

    Thread runner;
    LinkedBlockingQueue routeHandler;

    // define BGP command TYPE's
    public static final byte BGP_OPEN = 0x01, BGP_UPDATE = 0x02, BGP_NOTIFICATION = 0x03, BGP_KEEPALIVE = 0x04;

    // BGP ERROR CODES
    private String[] bgpNotificationCodes = new String[] {"", "MESSAGE_HEADER_ERROR", "OPEN_MESSAGE_ERROR", "UPDATE_MESSAGE_ERROR", "HOLD_TIME_EXPIRED", "FINITE_STATE_ERROR","CEASE"};

    // Construct a multidimensinal array with BGP error cores in Plain Text
    private String[][] bgpNotificationSubCodes = new String[][]{
            {},
            {"", "Connection Not Synchronized", "Bad Message Length", "Bad Message Type"},
            {"", "Unsupported Version Number", "Bad Peer AS", "Bad BGP Identifier", "Unsupported Optional Parameter", "Deprecated", "Unacceptable Hold Time"}
    };

    // define BGP command tags
    private int bgpCommand;

    private ServerSocket conn;
    private DataInputStream is;
    private BufferedOutputStream os;

    private Socket socket;

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
                    socket = conn.accept();
                    is = new DataInputStream(socket.getInputStream()); // receiving stuff
                    os = new BufferedOutputStream(socket.getOutputStream()); // sending stuff

                    // bgp header is 19 bytes
                    byte[] bgpHeader = new byte[19];
                    byte[] bgpPayload;

                    while (conn != null) {
                        // BGP HEADER FORMAT:
                        //      0                   1                   2                   3
                        //      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
                        //      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                        //      |                                                               |
                        //      +                                                               +
                        //      |                                                               |
                        //      +                                                               +
                        //      |                           Marker                              |
                        //      +                                                               +
                        //      |                                                               |
                        //      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                        //      |          Length               |      Type     |
                        //      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

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
                        System.out.println("Received BGP message from " + socket.getInetAddress().toString());
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
                                byte[] bgpPeerAdditionalParametersLength = new byte[1];


                                System.arraycopy(bgpPayload, 0, bgpPeerVersion, 0, 1);
                                System.arraycopy(bgpPayload, 1, bgpPeerAutonomousSystem, 0, 2);
                                System.arraycopy(bgpPayload, 3, bgpPeerHoldTime, 0, 2);
                                System.arraycopy(bgpPayload, 5, bgpPeerIdentifier, 0, 4);
                                System.arraycopy(bgpPayload, 9, bgpPeerAdditionalParametersLength, 0, 1);


                                System.out.println("BGP_OPEN -> Version: " + (bgpPeerVersion[0] & 0xFF));
                                System.out.println("BGP_OPEN -> Peer AS Number: " + (((bgpPeerAutonomousSystem[0] & 0xFF ) << 8 ) | (bgpPeerAutonomousSystem[1]  & 0xFF)));
                                System.out.println("BGP_OPEN -> Peer Hold Time: " + (((bgpPeerHoldTime[0] & 0xFF ) << 8 ) | (bgpPeerHoldTime[1]  & 0xFF)));
                                System.out.println("BGP_OPEN -> Identifier: " + ((bgpPeerIdentifier[0] & 0xFF) + "." + (bgpPeerIdentifier[1] & 0xFF) + "." + (bgpPeerIdentifier[2] & 0xFF) + "." + (bgpPeerIdentifier[3] & 0xFF)));
                                //System.out.println("BGP_OPEN -> Additional Parameters Length: " + (bgpPeerAdditionalParametersLength[0] & 0xFF));

                                if ((bgpPeerAdditionalParametersLength[0] & 0xFF)>0) {
                                    // this is not it.... we received additional parameters
                                    // we will not parse these messages; just display the command codes and length...
                                    // as these extensions cover multiple RFC's -> https://www.bgp4.as/bgp-capability-codes

                                    byte[] bgpAdditionalParameters = new byte[(bgpPeerAdditionalParametersLength[0] & 0xFF)];
                                    System.arraycopy(bgpPayload, 10, bgpAdditionalParameters, 0, (bgpPeerAdditionalParametersLength[0] & 0xFF));

                                    int i = 0;

                                    while (i < (bgpPeerAdditionalParametersLength[0] & 0xFF)) {
                                        System.out.println("BGP_OPEN -> Additional Parameter: TYPE:" + bgpAdditionalParameters[0+i] + " LENGTH: " +  bgpAdditionalParameters[1+i] + " (No Further Parsing)");
                                        i=i+((bgpAdditionalParameters[1+i] & 0xFF)+2); // increase size of I with additional parameter length
                                    }
                                }

                                // after we received a BGP_OPEN we need to send a keepalive
                                // After a TCP connection is established, the first message sent by each
                                // side is an OPEN message.  If the OPEN message is acceptable, a
                                // KEEPALIVE message confirming the OPEN is sent back.

                                sendKeepAlive();

                                // sleep 3 seconds and then send our OPEN_MESSAGE
                                Thread.sleep(3000);

                                sendBgpOpen();


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
                                System.out.println("BGP_NOTIFICATION -> Error SubCode: " + bgpNotificationSubCodes[bgpErrorCode[0]][bgpErrorSubCode[0]]);

                                break;
                            case BGP_KEEPALIVE:
                                // A KEEPALIVE message consists of only the message header and has a length of 19 octets.

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

    private void sendBgpOpen() {
        // construct BGP keepAlive message as per RFC (16 x 0xFF, 0x00 length, type KEEPALIVE 0x04

        //04 8a 0f 00 b4 c8 64 32 3b 00

        byte[] sendOpen = new byte[29];
        Arrays.fill(sendOpen, (byte)0xFF); // fill with all 0xFF
        sendOpen[16] = 0; // msg length = 23 (0 content but 19 byte header)
        sendOpen[17] = 29; // header is 19 +
        sendOpen[18] = 1; // type BGP_OPEN
        sendOpen[19] = 4; // type BGP_VERSION_4
        sendOpen[20] = 10; // type BGP_AS
        sendOpen[21] = 10; // type BGP_AS
        sendOpen[28] = 0; // We do not send additional parameters
        // kiki
        // write data to socket
        try {
            System.out.println("--");
            System.out.println("Sending BGP_OPEN: " + byteArrayToHex(sendOpen));
            os.write(sendOpen);
            os.flush();
        } catch (Exception e) {
            System.out.println("Unable to send BGP_OPEN message: " + e);
        }
    }

    private void sendKeepAlive() {
        // construct BGP keepAlive message as per RFC (16 x 0xFF, 0x00 length, type KEEPALIVE 0x04
        byte[] keepAlive = new byte[19];
        Arrays.fill(keepAlive, (byte)0xFF); // fill with all 0xFF
        keepAlive[16] = 0x00; // msg length = 19 (0 content but 19 byte header)
        keepAlive[17] = 0x13; // msg length = 0 (0 content but 19 byte header)
        keepAlive[18] = 0x04; // type KEEPALIVE

        // write data to socket
        try {
            System.out.println("--");
            System.out.println("Sending KeepAlive: " + byteArrayToHex(keepAlive));
            os.write(keepAlive);
            os.flush();
        } catch (Exception e) {
            System.out.println("Unable to send keepAlive message: " + e);
        }

    }

    // stolen somewhere online for easy debugging packet data; displays byte array as hex string (SO NOT PART OF CODING CHALLENGE!)
    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x ", b));
        return sb.toString();
    }
}