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
import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.thing.Thing;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.AvgFiFoQueue;
import io.openems.impl.protocol.modbus.ModbusWriteLongChannel;
import io.openems.impl.protocol.simulator.SimulatorDeviceNature;
import io.openems.impl.protocol.simulator.SimulatorReadChannel;
import io.openems.test.utils.channel.UnitTestWriteChannel;

@ThingInfo(title = "Simulator ESS")
public class SimulatorAsymmetricEss extends SimulatorDeviceNature
		implements AsymmetricEssNature, ChannelChangeListener {

	private List<ChargerNature> chargerList;
	private ThingRepository repo = ThingRepository.getInstance();
	private LoadGenerator offGridActivePowerGenerator = new RandomLoadGenerator(-10000, 10000);
	private LoadGenerator offGridReactivePowerGenerator = new RandomLoadGenerator(-500, 500);

	/*
	 * Constructors
	 */
	public SimulatorAsymmetricEss(String thingId) throws ConfigException {
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
					energy -= (channels[0].value() + channels[1].value() + channels[2].value()) / 3600.0;
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
					} else if (energy < 0) {
						energy = 0;
					}
					return (long) (energy / capacity.value() * 100.0);
				} catch (InvalidValueException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return 0L;
			}

		}, this.activePowerL1, this.activePowerL2, this.activePowerL3);
	}

	private double energy;
	private AvgFiFoQueue activePowerQueueL1 = new AvgFiFoQueue(5, 1);
	private AvgFiFoQueue activePowerQueueL2 = new AvgFiFoQueue(5, 1);
	private AvgFiFoQueue activePowerQueueL3 = new AvgFiFoQueue(5, 1);
	private AvgFiFoQueue reactivePowerQueueL1 = new AvgFiFoQueue(5, 1);
	private AvgFiFoQueue reactivePowerQueueL2 = new AvgFiFoQueue(5, 1);
	private AvgFiFoQueue reactivePowerQueueL3 = new AvgFiFoQueue(5, 1);

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
	private SimulatorReadChannel activePowerL1 = new SimulatorReadChannel("ActivePoweL1", this);
	private SimulatorReadChannel activePowerL2 = new SimulatorReadChannel("ActivePoweL2", this);
	private SimulatorReadChannel activePowerL3 = new SimulatorReadChannel("ActivePoweL3", this);
	private SimulatorReadChannel allowedApparent = new SimulatorReadChannel("AllowedApparent", this);
	private SimulatorReadChannel allowedCharge = new SimulatorReadChannel("AllowedCharge", this);
	private SimulatorReadChannel allowedDischarge = new SimulatorReadChannel("AllowedDischarge", this);
	private SimulatorReadChannel gridMode = new SimulatorReadChannel("GridMode", this).label(0, ON_GRID).label(1,
			OFF_GRID);
	private SimulatorReadChannel reactivePowerL1 = new SimulatorReadChannel("ReactivePowerL1", this);
	private SimulatorReadChannel reactivePowerL2 = new SimulatorReadChannel("ReactivePowerL2", this);
	private SimulatorReadChannel reactivePowerL3 = new SimulatorReadChannel("ReactivePowerL3", this);
	private SimulatorReadChannel systemState = new SimulatorReadChannel("SystemState", this) //
			.label(1, START).label(2, STOP);
	private UnitTestWriteChannel<Long> setActivePowerL1 = new UnitTestWriteChannel<Long>("SetActivePowerL1", this);
	private UnitTestWriteChannel<Long> setActivePowerL2 = new UnitTestWriteChannel<Long>("SetActivePowerL2", this);
	private UnitTestWriteChannel<Long> setActivePowerL3 = new UnitTestWriteChannel<Long>("SetActivePowerL3", this);
	private UnitTestWriteChannel<Long> setReactivePowerL1 = new UnitTestWriteChannel<Long>("SetReactivePowerL1", this);
	private UnitTestWriteChannel<Long> setReactivePowerL2 = new UnitTestWriteChannel<Long>("SetReactivePowerL2", this);
	private UnitTestWriteChannel<Long> setReactivePowerL3 = new UnitTestWriteChannel<Long>("SetReactivePowerL3", this);
	private ModbusWriteLongChannel setWorkState = new ModbusWriteLongChannel("SetWorkState", this);
	private StaticValueChannel<Long> maxNominalPower = new StaticValueChannel<>("maxNominalPower", this, 40000L)
			.unit("VA");
	private StaticValueChannel<Long> capacity = new StaticValueChannel<>("capacity", this, 5000L).unit("Wh");
	@ConfigInfo(title = "charger", type = JsonArray.class, isOptional = true)
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
		Optional<Long> writtenActivePowerL1 = setActivePowerL1.getWrittenValue();
		if (writtenActivePowerL1.isPresent()) {
			activePowerQueueL1.add(writtenActivePowerL1.get());
		}
		Optional<Long> writtenActivePowerL2 = setActivePowerL2.getWrittenValue();
		if (writtenActivePowerL2.isPresent()) {
			activePowerQueueL2.add(writtenActivePowerL2.get());
		}
		Optional<Long> writtenActivePowerL3 = setActivePowerL3.getWrittenValue();
		if (writtenActivePowerL3.isPresent()) {
			activePowerQueueL3.add(writtenActivePowerL3.get());
		}
		Optional<Long> writtenReactivePowerL1 = setReactivePowerL1.getWrittenValue();
		if (writtenReactivePowerL1.isPresent()) {
			reactivePowerQueueL1.add(writtenReactivePowerL1.get());
		}
		Optional<Long> writtenReactivePowerL2 = setReactivePowerL2.getWrittenValue();
		if (writtenReactivePowerL2.isPresent()) {
			reactivePowerQueueL2.add(writtenReactivePowerL2.get());
		}
		Optional<Long> writtenReactivePowerL3 = setReactivePowerL3.getWrittenValue();
		if (writtenReactivePowerL3.isPresent()) {
			reactivePowerQueueL3.add(writtenReactivePowerL3.get());
		}
		// lastApparentPower = SimulatorTools.addRandomLong(lastApparentPower, -10000, 10000, 500);
		// lastCosPhi = SimulatorTools.addRandomDouble(lastCosPhi, -1.5, 1.5, 0.5);
		//
		// long activePower = ControllerUtils.calculateActivePowerFromApparentPower(lastApparentPower, lastCosPhi);
		// long reactivePower = ControllerUtils.calculateReactivePower(activePower, lastCosPhi);
		long activePowerL1 = 0;
		long activePowerL2 = 0;
		long activePowerL3 = 0;
		long reactivePowerL1 = 0;
		long reactivePowerL2 = 0;
		long reactivePowerL3 = 0;
		if (this.gridMode.labelOptional().equals(Optional.of(EssNature.OFF_GRID))) {
			activePowerL1 = offGridActivePowerGenerator.getLoad() / 3;
			activePowerL2 = offGridActivePowerGenerator.getLoad() / 3;
			activePowerL3 = offGridActivePowerGenerator.getLoad() / 3;
			reactivePowerL1 = offGridReactivePowerGenerator.getLoad() / 3;
			reactivePowerL2 = offGridReactivePowerGenerator.getLoad() / 3;
			reactivePowerL3 = offGridReactivePowerGenerator.getLoad() / 3;
		} else {
			activePowerL1 = activePowerQueueL1.avg();
			activePowerL2 = activePowerQueueL2.avg();
			activePowerL3 = activePowerQueueL3.avg();
			reactivePowerL1 = reactivePowerQueueL1.avg();
			reactivePowerL2 = reactivePowerQueueL2.avg();
			reactivePowerL3 = reactivePowerQueueL3.avg();
		}
		this.activePowerL1.updateValue(activePowerL1);
		this.activePowerL2.updateValue(activePowerL2);
		this.activePowerL3.updateValue(activePowerL3);
		this.reactivePowerL1.updateValue(reactivePowerL1);
		this.reactivePowerL2.updateValue(reactivePowerL2);
		this.reactivePowerL3.updateValue(reactivePowerL3);
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
	public WriteChannel<Long> setActivePowerL1() {
		return setActivePowerL1;
	}

	@Override
	public WriteChannel<Long> setActivePowerL2() {
		return setActivePowerL2;
	}

	@Override
	public WriteChannel<Long> setActivePowerL3() {
		return setActivePowerL3;
	}

	@Override
	public WriteChannel<Long> setReactivePowerL1() {
		return setReactivePowerL1;
	}

	@Override
	public WriteChannel<Long> setReactivePowerL2() {
		return setReactivePowerL2;
	}

	@Override
	public WriteChannel<Long> setReactivePowerL3() {
		return setReactivePowerL3;
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
