package eclipse.mosaic.apps;

import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.os.TrafficLightOperatingSystem;
import org.eclipse.mosaic.lib.util.scheduling.Event;


public final class TrafficLightApp extends AbstractApplication<TrafficLightOperatingSystem> {
    private static final String DEFAULT_PROGRAM = "0";
    
    public TrafficLightApp() {
    }

    public void onStartup() {
        this.getLog().infoSimTime(this, "Initialize application", new Object[0]);
        getOs().switchToProgram(DEFAULT_PROGRAM);
        this.getLog().infoSimTime(this, "Program changed to program 0");
        System.out.println(getOs().getPosition());
    }

    public void onShutdown() {
        this.getLog().infoSimTime(this, "Shutdown application", new Object[0]);
    }

    @Override
    public void processEvent(Event arg0) throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'processEvent'");
    }
}