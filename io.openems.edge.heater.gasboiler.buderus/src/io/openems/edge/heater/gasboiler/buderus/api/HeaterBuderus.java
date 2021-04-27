package io.openems.edge.heater.gasboiler.buderus.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.heater.Heater;

public interface HeaterBuderus extends Heater {


    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Input Registers, read only. The register address is in the channel name, so IR0 means input register 0.
        // Unsigned 16 bit, unless stated otherwise.

        //IR384_STRATEGIE_RETURN_TEMPERATURE -> Heater, RETURN_TEMPERATURE. Funktioniert nicht. Fällt weg. Nimm Wert vom Kessel.

        //IR385_STRATEGIE_FLOW_TEMPERATURE -> Heater, FLOW_TEMPERATURE. Fällt weg. Nimm Wert vom Kessel.

        /**
         * Status Strategie.
         * 0-Ubekannt
         * 1-Warnung
         * 2-Störung
         * 3-OK
         * 4-Nicht aktiv
         * 5-Kritisch
         * 6-Keine Info
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR386_STATUS_STRATEGIE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        //IR387_STRATEGIE_READ_EFFECTIVE_POWER_PERCENT -> Heater, READ_EFFECTIVE_POWER_PERCENT. Fällt weg. Nimm Wert vom Kessel.

        /**
         * Who requested heater to turn on?
         * 0-Nicht aktiv
         * 1-Regelgerät
         * 2-Intern
         * 3-Manueller Betrieb
         * 4-Extern
         * 5-Intern+Extern
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR390_RUNREQUEST_INITIATOR(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Strategie Bitblock.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR394_STRATEGIE_BITBLOCK(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Maximale Vorlauftemperatur angefordert. Degree Celsius (NOT dezidegree) vom Kessel, auf Umwandlung achten!
         * Signed.
         * Bedeutung vermutlich: Höchster Wert, der für die Vorlauftemperatur angefordert wurde.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Dezidegree Celsius
         * </ul>
         */
        IR395_MAX_FLOW_TEMP_ANGEFORDERT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        /**
         * Fehlerregister 1. Doubleword.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR476_FEHLERREGISTER1(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Fehlerregister 2. Doubleword.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR478_FEHLERREGISTER2(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Fehlerregister 3. Doubleword.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR480_FEHLERREGISTER3(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Fehlerregister 4. Doubleword.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR482_FEHLERREGISTER4(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        //IR8001_FLOW_TEMP_KESSEL1 -> Heater, FLOW_TEMPERATURE. d°C, signed.

        /**
         * Temperatur Vorlauf Änderungsgeschwindigkeit. Unit dezi Kelvin / min, signed.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Dezi Kelvin / min
         * </ul>
         */
        //TODO
        //IR8002_FLOW_TEMP_AENDERUNGSGESCHWINDIGKEIT_KESSEL1(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZI_KELVIN_PER_MINUTE).accessMode(AccessMode.READ_ONLY)),

        //IR8003_RETURN_TEMP_KESSEL1 -> Heater, RETURN_TEMPERATURE. d°C, signed.

        //IR8004_LEISTUNG_ISTWERT_KESSEL1 -> Heater, READ_EFFECTIVE_POWER_PERCENT. %, unsigned.

        /**
         * Wärmeerzeuger in Lastbegrenzung Kessel 1.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Percent
         * </ul>
         */
        IR8005_WAERMEERZEUGER_IN_LASTBEGRENZUNG_KESSEL1(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),

        //IR8006_BETRIEBS_TEMPERATUR -> liefert keinen Wert.

