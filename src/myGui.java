import RIP.ripRouteEntry;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.LinkedBlockingQueue;

public class myGui extends Frame {

    // Constructor to setup the GUI components
    public myGui(LinkedBlockingQueue loggingQueue) {

        JFrame.setDefaultLookAndFeelDecorated(true);

        JFrame frame = new JFrame("Java Coding Challenge!");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setSize(780,400);
        frame.setLayout(null);

        JTextArea logging = new JTextArea();
        logging.setSize(750,350);
        //frame.add(logging);

        JTable bgpNeighbourTable;

        String []colsTable0 = new String[] { "TEST", "TEST" };
        String [][]dataTable0 = new String[][] { new String[] {"1.1.1.1", "OpenSent"} };

        bgpNeighbourTable = new JTable(dataTable0, colsTable0);
        bgpNeighbourTable.setSize(200,100);

        frame.add( bgpNeighbourTable );
        frame.setVisible(true);

        JLabel label = new JLabel("STARTING MEH GUI!!@!!! OMG OGM");
        frame.getContentPane().add(label);

        while (true) {
            try {
                // fetch routes from queue and insert into into our 'routing' table
               String logLine = (String)loggingQueue.take();
               logging.setText(logging.getText() + "\n" + logLine);
            } catch (InterruptedException e) {
                System.out.println("Cannot sleep! Phun intended :)");
            }
        }

    }
}