package io.openems.edge.meter.watermeter;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.mbus.api.AbstractOpenemsWMbusComponent;
import io.openems.edge.bridge.mbus.api.BridgeWMbus;
import io.openems.edge.bridge.mbus.api.ChannelRecord;
import io.openems.edge.bridge.mbus.api.WMbusProtocol;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Designate(ocd = ConfigWirelessMbus.class, factory = true)
@Component(name = "WaterMeter.WirelessMbus",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class WaterMeterWirelessMbusImpl extends AbstractOpenemsWMbusComponent implements OpenemsComponent, WaterMeter {

    @Reference
    protected ConfigurationAdmin cm;

    private final Logger log = LoggerFactory.getLogger(WaterMeterWirelessMbusImpl.class);

    private WaterMeterModelWirelessMbus waterMeterModelWirelessMbus;
    private int volAddress;
    private int timeStampAddress;

    public WaterMeterWirelessMbusImpl() {
        super(OpenemsComponent.ChannelId.values(),
                WaterMeter.ChannelId.values(),
                ChannelId.values());
    }

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        SIGNAL_STRENGTH(Doc.of(OpenemsType.INTEGER) //
                .unit(Unit.DECIBEL_MILLIWATT)),
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

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setWMbus(BridgeWMbus wmbus) {
        super.setWMbus(wmbus);
    }

    @Activate
    void activate(ComponentContext context, ConfigWirelessMbus config) {
        if (config.radioAddress().length() != 8) {
            this.logError(this.log, "The radio address needs to be 8 characters long. The entered radio address "
                    + config.radioAddress() + " is " + config.radioAddress().length() + " characters long. Cannot activate.");
        }
        if (config.model().equals("Manual")) {
            this.volAddress = config.volAddress();
            this.timeStampAddress = config.timeStampAddress();
        } else {
            // Select meter model from enum list. String in config.model has to be in WaterMeterModel list.
            allocateAddressViaMeterModel(config.model());
            this.volAddress = waterMeterModelWirelessMbus.getVolumeCounterPosition();
            this.timeStampAddress = waterMeterModelWirelessMbus.getTimeStampPosition();
        }
        if (config.openEmsTimeStamp()) {
            // Address of "-1" for the timestamp means internal method is used to create the timestamp.
            this.timeStampAddress = -1;
        }

        super.activate(context, config.id(), config.alias(), config.enabled(),
                config.radioAddress(), cm, "WirelessMbus", config.wmbusBridgeId(), config.key());

    }

    private void allocateAddressViaMeterModel(String meterModel) {
        switch (meterModel) {
            case "Relay PadPuls M2W Channel 1":
                this.waterMeterModelWirelessMbus = WaterMeterModelWirelessMbus.RELAY_PADPULS_M2W_CHANNEL1;
                break;
            case "Relay PadPuls M2W Channel 2":
                this.waterMeterModelWirelessMbus = WaterMeterModelWirelessMbus.RELAY_PADPULS_M2W_CHANNEL2;
                break;
            case "Engelmann Waterstar M":
                this.waterMeterModelWirelessMbus = WaterMeterModelWirelessMbus.ENGELMANN_WATERSTAR_M;
                // The Waterstar transmits stored records before the current data. The number of stored records can vary,
                // so the address of the data after the stored records can change as well.
                super.dynamicDataAddress = true;
                break;
            case "Autosearch":
            default:
                this.waterMeterModelWirelessMbus = WaterMeterModelWirelessMbus.AUTOSEARCH;
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
    protected WMbusProtocol defineWMbusProtocol(String key) {
        WMbusProtocol protocol = new WMbusProtocol(this, key, this.getErrorChannel(),
                // The total_consumed_water channelRecord needs to be in the first position of this list, otherwise findRecordPositions()
                // for the data records won't work correctly.
                new ChannelRecord(this.channel(WaterMeter.ChannelId.TOTAL_CONSUMED_WATER), this.volAddress),

                // The timestamp_seconds channelRecord needs to be in the second position of this list, otherwise findRecordPositions()
                // for the data records won't work correctly.
                new ChannelRecord(this.channel(WaterMeter.ChannelId.TIMESTAMP_SECONDS), this.timeStampAddress),

                // TimestampString is always on address -2, since it's an internal method. This channel needs to be
                // called after the TimestampSeconds Channel, as it takes it's value from that channel.
                new ChannelRecord(this.channel(WaterMeter.ChannelId.TIMESTAMP_STRING), -2),
                new ChannelRecord(this.channel(ChannelId.MANUFACTURER_ID), ChannelRecord.DataType.Manufacturer),
                new ChannelRecord(this.channel(ChannelId.DEVICE_ID), ChannelRecord.DataType.DeviceId)
        );
        switch (waterMeterModelWirelessMbus) {
            case AUTOSEARCH:
            case ENGELMANN_WATERSTAR_M:
                break;
            case RELAY_PADPULS_M2W_CHANNEL1:
                String meterNumber1 = super.getRadioAddress().substring(2,8) + "01";
                protocol.setMeterNumber(meterNumber1);
                break;
            case RELAY_PADPULS_M2W_CHANNEL2:
                String meterNumber2 = super.getRadioAddress().substring(2,8) + "02";
                protocol.setMeterNumber(meterNumber2);
                break;
        }
        return protocol;
    }

    @Override
    public void logSignalStrength(int signalStrength) {
        this.channel(ChannelId.SIGNAL_STRENGTH).setNextValue(signalStrength);
    }

    @Override
    public void findRecordPositions(VariableDataStructure data, List<ChannelRecord> channelDataRecordsList) {

        // Search for the entries starting at the top of the list.
        if (waterMeterModelWirelessMbus == WaterMeterModelWirelessMbus.AUTOSEARCH) {
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

        // In the Waterstar, the entries for TOTAL_CONSUMED_WATER and TIMESTAMP_SECONDS are at the end of the record
        // list. So search the list starting from the end.
        if (waterMeterModelWirelessMbus == WaterMeterModelWirelessMbus.ENGELMANN_WATERSTAR_M) {
            List<DataRecord> dataRecords = data.getDataRecords();
            int numberOfEntries = dataRecords.size();
            boolean volumePositionFound = false;
            boolean timestampPositionFound = false;
            // Check to see if "openEMS timestamp" option is active, which sets the address to -1. If that is active,
            // don't change that address.
            if (channelDataRecordsList.get(1).getDataRecordPosition() < 0) {
                timestampPositionFound = true;
            }
            int dataRecordNumber = numberOfEntries - 1;
            while (volumePositionFound == false || timestampPositionFound == false) {
                DataRecord currentRecord = dataRecords.get(dataRecordNumber);
                if (volumePositionFound == false && currentRecord.getDescription() == DataRecord.Description.VOLUME) {
                    channelDataRecordsList.get(0).setDataRecordPosition(dataRecordNumber);  // Entry #0 in list is TOTAL_CONSUMED_WATER
                    volumePositionFound = true;
                } else if (timestampPositionFound == false && currentRecord.getDescription() == DataRecord.Description.DATE_TIME) {
                    channelDataRecordsList.get(1).setDataRecordPosition(dataRecordNumber);  // Entry #1 in list is TIMESTAMP_SECONDS
                    timestampPositionFound = true;
                } else {
                    dataRecordNumber--;
                    if (dataRecordNumber < 0) {
                        break;
                    }
                }
            }
        }
    }
}
