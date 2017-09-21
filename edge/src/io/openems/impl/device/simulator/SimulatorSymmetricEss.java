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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.FunctionalReadChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StaticValueChannel;
import io.openems.api.channel.StatusBitChannels;
import io.openems.api.channel.WriteChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.charger.ChargerNature;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.thing.Thing;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.AvgFiFoQueue;
import io.openems.core.utilities.ControllerUtils;
import io.openems.impl.protocol.modbus.ModbusWriteLongChannel;
import io.openems.impl.protocol.simulator.SimulatorDeviceNature;
import io.openems.impl.protocol.simulator.SimulatorReadChannel;
import io.openems.test.utils.channel.UnitTestWriteChannel;

@ThingInfo(title = "Simulator ESS")
public class SimulatorSymmetricEss extends SimulatorDeviceNature implements SymmetricEssNature, ChannelChangeListener {

	private List<ChargerNature> chargerList;
	private ThingRepository repo = ThingRepository.getInstance();
	private double energy;
	private AvgFiFoQueue activePowerQueue = new AvgFiFoQueue(3, 1);
	private AvgFiFoQueue reactivePowerQueue = new AvgFiFoQueue(3, 1);
	@ChannelInfo(title = "ActivePowerGeneratorConfig", type = JsonObject.class)
	public ConfigChannel<JsonObject> activePowerGeneratorConfig = new ConfigChannel<JsonObject>(
			"activePowerGeneratorConfig", this).addChangeListener(this).addChangeListener(this);
	@ChannelInfo(title = "ReactivePowerGeneratorConfig", type = JsonObject.class)
	public ConfigChannel<JsonObject> reactivePowerGeneratorConfig = new ConfigChannel<JsonObject>(
			"reactivePowerGeneratorConfig", this).addChangeListener(this).addChangeListener(this);
	private LoadGenerator offGridActivePowerGenerator;
	private LoadGenerator offGridReactivePowerGenerator;

