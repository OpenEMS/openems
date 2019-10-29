package io.openems.edge.raspberrypi.sensors;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.raspberrypi.sensors.temperaturesensor.Utils;
import io.openems.edge.raspberrypi.spi.SpiInitial;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;

import java.util.stream.Stream;


public abstract class Sensor extends AbstractOpenemsComponent implements OpenemsComponent {
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    private SpiInitial spiInitial;
    //@Reference
  //  protected ConfigurationAdmin cm;


    private final String id;
    private final String type;
    private final String circuitBoardId;
    private String versionId;
    private final int spiChannel;
    private final int pinPosition;
    private String servicePid;
    private boolean enabled;


    public Sensor(String id, String type, String circuitBoardId,
                  int spiChannel, int pinPosition, String servicePid, boolean enabled,
                  io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
                  io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);

        //  Stream<? extends AbstractReadChannel<?, ?>> stream = Utils.initializeChannels(this);
        //stream.forEach(channel -> this.addChannel(channel.channelId()));
        this.id = id;
        this.type = type;
        this.circuitBoardId = circuitBoardId;
        this.spiChannel = spiChannel;
        this.pinPosition = pinPosition;
        this.servicePid = servicePid;
        this.enabled = enabled;
    }

    @Activate
    public void activate(ComponentContext context) throws ConfigurationException {
        super.activate(context, this.id, "", true);

    }

	@Deactivate
    public void deactivate() {
        spiInitial.removeTask(this.id);
    }

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        ;
        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getCircuitBoardId() {
        return circuitBoardId;
    }

    public int getSpiChannel() {
        return spiChannel;
    }

    public int getPinPosition() {
        return pinPosition;
    }

    public SpiInitial getSpiInitial() {
        return spiInitial;
    }

    public String getVersionId() {
        return versionId;
    }


}
