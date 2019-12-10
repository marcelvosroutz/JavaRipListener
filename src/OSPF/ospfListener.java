package OSPF;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class ospfListener extends Thread  {

    Thread runner;
    LinkedBlockingQueue routeHandler, loggingQueue;

    // our BGP settings
    private static int ourAutonomousSystemNumber = 11111;
    private static int ourHoldTime = 180;
    private static int ourKeepAliveTimer = ourHoldTime/3;
    private byte[] ourIdentifier = new byte[4];

    private ServerSocket conn;

    private ArrayList bgpSessions = new ArrayList();

    public ospfListener(LinkedBlockingQueue routeHandler) {
        runner = new Thread(this, "bgpListener Thread");
        this.routeHandler = routeHandler;
    }


    public void run() {

        // Start thread for sending keepalive information
        //Thread keepAliveTread = new Thread (new keepAliveTread(ourHoldTime, ourKeepAliveTimer, bgpSessions));
        //keepAliveTread.start();

        try {
            conn = new ServerSocket(179);

            System.out.println("Waiting for peering request on TCP/179 (BGP) traffic");

            // if session gets closed.. start a new one instantly to listen for a new session
            while (true) {

            }
        } catch (Exception e) {}
    }
}