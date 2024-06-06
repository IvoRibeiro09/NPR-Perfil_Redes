package eclipse.mosaic.apps;

import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;

public final class LightMsg extends V2xMessage {
    private final String program;
    private final String secret;
    private final EncodedPayload payload;
    private final static long minLen = 128L;

    public LightMsg(MessageRouting routing, String p, String secret) {
        super(routing);
        payload = new EncodedPayload(16L, minLen);
        this.program = p;
        this.secret = secret;
    }

    public String getProgramId() {
        return program;
    }
    public String getSecret() {
        return secret;
    }


    public EncodedPayload getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("InterVehicleMsg{");
        sb.append("Program=").append(program);
        sb.append('}');
        return sb.toString();
    }
}
