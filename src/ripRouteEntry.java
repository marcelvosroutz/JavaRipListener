import java.io.DataInputStream;
import java.io.IOException;

public class ripRouteEntry {
    private int addressFamily = 0; // 2 bytes for Address Family of network
    private int routeTag = 0; // 2 bytes for route tag
    private String networkAddress = ""; // 4 bytes parsed into a String for network address
    private String subnetAddress = ""; // 4 bytes parsed into a String  for subnet mask
    private int maskBits = 0;
    private String nextHop = ""; // 4 bytes parsed into a String  for next hop address
    private int routeMetric = 0; // 4 bytes for route metric
    private long routeTimeStamp = System.currentTimeMillis();

    public  ripRouteEntry() {
        // constructor parses a byte[20] block containing the route information
    }

    public void parseRipRouteEntry(DataInputStream ripRoute, int ripVersion, String sourceIpAddress) throws IOException {

        addressFamily = ripRoute.readUnsignedShort(); // read two bytes (address family; always 02 for TCP/IP thanks for the lesson tom!)
        routeTag =  ripRoute.readUnsignedShort(); // read two bytes (RIP route Tag);
        int firstOctet = (ripRoute.readByte() & 0xFF); // store first octect for subnet calculations
        networkAddress = firstOctet + "." + (ripRoute.readByte()  & 0xFF) + "." + (ripRoute.readByte()  & 0xFF) + "." + (ripRoute.readByte()  & 0xFF); // read network address per octet
        if (ripVersion==2) {
            subnetAddress = (ripRoute.readByte() & 0xFF) + "." + (ripRoute.readByte() & 0xFF) + "." + (ripRoute.readByte() & 0xFF) + "." + (ripRoute.readByte() & 0xFF); // read subnet address per octet
        } else {
            // RIPv1 is classfull so we have to calculate the subnet based on the first byte of the address
            // Class A: 0 - 127
            // Class B: 128 - 191
            // Class C: 192 - 223
            // Class D: 224 - 239
            // Class E: 240 - 255

            ripRoute.skipBytes(4); // skip the other three bytes in the DataInputStream
            if (firstOctet >= 0 && firstOctet <= 127) {
                subnetAddress="255.0.0.0";
                maskBits=8;
            } else if (firstOctet >= 128 && firstOctet <= 191) {
                subnetAddress="255.255.0.0";
                maskBits=16;
            } else if (firstOctet >= 192 && firstOctet <= 223) {
                subnetAddress="255.255.255.0";
                maskBits=24;
            } else if (firstOctet >= 224 && firstOctet <= 239) {
                subnetAddress="no-subnet-mask";
            } else {
                subnetAddress="no-subnet-mask";
            }
            if (ripVersion==2) {
                nextHop = (ripRoute.readByte() & 0xFF) + "." + (ripRoute.readByte() & 0xFF) + "." + (ripRoute.readByte() & 0xFF) + "." + (ripRoute.readByte() & 0xFF); // read subnet address per octet
            } else {
                ripRoute.skipBytes(4); // skip 4 bytes in the DataInputStream
                nextHop=sourceIpAddress.substring(1);
            }
        }
        routeMetric = ripRoute.readInt(); // read metric
    }
    public int getAddressFamily() {
        return addressFamily;
    }
    public int getRouteTag() {
        return  routeTag;
    }
    public String getNetworkAddress() {
        return networkAddress;
    }
    public String getSubnetAddress() {
        return subnetAddress;
    }
    public String getNextHop() {
        return nextHop;
    }
    public int getRouteMetric() {
        return routeMetric;
    }
    public int getMaskBits() {
        return maskBits;
    }
    public void updateRouteTimeStamp() {
        routeTimeStamp = System.currentTimeMillis();
    }
}