package io.openems.impl.device.minireadonly;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StaticValueChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.meter.SymmetricMeterNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadLongChannel;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.UnsignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedWordElement;
import io.openems.impl.protocol.modbus.internal.range.ModbusRegisterRange;

@ThingInfo(title = "FENECON Mini Production-Meter")
public class FeneconMiniProductionMeter extends ModbusDeviceNature implements SymmetricMeterNature {

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
	private ModbusReadLongChannel activePower;
	// Dummies
	private StaticValueChannel<Long> reactivePower = new StaticValueChannel<Long>("ReactivePower", this, 0l);

	/*
	 * This Channels
	 */
	public ModbusReadLongChannel energy;

	@Override
	public ReadChannel<Long> activePower() {
		return this.activePower;
	}

	@Override
	public ReadChannel<Long> reactivePower() {
		return this.reactivePower;
	}

	@Override
	public ReadChannel<Long> apparentPower() {
		return this.activePower;
	}

	@Override
	public ReadChannel<Long> frequency() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReadChannel<Long> voltage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		ModbusProtocol protocol = new ModbusProtocol( //
				new ModbusRegisterRange(4006, ///
						new UnsignedWordElement(4006, //
								this.activePower = new ModbusReadLongChannel("ActivePower", this).unit("W"))),
				new ModbusRegisterRange(4036, ///
						new UnsignedDoublewordElement(4036, //
								this.energy = new ModbusReadLongChannel("Energy", this).unit("Wh"))));
		return protocol;
	}

}
