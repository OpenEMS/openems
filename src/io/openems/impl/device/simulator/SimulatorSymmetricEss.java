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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.FunctionalReadChannel;
import io.openems.api.channel.FunctionalReadChannelFunction;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StaticValueChannel;
import io.openems.api.channel.StatusBitChannels;
import io.openems.api.channel.WriteChannel;
import io.openems.api.device.nature.charger.ChargerNature;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.doc.ConfigInfo;
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
	private AvgFiFoQueue activePowerQueue = new AvgFiFoQueue(5, 1);
	private AvgFiFoQueue reactivePowerQueue = new AvgFiFoQueue(5, 1);

	/*
	 * Constructors
	 */
	public SimulatorSymmetricEss(String thingId) throws ConfigException {
		super(thingId);
		minSoc.addUpdateListener((channel, newValue) -> {
			// If chargeSoc was not set -> set it to minSoc minus 2
			if (channel == minSoc && !chargeSoc.valueOptional().isPresent()) {
				chargeSoc.updateValue((Integer) newValue.get() - 2, false);
			}
		});
		long initialSoc = SimulatorTools.addRandomLong(50, 0, 100, 20);
		this.energy = capacity.valueOptional().get() / 100 * initialSoc;
		this.soc = new FunctionalReadChannel<Long>("Soc", this, new FunctionalReadChannelFunction<Long>() {

			@Override
			public Long handle(ReadChannel<Long>... channels) {
				try {
					energy -= channels[0].value() / 3600.0;
				} catch (InvalidValueException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (chargerList != null) {
					for (ChargerNature charger : chargerList) {
						try {
							energy += charger.getActualPower().value() / 3600.0;
						} catch (InvalidValueException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				try {
					if (energy > capacity.value()) {
						energy = capacity.value();
					}
					return (long) (energy / capacity.value() * 100.0);
				} catch (InvalidValueException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return 0L;
			}

		}, this.activePower);
	}

	/*
	 * Config
	 */
	private ConfigChannel<Integer> minSoc = new ConfigChannel<Integer>("minSoc", this);
	private ConfigChannel<Integer> chargeSoc = new ConfigChannel<Integer>("chargeSoc", this);

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
	private SimulatorReadChannel activePower = new SimulatorReadChannel("ActivePower", this);
	private SimulatorReadChannel allowedApparent = new SimulatorReadChannel("AllowedApparent", this);
	private SimulatorReadChannel allowedCharge = new SimulatorReadChannel("AllowedCharge", this);
	private SimulatorReadChannel allowedDischarge = new SimulatorReadChannel("AllowedDischarge", this);
	private SimulatorReadChannel apparentPower = new SimulatorReadChannel("ApparentPower", this);
	private SimulatorReadChannel gridMode = new SimulatorReadChannel("GridMode", this).label(0, ON_GRID).label(1,
			OFF_GRID);
	private SimulatorReadChannel reactivePower = new SimulatorReadChannel("ReactivePower", this);
	private SimulatorReadChannel systemState = new SimulatorReadChannel("SystemState", this) //
			.label(1, START).label(2, STOP);
	private UnitTestWriteChannel<Long> setActivePower = new UnitTestWriteChannel<Long>("SetActivePower", this)
			.maxWriteChannel(allowedDischarge).minWriteChannel(allowedCharge);
	private UnitTestWriteChannel<Long> setReactivePower = new UnitTestWriteChannel<Long>("SetReactivePower", this);
	private ModbusWriteLongChannel setWorkState = new ModbusWriteLongChannel("SetWorkState", this);
	private StaticValueChannel<Long> maxNominalPower = new StaticValueChannel<>("maxNominalPower", this, 40000L)
			.unit("VA");
	private StaticValueChannel<Long> capacity = new StaticValueChannel<>("capacity", this, 5000L).unit("Wh");
	@ConfigInfo(title = "charger", type = JsonArray.class)
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

	private long getRandom(int min, int max) {
		return ThreadLocalRandom.current().nextLong(min, max + 1);
	}

	@Override
	public ReadChannel<Long> maxNominalPower() {
		return maxNominalPower;
	}

	/*
	 * Fields
	 */
	private long lastApparentPower = 0;
	private double lastCosPhi = 0;

	/*
	 * Methods
	 */
	@Override
	protected void update() {
		if (chargerList == null) {
			chargerList = new ArrayList<>();
			getCharger();
		}
		Optional<Long> activePower = setActivePower.getWrittenValue();
		if (activePower.isPresent()) {
			activePowerQueue.add(activePower.get());
		}
		Optional<Long> reactivePower = setReactivePower.getWrittenValue();
		if (reactivePower.isPresent()) {
			reactivePowerQueue.add(reactivePower.get());
		}
		// lastApparentPower = SimulatorTools.addRandomLong(lastApparentPower, -10000, 10000, 500);
		// lastCosPhi = SimulatorTools.addRandomDouble(lastCosPhi, -1.5, 1.5, 0.5);
		//
		// long activePower = ControllerUtils.calculateActivePowerFromApparentPower(lastApparentPower, lastCosPhi);
		// long reactivePower = ControllerUtils.calculateReactivePower(activePower, lastCosPhi);
		this.activePower.updateValue(activePowerQueue.avg());
		this.reactivePower.updateValue(reactivePowerQueue.avg());
		this.apparentPower
				.updateValue(ControllerUtils.calculateApparentPower(activePowerQueue.avg(), reactivePowerQueue.avg()));
		this.allowedCharge.updateValue(-9000L);
		this.allowedDischarge.updateValue(3000L);
		this.systemState.updateValue(1L);
		this.gridMode.updateValue(0L);
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
		}
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
