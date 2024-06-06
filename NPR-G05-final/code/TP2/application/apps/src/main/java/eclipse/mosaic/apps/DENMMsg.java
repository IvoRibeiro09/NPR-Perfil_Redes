package eclipse.mosaic.apps;

import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;

public final class DENMMsg extends V2xMessage {
    private final String name;
    private final String route;
    private final Double lon;
    private final Double lat;
    private final String roadState;
    private final int MsgNumber;
    private int hops;
    private final EncodedPayload payload;
    private final static long minLen = 128L;

    public DENMMsg(MessageRouting routing, String name, String route, Double x, Double y, String roadState, int msgN, int hops) {
        super(routing);
        this.name = name;
        this.route = route;
        this.lon = x;
        this.lat = y;
        this.roadState = roadState;
        this.MsgNumber = msgN;
        this.hops = hops;
        payload = new EncodedPayload(16L, minLen);
    }

    public DENMMsg(MessageRouting routing, DENMMsg other) {
        super(routing);
        this.name = other.name;
        this.route = other.route;
        this.lon = other.lon;
        this.lat = other.lat;
        this.roadState = other.roadState;
        this.MsgNumber = other.MsgNumber;
        this.hops = other.hops;
        this.payload = new EncodedPayload(16L, minLen); // Assuming payload is immutable or shared safely
    }

    public String getName() {
        return name;
    }

    public String getRoute() {
        return route;
    }

    public Double getLon() {
        return lon;
    }

    public Double getLat() {
        return lat;
    }

    public String getRoadState() {
        return roadState;
    }

    public int getMessageNumber(){
        return MsgNumber;
    }

    public EncodedPayload getPayload() {
        return payload;
    }

    public int getNHops(){
        return hops;
    }

    public void DecrementHops(){
        hops--;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DENMMsg{");
        sb.append(" Name= ").append(name);
        sb.append(" Route= ").append(route);
        sb.append(" Longitude= ").append(lon);
        sb.append(" Latitude= ").append(lat);
        sb.append(" RoadState= ").append(roadState);
        sb.append(" MsgNumber= ").append(MsgNumber);
        sb.append(" Hops= ").append(hops);
        sb.append('}');
        return sb.toString();
    }
}
