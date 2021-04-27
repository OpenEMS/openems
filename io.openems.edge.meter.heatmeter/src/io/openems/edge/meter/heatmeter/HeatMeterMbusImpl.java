package io.openems.edge.meter.heatmeter;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.mbus.api.AbstractOpenemsMbusComponent;
import io.openems.edge.bridge.mbus.api.BridgeMbus;
import io.openems.edge.bridge.mbus.api.ChannelRecord;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.heatmeter.api.HeatMeterMbus;
import org.openmuc.jmbus.VariableDataStructure;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.util.List;

@Designate(ocd = Config.class, factory = true)
@Component(name = "HeatMeter.Mbus",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE})
public class HeatMeterMbusImpl extends AbstractOpenemsMbusComponent implements OpenemsComponent, HeatMeterMbus, EventHandler {

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setMbus(BridgeMbus mbus) {
        super.setMbus(mbus);
    }

    HeatMeterModel heatMeterModel;

    public HeatMeterMbusImpl() {
        super(OpenemsComponent.ChannelId.values(),
                HeatMeterMbus.ChannelId.values(),
                ChannelId.values());
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            if (this.getFlowRateToMbus().value().isDefined()) {
                this.getFlowRate().setNextValue(this.getFlowRateToMbus().value().get());
            }
            if (this.getPowerToMbus().value().isDefined()) {
                this.getPower().setNextValue(this.getPowerToMbus().value().get());
            }
            if (this.getTotalConsumedEnergyToMbus().value().isDefined()) {
                this.getTotalConsumedEnergy().setNextValue(this.getTotalConsumedEnergyToMbus().value().get());
            }
        }
    }

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        MANUFACTURER_ID(Doc.of(OpenemsType.STRING) //
                .unit(Unit.NONE)), //
        DEVICE_ID(Doc.of(OpenemsType.STRING) //
                .unit(Unit.NONE)), //
        POWER_TO_MBUS(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT)),
        FLOW_RATE_TO_MBUS(Doc.of(OpenemsType.DOUBLE).unit(Unit.CUBICMETER_PER_HOUR)),
        TOTAL_CONSUMED_ENERGY_TO_MBUS(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),
        FLOW_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        FLOW_RATE(Doc.of(OpenemsType.DOUBLE).unit(Unit.CUBICMETER_PER_HOUR)),
        ;

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }
    }

    Channel<Integer> getPowerToMbus() {
        return this.channel(ChannelId.POWER_TO_MBUS);
    }

    Channel<Double> getFlowRateToMbus() {
        return this.channel(ChannelId.FLOW_RATE_TO_MBUS);
    }

    Channel<Integer> getTotalConsumedEnergyToMbus() {
        return this.channel(ChannelId.TOTAL_CONSUMED_ENERGY_TO_MBUS);
    }

    Channel<Integer> getFlowTemp() {
        return this.channel(ChannelId.FLOW_TEMP);
    }

    Channel<Double> getFlowRate() {
        return this.channel(ChannelId.FLOW_RATE);
    }

    @Activate
    public void activate(ComponentContext context, Config config) {

        //use data record positions as specified in HeatMeterType
        this.heatMeterModel = config.model();

        if (config.usePollingInterval()) {
            super.activate(context, config.id(), config.alias(), config.enabled(), config.primaryAddress(), this.cm, "mbus",
                    config.mbusBridgeId(), config.pollingIntervalSeconds(), this.getErrorMessageChannel());     // If you want to use the polling interval, put the time as the last argument in super.activate().
        } else {
            super.activate(context, config.id(), config.alias(), config.enabled(), config.primaryAddress(), this.cm, "mbus",
                    config.mbusBridgeId(), 0, this.getErrorMessageChannel());  // If you don't want to use the polling interval, use super.activate() without the last argument.
        }
    }


    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected void addChannelDataRecords() {
        this.channelDataRecordsList.add(new ChannelRecord(channel(ChannelId.TOTAL_CONSUMED_ENERGY_TO_MBUS), this.heatMeterModel.totalConsumptionEnergyAddress));
        this.channelDataRecordsList.add(new ChannelRecord(channel(ChannelId.FLOW_TEMP), this.heatMeterModel.flowTempAddress));
        this.channelDataRecordsList.add(new ChannelRecord(channel(HeatMeterMbus.ChannelId.RETURN_TEMP), this.heatMeterModel.returnTempAddress));
        this.channelDataRecordsList.add(new ChannelRecord(channel(ChannelId.POWER_TO_MBUS), this.heatMeterModel.powerAddress));
        this.channelDataRecordsList.add(new ChannelRecord(channel(ChannelId.FLOW_RATE_TO_MBUS), this.heatMeterModel.flowRateAddress));
        this.channelDataRecordsList.add(new ChannelRecord(channel(ChannelId.MANUFACTURER_ID), ChannelRecord.DataType.Manufacturer));
        this.channelDataRecordsList.add(new ChannelRecord(channel(ChannelId.DEVICE_ID), ChannelRecord.DataType.DeviceId));

        // Timestamp created by OpenEMS, not read from meter.
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(HeatMeterMbus.ChannelId.TIMESTAMP_SECONDS), -1));

        // TimestampString is always on address -2, since it's an internal method. This channel needs to be
        // called after the TimestampSeconds Channel, as it takes it's value from that channel.
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(HeatMeterMbus.ChannelId.TIMESTAMP_STRING), -2));
    }

    @Override
    public void findRecordPositions(VariableDataStructure data, List<ChannelRecord> channelDataRecordsList) {
        // Not available yet for this controller.
    }

}
