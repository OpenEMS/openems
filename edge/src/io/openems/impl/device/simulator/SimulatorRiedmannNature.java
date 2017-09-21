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
package io.openems.impl.device.simulator;

import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.device.Device;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.impl.device.custom.riedmann.RiedmannNature;
import io.openems.impl.protocol.simulator.SimulatorDeviceNature;
import io.openems.impl.protocol.simulator.SimulatorReadChannel;
import io.openems.impl.protocol.simulator.SimulatorWriteChannel;
import io.openems.test.utils.channel.UnitTestWriteChannel;

@ThingInfo(title = "Simulator ESS")
public class SimulatorRiedmannNature extends SimulatorDeviceNature implements RiedmannNature, ChannelChangeListener {

	/*
	 * Constructors
	 */
	public SimulatorRiedmannNature(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
	}

	/*
	 * Fields
	 */
	@ChannelInfo(title = "WaterLevel", type = Long.class)
	private ConfigChannel<Long> waterLevel = new ConfigChannel<Long>("WaterLevel", this).defaultValue(30L);
	private SimulatorWriteChannel<Long> setPivotOn = new SimulatorWriteChannel<Long>("SetPivotOn", this, 1L);
	private SimulatorWriteChannel<Long> setBorehole1On = new SimulatorWriteChannel<>("SetBorehole1On", this, 1L);
	private SimulatorWriteChannel<Long> setBorehole2On = new SimulatorWriteChannel<>("SetBorehole2On", this, 1L);
	private SimulatorWriteChannel<Long> setBorehole3On = new SimulatorWriteChannel<>("SetBorehole3On", this, 1L);
	private SimulatorWriteChannel<Long> setClima1On = new SimulatorWriteChannel<>("SetClima1On", this, 1L);
	private SimulatorWriteChannel<Long> setClima2On = new SimulatorWriteChannel<>("SetClima2On", this, 1L);
	private SimulatorWriteChannel<Long> setOfficeOn = new SimulatorWriteChannel<>("SetOfficeOn", this, 1L);
	private SimulatorWriteChannel<Long> setTraineeCenterOn = new SimulatorWriteChannel<>("SetTraineeCenterOn", this,
			1L);
	@ChannelInfo(title = "AutomaticMode", type = Long.class)
	private ConfigChannel<Long> automaticMode = new ConfigChannel<Long>("AutomaticMode", this).defaultValue(1L);
	@ChannelInfo(title = "ManualMode", type = Long.class)
	private ConfigChannel<Long> manualMode = new ConfigChannel<Long>("ManualMode", this).defaultValue(0L);
	@ChannelInfo(title = "EmergencyStop", type = Long.class)
	private ConfigChannel<Long> emergencyStop = new ConfigChannel<Long>("EmergencyStop", this).defaultValue(0L);
	@ChannelInfo(title = "SwitchStatePivotPump", type = Long.class)
	private ConfigChannel<Long> switchStatePivotPump = new ConfigChannel<Long>("SwitchStatePivotPump", this)
			.defaultValue(1L);
	@ChannelInfo(title = "SwitchStatePivotDrive", type = Long.class)
	private ConfigChannel<Long> switchStatePivotDrive = new ConfigChannel<Long>("SwitchStatePivotDrive", this)
			.defaultValue(1L);
	@ChannelInfo(title = "Error", type = Long.class)
	private ConfigChannel<Long> error = new ConfigChannel<Long>("Error", this).defaultValue(0L);
	private SimulatorReadChannel<Long> waterLevelBorehole1On = new SimulatorReadChannel<Long>("WaterLevelBorehole1On",
			this);
	private SimulatorReadChannel<Long> waterLevelBorehole1Off = new SimulatorReadChannel<Long>("WaterLevelBorehole1Off",
			this);
	private SimulatorReadChannel<Long> waterLevelBorehole2On = new SimulatorReadChannel<Long>("WaterLevelBorehole2On",
			this);
	private SimulatorReadChannel<Long> waterLevelBorehole2Off = new SimulatorReadChannel<Long>("WaterLevelBorehole2Off",
			this);
	private SimulatorReadChannel<Long> waterLevelBorehole3On = new SimulatorReadChannel<Long>("WaterLevelBorehole3On",
			this);
	private SimulatorReadChannel<Long> waterLevelBorehole3Off = new SimulatorReadChannel<Long>("WaterLevelBorehole3Off",
			this);
	private UnitTestWriteChannel<Long> setWaterLevelBorehole1On = new UnitTestWriteChannel<Long>(
			"SetWaterLevelBorehole1On", this);
	private UnitTestWriteChannel<Long> setWaterLevelBorehole1Off = new UnitTestWriteChannel<Long>(
			"SetWaterLevelBorehole1Off", this);
	private UnitTestWriteChannel<Long> setWaterLevelBorehole2On = new UnitTestWriteChannel<Long>(
			"SetWaterLevelBorehole2On", this);
	private UnitTestWriteChannel<Long> setWaterLevelBorehole2Off = new UnitTestWriteChannel<Long>(
			"SetWaterLevelBorehole2Off", this);
	private UnitTestWriteChannel<Long> setWaterLevelBorehole3On = new UnitTestWriteChannel<Long>(
			"SetWaterLevelBorehole3On", this);
	private UnitTestWriteChannel<Long> setWaterLevelBorehole3Off = new UnitTestWriteChannel<Long>(
			"SetWaterLevelBorehole3Off", this);
	private UnitTestWriteChannel<Long> signalBus1On = new UnitTestWriteChannel<Long>("SignalBus1On", this);
	private UnitTestWriteChannel<Long> signalBus2On = new UnitTestWriteChannel<Long>("SignalBus2On", this);
	private UnitTestWriteChannel<Long> signalOnGrid = new UnitTestWriteChannel<Long>("SignalOnGrid", this);
	private UnitTestWriteChannel<Long> signalSystemStop = new UnitTestWriteChannel<Long>("SignalSystemStop", this);
	private UnitTestWriteChannel<Long> signalWatchdog = new UnitTestWriteChannel<Long>("SignalWatchdog", this);

