package eclipse.mosaic.apps;

import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.VehicleApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.lib.util.scheduling.EventProcessor;
import org.eclipse.mosaic.rti.TIME;

import org.eclipse.mosaic.lib.enums.VehicleStopMode;
import org.eclipse.mosaic.lib.objects.road.SimpleRoadPosition;

public class OnVehicleUpdatedApp extends AbstractApplication<VehicleOperatingSystem> implements VehicleApplication , CommunicationApplication{
   
   private final static long TIME_INTERVAL = TIME.SECOND;
   private String onswitch = "ON";
   private String status = "RUNNING";
   final double RSUlat = 41.554117;
   final double RSUlon = -8.430884;
   private String vehicleNodeNumber;
   private String route;
   private int MsgNumber = 0;
   //private GeoPoint = (41.554117, -8.430884);
   public static final String SECRET = "open sesame!";
   private final Lock lock = new ReentrantLock();
   

   public OnVehicleUpdatedApp() {
   }
      
   public void onVehicleUpdated(VehicleData previousVehicleData, VehicleData updatedVehicleData) {
      this.getLog().infoSimTime(this, "Called #onVehicleUpdated", new Object[0]);
      Double VELO = updatedVehicleData.getSpeed();
      if (VELO > 13.0){
         // mais velocidade maior a frequencia de Cams
         getLog().infoSimTime(this, "Exced velocity! Sending new CAM");
         sendAdHocBroadcast(null);
      }
   }

   public void onStartup() {
      getLog().infoSimTime(this, "Initialize Vehicle application");
      getOs().getAdHocModule().enable(new AdHocModuleConfiguration()
         .addRadio()
         .channel(AdHocChannel.CCH)
         .distance(30)
         .power(0.002)
         .create());

      getLog().infoSimTime(this, "Activated WLAN Module");
      newEvent(1,  this::defineName);
      newEvent(1,  this::sendAdHocBroadcast);
      newEvent(9, this::getRoadState);
   }

   public void newEvent(int time, EventProcessor func){
      Event event = new Event(getOs().getSimulationTime() + time * TIME_INTERVAL,  func);
      getOs().getEventManager().addEvent(event);
   }
   
   private void sendAdHocBroadcast(Event event) {
      if (onswitch == "ON"){
         final MessageRouting routing = getOs().getAdHocModule().createMessageRouting().viaChannel(AdHocChannel.CCH).topoBroadCast();
         int hops;
         if ( getOs().getVehicleData().getSpeed() < 2) hops = 1;
         else hops = 2;
         final PacketMsg message = new PacketMsg(routing, vehicleNodeNumber,
                                                route,
                                                getOs().getVehicleData().getPosition().getLongitude(), 
                                                getOs().getVehicleData().getPosition().getLatitude(), 
                                                getOs().getVehicleData().getSpeed(),
                                                MsgNumber,
                                                hops);
         MsgNumber++;
         getOs().getAdHocModule().sendV2xMessage(message);
         getLog().infoSimTime(this, "Sending out AdHoc broadcast");
         newEvent(2, this::sendAdHocBroadcast);
      }
   }

   public void onShutdown() {
      onswitch = "OFF";
      this.getLog().infoSimTime(this, "Shutdown", new Object[0]);
   }

   public void defineName(Event event){
      this.vehicleNodeNumber = getOs().getId();
      this.route = getOs().getVehicleData().getRouteId();
      this.getLog().infoSimTime(this, "Define the name: {} and route: {}", this.vehicleNodeNumber, this.route);
   }

   public void onMessageReceived(ReceivedV2xMessage v2xMessage){
      if (v2xMessage.getMessage() instanceof PacketMsg){
         PacketMsg message = (PacketMsg) v2xMessage.getMessage();
         this.getLog().infoSimTime(this, "Recieve a message from other vehicle");
         processRecMsg(message);
      } else if (v2xMessage.getMessage() instanceof DENMMsg){
         DENMMsg message = (DENMMsg) v2xMessage.getMessage();
         this.getLog().infoSimTime(this, "Recieve a message from other vehicle");
         processDENMMsg(message);
      } else if (v2xMessage.getMessage() instanceof RSUPacketMsg) {
         RSUPacketMsg message = (RSUPacketMsg) v2xMessage.getMessage();
         this.getLog().infoSimTime(this, "Recieve a message from RSU");
         processRSUMsg(message);
      }
   }

