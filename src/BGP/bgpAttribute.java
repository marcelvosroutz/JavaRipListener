package BGP;

public class bgpAttribute {

    private int flag, typeCode, length;
    private byte[] attributeValue;
    private static final int OPTIONAL = 0x80, TRANSITIVE = 0x40, PARTIAL = 0x20, EXTENDED_LENGTH = 0x10;
    private boolean isOptional,isTransitive,isPartial,isExtendedLength;

    private String[][] bgpAttributesTable = new String[][]{
            // Type Code / Description / Optional / Transitive / Partial
            {"", "", "", "", ""}, /** NULL our first try as arrays start at 0 **/
            {"1", "ORIGIN", "", "", ""},
            {"2", "AS_PATH", "", "", ""},
            {"3", "NEXT_HOP", "", "", ""},
            {"4", "MULTI_EXIT_DISC", "", "", ""}
    };

    bgpAttribute(int flag, int typeCode, int length, byte[] attribute) {
        // parse attributeFlag
        this.flag = flag;
        isOptional =  ((OPTIONAL & flag) != 0);
        isTransitive = ((TRANSITIVE & flag) != 0);
        isPartial = ((PARTIAL & flag) != 0);
        isExtendedLength = ((EXTENDED_LENGTH & flag) != 0);
        this.typeCode = typeCode;
        this.length = length;
        this.attributeValue = new byte[length];
        this.attributeValue = attribute;
    }

    public String getDescription() {
        return bgpAttributesTable[typeCode][1];
    }

    public boolean isOptional() {
        return isOptional;
    }

    public boolean isTransitive() {
        return isTransitive;
    }

    public boolean isPartial() {
        return isPartial;
    }

    public boolean isExtendedLength() {
        return isExtendedLength;
    }

    public int getLength() {
        return length;
    }

    public int getTypeCode() {
        return typeCode;
    }

    public int getFlag() {
        return flag;
    }

    public byte[] getAttributeValue() {
        return attributeValue;
    }
}
