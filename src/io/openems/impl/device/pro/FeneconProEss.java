package io.openems.impl.device.pro;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ConfigChannelBuilder;
import io.openems.api.channel.numeric.NumericChannel;
import io.openems.api.channel.numeric.NumericChannelBuilder;
import io.openems.api.channel.numeric.NumericChannelBuilder.Aggregation;
import io.openems.api.channel.numeric.WriteableNumericChannel;
import io.openems.api.device.nature.EssNature;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.WriteableModbusChannel;
import io.openems.impl.protocol.modbus.internal.ElementBuilder;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.ModbusRange;
import io.openems.impl.protocol.modbus.internal.WritableModbusRange;
import io.openems.impl.protocol.modbus.internal.channel.ModbusChannelBuilder;
import io.openems.impl.protocol.modbus.internal.channel.WriteableModbusChannelBuilder;

public class FeneconProEss extends ModbusDeviceNature implements EssNature {

	private final ModbusChannel _systemState = new ModbusChannelBuilder().nature(this) //
			.label(0, STANDBY) //
			.label(1, "Off-grid") //
			.label(2, "On-grid") //
			.label(3, "Fail") //
			.label(4, "Off-grid PV").build();
	private final ModbusChannel _controlMode = new ModbusChannelBuilder().nature(this) //
			.label(1, "Remote") //
			.label(2, "Local").build();
	private final ModbusChannel _workMode = new ModbusChannelBuilder().nature(this) //
			.label(2, "Economy") //
			.label(6, "Remote dispatch") //
			.label(8, "Timing").build();
	private final ModbusChannel _batteryGroupState = new ModbusChannelBuilder().nature(this) //
			.label(0, "Initial") //
			.label(1, "Stop") //
			.label(2, "Starting") //
			.label(3, "Running") //
			.label(4, "Stopping") //
			.label(5, "Fail").build();
	private final ModbusChannel _pcsOperationState = new ModbusChannelBuilder().nature(this) //
			.label(0, "Self-checking") //
			.label(1, "Standby") //
			.label(2, "Off grid PV") //
			.label(3, "Off grid") //
			.label(4, ON_GRID) //
			.label(5, "Fail") //
			.label(6, "bypass 1") //
			.label(7, "bypass 2").build();
	private final ModbusChannel _batteryGroupAlarm = new ModbusChannelBuilder().nature(this) //
			.label(1, "Fail, The system should be stopped") //
			.label(2, "Common low voltage alarm") //
			.label(4, "Common high voltage alarm") //
			.label(8, "Charging over current alarm") //
			.label(16, "Discharging over current alarm") //
			.label(32, "Over temperature alarm")//
			.label(64, "Interal communication abnormal").build();
	private final ModbusChannel _totalBatteryChargeEnergy = new ModbusChannelBuilder().nature(this).unit("Wh").build();
	private final ModbusChannel _totalBatteryDischargeEnergy = new ModbusChannelBuilder().nature(this).unit("Wh")
			.build();
	private final ModbusChannel _soc = new ModbusChannelBuilder().nature(this).unit("%").build();
	private final ModbusChannel _batteryVoltage = new ModbusChannelBuilder().nature(this).unit("mV").multiplier(100)
			.build();
	private final ModbusChannel _batteryCurrent = new ModbusChannelBuilder().nature(this).unit("mA").multiplier(100)
			.build();
	private final ModbusChannel _batteryPower = new ModbusChannelBuilder().nature(this).unit("W").build();
	private final ModbusChannel _currentPhaseA = new ModbusChannelBuilder().nature(this).unit("mA").multiplier(100)
			.build();
	private final ModbusChannel _currentPhaseB = new ModbusChannelBuilder().nature(this).unit("mA").multiplier(100)
			.build();
	private final ModbusChannel _currentPhaseC = new ModbusChannelBuilder().nature(this).unit("mA").multiplier(100)
			.build();
	private final ModbusChannel _voltagePhaseA = new ModbusChannelBuilder().nature(this).unit("mV").multiplier(100)
			.build();
	private final ModbusChannel _voltagePhaseB = new ModbusChannelBuilder().nature(this).unit("mV").multiplier(100)
			.build();
	private final ModbusChannel _voltagePhaseC = new ModbusChannelBuilder().nature(this).unit("mV").multiplier(100)
			.build();
	private final ModbusChannel _activePowerPhaseA = new ModbusChannelBuilder().nature(this).unit("W").build();
	private final ModbusChannel _activePowerPhaseB = new ModbusChannelBuilder().nature(this).unit("W").build();
	private final ModbusChannel _activePowerPhaseC = new ModbusChannelBuilder().nature(this).unit("W").build();
	private final ModbusChannel _reactivePowerPhaseA = new ModbusChannelBuilder().nature(this).unit("var").build();
	private final ModbusChannel _reactivePowerPhaseB = new ModbusChannelBuilder().nature(this).unit("var").build();
	private final ModbusChannel _reactivePowerPhaseC = new ModbusChannelBuilder().nature(this).unit("var").build();
	private final ModbusChannel _frequencyPhaseA = new ModbusChannelBuilder().nature(this).unit("mHz").multiplier(10)
			.build();
	private final ModbusChannel _frequencyPhaseB = new ModbusChannelBuilder().nature(this).unit("mHz").multiplier(10)
			.build();
	private final ModbusChannel _frequencyPhaseC = new ModbusChannelBuilder().nature(this).unit("mHz").multiplier(10)
			.build();
	private final ModbusChannel _pcsAllowedApparentPower = new ModbusChannelBuilder().nature(this).unit("VA").build();
	private final ModbusChannel _apparentPower = new ModbusChannelBuilder().nature(this).unit("VA").build();
	private final NumericChannel _activePower = new NumericChannelBuilder<>().nature(this).unit("W")
			.channel(_activePowerPhaseA, _activePowerPhaseB, _activePowerPhaseC).aggregate(Aggregation.SUM).build();
	private final NumericChannel _reactivePower = new NumericChannelBuilder<>().nature(this).unit("W")
			.channel(_reactivePowerPhaseA, _reactivePowerPhaseB, _reactivePowerPhaseC).aggregate(Aggregation.SUM)
			.build();
	private final ModbusChannel _allowedDischarge = new ModbusChannelBuilder().nature(this).unit("W").build();
	private final ModbusChannel _allowedCharge = new ModbusChannelBuilder().nature(this).unit("W").multiplier(-1)
			.build();
	private final WriteableModbusChannel _setWorkState = new WriteableModbusChannelBuilder().nature(this) //
			.label(0, "Local control") //
			.label(1, START) // "Remote control on grid starting"
			.label(2, "Remote control off grid starting") //
			.label(3, STOP)//
			.label(4, "Emergency Stop").build();
	private final WriteableModbusChannel _setActivePower = new WriteableModbusChannelBuilder().nature(this).unit("W")
			.minWriteValue(_allowedCharge).maxWriteValue(_allowedDischarge).build();
	private final WriteableModbusChannel _setReactivePower = new WriteableModbusChannelBuilder().nature(this)
			.unit("var").minWriteValue(_allowedCharge).maxWriteValue(_allowedDischarge).build();
	private final ConfigChannel _minSoc = new ConfigChannelBuilder().nature(this).defaultValue(DEFAULT_MINSOC)
			.percentType().build();

