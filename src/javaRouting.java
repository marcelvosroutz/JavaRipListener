import RIP.*;
import OSPF.*;

// RAW socket support

import java.util.concurrent.LinkedBlockingQueue;

// Raw Sockets
import rocksaw.net.RawSocket;
import static rocksaw.net.RawSocket.PF_INET;
import static rocksaw.net.RawSocket.PF_INET6;
import static rocksaw.net.RawSocket.getProtocolByName;

// Routz RIP implementation challenge :)
// RIPv1 as per https://tools.ietf.org/html/rfc1058 in ripListener
// RIPv2 as per https://tools.ietf.org/html/rfc2453 in ripListener
// RIPv2 MD5: https://tools.ietf.org/html/rfc2082 in ripListener

public class javaRouting {

    // Lets define some addresses we want to send back
    public static final String[] defaultRoute = { "0.0.0.0", "0.0.0.0" }; // We will send a default of 0.0.0.0/0
    public static final String[] ourNetwork = { "10.50.75.0", "255.255.255.0" }; // We will send 10.50.75.0/24 

    // define RIP command tags
    public static final byte RIP_REQUEST = 0x01, RIP_RESPONSE = 0x02, RIP_TRACEON = 0x03, RIP_TRACEOFF =0x04, RIP_RESERVED = 0x05;

    // Set our RIP Timers
    public static int updateTimer = 30;
    public static int intervalTimer = 30;
    public static int holdTimer = 30;
    public static int flushTimer = 30;

    // create a routing table to store received routs
    private static routingTable routingTable = new routingTable();

    public static void main(String[] args) {
        System.out.println("Routz Coding Challenge v0.5");

        // construct a synchronised list to be used as  queue for exchange of RIP packets between broadcast and multicast listening threads
        LinkedBlockingQueue routeHandler = new LinkedBlockingQueue<>(1024);
        LinkedBlockingQueue loggingQueue = new LinkedBlockingQueue<>(1024);

        //myGui gui = new myGui(loggingQueue);

        // start thread for receiving RIPv2 traffic (As RIPv2 is backwards compatible; this one also handles RIPv1 traffic
        //Thread ripListener = new Thread (new ripListener(routeHandler));
        //ripListener.start();

        // start thread for initiating BGP peering session
        //Thread bgpListener = new Thread (new bgpListener(routeHandler));
        //bgpListener.start();

        // Lets do OSPF!
        Thread ospfListener = new Thread (new ospfListener(routeHandler));
        ospfListener.start();

        // start a thread for maintaining routing table entries
        // todo: cleanup routing tables.

        //Thread myGui = new Thread(new myGui());


        while (true) {
           try {
               // fetch routes from queue and insert into into our 'routing' table
               ripRouteEntry routeEntry = (ripRouteEntry)routeHandler.take();
               routingTable.addRipRouteEntry(routeEntry);

           } catch (InterruptedException e) {
               System.out.println("Cannot sleep! Phun intended :)");
           }
        }
    }
}