package eclipse.mosaic.apps;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.TrafficLightOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.util.scheduling.Event;


public final class TrafficLightApp extends AbstractApplication<TrafficLightOperatingSystem> implements CommunicationApplication {
    public static final String SECRET = "open sesame!";
    //private static final short GREEN_DURATION = 10;
    private static final String DEFAULT_PROGRAM = "1";
    //private static final String GREEN_PROGRAM = "0";
    //private static final Integer MIN_DISTANCE = 15;

    public TrafficLightApp() {
    }

    public void onStartup() {
        this.getLog().infoSimTime(this, "Initialize application", new Object[0]);
        getOs().getAdHocModule().enable(new AdHocModuleConfiguration()
               .addRadio()
               .channel(AdHocChannel.CCH)
               .distance(2)
               .power(5)
               .create());

        getOs().switchToProgram(DEFAULT_PROGRAM);
    }

    public void onShutdown() {
        this.getLog().infoSimTime(this, "Shutdown application", new Object[0]);
    }

    public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
        if (receivedV2xMessage.getMessage() instanceof LightMsg) {
            LightMsg message = (LightMsg)receivedV2xMessage.getMessage();
            if (message.getSecret().equals(SECRET)){
                this.getLog().infoSimTime(this, "Received a valid message from RSU!");
                processMsg(message);
            }
        }
    }

    public void processMsg(LightMsg msg){
        getOs().switchToProgram(msg.getProgramId());
        this.getLog().infoSimTime(this, "Program changed to program {}.", msg.getProgramId());
    }

    public void onAcknowledgementReceived(ReceivedAcknowledgement acknowledgedMessage) {
    }

    public void onCamBuilding(CamBuilder camBuilder) {
    }

    public void onMessageTransmitted(V2xMessageTransmission v2xMessageTransmission) {
    }

    public void processEvent(Event event) throws Exception {
    }
}