package eclipse.mosaic.apps;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.VehicleApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.lib.util.scheduling.EventProcessor;
import org.eclipse.mosaic.rti.TIME;

public class OnVehicleUpdatedApp extends AbstractApplication<VehicleOperatingSystem> implements VehicleApplication {
   
   private final static long TIME_INTERVAL = TIME.SECOND;
   private String onswitch = "ON";
   final double RSUlat = 41.554117;
   final double RSUlon = -8.430884;
   private String vehicleNodeNumber;
   private String route;
   private int MsgNumber = 0;
   

   public OnVehicleUpdatedApp() {
   }
      
   public void onVehicleUpdated(VehicleData previousVehicleData, VehicleData updatedVehicleData) {
      this.getLog().infoSimTime(this, "Called #onVehicleUpdated", new Object[0]);
      //Double POSx = updatedVehicleData.getPosition().getLatitude();
      //Double POSy = updatedVehicleData.getPosition().getLongitude();
      Double VELO = updatedVehicleData.getSpeed();
      if (VELO > 13.0){
         // mais velocidade maior a frequencia de Cams
         getLog().infoSimTime(this, "Exced velocity! Sending new CAM");
         sendAdHocBroadcast(null);
      }
      //this.getLog().infoSimTime(this, "Current velocity: {} m/s", new Object[]{VELO});
      //this.getLog().infoSimTime(this, "Current position: ({},{})", new Object[]{POSx, POSy});
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
   }

   public void newEvent(int time, EventProcessor func){
      Event event = new Event(getOs().getSimulationTime() + time * TIME_INTERVAL,  func);
      getOs().getEventManager().addEvent(event);
   }
   
   private void sendAdHocBroadcast(Event event) {
      if (onswitch == "ON"){
         final MessageRouting routing = getOs().getAdHocModule().createMessageRouting().viaChannel(AdHocChannel.CCH).topoBroadCast();
         final PacketMsg message = new PacketMsg(routing, vehicleNodeNumber,
                                                route,
                                                getOs().getVehicleData().getPosition().getLongitude(), 
                                                getOs().getVehicleData().getPosition().getLatitude(), 
                                                getOs().getVehicleData().getSpeed(),
                                                MsgNumber);
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

   public void processEvent(Event event) throws Exception {
      this.getLog().infoSimTime(this, "Scheduled event at time {}", new Object[]{TIME.format(event.getTime())});
   }

}