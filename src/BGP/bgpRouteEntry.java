package BGP;

import java.util.LinkedList;

public class bgpRouteEntry {

    private static byte[] prefix = new byte[4];
    private static int prefixLength;

    private LinkedList bgpAttributes;

    private Object[][] bgpAttributesTable = new Object[][]{
            // Type Code / Description / Optional / Transitive / Partial
            {"", "", "", "", ""}, /** NULL our first try as arrays start at 0 **/
            {"1", "ORIGIN", "", "", ""},
            {"2", "AS_PATH", "", "", ""},
            {"3", "NEXT_HOP", "", "", ""},
            {"4", "MULTI_EXIT_DISC", "", "", ""}
    };

    public bgpRouteEntry(byte[] pathAttributes) {

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
    }

    public String printAllAttributes() {
        return bgpAttributes.toString();
    }

    // stolen somewhere online for easy debugging packet data; displays byte array as hex string (SO NOT PART OF CODING CHALLENGE!)
    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x ", b));
        return sb.toString();
    }
}
