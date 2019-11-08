package RIP;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.LinkedBlockingQueue;

public class ripListener extends Thread  {

    Thread runner;
    LinkedBlockingQueue routeHandler;

    // define RIP command tags
    public static final byte RIP_REQUEST = 0x01, RIP_RESPONSE = 0x02, RIP_TRACEON = 0x03, RIP_TRACEOFF =0x04, RIP_RESERVED = 0x05;
    private int ripCommand , ripVersion;

    private MulticastSocket recvSock, sendSock;

    public ripListener(LinkedBlockingQueue routeHandler) {
        runner = new Thread(this, "ripMultiCastListener Thread");
        this.routeHandler = routeHandler;
    }

    public void run() {

        try {
            byte[] buffer = new byte[512];

            // construct our socket
            recvSock = new MulticastSocket(520);
            InetAddress multiCastGroup = InetAddress.getByName("224.0.0.9");
            recvSock.joinGroup(multiCastGroup);

            //sendSock = new MulticastSocket(); // create a socket for sending our 10.0.0.0/8 route
            //sendSock.setBroadcast(true); // make socket broadcast aware
            //sendSock.setLoopbackMode(false);

            byte[] ripHeader;
            byte[] ripPayload;

            System.out.println("Tread: Waiting for Broadcast (255.255.255.255) and Multicast (224.0.0.9) packets on UDP/520 (RIP v1/v2) traffic");

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (true) {
                    recvSock.receive(packet);

                    ripHeader = new byte[4];
                    System.arraycopy(packet.getData(), 0, ripHeader, 0, 4);

                    ripPayload = new byte[packet.getLength()-4];
                    System.arraycopy(packet.getData(), 4, ripPayload, 0, (packet.getLength() - 4));

                    ripCommand = ripHeader[0]; // read command
                    ripVersion = ripHeader[1]; // read version

                    switch (ripCommand) {
                        case RIP_REQUEST:
                            // I suppose we send our routing table here (or a specific route? RTFM what we should do when this command arrives)
                            break;
                        case RIP_RESPONSE:
                            int qty = ((packet.getLength() - 4) / 20); // first 4 bytes are 'unique' (command and version), the rest is a repetitive block of 20 bytes per routeEntry
                            System.out.println("Received RIP version: " + ripVersion + " update from " + packet.getAddress().getCanonicalHostName() + ", containing  " + qty + " addresses");
                            // traverse the packet and parse ripListeners.ripRouteEntry objects into ripRoutes until we read no more networks
                            try {
                                for (int i = 0; i <= qty; i++) {
                                    // parse block of 20 bytes as ripListeners.ripRouteEntry
                                    ripRouteEntry ripRouteEntry = new ripRouteEntry(ripVersion);
                                    ripRouteEntry.parseRipRouteEntry(ripPayload, ripVersion, packet.getAddress().getAddress());
                                    System.out.println(
                                            "Route Address Family: " + ripRouteEntry.getAddressFamily() + ", Route Tag: " + ripRouteEntry.getRouteTag() + ", Route: " + ripRouteEntry.getNetworkAddress() +
                                                    " Mask: " + ripRouteEntry.getSubnetAddress() + ", nextHop: " + ripRouteEntry.getNextHop() + " , Metric: " + ripRouteEntry.getRouteMetric()
                                    );
                                    try {
                                        // put the newly learned route into our queueHandler
                                        routeHandler.put(ripRouteEntry);
                                    } catch (InterruptedException e) {
                                        System.out.println("Unable to put newly learned route into queueHandler: " + e);
                                    }
                                }

                                // just send a default back to the router for now... will add this later
                                //sendDefaultRoute(sendSock, ripVersion);
                        } catch (IOException errorMessage) {
                                // error parsing RIP route entry
                                System.out.println("Error parsing RIP route entry:" + errorMessage);
                            }
                            break;
                        case RIP_TRACEON:
                            // Obsolete.  Messages containing this command are to be ignored
                            System.out.println("Received obsolete RIP command " + RIP_TRACEON);
                            break;
                        case RIP_TRACEOFF:
                            // Obsolete.  Messages containing this command are to be ignored.
                            System.out.println("Received obsolete RIP command " + RIP_TRACEOFF);
                            break;
                        case RIP_RESERVED:
                            //This value is used by Sun Microsystems for its own purposes.  If new commands are added in any succeeding version, they should begin with 6.
                            //Messages containing this command may safely be ignored by implementations that do not choose to respond to it.
                            System.out.println("Received obsolete RIP command " + RIP_RESERVED);
                            break;
                        default:
                            // Received unknown RIP command
                            System.out.println("Unknown RIP message received from " + packet.getAddress() + ", command: " + ripCommand + " size: " + packet.getLength());
                    }
                    //}

                          }
        } catch (IOException errorMessage) {
            System.out.println("Unable to listen on socket: " + errorMessage);
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