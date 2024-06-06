package eclipse.mosaic.apps;

public class VehicleInfo {
    private final String name;
    private String rota;
    private double lat;
    private double lon;
    private int msgNumber;
    private double distance;
    private long updateTime;
    private final long initTime;
    
    public VehicleInfo(PacketMsg message, double dist, long time) {
        name = message.getName();
        rota = message.getRoute();
        lat = message.getLat();
        lon = message.getLon();
        msgNumber = message.getMessageNumber();
        distance = dist;
        updateTime = time;
        initTime = time;
    }

    public void updateInfo(PacketMsg message, double dist, long time){
        rota = message.getRoute();
        lat = message.getLat();
        lon = message.getLon();
        msgNumber = message.getMessageNumber();
        distance = dist;
        updateTime = time;
    }

    public String getName() {
        return name;
    }

    public String getRota() {
        return rota;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public int getMsgNumber() {
        return msgNumber;
    }

    public double getDistance(){
        return distance;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public long getInitialTime() {
        return initTime;
    }

    public String toString() {
        return "VehicleInfo{" +
                "Name= " + name +
                ", Route= " + rota +
                ", Lat= " + lon +
                ", Lon= " + lat +
                ", MsgNumber= " + msgNumber +
                ", Distance2RSU= " + distance +
                ", updateTime= " + updateTime +
                '}';
    }
}