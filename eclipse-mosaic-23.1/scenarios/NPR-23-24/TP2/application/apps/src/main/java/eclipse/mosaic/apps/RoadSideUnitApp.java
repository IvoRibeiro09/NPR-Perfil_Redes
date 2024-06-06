package eclipse.mosaic.apps;

import java.util.ArrayList;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.RoadSideUnitOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.lib.util.scheduling.EventProcessor;
import org.eclipse.mosaic.rti.TIME;


public class RoadSideUnitApp extends AbstractApplication<RoadSideUnitOperatingSystem> implements CommunicationApplication {
    
    public static final String SECRET = "open sesame!";

    final double RSUlat = 41.554117;
    final double RSUlon = -8.430884;
    //final double limit = 10.0;
    //private final static long TIME_INTERVAL = TIME.SECOND;

    private ArrayList<VehicleInfo> neighbours = new ArrayList<>();
    private ArrayList<VehicleInfo> verticalList = new ArrayList<>();
    private ArrayList<VehicleInfo> horizontalList = new ArrayList<>();

    public RoadSideUnitApp(){
    } 

    public static double checkDistance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt((dx * dx) + (dy * dy));
    }
    
    public void clearNeighList(){
        // limpar a list de neighbours
        for (int i = 0; i < neighbours.size(); i++) {
            VehicleInfo neigh = neighbours.get(i);
            if (neigh.getUpdateTime() + 15 * TIME.SECOND < getOs().getSimulationTime()) {
                neighbours.remove(i);
                i--; // Ajustar o índice após a remoção
                this.getLog().infoSimTime(this, "Vehicle removed!");
                if (neigh.getRota().equals("r_0") || neigh.getRota().equals("r_5")){
                    horizontalList.remove(neigh);
                }else{
                    verticalList.remove(neigh);
                }
            }
        }
        //for ( VehicleInfo neigh : neighbours)System.out.println(neigh.toString());
        this.getLog().infoSimTime(this, "Neighbours list cleared!");
    }

    public void updateTrafficLight(){
        long horizontalListSize = 0; 
        long verticalListSize = 0;
        for (VehicleInfo v : horizontalList) horizontalListSize += getOs().getSimulationTime() - v.getInitialTime();
        for (VehicleInfo v : verticalList) verticalListSize += getOs().getSimulationTime() - v.getInitialTime();
        System.out.println("horizontalList: "+horizontalListSize+" | verticalList: "+verticalListSize);
        if ( horizontalListSize < verticalListSize){
            SetTLightP0();
            this.getLog().infoSimTime(this, "TrafficLight changed to {}.", "0");
        } else {
            SetTLightP1();
            this.getLog().infoSimTime(this, "TrafficLight changed to {}.", "1");
        }
    }

    public void processMsg(PacketMsg message){
        // Adicionar á lista de neighbours se estiverem na direção do semafro
        int pos = checkId(message.getName());
        //this.getLog().infoSimTime(this, "CheckId done!");
        if ( pos >= 0 ) {
            VehicleInfo v = neighbours.get(pos);
            if (v.getMsgNumber() < message.getMessageNumber()){
                Double dist = checkDistance(RSUlat, RSUlon, message.getLat(), message.getLon());
                // se estiver mais perto do semafro update
                if (dist <= v.getDistance()){
                    v.updateInfo(message, dist, getOs().getSimulationTime());
                    this.getLog().infoSimTime(this, "Vehicle updated!");
                    if (v.getRota().equals("r_0") || v.getRota().equals("r_5")){
                        if (!horizontalList.contains(v)) {
                            horizontalList.add(v);
                        }
                    }else{
                        //verticalList.add(v.getName());
                        if (!verticalList.contains(v)) {
                            verticalList.add(v);
                        }
                    }
                    this.getLog().infoSimTime(this, "Vehicle added!");
                }
                //caso contraio remover das listas
                else {
                    v.updateInfo(message, dist, getOs().getSimulationTime());
                    if (v.getRota().equals("r_0") || v.getRota().equals("r_5")){
                        horizontalList.remove(v);
                        this.getLog().infoSimTime(this, "Vehicle {} removed form horizontalList!", v.getName());
                    }else{
                        verticalList.remove(v);
                        this.getLog().infoSimTime(this, "Vehicle {} removed form verticalList!", v.getName());
                    }
                }    
                clearNeighList();
                updateTrafficLight();
            }else return;
        } else {
            Double dist = checkDistance(RSUlat, RSUlon, message.getLat(), message.getLon());
            VehicleInfo v = new VehicleInfo(message, dist, getOs().getSimulationTime());
            neighbours.add(v);
        }
        this.getLog().infoSimTime(this, "Message Processed!");
    }

    public int checkId(String id){
        int i = 0;
        for (VehicleInfo n : neighbours){
            if (n.getName().equals(id) && n.getDistance() != -999) return i;
            i++;
        }
        return -1;
    }

    public void sample() {
        ((RoadSideUnitOperatingSystem)this.getOs()).getEventManager().addEvent(((RoadSideUnitOperatingSystem)this.getOs()).getSimulationTime() + 2000000000L, new EventProcessor[]{this});
        this.getLog().infoSimTime(this, "Sending out AdHoc broadcast", new Object[0]);
    }

    public void onStartup() {
        getLog().infoSimTime(this, "Initialize RSU application");
        getOs().getAdHocModule().enable(new AdHocModuleConfiguration()
            .addRadio()
            .channel(AdHocChannel.CCH)
            .distance(40)
            .power(0.002)
            .create());

        this.getLog().infoSimTime(this, "Activated WLAN Module");
        //Event event = new Event(getOs().getSimulationTime() + 9 * TIME_INTERVAL,  this::SetTLightP1);
        //getOs().getEventManager().addEvent(event);
    }

    public void onShutdown() {
        this.getLog().infoSimTime(this, "Shutdown application", new Object[0]);
    }

    public void processEvent(Event event) {
        this.getLog().infoSimTime(this, "New Event");
    }

    public void onAcknowledgementReceived(ReceivedAcknowledgement arg0) {
        // TODO Auto-generated method stub
        //throw new UnsupportedOperationException("Unimplemented method 'onAcknowledgementReceived'");
    }

    
    public void onCamBuilding(CamBuilder arg0) {
        // TODO Auto-generated method stub
        //throw new UnsupportedOperationException("Unimplemented method 'onCamBuilding'");
    }

    public void onMessageTransmitted(V2xMessageTransmission msg) {
        if (msg.getMessage() instanceof LightMsg){
            this.getLog().infoSimTime(this, "Message to set trafficLight to program {}",((LightMsg) msg.getMessage()).getProgramId());
        }
    }

    public void onMessageReceived(ReceivedV2xMessage v2xMessage) {
        if (v2xMessage.getMessage() instanceof PacketMsg){
            final PacketMsg message = (PacketMsg) v2xMessage.getMessage();
            this.getLog().infoSimTime(this, "Received V2X message from vehicle: {}",(message.toString()));
            processMsg(message);
        }
    }

    public void SetTLightP1(){
        final MessageRouting routing = getOs().getAdHocModule().createMessageRouting()
                        .viaChannel(AdHocChannel.SCH6)
                        .topoBroadCast();
        final LightMsg message = new LightMsg(routing, "1", SECRET);
        getOs().getAdHocModule().sendV2xMessage(message);  
        //Event e = new Event(getOs().getSimulationTime() + 5 * TIME_INTERVAL,  this::SetTLightP0);
        //getOs().getEventManager().addEvent(e);
    }

    public void SetTLightP0(){
        final MessageRouting routing = getOs().getAdHocModule().createMessageRouting()
                        .viaChannel(AdHocChannel.SCH6)
                        .topoBroadCast();
        final LightMsg message = new LightMsg(routing, "0", SECRET);
        getOs().getAdHocModule().sendV2xMessage(message);  
    }
}