import java.util.*;

public class ripRouteTable {

    private List routingTable;

    public ripRouteTable() {
        // construct empty routing table
        // routing table will be unique based on combination of address+subnet+gateway+metric
        // probably can be improved to fully match specs of a routing table...
        routingTable = new LinkedList();
    }

    public void addRipRouteEntry (ripRouteEntry ripRouteEntry) {
        // traverse the routing table to see if we have duplicate route entries; if found, update the timestamp
        for (Iterator iter = routingTable.iterator() ; iter.hasNext();) {
            ripRouteEntry routeEntry = (ripRouteEntry) iter.next();
            // ugly loop to determine duplicate route entries...
            if (ripRouteEntry.getNetworkAddress().equals(routeEntry.getNetworkAddress()) && ripRouteEntry.getSubnetAddress().equals(routeEntry.getSubnetAddress()) && ripRouteEntry.getNextHop().equals(routeEntry.getNextHop()) && ripRouteEntry.getRouteMetric() == routeEntry.getRouteMetric()) {
                //System.out.println("Route already exist; updating lastUpdatedTimestamp. Routing table size: " + routingTable.size());
                routeEntry.updateRouteTimeStamp();
                return;
            }
        }
        // if we reached this part; lets assume its a new route :)
        //System.out.println("Adding new route to routing table");
        routingTable.add(ripRouteEntry);
    }

    public void printRoutingTable() {
        System.out.println("Routing Table:");
        for (Iterator iter = routingTable.iterator() ; iter.hasNext();) {
            ripRouteEntry routeEntry = (ripRouteEntry) iter.next();
            System.out.println("R   " + routeEntry.getNetworkAddress() + "/" + routeEntry.getMaskBits() + " [" + routeEntry.getRouteMetric() + "] via " + routeEntry.getNextHop());
        }
    }

    public int getRipRouteTableSize() {
        return routingTable.size();
    }
}