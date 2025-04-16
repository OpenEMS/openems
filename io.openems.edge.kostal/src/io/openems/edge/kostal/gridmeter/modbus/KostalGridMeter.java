package io.openems.edge.kostal.gridmeter.modbus;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;

public interface KostalGridMeter
		extends
			ElectricityMeter,
			ModbusComponent,
			OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// EnumReadChannels
		SCALE_FACTOR_CURRENT(Doc.of(OpenemsType.INTEGER)),
		SCALE_FACTOR_VOLTAGE(Doc.of(OpenemsType.INTEGER)),
		SCALE_FACTOR_POWER(Doc.of(OpenemsType.INTEGER)),
		SCALE_FACTOR_ENERGY(Doc.of(OpenemsType.INTEGER)),
		//
		SCALE_FACTOR_FREQUENCY(Doc.of(OpenemsType.INTEGER).unit(Unit.HERTZ)),
		//
		REAL_IMPORTED_ENERGY(Doc.of(OpenemsType.DOUBLE).unit(Unit.WATT_HOURS)),
		REAL_EXPORTED_ENERGY(Doc.of(OpenemsType.DOUBLE).unit(Unit.WATT_HOURS));
	  
    private final Doc doc;

    private ChannelId(Doc doc) {
      this.doc = doc;
    }

    @Override
    public Doc doc() {
      return this.doc;
    }
	}
    
    // Methoden für SCALE_FACTOR_CURRENT
    public default IntegerReadChannel getScaleFactorCurrentChannel() {
        return this.channel(ChannelId.SCALE_FACTOR_CURRENT);
    }

    public default Integer getScaleFactorCurrentValue() {
        return this.getScaleFactorCurrentChannel().value().get();
    }

    public default void _setScaleFactorCurrent(Integer value) {
        this.getScaleFactorCurrentChannel().setNextValue(value);
    }

    // Methoden für SCALE_FACTOR_VOLTAGE
    public default IntegerReadChannel getScaleFactorVoltageChannel() {
        return this.channel(ChannelId.SCALE_FACTOR_VOLTAGE);
    }

    public default Integer getScaleFactorVoltageValue() {
        return this.getScaleFactorVoltageChannel().value().get();
    }

    public default void _setScaleFactorVoltage(Integer value) {
        this.getScaleFactorVoltageChannel().setNextValue(value);
    }

    // Methoden für SCALE_FACTOR_POWER
    public default IntegerReadChannel getScaleFactorPowerChannel() {
        return this.channel(ChannelId.SCALE_FACTOR_POWER);
    }

    public default Integer getScaleFactorPowerValue() {
        return this.getScaleFactorPowerChannel().value().get();
    }

    public default  void _setScaleFactorPower(Integer value) {
        this.getScaleFactorPowerChannel().setNextValue(value);
    }

    // Methoden für SCALE_FACTOR_ENERGY
    public default IntegerReadChannel getScaleFactorEnergyChannel() {
        return this.channel(ChannelId.SCALE_FACTOR_ENERGY);
    }

    public default Integer getScaleFactorEnergyValue() {
        return this.getScaleFactorEnergyChannel().value().get();
    }

    public default void _setScaleFactorEnergy(Integer value) {
        this.getScaleFactorEnergyChannel().setNextValue(value);
    }
    
    // Methoden für SCALE_FACTOR_FREQUENCY
    public default IntegerReadChannel getScaleFactorFrequencyChannel() {
        return this.channel(ChannelId.SCALE_FACTOR_FREQUENCY);
    }

    public default Integer getScaleFactorFrequencyValue() {
        return this.getScaleFactorFrequencyChannel().value().get();
    }

    public default void _setScaleFactorFrequency(Integer value) {
        this.getScaleFactorFrequencyChannel().setNextValue(value);
    }

    // Methoden für REAL_IMPORTED_ENERGY
    public default DoubleReadChannel getRealImportedEnergyChannel() {
        return this.channel(ChannelId.REAL_IMPORTED_ENERGY);
    }

    public default Double getRealImportedEnergyValue() {
        return this.getRealImportedEnergyChannel().value().get();
    }

    public default void _setRealImportedEnergy(Double value) {
        this.getRealImportedEnergyChannel().setNextValue(value);
    }

    // Methoden für REAL_EXPORTED_ENERGY
    public default DoubleReadChannel getRealExportedEnergyChannel() {
        return this.channel(ChannelId.REAL_EXPORTED_ENERGY);
    }

    public default Double getRealExportedEnergyValue() {
        return this.getRealExportedEnergyChannel().value().get();
    }

    public default void _setRealExportedEnergy(Double value) {
        this.getRealExportedEnergyChannel().setNextValue(value);
    }

}
