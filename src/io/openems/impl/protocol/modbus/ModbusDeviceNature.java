/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.impl.protocol.modbus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.OpenemsModbusException;
import io.openems.api.thing.ThingChannelsUpdatedListener;
import io.openems.impl.protocol.modbus.internal.CoilElement;
import io.openems.impl.protocol.modbus.internal.DoublewordElement;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.WordElement;
import io.openems.impl.protocol.modbus.internal.range.ModbusCoilRange;
import io.openems.impl.protocol.modbus.internal.range.ModbusRange;
import io.openems.impl.protocol.modbus.internal.range.WriteableModbusCoilRange;
import io.openems.impl.protocol.modbus.internal.range.WriteableModbusRange;

public abstract class ModbusDeviceNature implements DeviceNature, ChannelChangeListener {
	protected final Logger log;
	private ModbusProtocol protocol = null;
	private final String thingId;
	private List<ThingChannelsUpdatedListener> listeners;

	public ModbusDeviceNature(String thingId) throws ConfigException {
		this.thingId = thingId;
		log = LoggerFactory.getLogger(this.getClass());
		// this.protocol = defineModbusProtocol();
		this.listeners = new ArrayList<>();
	}

	private ModbusProtocol getProtocol() {
		if (protocol == null) {
			createModbusProtocol();
		}
		return this.protocol;
	}

	@Override
	public void addListener(ThingChannelsUpdatedListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void removeListener(ThingChannelsUpdatedListener listener) {
		this.listeners.remove(listener);
	}

	@Override
	public void init() {
		DeviceNature.super.init();
		createModbusProtocol();
	}

	@Override
	public String id() {
		return thingId;
	}

	@Override
	/**
	 * Sets a Channel as required. The Range with this Channel will be added to ModbusProtocol.RequiredRanges.
	 */
	public void setAsRequired(Channel channel) {
		getProtocol().setAsRequired(channel);
	}

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		createModbusProtocol();
	}

	private void createModbusProtocol() {
		try {
			this.protocol = defineModbusProtocol();
			for (ThingChannelsUpdatedListener listener : this.listeners) {
				listener.thingChannelsUpdated(this);
			}
		} catch (ConfigException e) {
			log.error("Failed to define modbus protocol!", e);
		}
	}

