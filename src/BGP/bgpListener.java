package BGP;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class bgpListener extends Thread  {

    Thread runner;
    LinkedBlockingQueue routeHandler, loggingQueue;

    // our BGP settings
    private static int ourAutonomousSystemNumber = 11111;
    private static int ourHoldTime = 60;
    private static int ourKeepAliveTimer = ourHoldTime/3;
    private byte[] ourIdentifier = new byte[4];

    private ServerSocket conn;

    private ArrayList bgpSessions = new ArrayList();

    public bgpListener(LinkedBlockingQueue routeHandler) {
        runner = new Thread(this, "bgpListener Thread");
        this.routeHandler = routeHandler;
    }


    public void run() {

        // Start thread for sending keepalive information
        Thread keepAliveTread = new Thread (new keepAliveTread(ourHoldTime, ourKeepAliveTimer, bgpSessions));
        keepAliveTread.start();

        try {
            conn = new ServerSocket(179);

            System.out.println("Waiting for peering request on TCP/179 (BGP) traffic");

            // if session gets closed.. start a new one instantly to listen for a new session
            while (true) {
                try {
                    // Accept connection
                    Socket clientSocket = conn.accept();
                    bgpSession client = new bgpSession(clientSocket, ourAutonomousSystemNumber, ourHoldTime, ourKeepAliveTimer, ourIdentifier);
                    client.start();

                    // store session in our session-manager
                    bgpSessions.add(client);
                } catch (IOException errorMessage) {
                    System.out.println("TCP Session closed");
                }
            }
        } catch (Exception e) {}
    }
}