package BGP;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

public class bgpSession extends Thread  {

    Thread runner;
    LinkedBlockingQueue routeHandler, loggingQueue;

    // our BGP settings
    private static int ourAutonomousSystemNumber;
    private static int ourHoldTime;
    private static int ourKeepAliveTime;
    private byte[] ourIdentifier;
    private int ourCurrentState = STATE_IDLE;

    // define BGP command TYPE's
    private static final byte BGP_OPEN = 0x01, BGP_UPDATE = 0x02, BGP_NOTIFICATION = 0x03, BGP_KEEPALIVE = 0x04;

    // define finite state machine
    public static final int STATE_IDLE = 1, STATE_CONNECT = 2, STATE_ACTIVE = 3, STATE_OPEN_SENT = 4, STATE_OPEN_CONFIRM = 5, STATE_ESTABLISHED = 6;

    // BGP ERROR CODES
    private String[] bgpNotificationCodes = new String[] {"", "MESSAGE_HEADER_ERROR", "OPEN_MESSAGE_ERROR", "UPDATE_MESSAGE_ERROR", "HOLD_TIME_EXPIRED", "FINITE_STATE_ERROR","CEASE"};

    // Construct a MultiDimensional array with BGP error cores in Plain Text
    private String[][] bgpNotificationSubCodes = new String[][]{
            {"", "", "", "", "", "", "", ""},
            {"", "Connection Not Synchronized", "Bad Message Length", "Bad Message Type"},
            {"", "Unsupported Version Number", "Bad Peer AS", "Bad BGP Identifier", "Unsupported Optional Parameter", "Deprecated", "Unacceptable Hold Time"},
            {"", "", "", "", "", "", "", ""},
            {"", "", "", "", "", "", "", ""}
    };

    private DataInputStream is;
    private BufferedOutputStream os;
    
    private long lastSeenTimeStamp;

    private Socket socket;

    public bgpSession(Socket clientSocket, int autonomousSystemNumber, int holdTime, int keepAliveTime, byte[] identifier) {
        runner = new Thread(this, "bgpListener Thread");
        this.socket = clientSocket;
        this.ourAutonomousSystemNumber = autonomousSystemNumber;
        this.ourKeepAliveTime = keepAliveTime;
        this.ourHoldTime = holdTime;
        this.ourIdentifier = identifier;

        // Update finite state machine
        ourCurrentState = STATE_CONNECT;
        System.out.println("BGP peer transitioning from Idle to Connect");
    }

