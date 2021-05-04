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

/**
 * This class implements a water meter communicating via Wireless M-Bus.
 * WM-Bus devices regularly send radio messages in an interval that is defined by the device. An active WM-Bus bridge
 * listens to these messages. The messages contain some identification information in the header and a usually encrypted
 * message body called the variable data structure. Part of the identification information is the radio address of the
 * device, that is usually printed on the meter.
 * This class uses the radio address of the meter to identify it's radio messages. The radio address is registered with
 * the WM-Bus bridge, who then assigns any received messages from this radio address to this device. If a decryption key
 * is entered when configuring the module, this key is used to decrypt the messages.
 * The data in a message is organized as numbered data records that contain a value and a unit. The basic case is that
 * volume of consumed water is on record number 0, and the timestamp is on record number 1. On which record number which
 * data is stored depends on the meter model. Some meters even have dynamic addresses/positions.
 * Data values are transferred to channels by assigning a channel to a data record address. So you need to know on which
 * record position your meters stores which data. Since the data records have units, an autosearch function is possible
 * that compares the data record's unit with the channel unit until it finds a match. The units comparison is also used
 * to automatically scale the value to the channel unit. If that is not possible because of unit mismatch, an error
 * message is written in the error message channel.
 * This module includes a list of meters with their data record positions for volume and timestamp. If your meter is not
 * in that list, you can manually enter the record positions for volume and timestamp or use the autosearch function. It
 * is also possible to let OpenEMS create the timestamp instead of reading it from the meter.
 */

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
            this.allocateAddressViaMeterModel(config.model());
            this.volAddress = this.waterMeterModelWirelessMbus.getVolumeCounterPosition();
            this.timeStampAddress = this.waterMeterModelWirelessMbus.getTimeStampPosition();
        }
        if (config.openEmsTimeStamp()) {
            // Address of "-1" for the timestamp means OpenEMS time is used to create the timestamp.
            this.timeStampAddress = -1;
        }

        super.activate(context, config.id(), config.alias(), config.enabled(),
                config.radioAddress(), this.cm, "WirelessMbus", config.wmbusBridgeId(), config.key());

    }

    /**
     * Maps the config option "Model" to the values in enum WaterMeterModelWirelessMbus and enables dynamicDataAddress
     * if needed by that meter model.
     *
     * @param meterModel the meter model as a string.
     */
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
                /* The Waterstar transmits stored records before the current data. The number of stored records can vary,
                   so the address of the data after the stored records can change as well. */
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
        WMbusProtocol protocol = new WMbusProtocol(this, key, this.getErrorMessageChannel(),
                /* The total_consumed_water channelRecord needs to be in the first position of this list, otherwise findRecordPositions()
                   for the data records won't work correctly. */
                new ChannelRecord(this.channel(WaterMeter.ChannelId.TOTAL_CONSUMED_WATER), this.volAddress),

                /* The timestamp_seconds channelRecord needs to be in the second position of this list, otherwise findRecordPositions()
                   for the data records won't work correctly. */
                new ChannelRecord(this.channel(WaterMeter.ChannelId.TIMESTAMP_SECONDS), this.timeStampAddress),

                /* TimestampString is always on address -2, since it's an internal method. This channel needs to be
                   called after the TimestampSeconds Channel, as it takes it's value from that channel. */
                new ChannelRecord(this.channel(WaterMeter.ChannelId.TIMESTAMP_STRING), -2),
                new ChannelRecord(this.channel(ChannelId.MANUFACTURER_ID), ChannelRecord.DataType.Manufacturer),
                new ChannelRecord(this.channel(ChannelId.DEVICE_ID), ChannelRecord.DataType.DeviceId)
        );
        switch (this.waterMeterModelWirelessMbus) {
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

        /* This is the code used for the "Autosearch" option.
           Entry 0 in channelDataRecordsList is the volume channel, entry 1 is the timestamp channel. This is defined in
           the "defineWMbusProtocol()" method.
           Look at the units in the WM-Bus data records. Find a data record that has the unit volume and another with
           unit date_time. Start searching from the top of the list. When a match is found, write the record position in
           the channelDataRecordsList. */
        if (this.waterMeterModelWirelessMbus == WaterMeterModelWirelessMbus.AUTOSEARCH) {
            List<DataRecord> dataRecords = data.getDataRecords();
            int numberOfEntries = dataRecords.size();
            boolean volumePositionFound = false;
            boolean timestampPositionFound = false;
            /* Check to see if "openEMS timestamp" option is active, which sets the address to -1. If that is active,
               don't change that address. */
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

        /* This is the code used for the water meter model "Engelmann Waterstar M".
           In the Waterstar, the entries for TOTAL_CONSUMED_WATER and TIMESTAMP_SECONDS are at the end of the record
           list. So search the list starting from the end. */
        if (this.waterMeterModelWirelessMbus == WaterMeterModelWirelessMbus.ENGELMANN_WATERSTAR_M) {
            List<DataRecord> dataRecords = data.getDataRecords();
            int numberOfEntries = dataRecords.size();
            boolean volumePositionFound = false;
            boolean timestampPositionFound = false;
            /* Check to see if "openEMS timestamp" option is active, which sets the address to -1. If that is active,
               don't change that address. */
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