        /**
         * Maximale Leistung Kessel 1.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Kilowatt
         * </ul>
         */
        IR8007_MAXIMUM_POWER_KESSEL1(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT).accessMode(AccessMode.READ_ONLY)),

        /**
         * Minimale Leistung Kessel 1.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Percent
         * </ul>
         */
        IR8008_MINIMUM_POWER_PERCENT_KESSEL1(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),

        /**
         * Maximale Vorlauftemp Kessel 1. Degree Celsius (NOT dezidegree) vom Kessel, auf Umwandlung achten! unsigned.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Dezidegree Celsius
         * </ul>
         */
        IR8011_MAXIMALE_VORLAUFTEMP_KESSEL1(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        /**
         * Status Kessel 1.
         * 0-Ubekannt
         * 1-Warnung
         * 2-Störung
         * 3-OK
         * 4-Nicht aktiv
         * 5-Kritisch
         * 6-Keine Info
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR8012_STATUS_KESSEL1(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Bitblock Kessel 1.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR8013_BITBLOCK_KESSEL1(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Angeforderte Sollwertemperatur Kessel 1. Degree Celsius (NOT dezidegree) vom Kessel, auf Umwandlung achten!
         * unsigned.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Dezidegree Celsius
         * </ul>
         */
        IR8015_ANGEFORDERTE_SOLLWERTTEMP_KESSEL1(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        /**
         * Sollwert Leistung Kessel 1, %.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Percent
         * </ul>
         */
        IR8016_SOLLWERT_LEISTUNG_KESSEL1(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),

        /**
         * Druck Kessel 1. Decibar, signed.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decibar
         * </ul>
         */
        IR8017_DRUCK_KESSEL1(Doc.of(OpenemsType.INTEGER).unit(Unit.DECI_BAR).accessMode(AccessMode.READ_ONLY)),

        /**
         * Fehlercode Kessel 1.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR8018_FEHLERCODE_KESSEL1(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Fehleranzeigecode im Display Kessel 1.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR8019_FEHLERCODE_DISPLAY_KESSEL1(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Anzahl Starts Kessel 1. Doubleword.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR8021_ANZAHL_STARTS_KESSEL1(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Betriebszeit Kessel 1. Doubleword.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Minutes
         * </ul>
         */
        IR8023_BETRIEBSZEIT_KESSEL1(Doc.of(OpenemsType.INTEGER).unit(Unit.MINUTE).accessMode(AccessMode.READ_ONLY)),

        // Holding Registers, read/write. The register address is in the channel name, so HR0 means holding register 0.
        // Unsigned 16 bit, unless stated otherwise.

        /**
         * Heart beat, value that is written to the device.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR0_HEARTBEAT_IN(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heart beat, value that has been received by the device. If "Heart Beat" is activated in the device
         * configuration, a value needs to be constantly written to HR0_HEARTBEAT_IN. If the value is received by the
         * device, it will appear in HR1_HEARTBEAT_OUT for confirmation. The value written to HR0_HEARTBEAT_IN needs to
         * be different than the last value. The suggested algorithm is to read the value from HR1_HEARTBEAT_OUT,
         * increment it and then send it to HR0_HEARTBEAT_IN. With an overflow protection, to start again with 1 if the
         * counter has reached a certain threshold (remember 16 bit limitation).
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR1_HEARTBEAT_OUT(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        //HR400_SET_POINT_FLOW_TEMPERATUR -> Heater, SET_POINT_TEMPERATURE, d°C. Unit at heater is °C, not d°C, so watch the conversion.

        //HR401_SET_POINT_POWER_PERCENT -> Heater, SET_POINT_POWER_PERCENT, %. Unit at heater is also %, a value of 50 in the channel means 50%.

        /**
         * Give the heater run permission.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR402_RUN_PERMISSION(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Register containing command bits.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR405_COMMAND_BITS(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),


        // Non Modbus channels

        /**
         * Operation mode, set point temperature (0) or set point power percent (1). Default is set point power percent.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        OPERATING_MODE(Doc.of(OperatingMode.values()).accessMode(AccessMode.READ_WRITE)
                .onInit(channel -> { //
                    // on each Write to the channel -> set the value
                    ((EnumWriteChannel) channel).onSetNextWrite(value -> {
                        channel.setNextValue(value);
                    });
                })),

        /**
         * Status message of the heater.
         * <ul>
         *      <li> Type: String
         * </ul>
         */
        STATUS_MESSAGE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    // Input Registers. Read only.

    /**
     * Gets the Channel for {@link ChannelId#IR386_STATUS_STRATEGIE}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getIR386StatusStrategieChannel() {
        return this.channel(ChannelId.IR386_STATUS_STRATEGIE);
    }

    /**
     * Status Strategie.
     * 0-Ubekannt
     * 1-Warnung
     * 2-Störung
     * 3-OK
     * 4-Nicht aktiv
     * 5-Kritisch
     * 6-Keine Info
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getIR386StatusStrategie() {
        return this.getIR386StatusStrategieChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR390_RUNREQUEST_INITIATOR}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getIR390RunrequestInitiatorChannel() {
        return this.channel(ChannelId.IR390_RUNREQUEST_INITIATOR);
    }

    /**
     * Who requested heater to turn on?
     * 0-Nicht aktiv
     * 1-Regelgerät
     * 2-Intern
     * 3-Manueller Betrieb
     * 4-Extern
     * 5-Intern+Extern
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getIR390RunrequestInitiator() {
        return this.getIR390RunrequestInitiatorChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR394_STRATEGIE_BITBLOCK}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getIR394StrategieBitblockChannel() {
        return this.channel(ChannelId.IR394_STRATEGIE_BITBLOCK);
    }

    /**
     * Strategie Bitblock.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getIR394StrategieBitblock() {
        return this.getIR394StrategieBitblockChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR395_MAX_FLOW_TEMP_ANGEFORDERT}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getIR395MaxFlowTempAngefordertChannel() {
        return this.channel(ChannelId.IR395_MAX_FLOW_TEMP_ANGEFORDERT);
    }

    /**
     * Maximale Vorlauftemperatur angefordert.
     * Bedeutung vermutlich: Höchster Wert, der für die Vorlauftemperatur angefordert wurde.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getIR395MaxFlowTempAngefordert() {
        return this.getIR395MaxFlowTempAngefordertChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR476_FEHLERREGISTER1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getIR476Fehlerregister1Channel() {
        return this.channel(ChannelId.IR476_FEHLERREGISTER1);
    }

    /**
     * Fehlerregister 1.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getIR476Fehlerregister1() {
        return this.getIR476Fehlerregister1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR478_FEHLERREGISTER2}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getIR478Fehlerregister2Channel() {
        return this.channel(ChannelId.IR478_FEHLERREGISTER2);
    }

    /**
     * Fehlerregister 2.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getIR478Fehlerregister2() {
        return this.getIR478Fehlerregister2Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR480_FEHLERREGISTER3}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getIR480Fehlerregister3Channel() {
        return this.channel(ChannelId.IR480_FEHLERREGISTER3);
    }

    /**
     * Fehlerregister 3.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getIR480Fehlerregister3() {
        return this.getIR480Fehlerregister3Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR482_FEHLERREGISTER4}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getIR482Fehlerregister4Channel() {
        return this.channel(ChannelId.IR482_FEHLERREGISTER4);
    }

    /**
     * Fehlerregister 4.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getIR482Fehlerregister4() {
        return this.getIR482Fehlerregister4Channel().value();
    }

    // TODO

    /**
     * Gets the Channel for {@link ChannelId#IR8002_FLOW_TEMP_AENDERUNGSGESCHWINDIGKEIT_KESSEL1}.
     *
     * @return the Channel
     */
 /*   public default IntegerReadChannel getIR8002FlowTempChangeSpeedChannel() {
        return this.channel(ChannelId.IR8002_FLOW_TEMP_AENDERUNGSGESCHWINDIGKEIT_KESSEL1);
    }*/

    /**
     * Flow temperature change speed, unit is deci Kelvin / min.
     *
     * @return the Channel {@link Value}
     */
  /*  public default Value<Integer> getIR8002FlowTempChangeSpeed() {
        return this.getIR8002FlowTempChangeSpeedChannel().value();
    }*/

    /**
     * Gets the Channel for {@link ChannelId#IR8005_WAERMEERZEUGER_IN_LASTBEGRENZUNG_KESSEL1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getIR8005WaermeerzeugerInLastbegrenzungChannel() {
        return this.channel(ChannelId.IR8005_WAERMEERZEUGER_IN_LASTBEGRENZUNG_KESSEL1);
    }

    /**
     * Waermeerzeuger in Lastbegrenzung, unit is %.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getIR8005WaermeerzeugerInLastbegrenzung() {
        return this.getIR8005WaermeerzeugerInLastbegrenzungChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8007_MAXIMUM_POWER_KESSEL1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getMaximumPowerKessel1Channel() {
        return this.channel(ChannelId.IR8007_MAXIMUM_POWER_KESSEL1);
    }

    /**
     * Get the maximum thermal output of Kessel 1, unit is kW.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getMaximumPowerKessel1() {
        return this.getMaximumPowerKessel1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8008_MINIMUM_POWER_PERCENT_KESSEL1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getMinimumPowerPercentKessel1Channel() {
        return this.channel(ChannelId.IR8008_MINIMUM_POWER_PERCENT_KESSEL1);
    }

    /**
     * Get the minimum thermal output of Kessel 1 in percent.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getMinimumPowerPercentKessel1() {
        return this.getMinimumPowerPercentKessel1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8011_MAXIMALE_VORLAUFTEMP_KESSEL1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getMaximumFlowTempKessel1Channel() {
        return this.channel(ChannelId.IR8011_MAXIMALE_VORLAUFTEMP_KESSEL1);
    }

    /**
     * Get the maximum flow temperature of Kessel 1, unit is dezidegree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getMaximumFlowTempKessel1() {
        return this.getMaximumFlowTempKessel1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8012_STATUS_KESSEL1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getStatusKessel1Channel() {
        return this.channel(ChannelId.IR8012_STATUS_KESSEL1);
    }

    /**
     * Status Kessel 1.
     * 0-Ubekannt
     * 1-Warnung
     * 2-Störung
     * 3-OK
     * 4-Nicht aktiv
     * 5-Kritisch
     * 6-Keine Info
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getStatusKessel1() {
        return this.getStatusKessel1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8013_BITBLOCK_KESSEL1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getBitblockKessel1Channel() {
        return this.channel(ChannelId.IR8013_BITBLOCK_KESSEL1);
    }

    /**
     * Bitblock Kessel 1.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getBitblockKessel1() {
        return this.getBitblockKessel1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8015_ANGEFORDERTE_SOLLWERTTEMP_KESSEL1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getRequestedTemperatureSetPointKessel1Channel() {
        return this.channel(ChannelId.IR8015_ANGEFORDERTE_SOLLWERTTEMP_KESSEL1);
    }

    /**
     * Requested temperature set point Kessel 1, unit is dezidegree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getRequestedTemperatureSetPointKessel1() {
        return this.getRequestedTemperatureSetPointKessel1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8016_SOLLWERT_LEISTUNG_KESSEL1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getRequestedPowerPercentSetPointKessel1Channel() {
        return this.channel(ChannelId.IR8016_SOLLWERT_LEISTUNG_KESSEL1);
    }

    /**
     * Requested power percent set point Kessel 1, unit is percent.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getRequestedPowerPercentSetPointKessel1() {
        return this.getRequestedPowerPercentSetPointKessel1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8017_DRUCK_KESSEL1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getPressureKessel1Channel() {
        return this.channel(ChannelId.IR8017_DRUCK_KESSEL1);
    }

    /**
     * Pressure Kessel 1, unit is deci Bar.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getPressureKessel1() {
        return this.getPressureKessel1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8018_FEHLERCODE_KESSEL1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getErrorCodeKessel1Channel() {
        return this.channel(ChannelId.IR8018_FEHLERCODE_KESSEL1);
    }

    /**
     * Error code Kessel 1.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getErrorCodeKessel1() {
        return this.getErrorCodeKessel1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8019_FEHLERCODE_DISPLAY_KESSEL1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getErrorCodeDisplayKessel1Channel() {
        return this.channel(ChannelId.IR8019_FEHLERCODE_DISPLAY_KESSEL1);
    }

    /**
     * Error code display Kessel 1.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getErrorCodeDisplayKessel1() {
        return this.getErrorCodeDisplayKessel1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8021_ANZAHL_STARTS_KESSEL1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getNumberOfStartsKessel1Channel() {
        return this.channel(ChannelId.IR8021_ANZAHL_STARTS_KESSEL1);
    }

    /**
     * Number of starts Kessel 1.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getNumberOfStartsKessel1() {
        return this.getNumberOfStartsKessel1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8023_BETRIEBSZEIT_KESSEL1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getRunningTimeKessel1Channel() {
        return this.channel(ChannelId.IR8023_BETRIEBSZEIT_KESSEL1);
    }

    /**
     * Running time Kessel 1, unit is minutes.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getRunningTimeKessel1() {
        return this.getRunningTimeKessel1Channel().value();
    }

    // Holding Registers. Read/write.

    /**
     * Gets the Channel for {@link ChannelId#HR0_HEARTBEAT_IN}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getHeartBeatInChannel() {
        return this.channel(ChannelId.HR0_HEARTBEAT_IN);
    }

    /**
     * Sets the value to send as hear beat. If "Heart Beat" is activated in the device configuration, a value needs to
     * be constantly written to setHeartBeatIn. If the value is received by the device, it will appear in getHeartBeatOut
     * for confirmation. The value written to setHeartBeatIn needs to be different than the last value. The suggested
     * algorithm is to read the value from getHeartBeatOut, increment it and then send it to setHeartBeatIn. With an
     * overflow protection, to start again with 1 if the counter has reached a certain threshold (remember 16 bit
     * limitation).
     */
    public default void setHeartBeatIn(Integer value) throws OpenemsError.OpenemsNamedException {
        this.getHeartBeatInChannel().setNextWriteValue(value);
    }

    /**
     * Sets the value to send as hear beat. If "Heart Beat" is activated in the device configuration, a value needs to
     * be constantly written to setHeartBeatIn. If the value is received by the device, it will appear in getHeartBeatOut
     * for confirmation. The value written to setHeartBeatIn needs to be different than the last value. The suggested
     * algorithm is to read the value from getHeartBeatOut, increment it and then send it to setHeartBeatIn. With an
     * overflow protection, to start again with 1 if the counter has reached a certain threshold (remember 16 bit
     * limitation).
     */
    public default void setHeartBeatIn(int value) throws OpenemsError.OpenemsNamedException {
        this.getHeartBeatInChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR1_HEARTBEAT_OUT}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHeartBeatOutChannel() {
        return this.channel(ChannelId.HR1_HEARTBEAT_OUT);
    }

    /**
     * Read the last heart beat value received by the device. If "Heart Beat" is activated in the device configuration,
     * a value needs to be constantly written to setHeartBeatIn. If the value is received by the device, it will appear
     * in getHeartBeatOut for confirmation. The value written to setHeartBeatIn needs to be different than the last
     * value. The suggested algorithm is to read the value from getHeartBeatOut, increment it and then send it to
     * setHeartBeatIn. With an overflow protection, to start again with 1 if the counter has reached a certain threshold
     * (remember 16 bit limitation).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHeartBeatOut() {
        return this.getHeartBeatOutChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR402_RUN_PERMISSION}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getRunPermissionChannel() {
        return this.channel(ChannelId.HR402_RUN_PERMISSION);
    }

    /**
     * Give the heater permission to run or not.
     */
    public default void setRunPermission(Boolean value) throws OpenemsError.OpenemsNamedException {
        this.getRunPermissionChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR405_COMMAND_BITS}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getCommandBitsChannel() {
        return this.channel(ChannelId.HR405_COMMAND_BITS);
    }

    /**
     * Sets the command bits.
     */
    public default void setCommandBits(Integer value) throws OpenemsError.OpenemsNamedException {
        this.getCommandBitsChannel().setNextWriteValue(value);
    }

    /**
     * Sets the command bits.
     */
    public default void setCommandBits(int value) throws OpenemsError.OpenemsNamedException {
        this.getCommandBitsChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#OPERATING_MODE}.
     *
     * @return the Channel
     */
    public default EnumWriteChannel getOperatingModeChannel() {
        return this.channel(ChannelId.OPERATING_MODE);
    }

    /**
     * Get the operating mode.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getOperatingMode() {
        return this.getOperatingModeChannel().value();
    }

    /**
     * Operating mode, set point temperature (0) or set point power percent (1). Default is set point power percent.
     */
    public default void setOperatingMode(Integer value) throws OpenemsError.OpenemsNamedException {
        this.getOperatingModeChannel().setNextWriteValue(value);
    }

    /**
     * Operating mode, set point temperature (0) or set point power percent (1). Default is set point power percent.
     */
    public default void setOperatingMode(int value) throws OpenemsError.OpenemsNamedException {
        this.getOperatingModeChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#STATUS_MESSAGE}.
     *
     * @return the Channel
     */
    public default StringReadChannel getStatusMessageChannel() {
        return this.channel(ChannelId.STATUS_MESSAGE);
    }

    /**
     * Get the status message.
     *
     * @return the Channel {@link Value}
     */
    public default Value<String> getStatusMessage() {
        return this.getStatusMessageChannel().value();
    }

    /**
     * Internal method.
     */
    public default void _setStatusMessage(String value) {
        this.getStatusMessageChannel().setNextValue(value);
    }
}
