package io.openems.impl.device.minireadonly;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StaticValueChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.meter.AsymmetricMeterNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadLongChannel;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.UnsignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedWordElement;
import io.openems.impl.protocol.modbus.internal.range.ModbusRegisterRange;

@ThingInfo(title = "FENECON Mini Production-Meter")
public class FeneconMiniProductionMeter extends ModbusDeviceNature implements AsymmetricMeterNature {

	/*
	 * Constructors
	 */
	public FeneconMiniProductionMeter(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
	}

	/*
	 * Config
	 */
	private final ConfigChannel<String> type = new ConfigChannel<String>("type", this).defaultValue("production");

	@Override
	public ConfigChannel<String> type() {
		return this.type;
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

	/*
	 * Inherited Channels
	 */
	private ModbusReadLongChannel activePowerL1;
	private ModbusReadLongChannel activePowerL2;
	private ModbusReadLongChannel activePowerL3;
	// Dummies
	private StaticValueChannel<Long> reactivePowerL1 = new StaticValueChannel<Long>("ReactivePowerL1", this, 0l);
	private StaticValueChannel<Long> reactivePowerL2 = new StaticValueChannel<Long>("ReactivePowerL2", this, 0l);
	private StaticValueChannel<Long> reactivePowerL3 = new StaticValueChannel<Long>("ReactivePowerL3", this, 0l);

	/*
	 * This Channels
	 */
	public ModbusReadLongChannel energyL1;
	public ModbusReadLongChannel energyL2;
	public ModbusReadLongChannel energyL3;

	@Override
	public ReadChannel<Long> activePowerL1() {
		return this.activePowerL1;
	}

	@Override
	public ReadChannel<Long> activePowerL2() {
		return this.activePowerL2;
	}

	@Override
	public ReadChannel<Long> activePowerL3() {
		return this.activePowerL3;
	}

	@Override
	public ReadChannel<Long> reactivePowerL1() {
		return this.reactivePowerL1;
	}

	@Override
	public ReadChannel<Long> reactivePowerL2() {
		return this.reactivePowerL2;
	}

	@Override
	public ReadChannel<Long> reactivePowerL3() {
		return this.reactivePowerL3;
	}

	@Override
	public ReadChannel<Long> currentL1() {
		return null;
	}

	@Override
	public ReadChannel<Long> currentL2() {
		return null;
	}

	@Override
	public ReadChannel<Long> currentL3() {
		return null;
	}

	@Override
	public ReadChannel<Long> voltageL1() {
		return null;
	}

	@Override
	public ReadChannel<Long> voltageL2() {
		return null;
	}

	@Override
	public ReadChannel<Long> voltageL3() {
		return null;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		ModbusProtocol protocol = new ModbusProtocol( //
				new ModbusRegisterRange(2035, //
						new UnsignedDoublewordElement(2035, //
								this.energyL1 = new ModbusReadLongChannel("EnergyL1", this).unit("Wh").multiplier(2)),
						new DummyElement(2037, 2065), //
						new UnsignedWordElement(2066, //
								this.activePowerL1 = new ModbusReadLongChannel("ActivePowerL1", this).unit("W")
										.delta(10000l))),
				new ModbusRegisterRange(2135, //
						new UnsignedDoublewordElement(2135, //
								this.energyL2 = new ModbusReadLongChannel("EnergyL2", this).unit("Wh").multiplier(2)),
						new DummyElement(2137, 2165), //
						new UnsignedWordElement(2166, //
								this.activePowerL2 = new ModbusReadLongChannel("ActivePowerL2", this).unit("W")
										.delta(10000l))),
				new ModbusRegisterRange(2235, //
						new UnsignedDoublewordElement(2235, //
								this.energyL3 = new ModbusReadLongChannel("EnergyL3", this).unit("Wh").multiplier(2)),
						new DummyElement(2237, 2265), //
						new UnsignedWordElement(2266, //
								this.activePowerL3 = new ModbusReadLongChannel("ActivePowerL3", this).unit("W")
										.delta(10000l))));
		return protocol;
	}

}
