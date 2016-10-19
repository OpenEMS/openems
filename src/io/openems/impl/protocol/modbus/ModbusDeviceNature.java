package io.openems.impl.protocol.modbus;

import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.api.channel.Channel;
import io.openems.api.channel.WriteableChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.OpenemsModbusException;
import io.openems.impl.protocol.modbus.internal.DoublewordElement;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.ModbusRange;
import io.openems.impl.protocol.modbus.internal.WordElement;
import io.openems.impl.protocol.modbus.internal.WritableModbusRange;

public abstract class ModbusDeviceNature implements DeviceNature {
	protected final Logger log;
	private ModbusProtocol protocol = null;
	private final String thingId;

	public ModbusDeviceNature(String thingId) {
		this.thingId = thingId;
		log = LoggerFactory.getLogger(this.getClass());
	}

	@Override
	public String getThingId() {
		return thingId;
	}

	@Override
	/**
	 * Sets a Channel as required. The Range with this Channel will be added to ModbusProtocol.RequiredRanges.
	 */
	public void setAsRequired(Channel channel) throws ConfigException {
		getProtocol().setAsRequired(channel);
	}

	protected abstract ModbusProtocol defineModbusProtocol() throws ConfigException;

	protected ModbusProtocol getProtocol() throws ConfigException {
		if (protocol == null) {
			this.protocol = defineModbusProtocol();
		}
		return protocol;
	}

	protected void update(int unitId, ModbusBridge bridge) throws ConfigException {
		/**
		 * Update required ranges
		 */
		for (ModbusRange range : getProtocol().getRequiredRanges()) {
			update(unitId, bridge, range);
		}
		/**
		 * Update other ranges
		 */
		// TODO: update only range at a time
		for (ModbusRange range : getProtocol().getOtherRanges()) {
			update(unitId, bridge, range);
		}
	}

	protected void write(int modbusUnitId, ModbusBridge modbusBridge) throws ConfigException {
		for (WritableModbusRange range : getProtocol().getWritableRanges()) {
			// TODO: combine writes to a Multi
			for (ModbusElement element : range.getElements()) {
				// Check if Channel is writable (should be always the case
				if (element.getChannel() instanceof WriteableChannel) {
					WriteableChannel writeableChannel = (WriteableChannel) element.getChannel();
					// take the value from the Channel and initialize it
					BigInteger writeValue = writeableChannel.popRawWriteValue();
					if (writeValue != null) {
						if (element instanceof WordElement) {
							try {
								WordElement wordElement = (WordElement) element;
								Register register = wordElement.toRegister(writeValue);
								modbusBridge.write(modbusUnitId, element.getAddress(), register);
								log.debug("Wrote successfully: Thing [" + this.getThingId() + "], Address ["
										+ element.getAddress() + "], Value [" + register.getValue() + "]");

							} catch (OpenemsModbusException e) {
								log.error("Modbus write failed. " //
										+ "Bridge [" + modbusBridge.getThingId() + "], Range ["
										+ range.getStartAddress() + "]: {}", e.getMessage());
								modbusBridge.triggerInitialize();
							}

						} else {
							log.error("No WordElement: NOT IMPLEMENTED!");
						}
					}

				} else {
					throw new ConfigException("Handling WritableModbusRange [" + range.getStartAddress()
							+ "] but Channel for Element [" + element.getAddress() + "] is not writable!");
				}
			}
		}
	}

	@SuppressWarnings("null")
	private void update(int modbusUnitId, ModbusBridge modbusBridge, ModbusRange range) {
		try {
			// Query using this Range
			Register[] registers = modbusBridge.query(modbusUnitId, range);

			// Fill channels
			int position = 0;
			for (ModbusElement element : range.getElements()) {
				int length = element.getLength();
				if (element instanceof WordElement) {
					// Use _one_ Register for the element
					((WordElement) element).setValue(registers[position]);
				} else if (element instanceof DoublewordElement) {
					// Use _two_ registers for the element
					((DoublewordElement) element).setValue(registers[position], registers[position + 1]);
				} else if (element instanceof DummyElement) {
					// ignore dummy
				} else {
					log.warn("Element type not defined: Element [" + element.getAddress() + "], Bridge [" + modbusBridge
							+ "], Range [" + range.getStartAddress() + "]: ");
				}
				position += length;
			}
		} catch (OpenemsModbusException e) {
			log.error(
					"Modbus query failed. " //
							+ "Bridge [" + modbusBridge + "], Range [" + range.getStartAddress() + "]: {}",
					e.getMessage());
			modbusBridge.triggerInitialize();
			// set all elements to invalid
			for (ModbusElement element : range.getElements()) {
				element.setValue(null);
			}
		}
	}
}