	public FeneconProEss(String thingId) {
		super(thingId);
	}

	@Override
	public NumericChannel activePower() {
		return _activePower;
	}

	@Override
	public NumericChannel allowedCharge() {
		return _allowedCharge;
	}

	@Override
	public NumericChannel allowedDischarge() {
		return _allowedDischarge;
	}

	@Override
	public NumericChannel apparentPower() {
		return _apparentPower;
	}

	@Override
	public NumericChannel gridMode() {
		return _pcsOperationState;
	}

	@Override
	public NumericChannel minSoc() {
		return _minSoc;
	}

	@Override
	public NumericChannel reactivePower() {
		return _reactivePower;
	}

	@Override
	public WriteableNumericChannel setActivePower() {
		return _setActivePower;
	}

	@Override
	public void setMinSoc(Integer minSoc) {
		this._minSoc.updateValue(Long.valueOf(minSoc));
	}

	@Override
	public WriteableNumericChannel setWorkState() {
		return _setWorkState;
	}

	@Override
	public NumericChannel soc() {
		return _soc;
	}

	@Override
	public NumericChannel systemState() {
		return _systemState;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		return new ModbusProtocol( //
				new ModbusRange(100, //
						new ElementBuilder().address(100).channel(_systemState).build(), //
						new ElementBuilder().address(101).channel(_controlMode).build(), //
						new ElementBuilder().address(102).channel(_workMode).build(), //
						new ElementBuilder().address(103).dummy().build(), //
						new ElementBuilder().address(104).channel(_totalBatteryChargeEnergy).doubleword().build(), //
						new ElementBuilder().address(106).channel(_totalBatteryDischargeEnergy).doubleword().build(), //
						new ElementBuilder().address(108).channel(_batteryGroupState).build(), //
						new ElementBuilder().address(109).channel(_soc).build(), //
						new ElementBuilder().address(110).channel(_batteryVoltage).build(), //
						new ElementBuilder().address(111).channel(_batteryCurrent).build(), //
						new ElementBuilder().address(112).channel(_batteryPower).build(), //
						new ElementBuilder().address(113).channel(_batteryGroupAlarm).build(), //
						new ElementBuilder().address(114).channel(_pcsOperationState).build(), //
						new ElementBuilder().address(115).dummy(118 - 115).build(), //
						new ElementBuilder().address(118).channel(_currentPhaseA).signed().build(), //
						new ElementBuilder().address(119).channel(_currentPhaseB).signed().build(), //
						new ElementBuilder().address(120).channel(_currentPhaseC).signed().build(), //
						new ElementBuilder().address(121).channel(_voltagePhaseA).build(), //
						new ElementBuilder().address(122).channel(_voltagePhaseB).build(), //
						new ElementBuilder().address(123).channel(_voltagePhaseC).build(), //
						new ElementBuilder().address(124).channel(_activePowerPhaseA).signed().build(), //
						new ElementBuilder().address(125).channel(_activePowerPhaseB).signed().build(), //
						new ElementBuilder().address(126).channel(_activePowerPhaseC).signed().build(), //
						new ElementBuilder().address(127).channel(_reactivePowerPhaseA).signed().build(), //
						new ElementBuilder().address(128).channel(_reactivePowerPhaseB).signed().build(), //
						new ElementBuilder().address(129).channel(_reactivePowerPhaseC).signed().build(), //
						new ElementBuilder().address(130).dummy().build(), //
						new ElementBuilder().address(131).channel(_frequencyPhaseA).build(), //
						new ElementBuilder().address(132).channel(_frequencyPhaseB).build(), //
						new ElementBuilder().address(133).channel(_frequencyPhaseC).build(), //
						new ElementBuilder().address(134).channel(_pcsAllowedApparentPower).build(), //
						new ElementBuilder().address(135).dummy(141 - 135).build(), //
						new ElementBuilder().address(141).channel(_allowedCharge).build(), //
						new ElementBuilder().address(142).channel(_allowedDischarge).build() //
				), //
				new WritableModbusRange(200, //
						new ElementBuilder().address(200).channel(_setWorkState).build(),
						new ElementBuilder().address(201).channel(_setActivePower).signed().build(),
						new ElementBuilder().address(202).channel(_setReactivePower).signed().build()));
	}

}
