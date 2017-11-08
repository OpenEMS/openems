package io.openems.impl.device.bcontrol;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.FunctionalReadChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.meter.AsymmetricMeterNature;
import io.openems.api.device.nature.meter.SymmetricMeterNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadLongChannel;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.UnsignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.range.ModbusInputRegisterRange;

@ThingInfo(title = "B-Control Energy Meter")
public class BControlMeter extends ModbusDeviceNature implements SymmetricMeterNature, AsymmetricMeterNature {

	public BControlMeter(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
	}

	/*
	 * Config
	 */
	private final ConfigChannel<String> type = new ConfigChannel<String>("type", this);

	@Override
	public ConfigChannel<String> type() {
		return type;
	}

	private final ConfigChannel<Long> maxActivePower = new ConfigChannel<Long>("maxActivePower", this);

	@Override
	public ConfigChannel<Long> maxActivePower() {
		return maxActivePower;
	}

	private final ConfigChannel<Long> minActivePower = new ConfigChannel<Long>("minActivePower", this);

	@Override
	public ConfigChannel<Long> minActivePower() {
		return minActivePower;
	}

	private FunctionalReadChannel<Long> activePower;
	private FunctionalReadChannel<Long> apparentPower;
	private FunctionalReadChannel<Long> reactivePower;
	private FunctionalReadChannel<Long> activePowerL1;
	private FunctionalReadChannel<Long> activePowerL2;
	private FunctionalReadChannel<Long> activePowerL3;
	private FunctionalReadChannel<Long> reactivePowerL1;
	private FunctionalReadChannel<Long> reactivePowerL2;
	private FunctionalReadChannel<Long> reactivePowerL3;
	private ModbusReadLongChannel activePowerPos;
	private ModbusReadLongChannel apparentPowerPos;
	private ModbusReadLongChannel reactivePowerPos;
	private ModbusReadLongChannel activePowerL1Pos;
	private ModbusReadLongChannel activePowerL2Pos;
	private ModbusReadLongChannel activePowerL3Pos;
	private ModbusReadLongChannel reactivePowerL1Pos;
	private ModbusReadLongChannel reactivePowerL2Pos;
	private ModbusReadLongChannel reactivePowerL3Pos;
	private ModbusReadLongChannel activePowerNeg;
	private ModbusReadLongChannel apparentPowerNeg;
	private ModbusReadLongChannel reactivePowerNeg;
	private ModbusReadLongChannel activePowerL1Neg;
	private ModbusReadLongChannel activePowerL2Neg;
	private ModbusReadLongChannel activePowerL3Neg;
	private ModbusReadLongChannel reactivePowerL1Neg;
	private ModbusReadLongChannel reactivePowerL2Neg;
	private ModbusReadLongChannel reactivePowerL3Neg;
	private ModbusReadLongChannel voltageL1;
	private ModbusReadLongChannel voltageL2;
	private ModbusReadLongChannel voltageL3;
	private ModbusReadLongChannel currentL1;
	private ModbusReadLongChannel currentL2;
	private ModbusReadLongChannel currentL3;
	private ModbusReadLongChannel frequency;

	@Override
	public ReadChannel<Long> activePower() {
		return activePower;
	}

	@Override
	public ReadChannel<Long> apparentPower() {
		return apparentPower;
	}

	@Override
	public ReadChannel<Long> reactivePower() {
		return reactivePower;
	}

	@Override
	public ReadChannel<Long> activePowerL1() {
		return activePowerL1;
	}

	@Override
	public ReadChannel<Long> activePowerL2() {
		return activePowerL2;
	}

	@Override
	public ReadChannel<Long> activePowerL3() {
		return activePowerL3;
	}

	@Override
	public ReadChannel<Long> reactivePowerL1() {
		return reactivePowerL1;
	}

	@Override
	public ReadChannel<Long> reactivePowerL2() {
		return reactivePowerL2;
	}

	@Override
	public ReadChannel<Long> reactivePowerL3() {
		return reactivePowerL3;
	}

	@Override
	public ReadChannel<Long> currentL1() {
		return currentL1;
	}

