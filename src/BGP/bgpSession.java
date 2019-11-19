package BGP;

public class bgpSession {
    // implements session tracking for BGP
    // implements BGP finite state machine

    // define finite state machines starting from default (IDLE) to (ESTABLISHED)
    public static final byte BGP_IDLE = 0x01, BGP_CONNECT = 0x02, BGP_ACTIVE = 0x03, BGP_OPENSENT = 0x04, BGP_OPENCONFIRM = 0x05, BGP_ESTABLISHED = 0x06;


}
