package BGP;

import java.util.ArrayList;
import java.util.Iterator;

public class keepAliveTread extends Thread {
    Thread runner;

    private int holdTime, keepAliveTime;
    ArrayList bgpSessions;

    public keepAliveTread(int holdTime, int keepAliveTime, ArrayList bgpSessions) {
        runner = new Thread(this, "keepAliveTread Thread");
        this.holdTime = holdTime;
        this.keepAliveTime = keepAliveTime;
        this.bgpSessions = bgpSessions;
    }

    public void run() {
        while (true) {
            try {
              // make thread sleep for keepAliveTime * 1000 (msec) before sending keepalive (holdTimer/3)
              sleep(keepAliveTime*1000);

              // synchronize bgpSessions object (arrayList is not threadsafe!)
              synchronized (bgpSessions) {
                  Iterator iterator = bgpSessions.iterator();
                  while (iterator.hasNext()) {
                      bgpSession bgpSession = (bgpSession)iterator.next();

                      synchronized (bgpSession) {
                          // only when BGP peer has transitioned to Established state, start to send keepAlives
                          if (bgpSession.getFiniteStateMode() == bgpSession.STATE_ESTABLISHED) {
                              bgpSession.sendKeepAlive();
                          }
                      }
                  }
              }
            } catch (InterruptedException e) {
                System.out.println("Can't Sleep! Phun intended :)");
            }
        }
    }

}