	/*
	 * Constructors
	 */
	public SimulatorSymmetricEss(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
		minSoc.addUpdateListener((channel, newValue) -> {
			// If chargeSoc was not set -> set it to minSoc minus 2
			if (channel == minSoc && !chargeSoc.valueOptional().isPresent()) {
				chargeSoc.updateValue((Integer) newValue.get() - 2, false);
			}
		});
		long initialSoc = SimulatorTools.addRandomLong(50, 0, 100, 20);
		this.energy = capacity.valueOptional().get() / 100 * initialSoc;
		this.soc = new FunctionalReadChannel<Long>("Soc", this, (channels) -> {
			try {
				energy -= channels[0].value() / 3600.0;
			} catch (InvalidValueException e) {

			}
			if (chargerList != null) {
				for (ChargerNature charger : chargerList) {
					try {
						energy += charger.getActualPower().value() / 3600.0;
					} catch (InvalidValueException e) {}
				}
			}
			try {
				if (energy > capacity.value()) {
					energy = capacity.value();
				} else if (energy < 0) {
					energy = 0;
				}
				return (long) (energy / capacity.value() * 100.0);
			} catch (InvalidValueException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return 0L;
		}, this.activePower);
	}

	/*
	 * Config
	 */
	private ConfigChannel<Integer> minSoc = new ConfigChannel<Integer>("minSoc", this);
	private ConfigChannel<Integer> chargeSoc = new ConfigChannel<Integer>("chargeSoc", this);
	@ChannelInfo(title = "GridMode", type = Long.class)
	public ConfigChannel<Long> gridMode = new ConfigChannel<Long>("gridMode", this).label(0L, ON_GRID)
			.label(1L, OFF_GRID).defaultValue(0L);
	@ChannelInfo(title = "SystemState", type = Long.class)
	public ConfigChannel<Long> systemState = new ConfigChannel<Long>("systemState", this) //
			.label(1L, START).label(2L, STOP).label(5L, FAULT).defaultValue(1L);

	@Override
	public ConfigChannel<Integer> minSoc() {
		return minSoc;
	}

	@Override
	public ConfigChannel<Integer> chargeSoc() {
		return chargeSoc;
	}

	/*
	 * Inherited Channels
	 */
	private StatusBitChannels warning = new StatusBitChannels("Warning", this);;
	private FunctionalReadChannel<Long> soc;
	private SimulatorReadChannel<Long> activePower = new SimulatorReadChannel<Long>("ActivePower", this);
	private StaticValueChannel<Long> allowedApparent = new StaticValueChannel<Long>("AllowedApparent", this, 40000L);
	private SimulatorReadChannel<Long> allowedCharge = new SimulatorReadChannel<Long>("AllowedCharge", this);
	private SimulatorReadChannel<Long> allowedDischarge = new SimulatorReadChannel<Long>("AllowedDischarge", this);
	private SimulatorReadChannel<Long> apparentPower = new SimulatorReadChannel<Long>("ApparentPower", this);
	private SimulatorReadChannel<Long> reactivePower = new SimulatorReadChannel<Long>("ReactivePower", this);
	private UnitTestWriteChannel<Long> setActivePower = new UnitTestWriteChannel<Long>("SetActivePower", this)
			.maxWriteChannel(allowedDischarge).minWriteChannel(allowedCharge);
	private UnitTestWriteChannel<Long> setReactivePower = new UnitTestWriteChannel<Long>("SetReactivePower", this);
	private ModbusWriteLongChannel setWorkState = new ModbusWriteLongChannel("SetWorkState", this).label(1, START)
			.label(2, STOP);
	private StaticValueChannel<Long> maxNominalPower = new StaticValueChannel<>("maxNominalPower", this, 40000L)
			.unit("VA");
	private StaticValueChannel<Long> capacity = new StaticValueChannel<>("capacity", this, 5000L).unit("Wh");
	@ChannelInfo(title = "charger", type = JsonArray.class, isOptional = true)
	public ConfigChannel<JsonArray> charger = new ConfigChannel<JsonArray>("charger", this).addChangeListener(this);

	@Override
	public ReadChannel<Long> gridMode() {
		return gridMode;
	}

	@Override
	public ReadChannel<Long> soc() {
		return soc;
	}

	@Override
	public ReadChannel<Long> systemState() {
		return systemState;
	}

	@Override
	public ReadChannel<Long> allowedCharge() {
		return allowedCharge;
	}

	@Override
	public ReadChannel<Long> allowedDischarge() {
		return allowedDischarge;
	}

	@Override
	public WriteChannel<Long> setWorkState() {
		return setWorkState;
	}

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
	public WriteChannel<Long> setActivePower() {
		return setActivePower;
	}

	@Override
	public WriteChannel<Long> setReactivePower() {
		return setReactivePower;
	}

	@Override
	public StatusBitChannels warning() {
		return warning;
	}

	@Override
	public ReadChannel<Long> allowedApparent() {
		return allowedApparent;
	}

	@Override
	public ReadChannel<Long> maxNominalPower() {
		return maxNominalPower;
	}

	/*
	 * Methods
	 */
	@Override
	protected void update() {
		if (chargerList == null) {
			chargerList = new ArrayList<>();
			getCharger();
		}
		Optional<Long> writtenActivePower = setActivePower.getWrittenValue();
		if (writtenActivePower.isPresent()) {
			activePowerQueue.add(writtenActivePower.get());
		}
		Optional<Long> writtenReactivePower = setReactivePower.getWrittenValue();
		if (writtenReactivePower.isPresent()) {
			reactivePowerQueue.add(writtenReactivePower.get());
		}
		// lastApparentPower = SimulatorTools.addRandomLong(lastApparentPower, -10000, 10000, 500);
		// lastCosPhi = SimulatorTools.addRandomDouble(lastCosPhi, -1.5, 1.5, 0.5);
		//
		// long activePower = ControllerUtils.calculateActivePowerFromApparentPower(lastApparentPower, lastCosPhi);
		// long reactivePower = ControllerUtils.calculateReactivePower(activePower, lastCosPhi);
		long activePower = 0;
		long reactivePower = 0;
		if (this.systemState.labelOptional().equals(Optional.of(EssNature.START))) {
			if (this.gridMode.labelOptional().equals(Optional.of(EssNature.OFF_GRID))) {
				activePower = offGridActivePowerGenerator.getLoad();
				reactivePower = offGridReactivePowerGenerator.getLoad();
			} else {
				activePower = activePowerQueue.avg();
				reactivePower = reactivePowerQueue.avg();
			}
		}
		this.activePower.updateValue(activePower);
		this.reactivePower.updateValue(reactivePower);
		this.apparentPower.updateValue(ControllerUtils.calculateApparentPower(activePower, reactivePower));
		this.allowedCharge.updateValue(-9000L);
		this.allowedDischarge.updateValue(3000L);
	}

	@Override
	public StaticValueChannel<Long> capacity() {
		return capacity;
	}

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		if (channel.equals(charger)) {
			if (chargerList != null) {
				getCharger();
			}
		} else if (channel.equals(activePowerGeneratorConfig)) {
			if (activePowerGeneratorConfig.valueOptional().isPresent()) {
				offGridActivePowerGenerator = getGenerator(activePowerGeneratorConfig.valueOptional().get());
			}
		} else if (channel.equals(reactivePowerGeneratorConfig)) {
			if (reactivePowerGeneratorConfig.valueOptional().isPresent()) {
				offGridReactivePowerGenerator = getGenerator(reactivePowerGeneratorConfig.valueOptional().get());
			}
		}
	}

	private LoadGenerator getGenerator(JsonObject config) {
		try {
			Class<?> clazz = Class.forName(config.get("className").getAsString());
			if (config.get("config") != null) {
				try {
					Constructor<?> constructor = clazz.getConstructor(JsonObject.class);
					return (LoadGenerator) constructor.newInstance(config.get("config").getAsJsonObject());
				} catch (NoSuchMethodException e) {

				}
			}
			return (LoadGenerator) clazz.newInstance();

		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private void getCharger() {
		if (chargerList != null) {
			for (ChargerNature charger : chargerList) {
				soc.removeChannel(charger.getActualPower());
			}
			chargerList.clear();
			if (charger.valueOptional().isPresent()) {
				JsonArray ids = charger.valueOptional().get();
				for (JsonElement e : ids) {
					Optional<Thing> t = repo.getThingById(e.getAsString());
					if (t.isPresent()) {
						if (t.get() instanceof ChargerNature) {
							ChargerNature charger = (ChargerNature) t.get();
							chargerList.add(charger);
							soc.addChannel(charger.getActualPower());
						}
					}
				}
			}
		}
	}

}
