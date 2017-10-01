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
import io.openems.impl.protocol.modbus.internal.SignedWordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.range.ModbusRegisterRange;

@ThingInfo(title = "FENECON Mini Grid-Meter")
public class FeneconMiniGridMeter extends ModbusDeviceNature implements SymmetricMeterNature {

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
	private ModbusReadLongChannel activePower;
	// Dummies
	private StaticValueChannel<Long> reactivePower = new StaticValueChannel<Long>("ReactivePower", this, 0l);

	/*
	 * This Channels
	 */
	public ModbusReadLongChannel buyFromGridEnergy;
	public ModbusReadLongChannel sellToGridEnergy;

	@Override
	public ReadChannel<Long> activePower() {
		return this.activePower;
	}

	@Override
	public ReadChannel<Long> apparentPower() {
		return this.activePower;
	}

	@Override
	public ReadChannel<Long> reactivePower() {
		return this.reactivePower;
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
				new ModbusRegisterRange(4004, //
						new SignedWordElement(4004, //
								this.activePower = new ModbusReadLongChannel("ActivePower", this).unit("W").negate())),
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