	@Override
	public ReadChannel<Long> currentL2() {
		return currentL2;
	}

	@Override
	public ReadChannel<Long> currentL3() {
		return currentL3;
	}

	@Override
	public ReadChannel<Long> voltageL1() {
		return voltageL1;
	}

	@Override
	public ReadChannel<Long> voltageL2() {
		return voltageL2;
	}

	@Override
	public ReadChannel<Long> voltageL3() {
		return voltageL3;
	}

	@Override
	public ReadChannel<Long> frequency() {
		return frequency;
	}

	@Override
	public ReadChannel<Long> voltage() {
		return voltageL1;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		ModbusProtocol mp = new ModbusProtocol( //
				new ModbusInputRegisterRange(0, //
						new UnsignedDoublewordElement(0,
								activePowerPos = new ModbusReadLongChannel("ActivePowerPos", this).unit("W")
								.multiplier(-1)),
						new UnsignedDoublewordElement(2,
								activePowerNeg = new ModbusReadLongChannel("ActivePowerNeg", this).unit("W")
								.multiplier(-1)),
						new UnsignedDoublewordElement(4,
								reactivePowerPos = new ModbusReadLongChannel("ReactivePowerPos", this).unit("Var")
								.multiplier(-1)),
						new UnsignedDoublewordElement(6,
								reactivePowerNeg = new ModbusReadLongChannel("ReactivePowerNeg", this).unit("Var")
								.multiplier(-1)),
						new DummyElement(8, 15),
						new UnsignedDoublewordElement(16,
								apparentPowerPos = new ModbusReadLongChannel("ApparentPowerPos", this).unit("VA")
								.multiplier(-1)),
						new UnsignedDoublewordElement(18,
								apparentPowerNeg = new ModbusReadLongChannel("ApparentPowerNeg", this)
								.unit("VA").multiplier(-1)),
						new DummyElement(20, 25), new UnsignedDoublewordElement(26, //
								frequency = new ModbusReadLongChannel("Frequency", this).unit("mHZ"))),
				new ModbusInputRegisterRange(40, new UnsignedDoublewordElement(40, //
						activePowerL1Pos = new ModbusReadLongChannel("ActivePowerL1Pos", this).unit("W")
						.multiplier(-1)),
						new UnsignedDoublewordElement(42, //
								activePowerL1Neg = new ModbusReadLongChannel("ActivePowerL1Neg", this).unit("W")
								.multiplier(-1)),
						new UnsignedDoublewordElement(44, //
								reactivePowerL1Pos = new ModbusReadLongChannel("ReactivePowerL1Pos", this).unit("Var")
								.multiplier(-1)),
						new UnsignedDoublewordElement(46, //
								reactivePowerL1Neg = new ModbusReadLongChannel("ReactivePowerL1Neg", this)
								.unit("Var").multiplier(-1)),
						new DummyElement(48, 59), new UnsignedDoublewordElement(60, //
								currentL1 = new ModbusReadLongChannel("CurrentL1", this).unit("mA")),
						new UnsignedDoublewordElement(62, //
								voltageL1 = new ModbusReadLongChannel("VoltageL1", this).unit("mV"))),
				new ModbusInputRegisterRange(80, new UnsignedDoublewordElement(80, //
						activePowerL2Pos = new ModbusReadLongChannel("ActivePowerL2Pos", this).unit("W")
						.multiplier(-1)),
						new UnsignedDoublewordElement(82, //
								activePowerL2Neg = new ModbusReadLongChannel("ActivePowerL2Neg", this).unit("W")
								.multiplier(-1)),
						new UnsignedDoublewordElement(84, //
								reactivePowerL2Pos = new ModbusReadLongChannel("ReactivePowerL2Pos", this).unit("Var")
								.multiplier(-1)),
						new UnsignedDoublewordElement(86, //
								reactivePowerL2Neg = new ModbusReadLongChannel("ReactivePowerL2Neg", this)
								.unit("Var").multiplier(-1)),
						new DummyElement(88, 99), new UnsignedDoublewordElement(100, //
								currentL2 = new ModbusReadLongChannel("CurrentL2", this).unit("mA")),
						new UnsignedDoublewordElement(102, //
								voltageL2 = new ModbusReadLongChannel("VoltageL2", this).unit("mV"))),
				new ModbusInputRegisterRange(120, new UnsignedDoublewordElement(120, //
						activePowerL3Pos = new ModbusReadLongChannel("ActivePowerL3Pos", this).unit("W")
						.multiplier(-1)),
						new UnsignedDoublewordElement(122, //
								activePowerL3Neg = new ModbusReadLongChannel("ActivePowerL3Neg", this).unit("W")
								.multiplier(-1)),
						new UnsignedDoublewordElement(124, //
								reactivePowerL3Pos = new ModbusReadLongChannel("ReactivePowerL3Pos", this).unit("Var")
								.multiplier(-1)),
						new UnsignedDoublewordElement(126, //
								reactivePowerL3Neg = new ModbusReadLongChannel("ReactivePowerL3Neg", this)
								.unit("Var").multiplier(-1)),
						new DummyElement(128, 139), new UnsignedDoublewordElement(140, //
								currentL3 = new ModbusReadLongChannel("CurrentL3", this).unit("mA")),
						new UnsignedDoublewordElement(142, //
								voltageL3 = new ModbusReadLongChannel("VoltageL3", this).unit("mV"))));
		activePower = new FunctionalReadChannel<Long>("ActivePower", this, (channels) -> {
			return channels[0].valueOptional().orElse(0L) + (channels[1].valueOptional().orElse(0L) * -1);
		}, activePowerPos, activePowerNeg).unit("W");
		activePowerL1 = new FunctionalReadChannel<Long>("ActivePowerL1", this, (channels) -> {
			return channels[0].valueOptional().orElse(0L) + (channels[1].valueOptional().orElse(0L) * -1);
		}, activePowerL1Pos, activePowerL1Neg).unit("W");
		activePowerL2 = new FunctionalReadChannel<Long>("ActivePowerL2", this, (channels) -> {
			return channels[0].valueOptional().orElse(0L) + (channels[1].valueOptional().orElse(0L) * -1);
		}, activePowerL2Pos, activePowerL2Neg).unit("W");
		activePowerL3 = new FunctionalReadChannel<Long>("ActivePowerL3", this, (channels) -> {
			return channels[0].valueOptional().orElse(0L) + (channels[1].valueOptional().orElse(0L) * -1);
		}, activePowerL3Pos, activePowerL3Neg).unit("W");
		reactivePower = new FunctionalReadChannel<Long>("ReactivePower", this, (channels) -> {
			return channels[0].valueOptional().orElse(0L) + (channels[1].valueOptional().orElse(0L) * -1);
		}, reactivePowerPos, reactivePowerNeg).unit("Var");
		reactivePowerL1 = new FunctionalReadChannel<Long>("ReactivePowerL1", this, (channels) -> {
			return channels[0].valueOptional().orElse(0L) + (channels[1].valueOptional().orElse(0L) * -1);
		}, reactivePowerL1Pos, reactivePowerL1Neg).unit("Var");
		reactivePowerL2 = new FunctionalReadChannel<Long>("ReactivePowerL2", this, (channels) -> {
			return channels[0].valueOptional().orElse(0L) + (channels[1].valueOptional().orElse(0L) * -1);
		}, reactivePowerL2Pos, reactivePowerL2Neg).unit("Var");
		reactivePowerL3 = new FunctionalReadChannel<Long>("ReactivePowerL3", this, (channels) -> {
			return channels[0].valueOptional().orElse(0L) + (channels[1].valueOptional().orElse(0L) * -1);
		}, reactivePowerL3Pos, reactivePowerL3Neg).unit("Var");
		apparentPower = new FunctionalReadChannel<Long>("ApparentPower", this, (channels) -> {
			return channels[0].valueOptional().orElse(0L) + (channels[1].valueOptional().orElse(0L) * -1);
		}, apparentPowerPos, apparentPowerNeg).unit("VA");
		return mp;
	}
}
