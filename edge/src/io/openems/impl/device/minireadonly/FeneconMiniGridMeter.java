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
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.UnsignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedWordElement;
import io.openems.impl.protocol.modbus.internal.range.ModbusRegisterRange;

@ThingInfo(title = "FENECON Mini Grid-Meter")
public class FeneconMiniGridMeter extends ModbusDeviceNature implements AsymmetricMeterNature {

	/*
	 * Constructors
	 */
	public FeneconMiniGridMeter(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
	}

	/*
	 * Config
	 */
	private final ConfigChannel<String> type = new ConfigChannel<String>("type", this).defaultValue("grid");

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
	public ModbusReadLongChannel buyFromGridEnergy;
	public ModbusReadLongChannel sellToGridEnergy;

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
				new ModbusRegisterRange(2018, //
						new UnsignedWordElement(2018, //
								this.activePowerL1 = new ModbusReadLongChannel("ActivePowerL1", this).unit("W")
										.delta(10000l))),
				new ModbusRegisterRange(2118, //
						new UnsignedWordElement(2118, //
								this.activePowerL2 = new ModbusReadLongChannel("ActivePowerL2", this).unit("W")
										.delta(10000l))),
				new ModbusRegisterRange(2218, //
						new UnsignedWordElement(2218, //
								this.activePowerL3 = new ModbusReadLongChannel("ActivePowerL3", this).unit("W")
										.delta(10000l))),
				new ModbusRegisterRange(5003, //
						new UnsignedDoublewordElement(5003, //
								this.sellToGridEnergy = new ModbusReadLongChannel("SellToGridEnergy", this).unit("Wh")
										.multiplier(2)),
						new UnsignedDoublewordElement(5005, //
								this.buyFromGridEnergy = new ModbusReadLongChannel("BuyFromGridEnergy", this).unit("Wh")
										.multiplier(2))));

		return protocol;
	}

}
