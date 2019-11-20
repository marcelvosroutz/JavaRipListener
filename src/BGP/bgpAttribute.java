package BGP;

public class bgpAttribute {

    private int flag, typeCode, length;
    private byte[] attributeValue;
    private static final int OPTIONAL = 0x80, TRANSITIVE = 0x40, PARTIAL = 0x20, EXTENDED_LENGTH = 0x10;
    boolean isOptional,isTransitive,isPartial,isExtendedLength;

    public bgpAttribute(int flag, int typeCode, int length, byte[] attribute) {
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


}
