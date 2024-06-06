package eclipse.mosaic.apps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.RoadSideUnitOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.geo.GeoCircle;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.lib.util.scheduling.EventProcessor;
import org.eclipse.mosaic.rti.TIME;



public class RoadSideUnitApp extends AbstractApplication<RoadSideUnitOperatingSystem> implements CommunicationApplication {
    
    public static final String SECRET = "open sesame!";
    final double RSUlat = 41.554117;
    final double RSUlon = -8.4308808;

    final double dist_limit = 0.000109;

    private Map<String, VehicleInfo> neighbours = new HashMap<>();
    private ArrayList<String> onTL = new ArrayList<>();
    private ArrayList<String> outTL = new ArrayList<>();


    public RoadSideUnitApp(){
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
        newEvent(2, this::sendStopMessage);
        //newEvent(2, this::trafficLightControl);
    }

    public static double checkDistance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt((dx * dx) + (dy * dy));
    }

    public void trafficLightControl(Event e){
        
        if (onTL.size() > 0) {
            String name = onTL.get(0);
            VehicleInfo v = neighbours.get(name);
            onTL.remove(name);
            outTL.add(name);
            MessageRouting routing = getOs().getAdHocModule()
                            .createMessageRouting()
                            .viaChannel(AdHocChannel.CCH).topoCast(name, 1);

            RSUPacketMsg message = new RSUPacketMsg(routing, 
                                                SECRET,
                                                v.getName(),
                                                "start");
            getOs().getAdHocModule().sendV2xMessage(message);
            this.getLog().infoSimTime(this, "Vehicle {} should start", v.getName());
            System.out.println("Vehicle "+v.getName()+ " should start");
        }
        newEvent(4, this::trafficLightControl);
    }

    public void processMsg(PacketMsg message){
        // Adicionar á lista de neighbours se estiverem na direção do semafro
        String name = message.getName();
        //this.getLog().infoSimTime(this, "CheckId done!");
        if ( neighbours.containsKey(name) ) {
            VehicleInfo v = neighbours.get(name);
            if (v.getMsgNumber() < message.getMessageNumber()){
                Double dist = checkDistance(RSUlat, RSUlon, message.getLat(), message.getLon());
                if (dist < dist_limit){
                    if ( !onTL.contains(name) && !outTL.contains(name)) onTL.add(v.getName());
                }
                v.updateInfo(message, dist, getOs().getSimulationTime());
                this.getLog().infoSimTime(this, "Vehicle updated!");
            }else return;
        } else {
            Double dist = checkDistance(RSUlat, RSUlon, message.getLat(), message.getLon());
            VehicleInfo v = new VehicleInfo(message, dist, getOs().getSimulationTime());
            neighbours.put(v.getName(), v);
        }
        this.getLog().infoSimTime(this, "Message Processed!");
    }
    

    public void sendStopMessage(Event e){
        GeoPoint pointA = GeoPoint.latLon(41.554118, -8.4308808);
        //GeoPoint pointB = GeoPoint.latLon(41.554116, -8.4308808);

        GeoCircle spotArea = new GeoCircle(pointA, 14);
        //GeoCircle spotArea2 = new GeoCircle(pointB, 11);
        
        MessageRouting routing = getOs().getAdHocModule()
                        .createMessageRouting().geoBroadCast(spotArea);
        //MessageRouting routing0 = getOs().getAdHocModule()
                        //.createMessageRouting().geoBroadCast(spotArea2);
        
        RSUPacketMsg stopmessage = new RSUPacketMsg(routing, 
                                            SECRET,
                                            "RSU",
                                            "stop");
        //RSUPacketMsg stopmessage0 = new RSUPacketMsg(routing0, SECRET,"RSU", "stop");
        
        getOs().getAdHocModule().sendV2xMessage(stopmessage);
        //getOs().getAdHocModule().sendV2xMessage(stopmessage0);
        
        newEvent(0.7, this::sendStopMessage);
    
    }
    
    public void sendSlowdownMessage(Event e){
        GeoCircle slowdownArea = new GeoCircle(getOs().getPosition(), 55);
        MessageRouting routing1 = getOs().getAdHocModule()
                .createMessageRouting().geoBroadCast(slowdownArea);

        RSUPacketMsg slowdownmessage = new RSUPacketMsg(routing1, 
                                            SECRET,
                                            "RSU",
                                            "slowdown");
        getOs().getAdHocModule().sendV2xMessage(slowdownmessage);

        newEvent(1, this::sendStopMessage);
    }

    public void onShutdown() {
        this.getLog().infoSimTime(this, "Shutdown application", new Object[0]);
    }

    public void processEvent(Event event) {
        this.getLog().infoSimTime(this, "New Event");
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

    public void newEvent(double time, EventProcessor func){
        Event event = new Event((long) (getOs().getSimulationTime() + time * TIME.SECOND),  func);
        getOs().getEventManager().addEvent(event);
    }

    public void onAcknowledgementReceived(ReceivedAcknowledgement arg0){}

    public void onCamBuilding(CamBuilder arg0) {}
}