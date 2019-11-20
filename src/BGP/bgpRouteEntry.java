package BGP;

import java.util.Iterator;
import java.util.LinkedList;

public class bgpRouteEntry {

    private static byte[] prefix = new byte[4];
    private static int prefixLength;

    private LinkedList bgpAttributes = new LinkedList();

    public bgpRouteEntry(byte[] pathAttributes, byte[] prefix, int prefixLength) {

        // parse BGP path attributes + Network Layer Route Information (NLRI) to form a bgpRouteEntry object.
        int offset = 0;
        int flag = 0, typeCode = 0, attributeLength = 0;

        if (pathAttributes.length > 0) {
            while (offset < pathAttributes.length) {
                // we got at least one path attribute
                flag = pathAttributes[(0 + offset)]; // last 0000 of byte are 0;
                typeCode = pathAttributes[(1 + offset)];
                attributeLength = pathAttributes[(2 + offset)];
                byte[] attributeValue = new byte[attributeLength];
                System.arraycopy(pathAttributes, (3 + offset), attributeValue, 0, attributeLength);
                bgpAttribute bgpAttribute = new bgpAttribute(flag, typeCode, attributeLength, attributeValue);
                bgpAttributes.add(bgpAttribute);
                offset = offset + attributeLength + 3; // offset for next attributeValue
            }
        }

        this.prefix = prefix;
        this.prefixLength = prefixLength;
    }

    public String printPrefixEntry() {

        // convert prefix to human readable prefix
        String prefixString = new String();
        for (int i = 0; i < prefix.length; i++) {
            prefixString = prefixString + "." + (prefix[i] & 0xFF);
        }
        prefixString = prefixString.substring(1);

        System.out.println("    BGP_UPDATE -> PREFIX: " + prefixString + "/" + prefixLength + " attributes:");

        Iterator iterator = bgpAttributes.iterator();
        while (iterator.hasNext()) {
            bgpAttribute attributes = (bgpAttribute)iterator.next();
            System.out.println("             * Type: " + attributes.getTypeCode() + " (" + attributes.getDescription() + ")  [Optional: " + attributes.isOptional() + ", Transitive: " + attributes.isTransitive() + ", Partial: " + attributes.isPartial() + "]"
                            + " Set to :" + byteArrayToHex(attributes.getAttributeValue()));
        }

        return "";
    }

    // stolen somewhere online for easy debugging packet data; displays byte array as hex string (SO NOT PART OF CODING CHALLENGE!)
    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x ", b));
        return sb.toString();
    }
}
