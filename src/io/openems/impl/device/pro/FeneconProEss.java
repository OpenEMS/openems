package io.openems.impl.device.pro;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ConfigChannelBuilder;
import io.openems.api.channel.IsChannel;
import io.openems.api.channel.numeric.NumericChannel;
import io.openems.api.channel.numeric.NumericChannelBuilder;
import io.openems.api.channel.numeric.NumericChannelBuilder.Aggregation;
import io.openems.api.channel.numeric.WriteableNumericChannel;
import io.openems.api.controller.IsThingMap;
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

@IsThingMap(type = EssNature.class)
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
	private final ModbusChannel _pcsAlarm1PhaseA = new ModbusChannelBuilder().nature(this) //
			.label(1, "Grid undervoltage") //
			.label(2, "Grid overvoltage") //
			.label(4, "Grid under frequency") //
			.label(8, "Grid over frequency") //
			.label(16, "Grid power supply off") //
			.label(32, "Grid condition unmeet")//
			.label(64, "DC under voltage")//
			.label(128, "Input over resistance")//
			.label(256, "Combination error")//
			.label(512, "Comm with inverter error")//
			.label(1024, "Tme error")//
			.build();
	private final ModbusChannel _pcsAlarm2PhaseA = new ModbusChannelBuilder().nature(this) //
			.build();
	private final ModbusChannel _pcsFault1PhaseA = new ModbusChannelBuilder().nature(this) //
			.label(1, "Control current overload 100%")//
			.label(2, "Control current overload 110%")//
			.label(4, "Control current overload 150%")//
			.label(8, "Control current overload 200%")//
			.label(16, "Control current overload 120%")//
			.label(32, "Control current overload 300%")//
			.label(64, "Control transient load 300%")//
			.label(128, "Grid over current")//
			.label(256, "Locking waveform too many times")//
			.label(512, "Inverter voltage zero drift error")//
			.label(1024, "Grid voltage zero drift error")//
			.label(2048, "Control current zero drift error")//
			.label(4096, "Inverter current zero drift error")//
			.label(8192, "Grid current zero drift error")//
			.label(16384, "PDP protection")//
			.label(32768, "Hardware control current protection")//
			.build();
	private final ModbusChannel _pcsFault2PhaseA = new ModbusChannelBuilder().nature(this) //
			.label(1, "Hardware AC volt. protection")//
			.label(2, "Hardware DC curr. protection")//
			.label(4, "Hardware temperature protection")//
			.label(8, "No capturing signal")//
			.label(16, "DC overvoltage")//
			.label(32, "DC disconnected")//
			.label(64, "Inverter undervoltage")//
			.label(128, "Inverter overvoltage")//
			.label(256, "Current sensor fail")//
			.label(512, "Voltage sensor fail")//
			.label(1024, "Power uncontrollable")//
			.label(2048, "Current uncontrollable")//
			.label(4096, "Fan error")//
			.label(8192, "Phase lack")//
			.label(16384, "Inverter relay fault")//
			.label(32768, "Grid relay fault")//
			.build();
	private final ModbusChannel _pcsFault3PhaseA = new ModbusChannelBuilder().nature(this) //
			.label(1, "Control panel overtemp")//
			.label(2, "Power panel overtemp")//
			.label(4, "DC input overcurrent")//
			.label(8, "Capacitor overtemp")//
			.label(16, "Radiator overtemp")//
			.label(32, "Transformer overtemp")//
			.label(64, "Combination comm error")//
			.label(128, "EEPROM error")//
			.label(256, "Load current zero drift error")//
			.label(512, "Current limit-R error")//
			.label(1024, "Phase sync error")//
			.label(2048, "External PV current zero drift error")//
			.label(4096, "External grid current zero drift error")//
			.build();
	private final ModbusChannel _pcsAlarm1PhaseB = new ModbusChannelBuilder().nature(this) //
			.label(1, "Grid undervoltage") //
			.label(2, "Grid overvoltage") //
			.label(4, "Grid under frequency") //
			.label(8, "Grid over frequency") //
			.label(16, "Grid power supply off") //
			.label(32, "Grid condition unmeet")//
			.label(64, "DC under voltage")//
			.label(128, "Input over resistance")//
			.label(256, "Combination error")//
			.label(512, "Comm with inverter error")//
			.label(1024, "Tme error")//
			.build();
	private final ModbusChannel _pcsAlarm2PhaseB = new ModbusChannelBuilder().nature(this) //
			.build();
	private final ModbusChannel _pcsFault1PhaseB = new ModbusChannelBuilder().nature(this) //
			.label(1, "Control current overload 100%")//
			.label(2, "Control current overload 110%")//
			.label(4, "Control current overload 150%")//
			.label(8, "Control current overload 200%")//
			.label(16, "Control current overload 120%")//
			.label(32, "Control current overload 300%")//
			.label(64, "Control transient load 300%")//
			.label(128, "Grid over current")//
			.label(256, "Locking waveform too many times")//
			.label(512, "Inverter voltage zero drift error")//
			.label(1024, "Grid voltage zero drift error")//
			.label(2048, "Control current zero drift error")//
			.label(4096, "Inverter current zero drift error")//
			.label(8192, "Grid current zero drift error")//
			.label(16384, "PDP protection")//
			.label(32768, "Hardware control current protection")//
			.build();
	private final ModbusChannel _pcsFault2PhaseB = new ModbusChannelBuilder().nature(this) //
			.label(1, "Hardware AC volt. protection")//
			.label(2, "Hardware DC curr. protection")//
			.label(4, "Hardware temperature protection")//
			.label(8, "No capturing signal")//
			.label(16, "DC overvoltage")//
			.label(32, "DC disconnected")//
			.label(64, "Inverter undervoltage")//
			.label(128, "Inverter overvoltage")//
			.label(256, "Current sensor fail")//
			.label(512, "Voltage sensor fail")//
			.label(1024, "Power uncontrollable")//
			.label(2048, "Current uncontrollable")//
			.label(4096, "Fan error")//
			.label(8192, "Phase lack")//
			.label(16384, "Inverter relay fault")//
			.label(32768, "Grid relay fault")//
			.build();
	private final ModbusChannel _pcsFault3PhaseB = new ModbusChannelBuilder().nature(this) //
			.label(1, "Control panel overtemp")//
			.label(2, "Power panel overtemp")//
			.label(4, "DC input overcurrent")//
			.label(8, "Capacitor overtemp")//
			.label(16, "Radiator overtemp")//
			.label(32, "Transformer overtemp")//
			.label(64, "Combination comm error")//
			.label(128, "EEPROM error")//
			.label(256, "Load current zero drift error")//
			.label(512, "Current limit-R error")//
			.label(1024, "Phase sync error")//
			.label(2048, "External PV current zero drift error")//
			.label(4096, "External grid current zero drift error")//
			.build();
	private final ModbusChannel _pcsAlarm1PhaseC = new ModbusChannelBuilder().nature(this) //
			.label(1, "Grid undervoltage") //
			.label(2, "Grid overvoltage") //
			.label(4, "Grid under frequency") //
			.label(8, "Grid over frequency") //
			.label(16, "Grid power supply off") //
			.label(32, "Grid condition unmeet")//
			.label(64, "DC under voltage")//
			.label(128, "Input over resistance")//
			.label(256, "Combination error")//
			.label(512, "Comm with inverter error")//
			.label(1024, "Tme error")//
			.build();
	private final ModbusChannel _pcsAlarm2PhaseC = new ModbusChannelBuilder().nature(this) //
			.build();
	private final ModbusChannel _pcsFault1PhaseC = new ModbusChannelBuilder().nature(this) //
			.label(1, "Control current overload 100%")//
			.label(2, "Control current overload 110%")//
			.label(4, "Control current overload 150%")//
			.label(8, "Control current overload 200%")//
			.label(16, "Control current overload 120%")//
			.label(32, "Control current overload 300%")//
			.label(64, "Control transient load 300%")//
			.label(128, "Grid over current")//
			.label(256, "Locking waveform too many times")//
			.label(512, "Inverter voltage zero drift error")//
			.label(1024, "Grid voltage zero drift error")//
			.label(2048, "Control current zero drift error")//
			.label(4096, "Inverter current zero drift error")//
			.label(8192, "Grid current zero drift error")//
			.label(16384, "PDP protection")//
			.label(32768, "Hardware control current protection")//
			.build();
	private final ModbusChannel _pcsFault2PhaseC = new ModbusChannelBuilder().nature(this) //
			.label(1, "Hardware AC volt. protection")//
			.label(2, "Hardware DC curr. protection")//
			.label(4, "Hardware temperature protection")//
			.label(8, "No capturing signal")//
			.label(16, "DC overvoltage")//
			.label(32, "DC disconnected")//
			.label(64, "Inverter undervoltage")//
			.label(128, "Inverter overvoltage")//
			.label(256, "Current sensor fail")//
			.label(512, "Voltage sensor fail")//
			.label(1024, "Power uncontrollable")//
			.label(2048, "Current uncontrollable")//
			.label(4096, "Fan error")//
			.label(8192, "Phase lack")//
			.label(16384, "Inverter relay fault")//
			.label(32768, "Grid relay fault")//
			.build();
	private final ModbusChannel _pcsFault3PhaseC = new ModbusChannelBuilder().nature(this) //
			.label(1, "Control panel overtemp")//
			.label(2, "Power panel overtemp")//
			.label(4, "DC input overcurrent")//
			.label(8, "Capacitor overtemp")//
			.label(16, "Radiator overtemp")//
			.label(32, "Transformer overtemp")//
			.label(64, "Combination comm error")//
			.label(128, "EEPROM error")//
			.label(256, "Load current zero drift error")//
			.label(512, "Current limit-R error")//
			.label(1024, "Phase sync error")//
			.label(2048, "External PV current zero drift error")//
			.label(4096, "External grid current zero drift error")//
			.build();
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
	@IsChannel(id = "SetActivePowerPhaseA")
	private final WriteableModbusChannel _setActivePowerPhaseA = new WriteableModbusChannelBuilder().nature(this)
			.unit("W").minWriteValue(_allowedCharge).maxWriteValue(_allowedDischarge).build();
	@IsChannel(id = "SetActivePowerPhaseB")
	private final WriteableModbusChannel _setActivePowerPhaseB = new WriteableModbusChannelBuilder().nature(this)
			.unit("W").minWriteValue(_allowedCharge).maxWriteValue(_allowedDischarge).build();
	@IsChannel(id = "SetActivePowerPhaseC")
	private final WriteableModbusChannel _setActivePowerPhaseC = new WriteableModbusChannelBuilder().nature(this)
			.unit("W").minWriteValue(_allowedCharge).maxWriteValue(_allowedDischarge).build();
	@IsChannel(id = "SetReactivePowerPhaseA")
	private final WriteableModbusChannel _setReactivePowerPhaseA = new WriteableModbusChannelBuilder().nature(this)
			.unit("var").minWriteValue(_allowedCharge).maxWriteValue(_allowedDischarge).build();
	@IsChannel(id = "SetReactivePowerPhaseB")
	private final WriteableModbusChannel _setReactivePowerPhaseB = new WriteableModbusChannelBuilder().nature(this)
			.unit("var").minWriteValue(_allowedCharge).maxWriteValue(_allowedDischarge).build();
	@IsChannel(id = "SetReactivePowerPhaseC")
	private final WriteableModbusChannel _setReactivePowerPhaseC = new WriteableModbusChannelBuilder().nature(this)
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
		return null;
	}

	@Override
	public WriteableNumericChannel setReactivePower() {
		return null;
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
						new ElementBuilder().address(142).channel(_allowedDischarge).build(), //
						new ElementBuilder().address(143).dummy(150 - 143).build(), //
						new ElementBuilder().address(150).channel(_pcsAlarm1PhaseA).build(), //
						new ElementBuilder().address(151).channel(_pcsAlarm2PhaseA).build(), //
						new ElementBuilder().address(152).channel(_pcsFault1PhaseA).build(), //
						new ElementBuilder().address(153).channel(_pcsFault2PhaseA).build(), //
						new ElementBuilder().address(154).channel(_pcsFault3PhaseA).build(), //
						new ElementBuilder().address(150).channel(_pcsAlarm1PhaseB).build(), //
						new ElementBuilder().address(151).channel(_pcsAlarm2PhaseB).build(), //
						new ElementBuilder().address(152).channel(_pcsFault1PhaseB).build(), //
						new ElementBuilder().address(153).channel(_pcsFault2PhaseB).build(), //
						new ElementBuilder().address(154).channel(_pcsFault3PhaseB).build(), //
						new ElementBuilder().address(150).channel(_pcsAlarm1PhaseC).build(), //
						new ElementBuilder().address(151).channel(_pcsAlarm2PhaseC).build(), //
						new ElementBuilder().address(152).channel(_pcsFault1PhaseC).build(), //
						new ElementBuilder().address(153).channel(_pcsFault2PhaseC).build(), //
						new ElementBuilder().address(154).channel(_pcsFault3PhaseC).build()), //
				new WritableModbusRange(200, //
						new ElementBuilder().address(200).channel(_setWorkState).build(),
						new ElementBuilder().address(201).dummy().build(),
						new ElementBuilder().address(202).dummy().build(),
						new ElementBuilder().address(203).dummy().build(),
						new ElementBuilder().address(204).dummy().build(),
						new ElementBuilder().address(205).dummy().build(),
						new ElementBuilder().address(206).channel(_setActivePowerPhaseA).build(),
						new ElementBuilder().address(207).channel(_setReactivePowerPhaseA).build(),
						new ElementBuilder().address(208).channel(_setActivePowerPhaseB).build(),
						new ElementBuilder().address(209).channel(_setReactivePowerPhaseB).build(),
						new ElementBuilder().address(210).channel(_setActivePowerPhaseC).build(),
						new ElementBuilder().address(211).channel(_setReactivePowerPhaseC).build()));
	}

}
