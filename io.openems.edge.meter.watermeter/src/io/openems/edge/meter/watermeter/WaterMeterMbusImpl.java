package io.openems.edge.meter.watermeter;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.mbus.api.AbstractOpenemsMbusComponent;
import io.openems.edge.bridge.mbus.api.BridgeMbus;
import io.openems.edge.bridge.mbus.api.ChannelRecord;
import io.openems.edge.bridge.mbus.api.ChannelRecord.DataType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.watermeter.api.WaterMeter;
import org.openmuc.jmbus.DataRecord;
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
import org.osgi.service.metatype.annotations.Designate;

import java.util.List;

@Designate(ocd = ConfigMbus.class, factory = true)
@Component(name = "WaterMeter.Mbus", //
        immediate = true, //
        configurationPolicy = ConfigurationPolicy.REQUIRE)

public class WaterMeterMbusImpl extends AbstractOpenemsMbusComponent
        implements OpenemsComponent, WaterMeter {

    private WaterMeterModelMbus waterMeterModelMbus;
    private int volAddress;
    private int timeStampAddress;

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setMbus(BridgeMbus mbus) {
        super.setMbus(mbus);
    }


    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        MANUFACTURER_ID(Doc.of(OpenemsType.STRING) //
                .unit(Unit.NONE)), //
        DEVICE_ID(Doc.of(OpenemsType.STRING) //
                .unit(Unit.NONE)), //
        ;

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }
    }

    public WaterMeterMbusImpl() {
        super(OpenemsComponent.ChannelId.values(), //
                WaterMeter.ChannelId.values(),
                ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, ConfigMbus config) {
        if (config.model().equals("Manual")) {
            this.volAddress = config.volAddress();
            this.timeStampAddress = config.timeStampAddress();
        } else {
            // Select meter model from enum list. String in config.model has to be in WaterMeterModel list.
            allocateAddressViaMeterModel(config.model());
            this.volAddress = waterMeterModelMbus.getVolumeCounterPosition();
            this.timeStampAddress = waterMeterModelMbus.getTimeStampPosition();
        }
        if (config.openEmsTimeStamp()) {
            // Address of "-1" for the timestamp means internal method is used to create the timestamp.
            this.timeStampAddress = -1;
        }
        if (config.usePollingInterval()) {
            super.activate(context, config.id(), config.alias(), config.enabled(), config.primaryAddress(), this.cm, "mbus",
                    config.mbusBridgeId(), config.pollingIntervalSeconds(), this.getErrorChannel());     // If you want to use the polling interval, put the time as the last argument in super.activate().
        } else {
            super.activate(context, config.id(), config.alias(), config.enabled(), config.primaryAddress(), this.cm, "mbus",
                    config.mbusBridgeId(), 0, this.getErrorChannel());  // If you don't want to use the polling interval, use super.activate() without the last argument.
        }
    }

    private void allocateAddressViaMeterModel(String meterModel) {
        switch (meterModel) {
            case "PAD_PULS_M2":
                this.waterMeterModelMbus = WaterMeterModelMbus.PAD_PULS_M2;
                break;
            case "ITRON_BM_M":
                this.waterMeterModelMbus = WaterMeterModelMbus.ITRON_BM_M;
                break;
            case "Autosearch":
            default:
                this.waterMeterModelMbus = WaterMeterModelMbus.AUTOSEARCH;
                // Use findRecordPositions() to find the right data record positions. Should work for most meter types.
                super.dynamicDataAddress = true;
                break;
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    protected void addChannelDataRecords() {
        // The total_consumed_water channelRecord needs to be in the first position of this list, otherwise findRecordPositions()
        // for the data records won't work correctly.
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(WaterMeter.ChannelId.TOTAL_CONSUMED_WATER), this.volAddress));

        // The timestamp_seconds channelRecord needs to be in the second position of this list, otherwise findRecordPositions()
        // for the data records won't work correctly.
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(WaterMeter.ChannelId.TIMESTAMP_SECONDS), this.timeStampAddress));

        // TimestampString is always on address -2, since it's an internal method. This channel needs to be
        // called after the TimestampSeconds Channel, as it takes it's value from that channel.
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(WaterMeter.ChannelId.TIMESTAMP_STRING), -2));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(ChannelId.MANUFACTURER_ID), DataType.Manufacturer));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(ChannelId.DEVICE_ID), DataType.DeviceId));
    }

    @Override
    public void findRecordPositions(VariableDataStructure data, List<ChannelRecord> channelDataRecordsList) {

        // Search for the entries starting at the top of the list.
        if (waterMeterModelMbus == WaterMeterModelMbus.AUTOSEARCH) {
            List<DataRecord> dataRecords = data.getDataRecords();
            int numberOfEntries = dataRecords.size();
            boolean volumePositionFound = false;
            boolean timestampPositionFound = false;
            // Check to see if "openEMS timestamp" option is active, which sets the address to -1. If that is active,
            // don't change that address.
            if (channelDataRecordsList.get(1).getDataRecordPosition() < 0) {
                timestampPositionFound = true;
            }
            int dataRecordNumber = 0;
            while (volumePositionFound == false || timestampPositionFound == false) {
                DataRecord currentRecord = dataRecords.get(dataRecordNumber);
                if (volumePositionFound == false && currentRecord.getDescription() == DataRecord.Description.VOLUME) {
                    channelDataRecordsList.get(0).setDataRecordPosition(dataRecordNumber);  // Entry #0 in list is TOTAL_CONSUMED_WATER
                    volumePositionFound = true;
                } else if (timestampPositionFound == false && currentRecord.getDescription() == DataRecord.Description.DATE_TIME) {
                    channelDataRecordsList.get(1).setDataRecordPosition(dataRecordNumber);  // Entry #1 in list is TIMESTAMP_SECONDS
                    timestampPositionFound = true;
                } else {
                    dataRecordNumber++;
                    if (dataRecordNumber > numberOfEntries - 1) {
                        break;
                    }
                }
            }
        }
    }


}
