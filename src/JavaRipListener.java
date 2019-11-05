import java.io.*;
import java.net.*;

// Routz RIP implementation challenge :)
// Implemented as per https://tools.ietf.org/html/rfc1058
// 5/11/2019 routing table added
// todo: RIPV1 and RIPv2 support; can be started either in V1 or V2 mode
// todo: implement runner to send routing table every 30 seconds (timers) so we also send something back && clean up expired routes

public class JavaRipListener {

    public static final byte RIP_REQUEST = 0x01, RIP_RESPONSE = 0x02;
    public static final byte ripMode=1; // only v1 listener supported at the moment; but routeEntryClass already supports v2 packet format

    public static final byte[] ourNetwork = new byte[] { 10,0,0,0 };

    public static void main(String[] args){
        System.out.println("Java Rip Listener Challenge v0.2");
        DatagramSocket recvSock = null, sendSock;

        ripRouteTable ripRouteTable = new ripRouteTable();

        try {
            byte[] buffer = new byte[512];
            int type, ripVersion;

            if (ripMode==1) {
                recvSock = new DatagramSocket(520); // listen on all interfaces on port 520 (RIPv1)
                sendSock = new DatagramSocket(); // create a socket for sending our 10.0.0.0/8 route
                recvSock.setBroadcast(true); // make socket broadcast aware? Seems to be only needed if we want to send broadcast traffic..
                sendSock.setBroadcast(true); // make socket broadcast aware

            }

            System.out.println("Waiting for broadcast packet on UDP/520 (RIPv1) traffic");

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (true) {
                recvSock.receive(packet);

                if (!packet.getAddress().toString().equals("/192.168.0.112")) { // dont parse our own packets! loop loop loop!
                    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
                    type = dis.readByte(); // read command
                    ripVersion = dis.read(); // read version
                    dis.skipBytes(2); // read 2 bogus always 0 bytes according to RFC

                    switch (type) {
                        case RIP_REQUEST:
                            // I suppose we send our routing table here (or a specific route? RTFM what we should do when this command arrives)

                            break;
                        case RIP_RESPONSE:
                            int qty = ((packet.getLength() - 4) / 20); // first 4 bytes are 'unique' (command and version), the rest is a repetitive block of 20 bytes per routeEntry
                            System.out.println("Received RIP version: " + ripVersion + " update from " + packet.getAddress().getCanonicalHostName() + ", containing  " + qty + " addresses");
                            // traverse the packet and parse ripRouteEntry objects into ripRoutes until we read no more networks
                            try {
                                while (dis.available() > 0) {
                                    // parse block of 20 bytes as ripRouteEntry
                                    ripRouteEntry ripRouteEntry = new ripRouteEntry();
                                    ripRouteEntry.parseRipRouteEntry(dis, ripVersion, packet.getAddress().toString());
                                    System.out.println(
                                            "Route Address Family: " + ripRouteEntry.getAddressFamily() + ", Route Tag: " + ripRouteEntry.getRouteTag() + ", Route: " + ripRouteEntry.getNetworkAddress() +
                                                    " Mask: " + ripRouteEntry.getSubnetAddress() + ", nextHop: " + ripRouteEntry.getNextHop() + ", Metric: " + ripRouteEntry.getRouteMetric()
                                    );

                                    // insert learned routes into our 'routing' table
                                    ripRouteTable.addRipRouteEntry(ripRouteEntry);


                                }
                                ripRouteTable.printRoutingTable();

                                // as we dont have a runner thread yet; lets send our routing table in return (should be 30 seconds ish...)
                                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                DataOutputStream dos = new DataOutputStream(bos);

                                // construct the 4 byte header
                                dos.write(RIP_RESPONSE); // write command tag
                                dos.write(ripVersion); // write rip version
                                dos.writeShort(0); // write zero-reserved fields

                                // construct the 20 byte route payload
                                dos.writeShort( 0x2); // write address family
                                dos.writeShort(0); // write zero-reserved fields
                                dos.writeInt(0); // write IP
                                dos.writeInt(0); // write reserved (subnet in ripv2)
                                dos.writeInt(0); // write reserved (nexthop in ripv2)
                                dos.writeInt(1); // write metric of 1

                                dos.close();

                                // send our packet
                                byte[] ourPacket = bos.toByteArray();
                                System.out.println("Sending our 'default' route back: " + byteArrayToHex(ourPacket) + " length: " + ourPacket.length);
                                DatagramPacket sendPacket = new DatagramPacket(ourPacket, ourPacket.length, InetAddress.getByName("255.255.255.255"), 520);
                                sendSock.send(sendPacket);

                            } catch (IOException errorMessage) {
                                // error parsing RIP route entry
                                System.out.println("Error parsing RIP route entry:" + errorMessage);
                            }
                            break;
                        default:
                            System.out.println("Unknown RIP message received from " + packet.getAddress() + ", type: " + type + " size: " + packet.getLength());
                    }
                }
            }
        } catch (IOException errorMessage) {
            System.out.println("Unable to listen on socket: " + errorMessage);
        }
        recvSock.close();
    }

    // stolen somewhere online for easy debugging packet data; displays byte array as hex string
    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x ", b));
        return sb.toString();
    }
}