	protected abstract ModbusProtocol defineModbusProtocol() throws ConfigException;

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
		Optional<ModbusRange> range = getProtocol().getNextOtherRange();
		if (range.isPresent()) {
			update(unitId, bridge, range.get());
		}
	}

	protected void write(int modbusUnitId, ModbusBridge modbusBridge) throws ConfigException {
		for (WriteableModbusRange range : getProtocol().getWritableRanges()) {
			if (range instanceof WriteableModbusCoilRange) {
				/*
				 * Build a list of start-addresses of elements without holes. The start-addresses map to a list
				 * of elements and their values (sorted by order of insertion using LinkedHashMap)
				 */
				LinkedHashMap<Integer, LinkedHashMap<ModbusElement, Boolean>> elements = new LinkedHashMap<>();
				Integer nextStartAddress = null;
				LinkedHashMap<ModbusElement, Boolean> values = new LinkedHashMap<ModbusElement, Boolean>();
				;
				for (ModbusElement element : range.getElements()) {
					// Test if Channel is a dummy or writable and receive write value
					boolean value = false;
					if (element instanceof DummyElement) {
						value = false;
					} else if (element.getChannel() != null && element.getChannel() instanceof ModbusCoilWriteChannel) {
						Optional<Boolean> valueOptional = ((ModbusCoilWriteChannel) element.getChannel())
								.writeShadowCopy();
						if (valueOptional.isPresent()) {
							value = valueOptional.get();
						} else {
							continue;
						}
					} else {
						// no dummy, no WriteChannel, no value -> try next element
						continue;
					}
					// Test if there is a "hole", if yes -> create new values map
					if (nextStartAddress == null || nextStartAddress != element.getAddress()) {
						values = new LinkedHashMap<ModbusElement, Boolean>();
						elements.put(element.getAddress(), values);
					}
					// store this element and the value
					values.put(element, value);
					// store for next run
					nextStartAddress = element.getAddress() + element.getLength();
				}

				/*
				 * Write all elements
				 */
				for (Entry<Integer, LinkedHashMap<ModbusElement, Boolean>> entry : elements.entrySet()) {
					/*
					 * Get start address and all registers
					 */
					int address = entry.getKey();
					List<Boolean> coils = new ArrayList<>();
					for (Entry<ModbusElement, Boolean> value : entry.getValue().entrySet()) {
						ModbusElement element = value.getKey();
						if (element instanceof CoilElement) {
							coils.add(value.getValue());
						} else { // DummyElement -> write false;
							for (int i = 0; i < element.getLength(); i++) {
								coils.add(false);
							}
						}
					}
					/*
					 * Write
					 */
					try {
						modbusBridge.writeCoil(modbusUnitId, address, coils);
					} catch (OpenemsModbusException e) {
						log.error("Bridge [" + modbusBridge.id() + "], Range [" + range.getStartAddress() + "/0x"
								+ Integer.toHexString(range.getStartAddress()) + "]: " + e.getMessage());
						modbusBridge.triggerInitialize();
					}
				}
			} else {
				/*
				 * Build a list of start-addresses of elements without holes. The start-addresses map to a list
				 * of elements and their values (sorted by order of insertion using LinkedHashMap)
				 */
				LinkedHashMap<Integer, LinkedHashMap<ModbusElement, Long>> elements = new LinkedHashMap<>();
				Integer nextStartAddress = null;
				LinkedHashMap<ModbusElement, Long> values = new LinkedHashMap<ModbusElement, Long>();
				;
				for (ModbusElement element : range.getElements()) {
					// Test if Channel is a dummy or writable and receive write value
					long value = 0L;
					if (element instanceof DummyElement) {
						value = 0L;
					} else if (element.getChannel() != null && element.getChannel() instanceof ModbusWriteLongChannel) {
						Optional<Long> valueOptional = ((ModbusWriteLongChannel) element.getChannel())
								.writeShadowCopy();
						if (valueOptional.isPresent()) {
							value = valueOptional.get();
						} else {
							continue;
						}
					} else {
						// no dummy, no WriteChannel, no value -> try next element
						continue;
					}
					// Test if there is a "hole", if yes -> create new values map
					if (nextStartAddress == null || nextStartAddress != element.getAddress()) {
						values = new LinkedHashMap<ModbusElement, Long>();
						elements.put(element.getAddress(), values);
					}
					// store this element and the value
					values.put(element, value);
					// store for next run
					nextStartAddress = element.getAddress() + element.getLength();
				}

				/*
				 * Write all elements
				 */
				for (Entry<Integer, LinkedHashMap<ModbusElement, Long>> entry : elements.entrySet()) {
					/*
					 * Get start address and all registers
					 */
					int address = entry.getKey();
					List<Register> registers = new ArrayList<>();
					for (Entry<ModbusElement, Long> value : entry.getValue().entrySet()) {
						ModbusElement element = value.getKey();
						if (element instanceof WordElement) {
							registers.add( //
									((WordElement) element).toRegister(value.getValue()));
						} else if (element instanceof DoublewordElement) {
							registers.addAll(Arrays.asList( //
									((DoublewordElement) element).toRegisters(value.getValue())));
						} else { // DummyElement -> write 0;
							for (int i = 0; i < element.getLength(); i++) {
								registers.add(new SimpleRegister(value.getValue().intValue()));
							}
						}
					}
					/*
					 * Write
					 */
					try {
						modbusBridge.write(modbusUnitId, address, registers);
					} catch (OpenemsModbusException e) {
						log.error("Bridge [" + modbusBridge.id() + "], Range [" + range.getStartAddress() + "/0x"
								+ Integer.toHexString(range.getStartAddress()) + "]: " + e.getMessage());
						modbusBridge.triggerInitialize();
					}
				}
			}
		}
	}

	private void update(int modbusUnitId, ModbusBridge modbusBridge, ModbusRange range) {
		if (range instanceof ModbusCoilRange) {
			try {
				// Query using this Range
				boolean[] coils = modbusBridge.queryCoil(modbusUnitId, range);

				// Fill channels
				int position = 0;
				for (ModbusElement<Boolean> element : range.getElements()) {
					if (element instanceof CoilElement) {
						// Use _one_ Register for the element
						((CoilElement) element).setValue(coils[position]);
					} else {
						log.warn("Element type not defined: Element [" + element.getAddress() + "], Bridge ["
								+ modbusBridge + "], Coil [" + range.getStartAddress() + "]: ");
					}
					position += element.getLength();
				}
			} catch (OpenemsModbusException e) {
				log.error(
						"Modbus query failed. " //
								+ "Bridge [" + modbusBridge.id() + "], Coil [" + range.getStartAddress() + "]: {}",
						e.getMessage());
				modbusBridge.triggerInitialize();
				// set all elements to invalid
				for (ModbusElement<Boolean> element : range.getElements()) {
					element.setValue(null);
				}
			}
		} else {
			try {
				// Query using this Range
				InputRegister[] registers = modbusBridge.query(modbusUnitId, range);

				// Fill channels
				int position = 0;
				for (ModbusElement element : range.getElements()) {
					if (element instanceof DummyElement) {
						// ignore dummy
					} else if (element instanceof WordElement) {
						// Use _one_ Register for the element
						((WordElement) element).setValue(registers[position]);
					} else if (element instanceof DoublewordElement) {
						// Use _two_ registers for the element
						((DoublewordElement) element).setValue(registers[position], registers[position + 1]);
					} else {
						log.warn("Element type not defined: Element [" + element.getAddress() + "], Bridge ["
								+ modbusBridge + "], Range [" + range.getStartAddress() + "]: ");
					}
					position += element.getLength();
				}
			} catch (OpenemsModbusException e) {
				log.error(
						"Modbus query failed. " //
								+ "Bridge [" + modbusBridge.id() + "], Range [" + range.getStartAddress() + "]: {}",
						e.getMessage());
				modbusBridge.triggerInitialize();
				// set all elements to invalid
				for (ModbusElement element : range.getElements()) {
					element.setValue(null);
				}
			}
		}
	}
}