	/*
	 * Inherited Channels
	 */
	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {}

	@Override
	public ReadChannel<Long> getWaterlevel() {
		return waterLevel;
	}

	@Override
	public ReadChannel<Long> getGetPivotOn() {
		return setPivotOn;
	}

	@Override
	public ReadChannel<Long> getBorehole1On() {
		return setBorehole1On;
	}

	@Override
	public ReadChannel<Long> getBorehole2On() {
		return setBorehole2On;
	}

	@Override
	public ReadChannel<Long> getBorehole3On() {
		return setBorehole3On;
	}

	@Override
	public ReadChannel<Long> getClima1On() {
		return setClima1On;
	}

	@Override
	public ReadChannel<Long> getClima2On() {
		return setClima2On;
	}

	@Override
	public ReadChannel<Long> getOfficeOn() {
		return setOfficeOn;
	}

	@Override
	public ReadChannel<Long> getTraineeCenterOn() {
		return setTraineeCenterOn;
	}

	@Override
	public ReadChannel<Long> getAutomaticMode() {
		return automaticMode;
	}

	@Override
	public ReadChannel<Long> getManualMode() {
		return manualMode;
	}

	@Override
	public ReadChannel<Long> getEmergencyStop() {
		return emergencyStop;
	}

	@Override
	public ReadChannel<Long> getSwitchStatePivotPump() {
		return switchStatePivotPump;
	}

	@Override
	public ReadChannel<Long> getSwitchStatePivotDrive() {
		return switchStatePivotDrive;
	}

	@Override
	public ReadChannel<Long> getError() {
		return error;
	}

	@Override
	public ReadChannel<Long> getWaterLevelBorehole1On() {
		return waterLevelBorehole1On;
	}

	@Override
	public ReadChannel<Long> getWaterLevelBorehole1Off() {
		return waterLevelBorehole1Off;
	}

	@Override
	public ReadChannel<Long> getWaterLevelBorehole2On() {
		return waterLevelBorehole2On;
	}

	@Override
	public ReadChannel<Long> getWaterLevelBorehole2Off() {
		return waterLevelBorehole2Off;
	}

