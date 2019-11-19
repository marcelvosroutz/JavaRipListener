package BGP;

import java.io.BufferedOutputStream;
import java.util.Arrays;

public class keepAliveTread extends Thread {
    Thread runner;

    private int holdTime, keepAliveTime;
    bgpListener bgpListener;

    public keepAliveTread(int holdTime, int keepAliveTime, bgpListener bgpListener) {
        runner = new Thread(this, "keepAliveTread Thread");
        this.holdTime=holdTime;
        this.keepAliveTime=keepAliveTime;
        this.bgpListener = bgpListener;
    }

    public void run() {
        while (true) {
            try {
              // make thread sleep for keepAliveTime * 1000 (msec) before sending keepalive
              sleep(keepAliveTime*1000);
              System.out.println("Sending our keepalive!");
            } catch (InterruptedException e) {
                System.out.println("Cannot sleep! Phun intended :)");
            }
        }
    }

}