   public void processRecMsg(PacketMsg message){
      Double mydist = checkDistance(RSUlat, RSUlon,
                        getOs().getVehicleData().getPosition().getLatitude(),
                        getOs().getVehicleData().getPosition().getLongitude());
      if (mydist < checkDistance(RSUlat, RSUlon, message.getLat(), message.getLon())){
         // Estou melhor posicionado vou fazer fowarding
         message.DecrementHops();
         if (message.getNHops() > 0){
            final MessageRouting routing = getOs().getAdHocModule().createMessageRouting().viaChannel(AdHocChannel.CCH).topoBroadCast();
            PacketMsg resendMSG = new PacketMsg(routing, message);
            getOs().getAdHocModule().sendV2xMessage(resendMSG);
            this.getLog().infoSimTime(this, "Recending a message from other vehicle");
         }else{
            this.getLog().infoSimTime(this, "I'm in better position but no more hops in message");
         }
      }else{
         this.getLog().infoSimTime(this, "My position is not better!");
      }
   }

   public static double checkDistance(double x1, double y1, double x2, double y2) {
      double dx = x2 - x1;
      double dy = y2 - y1;
      return Math.sqrt((dx * dx) + (dy * dy));
   }

   public void processEvent(Event event) throws Exception {
      this.getLog().infoSimTime(this, "Scheduled event at time {}", new Object[]{TIME.format(event.getTime())});
   }

   public void getRoadState(Event event) {
      Random random = new Random();
      int randomNumber = random.nextInt(100) + 1; // Gera um número entre 1 e 100
      String tipoPiso;
      if (randomNumber <= 80) {
         tipoPiso = "seco";
      } else if (randomNumber <= 90) {
         tipoPiso = "piso-molhado";
      } else {
         tipoPiso = "nevoeiro";
      }
      final MessageRouting routing = getOs().getAdHocModule().createMessageRouting().viaChannel(AdHocChannel.CCH).topoBroadCast();
      final DENMMsg message = new DENMMsg(routing, vehicleNodeNumber,
                                          route,
                                          getOs().getVehicleData().getPosition().getLongitude(), 
                                          getOs().getVehicleData().getPosition().getLatitude(), 
                                          tipoPiso,
                                          MsgNumber,
                                          2);
      getOs().getAdHocModule().sendV2xMessage(message);
      getLog().infoSimTime(this, "Sending out DENM broadcast");
   }
   
   public void processDENMMsg(DENMMsg message){
      getLog().infoSimTime(this, "DENM message Recived from {} -info : {}", message.getName(), message.getRoadState());
      if (message.getRoadState().equals("nevoiro") || message.getRoadState().equals("piso-molhado") ){
         // Slow down for 5 seg
         getOs().slowDown(3 ,5 * TIME_INTERVAL);
         //getOs().changeSpeedWithInterval(6.0, 5 * TIME.SECOND);
         getLog().infoSimTime(this, "I slow down due to road state");
      }
      message.DecrementHops();
      if (message.getNHops() > 0){
         final MessageRouting routing = getOs().getAdHocModule().createMessageRouting().viaChannel(AdHocChannel.CCH).topoBroadCast();
         DENMMsg resendMSG = new DENMMsg(routing, message);
         getOs().getAdHocModule().sendV2xMessage(resendMSG);
         this.getLog().infoSimTime(this, "Recending a DENM message from other vehicle");
      }else{
         this.getLog().infoSimTime(this, "No more hops in DENM message");
      }
   }

   public void processRSUMsg(RSUPacketMsg message){
      lock.lock();
      String st = status;
      lock.unlock();
      if ( st.equals("RUNNING") ){
         if ( message.getType().equals("stop")) {
            //getOs().stopNow(VehicleStopMode.STOP, 999*TIME_INTERVAL);
            getOs().changeSpeedWithInterval(0, 0);
            lock.lock();
            status = "STOPED";
            lock.unlock();
            this.getLog().infoSimTime(this, "RSU Stopped me!!!");
         }else if ( message.getType().equals("slowdown")) {
            if ( getOs().getVehicleData().getSpeed() > 3){
               getOs().slowDown(2 , 2 * TIME_INTERVAL);
            }
         }
      } else if ( st.equals("STOPED") ){
         if ( message.getType().equals("start")  && message.getName().equals(vehicleNodeNumber) ){
            getOs().resetSpeed();
            //getOs().resume();
            this.getLog().infoSimTime(this, "RSU ordered to advance!!!");
         }
      }
   }

   @Override
   public void onAcknowledgementReceived(ReceivedAcknowledgement arg0) {}

   @Override
   public void onCamBuilding(CamBuilder arg0) { }

   @Override
   public void onMessageTransmitted(V2xMessageTransmission arg0) {}
}