	@Override
	public ReadChannel<Long> getWaterLevelBorehole3On() {
		return waterLevelBorehole3On;
	}

	@Override
	public ReadChannel<Long> getWaterLevelBorehole3Off() {
		return waterLevelBorehole3Off;
	}

	@Override
	public WriteChannel<Long> getSetPivotOn() {
		return setPivotOn;
	}

	@Override
	public WriteChannel<Long> getSetBorehole1On() {
		return setBorehole1On;
	}

	@Override
	public WriteChannel<Long> getSetBorehole2On() {
		return setBorehole2On;
	}

	@Override
	public WriteChannel<Long> getSetBorehole3On() {
		return setBorehole3On;
	}

	@Override
	public WriteChannel<Long> getSetClima1On() {
		return setClima1On;
	}

	@Override
	public WriteChannel<Long> getSetClima2On() {
		return setClima2On;
	}

	@Override
	public WriteChannel<Long> getSetOfficeOn() {
		return setOfficeOn;
	}

	@Override
	public WriteChannel<Long> getSetTraineeCenterOn() {
		return setTraineeCenterOn;
	}

	@Override
	public WriteChannel<Long> getSignalBus1On() {
		return signalBus1On;
	}

	@Override
	public WriteChannel<Long> getSignalBus2On() {
		return signalBus2On;
	}

	@Override
	public WriteChannel<Long> getSignalGridOn() {
		return signalOnGrid;
	}

	@Override
	public WriteChannel<Long> getSignalSystemStop() {
		return signalSystemStop;
	}

	@Override
	public WriteChannel<Long> getSignalWatchdog() {
		return signalWatchdog;
	}

	@Override
	public WriteChannel<Long> getSetWaterLevelBorehole1On() {
		return setWaterLevelBorehole1On;
	}

	@Override
	public WriteChannel<Long> getSetWaterLevelBorehole1Off() {
		return setWaterLevelBorehole1Off;
	}

	@Override
	public WriteChannel<Long> getSetWaterLevelBorehole2On() {
		return setWaterLevelBorehole2On;
	}

	@Override
	public WriteChannel<Long> getSetWaterLevelBorehole2Off() {
		return setWaterLevelBorehole2Off;
	}

	@Override
	public WriteChannel<Long> getSetWaterLevelBorehole3On() {
		return setWaterLevelBorehole3On;
	}

	@Override
	public WriteChannel<Long> getSetWaterLevelBorehole3Off() {
		return setWaterLevelBorehole3Off;
	}

	@Override
	protected void update() {
		waterLevelBorehole1Off.updateValue(setWaterLevelBorehole1Off.getWrittenValue().orElse(100L));
		waterLevelBorehole1On.updateValue(setWaterLevelBorehole1On.getWrittenValue().orElse(60L));
		waterLevelBorehole2Off.updateValue(setWaterLevelBorehole2Off.getWrittenValue().orElse(80L));
		waterLevelBorehole2On.updateValue(setWaterLevelBorehole2On.getWrittenValue().orElse(40L));
		waterLevelBorehole3Off.updateValue(setWaterLevelBorehole3Off.getWrittenValue().orElse(60L));
		waterLevelBorehole3On.updateValue(setWaterLevelBorehole3On.getWrittenValue().orElse(20L));
		Optional<Long> writeValueBus1On = signalBus1On.getWrittenValue();
		if (writeValueBus1On.isPresent()) {
			signalBus1On.setValue(writeValueBus1On.get());
		}
		Optional<Long> writeValueBus2On = signalBus2On.getWrittenValue();
		if (writeValueBus2On.isPresent()) {
			signalBus2On.setValue(writeValueBus2On.get());
		}
		Optional<Long> writeValueOnGrid = signalOnGrid.getWrittenValue();
		if (writeValueOnGrid.isPresent()) {
			signalOnGrid.setValue(writeValueOnGrid.get());
		}
		Optional<Long> writeValueWatchdog = signalWatchdog.getWrittenValue();
		if (writeValueWatchdog.isPresent()) {
			signalWatchdog.setValue(writeValueWatchdog.get());
		}
	}

}
