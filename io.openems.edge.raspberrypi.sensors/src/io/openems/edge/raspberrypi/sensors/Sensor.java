package io.openems.edge.raspberrypi.sensors;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.raspberrypi.circuitboard.api.adc.Adc;
import io.openems.edge.raspberrypi.sensors.Utils.Utils;
import io.openems.edge.raspberrypi.spi.SpiInitial;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import io.openems.edge.raspberrypi.circuitboard.CircuitBoard;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import java.util.stream.Stream;


public abstract class Sensor extends AbstractOpenemsComponent implements OpenemsComponent {
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    private SpiInitial spiInitial;
    @Reference
    protected ConfigurationAdmin cm;
    private final String id;
    private final String type;
    private final String circuitBoardId;
    private String versionId;
    private final int adcId;
    private final int pinPosition;
    private int indexAdcOfCircuitBoard;
    private String servicePid;
    private boolean enabled;


    public Sensor(String id, String type, String circuitBoardId,
                  int adcId, int pinPosition, String servicePid, boolean enabled,
				  io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
				  io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);

        Stream<? extends AbstractReadChannel<?, ?>> stream = Utils.initializeChannels(this);
        stream.forEach(channel -> this.addChannel(channel.channelId()));


        this.id = id;
        this.type = type;
        this.circuitBoardId = circuitBoardId;
        this.adcId = adcId;
        this.pinPosition = pinPosition;
        this.servicePid = servicePid;
        this.enabled = enabled;


    }




    @Activate
    public void activate(ComponentContext context) throws ConfigurationException {
        super.activate(context, this.servicePid, this.id(), this.enabled);
        this.addToCircuitBoard();
        //if(OpenemsComponent.updateReferenceFilter(cm, config.service_pid()), "spiInitial")

    }

    protected void addToCircuitBoard() throws ConfigurationException {
        for (CircuitBoard consolinno : spiInitial.getCircuitBoards()) {
            if (consolinno.getType().equals(this.type) && consolinno.getCircuitBoardId().equals(this.circuitBoardId)) {
                if (this.adcId > consolinno.getMcpListViaId().size()) {
                    throw new org.osgi.service.cm.ConfigurationException("", "Wrong ADC Position given, max size is "
                            + consolinno.getMcpListViaId().size() + "First Position is 0 second is 1 etc");
                } else {
                    this.indexAdcOfCircuitBoard = consolinno.getMcpListViaId().get(this.adcId);
                    Adc allocatePin = spiInitial.getAdcList().get(indexAdcOfCircuitBoard);
                    if (allocatePin.getPins().get(this.pinPosition).isUsed()) {
                        throw new org.osgi.service.cm.ConfigurationException("",
                                "Wrong Pin, Pin already used by: "
                                        + allocatePin.getPins().get(this.pinPosition).isUsed());
                    } else {
                        allocatePin.getPins().get(this.pinPosition).setUsedBy(this.id);


                    }
                }

                consolinno.addToSensors(this);
                this.versionId = consolinno.getVersionId();
            }
        }
    }



	@Deactivate
    public void deactivate() {
        spiInitial.getAdcList().get(this.indexAdcOfCircuitBoard).getPins().get(this.pinPosition).setUsed(false);
        for (CircuitBoard consolinno : spiInitial.getCircuitBoards()
        ) {
            if (consolinno.getCircuitBoardId().equals(this.circuitBoardId)) {
                for (Sensor sensor :
                        consolinno.getSensors()) {
                    if (sensor.id.equals(this.id)) {
                        consolinno.getSensors().remove(sensor);
                        break;
                    }
                }
            }
        }
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

    public int getAdcId() {
        return adcId;
    }

    public int getPinPosition() {
        return pinPosition;
    }

    public int getIndexAdcOfCircuitBoard() {
        return indexAdcOfCircuitBoard;
    }

    public SpiInitial getSpiInitial() {
        return spiInitial;
    }

    public String getVersionId() {
        return versionId;
    }


}
