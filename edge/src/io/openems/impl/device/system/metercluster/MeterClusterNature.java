package io.openems.impl.device.system.metercluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.meter.AsymmetricMeterNature;
import io.openems.api.device.nature.meter.SymmetricMeterNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.thing.Thing;
import io.openems.api.thing.ThingChannelsUpdatedListener;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.ControllerUtils;
import io.openems.impl.protocol.simulator.SimulatorDeviceNature;
import io.openems.impl.protocol.simulator.SimulatorReadChannel;

@ThingInfo(title = "Meter Cluster")
public class MeterClusterNature extends SimulatorDeviceNature
		implements SymmetricMeterNature, AsymmetricMeterNature, ChannelChangeListener {

	private final Logger log;
	private List<ThingChannelsUpdatedListener> listeners;
	private ThingRepository repo;
	private List<SymmetricMeterNature> symmetricMeterList = new ArrayList<>();
	private List<AsymmetricMeterNature> asymmetricMeterList = new ArrayList<>();

	/*
	 * Channels
	 */

	private SimulatorReadChannel<Long> activePower = new SimulatorReadChannel<>("ActivePower", this);
	private SimulatorReadChannel<Long> apparentPower = new SimulatorReadChannel<>("ApparentPower", this);
	private SimulatorReadChannel<Long> activePowerL1 = new SimulatorReadChannel<>("ActivePowerL1", this);
	private SimulatorReadChannel<Long> activePowerL2 = new SimulatorReadChannel<>("ActivePowerL2", this);
	private SimulatorReadChannel<Long> activePowerL3 = new SimulatorReadChannel<>("ActivePowerL3", this);
	private SimulatorReadChannel<Long> reactivePower = new SimulatorReadChannel<>("ReactivePower", this);
	private SimulatorReadChannel<Long> reactivePowerL1 = new SimulatorReadChannel<>("ReactivePowerL1", this);
	private SimulatorReadChannel<Long> reactivePowerL2 = new SimulatorReadChannel<>("ReactivePowerL2", this);
	private SimulatorReadChannel<Long> reactivePowerL3 = new SimulatorReadChannel<>("ReactivePowerL3", this);
	private SimulatorReadChannel<Long> currentL1 = new SimulatorReadChannel<>("CurrentL1", this);
	private SimulatorReadChannel<Long> currentL2 = new SimulatorReadChannel<>("CurrentL2", this);
	private SimulatorReadChannel<Long> currentL3 = new SimulatorReadChannel<>("CurrentL3", this);
	private SimulatorReadChannel<Long> voltage = new SimulatorReadChannel<>("Voltage", this);
	private SimulatorReadChannel<Long> voltageL1 = new SimulatorReadChannel<>("VoltageL1", this);
	private SimulatorReadChannel<Long> voltageL2 = new SimulatorReadChannel<>("VoltageL2", this);
	private SimulatorReadChannel<Long> voltageL3 = new SimulatorReadChannel<>("VoltageL3", this);
	private SimulatorReadChannel<Long> frequency = new SimulatorReadChannel<>("Frequency", this);

	public MeterClusterNature(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
		log = LoggerFactory.getLogger(this.getClass());
		this.listeners = new ArrayList<>();
		this.repo = ThingRepository.getInstance();
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Meter", description = "Sets the Meter devices for the cluster.", type = JsonArray.class)
	public ConfigChannel<JsonArray> meter = new ConfigChannel<JsonArray>("meter", this).addChangeListener(this);

	private final ConfigChannel<String> type = new ConfigChannel<String>("type", this);

	@Override
	public ConfigChannel<String> type() {
		return type;
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

	@Override
	protected void update() {
		Long activePower = null;
		Long activePowerL1 = null;
		Long activePowerL2 = null;
		Long activePowerL3 = null;
		Long reactivePower = null;
		Long reactivePowerL1 = null;
		Long reactivePowerL2 = null;
		Long reactivePowerL3 = null;
		Long voltage = null;
		Long voltageL1 = null;
		Long voltageL2 = null;
		Long voltageL3 = null;
		synchronized (asymmetricMeterList) {
			for (AsymmetricMeterNature meter : asymmetricMeterList) {
				if (meter.activePowerL1().valueOptional().isPresent()) {
					if (activePower == null) {
						activePower = 0L;
					}
					if (activePowerL1 == null) {
						activePowerL1 = 0L;
					}
					activePower += meter.activePowerL1().valueOptional().get();
					activePowerL1 += meter.activePowerL1().valueOptional().get();
				} else {
					log.warn(meter.id() + ": activePowerL1 is invalid!");
				}
				if (meter.activePowerL2().valueOptional().isPresent()) {
					if (activePower == null) {
						activePower = 0L;
					}
					if (activePowerL2 == null) {
						activePowerL2 = 0L;
					}
					activePower += meter.activePowerL2().valueOptional().get();
					activePowerL2 += meter.activePowerL2().valueOptional().get();
				} else {
					log.warn(meter.id() + ": activePowerL2 is invalid!");
				}
				if (meter.activePowerL3().valueOptional().isPresent()) {
					if (activePower == null) {
						activePower = 0L;
					}
					if (activePowerL3 == null) {
						activePowerL3 = 0L;
					}
					activePower += meter.activePowerL3().valueOptional().get();
					activePowerL3 += meter.activePowerL3().valueOptional().get();
				} else {
					log.warn(meter.id() + ": activePowerL3 is invalid!");
				}
				if (meter.reactivePowerL1().valueOptional().isPresent()) {
					if (reactivePower == null) {
						reactivePower = 0L;
					}
					if (reactivePowerL1 == null) {
						reactivePowerL1 = 0L;
					}
					reactivePower += meter.reactivePowerL1().valueOptional().get();
					reactivePowerL1 += meter.reactivePowerL1().valueOptional().get();
				} else {
					log.warn(meter.id() + ": reactivePowerL1 is invalid!");
				}
				if (meter.reactivePowerL2().valueOptional().isPresent()) {
					if (reactivePower == null) {
						reactivePower = 0L;
					}
					if (reactivePowerL2 == null) {
						reactivePowerL2 = 0L;
					}
					reactivePower += meter.reactivePowerL2().valueOptional().get();
					reactivePowerL2 += meter.reactivePowerL2().valueOptional().get();
				} else {
					log.warn(meter.id() + ": reactivePowerL2 is invalid!");
				}
				if (meter.reactivePowerL3().valueOptional().isPresent()) {
					if (reactivePower == null) {
						reactivePower = 0L;
					}
					if (reactivePowerL3 == null) {
						reactivePowerL3 = 0L;
					}
					reactivePower += meter.reactivePowerL3().valueOptional().get();
					reactivePowerL3 += meter.reactivePowerL3().valueOptional().get();
				} else {
					log.warn(meter.id() + ": reactivePowerL3 is invalid!");
				}
				try {
					if (voltage == null) {
						voltage = meter.voltageL1().value();
					}
					if (voltageL1 == null) {
						voltageL1 = meter.voltageL1().value();
					}
				} catch (InvalidValueException e) {
					log.warn(meter.id() + ": voltageL1 is invalid!");
				}
				try {
					if (voltage == null) {
						voltage = meter.voltageL2().value();
					}
					if (voltageL2 == null) {
						voltageL2 = meter.voltageL2().value();
					}
				} catch (InvalidValueException e) {
					log.warn(meter.id() + ": voltageL2 is invalid!");
				}
				try {
					if (voltage == null) {
						voltage = meter.voltageL3().value();
					}
					if (voltageL3 == null) {
						voltageL3 = meter.voltageL3().value();
					}
				} catch (InvalidValueException e) {
					log.warn(meter.id() + ": voltageL3 is invalid!");
				}
			}
		}
		synchronized (symmetricMeterList) {
			for (SymmetricMeterNature meter : symmetricMeterList) {
				if (meter.activePower().valueOptional().isPresent()) {
					if (activePower == null) {
						activePower = 0L;
					}
					if (activePowerL1 == null) {
						activePowerL1 = 0L;
					}
					if (activePowerL2 == null) {
						activePowerL2 = 0L;
					}
					if (activePowerL3 == null) {
						activePowerL3 = 0L;
					}
					activePower += meter.activePower().valueOptional().get();
					activePowerL1 += meter.activePower().valueOptional().get() / 3;
					activePowerL2 += meter.activePower().valueOptional().get() / 3;
					activePowerL3 += meter.activePower().valueOptional().get() / 3;
				} else {
					log.warn(meter.id() + ": activePower is invalid!");
				}
				if (meter.activePower().valueOptional().isPresent()) {
					if (reactivePower == null) {
						reactivePower = 0L;
					}
					if (reactivePowerL1 == null) {
						reactivePowerL1 = 0L;
					}
					if (reactivePowerL2 == null) {
						reactivePowerL2 = 0L;
					}
					if (reactivePowerL3 == null) {
						reactivePowerL3 = 0L;
					}
					reactivePower += meter.reactivePower().valueOptional().get();
					reactivePowerL1 += meter.reactivePower().valueOptional().get() / 3;
					reactivePowerL2 += meter.reactivePower().valueOptional().get() / 3;
					reactivePowerL3 += meter.reactivePower().valueOptional().get() / 3;
				} else {
					log.warn(meter.id() + ": reactivePower is invalid!");
				}
				try {
					if (voltage == null) {
						voltage = meter.voltage().value();
					}
				} catch (InvalidValueException e) {
					log.warn(meter.id() + ": voltage is invalid!");
				}
			}
		}
		this.activePower.updateValue(activePower);
		this.activePowerL1.updateValue(activePowerL1);
		this.activePowerL2.updateValue(activePowerL2);
		this.activePowerL3.updateValue(activePowerL3);
		this.reactivePower.updateValue(reactivePower);
		this.reactivePowerL1.updateValue(reactivePowerL1);
		this.reactivePowerL2.updateValue(reactivePowerL2);
		this.reactivePowerL3.updateValue(reactivePowerL3);
		if (activePower != null && reactivePower != null) {
			this.apparentPower.updateValue(ControllerUtils.calculateApparentPower(activePower, reactivePower));
		} else {
			this.apparentPower.updateValue(null);
		}
		this.voltage.updateValue(voltage);
		this.voltageL1.updateValue(voltageL1);
		this.voltageL2.updateValue(voltageL2);
		this.voltageL3.updateValue(voltageL3);
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
	public ReadChannel<Long> currentL1() {
		return currentL1;
	}

	@Override
	public ReadChannel<Long> currentL2() {
		return currentL2;
	}

	@Override
	public ReadChannel<Long> currentL3() {
		return currentL3;
	}

	@Override
	public ReadChannel<Long> voltageL1() {
		return voltageL1;
	}

	@Override
	public ReadChannel<Long> voltageL2() {
		return voltageL2;
	}

	@Override
	public ReadChannel<Long> voltageL3() {
		return voltageL3;
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
	public ReadChannel<Long> frequency() {
		return frequency;
	}

	@Override
	public ReadChannel<Long> voltage() {
		return voltage;
	}

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		if (channel.equals(meter)) {
			loadMeter();
		}
	}

	private void loadMeter() {
		JsonArray meterIds;
		try {
			meterIds = meter.value();
			// remove old ess
			synchronized (symmetricMeterList) {
				synchronized (asymmetricMeterList) {
					symmetricMeterList.clear();
					asymmetricMeterList.clear();
					if (meterIds != null) {
						for (JsonElement id : meterIds) {
							Optional<Thing> nature = repo.getThingById(id.getAsString());
							if (nature.isPresent()) {
								if (nature.get() instanceof AsymmetricMeterNature) {
									AsymmetricMeterNature meter = (AsymmetricMeterNature) nature.get();
									asymmetricMeterList.add(meter);
								} else if (nature.get() instanceof SymmetricMeterNature) {
									SymmetricMeterNature meter = (SymmetricMeterNature) nature.get();
									symmetricMeterList.add(meter);
								} else {
									log.error("ThingID: " + id.getAsString() + " is no Meter!");
								}
							} else {
								log.warn("meter: " + id.getAsString() + " not found!");
							}
						}
					}
				}
			}
		} catch (InvalidValueException e) {
			log.error("meter value is invalid!", e);
		}
	}

	@Override
	public void init() {
		loadMeter();
		for (ThingChannelsUpdatedListener listener : this.listeners) {
			listener.thingChannelsUpdated(this);
		}
	}

	@Override
	public void addListener(ThingChannelsUpdatedListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void removeListener(ThingChannelsUpdatedListener listener) {
		this.listeners.remove(listener);
	}

}
