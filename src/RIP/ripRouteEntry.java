package RIP;

import javax.swing.plaf.synth.SynthTextAreaUI;
import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;

public class ripRouteEntry {
    private int ripVersion;
    private int addressFamily = 0; // ripVersion2 bytes for Address Family of network
    private int routeTag = 0; // 2 bytes for route tag
    private byte[] networkAddress = new byte[4]; // 4 bytes parsed into a String for network address
    private byte[] subnetAddress = new byte[4]; // 4 bytes parsed into a String  for subnet mask
    private byte[] nextHop = new byte[4]; // 4 bytes parsed into a String  for next hop address
    private String subnetAddressString = new String();
    private int maskBits = 0;
    private int routeMetric = 0; // 4 bytes for route metric
    private long routeTimeStamp = System.currentTimeMillis();

    public  ripRouteEntry(int ripVersion) {
        this.ripVersion = ripVersion;
        // constructor parses a byte[20] block containing the route information
    }

    public void parseRipRouteEntry(byte[] ripPayload, int ripVersion, byte[] sourceIpAddress, int offset) throws IOException {

        // parse addressFamily & routeTag
        addressFamily =  ((ripPayload[0+offset] & 0xFF) << 8 ) | ((ripPayload[1+offset] & 0xFF) << 0 );
        routeTag =  ((ripPayload[2+offset] & 0xFF) << 8 ) | ((ripPayload[3+offset] & 0xFF) << 0 );

        // read network address
        System.arraycopy(ripPayload, 4+offset, networkAddress, 0, 4);

        // subnet calculations
        System.arraycopy(ripPayload, 8+offset, subnetAddress, 0, 4);

        // Store subnet also as string for quick reference
        subnetAddressString = ((subnetAddress[0]  & 0xFF) + "." + (subnetAddress[1]  & 0xFF) + "." + (subnetAddress[2]  & 0xFF) + "." + (subnetAddress[3]  & 0xFF));

        // set CIDR mask
        maskBits = convertNetmaskToCIDR(InetAddress.getByName(subnetAddressString));

        // Do an override in case of RIPv1 / set to classfull addressing
        if (ripVersion==1) {
            int firstOctet = networkAddress[0] & 0xFF;
            if (firstOctet >= 0 && firstOctet <= 127) {
                subnetAddressString="255.0.0.0";
                maskBits=8;
            } else if (firstOctet >= 128 && firstOctet <= 191) {
                subnetAddressString="255.255.0.0";
                maskBits=16;
            } else if (firstOctet >= 192 && firstOctet <= 223) {
                subnetAddressString="255.255.255.0";
                maskBits=24;
            }
        }

        // nextHop calculations
        System.arraycopy(ripPayload, 12+offset, nextHop, 0, 4);

        if ((nextHop[0] + nextHop[1] + nextHop[2] + nextHop[3]) == 0) {
            nextHop = sourceIpAddress;
        }

        // Metric calculations
        routeMetric = (((ripPayload[16+offset] & 0xFF) << 24 ) | ((ripPayload[16+offset] & 0xFF) << 16 ) | ((ripPayload[16+offset] & 0xFF) << 8 ) | (ripPayload[17+offset] & 0xFF));

    }

    public int getAddressFamily() {
        return addressFamily;
    }

    public int getRouteTag() {
        return  routeTag;
    }

    public byte[] getNetworkAddressBytes() {
        return networkAddress;
    }

    public String getNetworkAddress() {
        return ((networkAddress[0] & 0xFF) + "." + (networkAddress[1] & 0xFF) + "." + (networkAddress[2] & 0xFF) + "." + (networkAddress[3] & 0xFF)); // read subnet address per octet
    }

    public int getCIDRMask() {
        return maskBits;
    }

    public String getSubnetAddress() {
        return ((subnetAddress[0] & 0xFF) + "." + (subnetAddress[1] & 0xFF) + "." + (subnetAddress[2] & 0xFF) + "." + (subnetAddress[3] & 0xFF)); // read subnet address per octet
    }
    public String getNextHop() {
        return ((nextHop[0] & 0xFF) + "." + (nextHop[1] & 0xFF) + "." + (nextHop[2] & 0xFF) + "." + (nextHop[3] & 0xFF)); // read subnet address per octet
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

    public byte[] toByteArray(int value) {
        return new byte[] {
                (byte)(value >> 24),
                (byte)(value >> 16),
                (byte)(value >> 8),
                (byte)value
        };
    }

    public int fromByteArray(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8 ) |
                ((bytes[3] & 0xFF) << 0 );
    }

    public static int convertNetmaskToCIDR(InetAddress netmask){

        byte[] netmaskBytes = netmask.getAddress();
        int cidr = 0;
        boolean zero = false;
        for(byte b : netmaskBytes){
            int mask = 0x80;

            for(int i = 0; i < 8; i++) {
                int result = b & mask;
                if(result == 0) {
                    zero = true;
                } else if(zero) {
                    throw new IllegalArgumentException("Invalid netmask.");
                } else {
                    cidr++;
                }
                mask >>>= 1;
            }
        }
        return cidr;
    }

}