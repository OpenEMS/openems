package io.openems.impl.device.pro;

import io.openems.api.channel.Channel;
import io.openems.api.channel.WriteableChannel;
import io.openems.api.device.nature.EssNature;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.internal.ElementBuilder;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.ModbusRange;
import io.openems.impl.protocol.modbus.internal.channel.ModbusChannelBuilder;

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

	public FeneconProEss(String thingId) {
		super(thingId);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Channel activePower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel allowedCharge() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel allowedDischarge() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel apparentPower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel gridMode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel minSoc() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel reactivePower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WriteableChannel setActivePower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setMinSoc(Integer minSoc) {
		// TODO Auto-generated method stub

	}

	@Override
	public WriteableChannel setWorkState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel soc() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel systemState() {
		// TODO Auto-generated method stub
		return null;
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
						new ElementBuilder().address(113).channel(_batteryGroupAlarm).build() //
				)//
		);
	}

}
