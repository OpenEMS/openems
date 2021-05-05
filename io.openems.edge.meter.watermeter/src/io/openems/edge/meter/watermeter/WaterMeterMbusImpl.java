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

/**
 * This class implements a water meter communicating via M-Bus.
 * M-Bus devices are polled by the M-Bus bridge in an interval configured by the module. This done to preserve battery
 * energy on battery powered meters. Meters are identified by their primary address, which needs to be entered at
 * configuration.
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
            this.allocateAddressViaMeterModel(config.model());
            this.volAddress = this.waterMeterModelMbus.getVolumeCounterPosition();
            this.timeStampAddress = this.waterMeterModelMbus.getTimeStampPosition();
        }
        if (config.openEmsTimeStamp()) {
            // Address of "-1" for the timestamp means OpenEMS time is used to create the timestamp.
            this.timeStampAddress = -1;
        }
        if (config.usePollingInterval()) {
            super.activate(context, config.id(), config.alias(), config.enabled(), config.primaryAddress(), this.cm, "mbus",
                    config.mbusBridgeId(), config.pollingIntervalSeconds(), this.getErrorMessageChannel());     // If you want to use the polling interval, put the time as the second last argument in super.activate().
        } else {
            super.activate(context, config.id(), config.alias(), config.enabled(), config.primaryAddress(), this.cm, "mbus",
                    config.mbusBridgeId(), 0, this.getErrorMessageChannel());  // If you don't want to use the polling interval, use 0 for the second last argument in super.activate().
        }
    }

    /**
     * Maps the config option "Model" to the values in enum WaterMeterModelMbus and enables dynamicDataAddress
     * if needed by that meter model.
     *
     * @param meterModel the meter model as a string.
     */
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
        /* The total_consumed_water channelRecord needs to be in the first position of this list, otherwise findRecordPositions()
           for the data records won't work correctly. */
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(WaterMeter.ChannelId.TOTAL_CONSUMED_WATER), this.volAddress));

        /* The timestamp_seconds channelRecord needs to be in the second position of this list, otherwise findRecordPositions()
           for the data records won't work correctly. */
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(WaterMeter.ChannelId.TIMESTAMP_SECONDS), this.timeStampAddress));

        /* TimestampString is always on address -2, since it's an internal method. This channel needs to be
           called after the TimestampSeconds Channel, as it takes it's value from that channel. */
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(WaterMeter.ChannelId.TIMESTAMP_STRING), -2));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(ChannelId.MANUFACTURER_ID), DataType.Manufacturer));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(ChannelId.DEVICE_ID), DataType.DeviceId));
    }

    @Override
    public void findRecordPositions(VariableDataStructure data, List<ChannelRecord> channelDataRecordsList) {

        /* This is the code used for the "Autosearch" option.
           Entry 0 in channelDataRecordsList is the volume channel, entry 1 is the timestamp channel. This is defined in
           the "defineWMbusProtocol()" method.
           Look at the units in the WM-Bus data records. Find a data record that has the unit volume and another with
           unit date_time. Start searching from the top of the list. When a match is found, write the record position in
           the channelDataRecordsList. */
        if (this.waterMeterModelMbus == WaterMeterModelMbus.AUTOSEARCH) {
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
    }


}
