package eclipse.mosaic.apps;

import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;

public final class RSUPacketMsg extends V2xMessage {
    private final String secret;
    private final String name;
    private final String type;
    private final EncodedPayload payload;
    private final static long minLen = 128L;

    public RSUPacketMsg(MessageRouting routing, String secret, String name, String type) {
        super(routing);
        this.secret = secret;
        this.name = name;
        this.type = type;
        payload = new EncodedPayload(16L, minLen);
    }

    public String getName() {
        return name;
    }

    public String getSecret() {
        return secret;
    }

    public String getType() {
        return type;
    }

    public EncodedPayload getPayload() {
        return payload;
    }
}