    public void run() {
        // accepted new BGP session
        System.out.println("New Client: Accepted BGP peering connection from + " + socket.getInetAddress().getHostName()+ ":" + socket.getPort());

        try {
               // define our in- and output streams
               is = new DataInputStream(socket.getInputStream()); // receiving stuff
               os = new BufferedOutputStream(socket.getOutputStream()); // sending stuff

                    //
                    // after we received a BGP_OPEN we need to send a keepalive
                    // After a TCP connection is established, the first message sent by each
                    // side is an OPEN message.  If the OPEN message is acceptable, a
                    // KEEPALIVE message confirming the OPEN is sent back.
                    sendBgpOpen();

                    // bgp header is 19 bytes
                    byte[] bgpHeader = new byte[19];
                    byte[] bgpPayload;

                    while (true) {
                        // loop datastream
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
                        //System.out.println("--");
                        //loggingQueue.add("Received BGP message from " + socket.getInetAddress().toString());
                        //System.out.println("BGP HEADER: " + byteArrayToHex(bgpHeader));
                        //System.out.println("BGP PAYLOAD: " + byteArrayToHex(bgpPayload));

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
                                System.out.println("BGP_OPEN -> Additional Parameters Length: " + (bgpPeerAdditionalParametersLength[0] & 0xFF));

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

                                if (ourCurrentState==STATE_OPEN_SENT) {
                                    ourCurrentState = STATE_OPEN_CONFIRM;
                                    System.out.println("BGP peer transitioning from OpenSent to OpenConfirm");
                                }

                                // after we received a BGP_OPEN we need to send a keepalive
                                // After a TCP connection is established, the first message sent by each
                                // side is an OPEN message.  If the OPEN message is acceptable, a
                                // KEEPALIVE message confirming the OPEN is sent back.
                                sendKeepAlive();


                                break;
                            case BGP_UPDATE:

                                // update peerLastSeen as we have got an update
                                lastSeenTimeStamp = System.currentTimeMillis();
                                
                                //      +-----------------------------------------------------+
                                //      |   Withdrawn Routes Length (2 octets)                |
                                //      +-----------------------------------------------------+
                                //      |   Withdrawn Routes (variable)                       |
                                //      +-----------------------------------------------------+
                                //      |   Total Path Attribute Length (2 octets)            |
                                //      +-----------------------------------------------------+
                                //      |   Path Attributes (variable)                        |
                                //      +-----------------------------------------------------+
                                //      |   Network Layer Reachability Information (variable) |
                                //      +-----------------------------------------------------+

                                System.out.println("bgpType: BGP_UPDATE");
                                System.out.println("PAYLOAD: " + byteArrayToHex(bgpPayload));


                                // handle the withdrawn routes
                                int withdrawnRoutesLenght = (((bgpPayload[0] & 0xFF ) << 8 ) | (bgpPayload[1]  & 0xFF));
                                System.out.println("Withdrawn routes: " + withdrawnRoutesLenght);

                                if (withdrawnRoutesLenght>0) {
                                    byte[] withdrawnRoutes = new byte[withdrawnRoutesLenght];
                                }

                                // skip 2 bytes + length of the routes
                                int offset = withdrawnRoutesLenght+2;

                                // handle the new routes
                                int totalPathAttributeLength = (((bgpPayload[0+offset] & 0xFF ) << 8 ) | (bgpPayload[1+offset]  & 0xFF));
                                System.out.println("Total Path Attribute Length: " + totalPathAttributeLength);

                                if (totalPathAttributeLength>0) {
                                    // parse BGP entry:
                                    byte[] pathAttributes = new byte[totalPathAttributeLength];
                                    System.arraycopy(bgpPayload, offset+2, pathAttributes, 0, totalPathAttributeLength);
                                    System.out.println("PathAttributes: " + byteArrayToHex(pathAttributes));

                                    bgpRouteEntry bgpRouteEntry = new bgpRouteEntry(pathAttributes);

                                    System.out.println("Parsed Route Entry attributes: " + bgpRouteEntry.printAllAttributes());


                                }

                                // skip 2 bytes + 2 bytes + length of the withdrawnroutes + length of  totalPathAttributeLength
                                offset = 2+2+withdrawnRoutesLenght+totalPathAttributeLength;

                                // parse prefix
                                int prefixLength = (int) Math.ceil((double) bgpPayload[0+offset] / 8);

                                int prefixBits =  (bgpPayload[0+offset]);
                                int prefixCount = 1;

                                if (prefixLength>0 ) { // we found a NLRI
                                    while (offset < bgpPayload.length) {

                                        // update prefix information
                                        prefixLength = (int) Math.ceil((double) bgpPayload[0+offset] / 8);
                                        prefixBits =  (bgpPayload[0+offset]);

                                        // loop multiple prefixes NLRI's
                                        byte prefix[] = new byte[4];
                                        System.arraycopy(bgpPayload, offset + 1, prefix, 0, prefixLength);

                                        // convert prefix to human readable prefix
                                        String prefixString = new String();
                                        for (int i = 0; i < prefix.length; i++) {
                                            prefixString = prefixString + "." + (prefix[i] & 0xFF);
                                        }
                                        prefixString = prefixString.substring(1);

                                        // echo results
                                        System.out.println("(" + prefixCount + ") NLRI -> Length: " + prefixLength + " prefix: " + prefixString + "/" + prefixBits);

                                        // skip 2 bytes + 2 bytes + length of the withdrawnroutes + length of  totalPathAttributeLength + 1 + prefixlength
                                        offset = offset + 1 + prefixLength;

                                        prefixCount++;
                                    }
                                }

                                break;
                            case BGP_NOTIFICATION:
                                // update peerLastSeen as we have got a package
                                lastSeenTimeStamp = System.currentTimeMillis();

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

                                // terminate session
                                socket.close();
                                ourCurrentState=STATE_IDLE;



                                break;
                            case BGP_KEEPALIVE:
                                // update peerLastSeen as we have got a package
                                lastSeenTimeStamp = System.currentTimeMillis();

                                // A KEEPALIVE message
                                // consists of only the message header and has a length of 19 octets.
                                //System.out.println("Received keepAlive from " + socket.getInetAddress().getHostName()+ ":" + socket.getPort());

                                // update our finite machine state, only allow to move to CONNECT state from OPEN_CONFIRM state
                                if (ourCurrentState==STATE_OPEN_CONFIRM) {
                                    ourCurrentState = STATE_ESTABLISHED;
                                    System.out.println("BGP peer transitioning from OpenConfirm to Connected");
                                }
                                break;

                            default:
                                System.out.print("bgpType: UNKNOWN bgp type: " + byteArrayToHex(bgpType));
                                break;
                        }
                    }
                } catch (IOException errorMessage) {
                    System.out.println("ERROR: " + errorMessage);
                    ourCurrentState=STATE_IDLE;
                }
    }

    private void sendBgpOpen() {
        // construct BGP keepAlive message as per RFC (16 x 0xFF, 0x00 length, type KEEPALIVE 0x04
        byte[] sendOpen = new byte[29];
        Arrays.fill(sendOpen, (byte)0xFF); // fill with all 0xFF
        sendOpen[16] = 0; // msg length = 23 (0 content but 19 byte header)
        sendOpen[17] = 29; // header is 19 + 10 bytes (open,version, as, holdtime, identfier
        sendOpen[18] = BGP_OPEN; // type BGP_OPEN = 1
        sendOpen[19] = 4; // type BGP_VERSION = 4
        sendOpen[20] = (byte) ((ourAutonomousSystemNumber >> 8) & 0xFF); // type BGP_AS
        sendOpen[21] = (byte) (ourAutonomousSystemNumber & 0xFF); // type BGP_AS
        sendOpen[22] = (byte) ((ourHoldTime >> 8) & 0xFF); // HOLD_TIME
        sendOpen[23] = (byte) (ourHoldTime & 0xFF); // HOLD_TIME
        sendOpen[24] = 1; // BGP IDENTIFIER
        sendOpen[25] = 2; // BGP IDENTIFIER
        sendOpen[26] = 3; // BGP IDENTIFIER
        sendOpen[27] = 4; // BGP IDENTIFIER
        sendOpen[28] = 0; // BGP ADDITIONAL PARAMETER LENGTH = 0 // We do not send additional parameters as this is a dumb java program :)

        // write data to socket
        try {
            System.out.println("--");
            System.out.println("Sending BGP_OPEN: " + byteArrayToHex(sendOpen));
            os.write(sendOpen);
            os.flush();
        } catch (Exception e) {
            System.out.println("Unable to send BGP_OPEN message: " + e);
        }

        // update our finite machine state
        if (ourCurrentState==STATE_CONNECT) {
            ourCurrentState = STATE_OPEN_SENT;
            System.out.println("BGP peer transitioning from Active to OpenSent");
        }
    }

    public void sendKeepAlive() {
        // construct BGP keepAlive message as per RFC (16 x 0xFF, 0x00 length, type KEEPALIVE 0x04
        byte[] keepAlive = new byte[19];
        Arrays.fill(keepAlive, (byte)0xFF); // fill with all 0xFF
        keepAlive[16] = 0x00; // msg length = 19 (0 content but 19 byte header)
        keepAlive[17] = 0x13; // msg length = 0 (0 content but 19 byte header)
        keepAlive[18] = 0x04; // type KEEPALIVE

        // write data to socket
        try {
            //System.out.println("Sending KeepAlive to " + socket.getInetAddress().getHostName() + ":" + socket.getPort());
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

    public int getOurHoldTime() {
        return ourHoldTime;
    }

    public int getOurKeepAliveTime() {
        return ourKeepAliveTime;
    }

    public int getFiniteStateMode() {
        return ourCurrentState;
    }
}