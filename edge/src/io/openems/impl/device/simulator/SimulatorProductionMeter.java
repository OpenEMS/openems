package io.openems.impl.device.simulator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.FunctionalReadChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.device.Device;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.core.utilities.ControllerUtils;
import io.openems.impl.protocol.simulator.SimulatorReadChannel;

@ThingInfo(title = "Simulated Production Meter")
public class SimulatorProductionMeter extends SimulatorMeter implements ChannelChangeListener {

	private final Logger log = LoggerFactory.getLogger(SimulatorProductionMeter.class);

	// @ConfigInfo(title = "ActivePower", type = Long.class)
	// public ConfigChannel<Long> activePower = new ConfigChannel("ActivePower", this);
	// @ConfigInfo(title = "ReactivePower", type = Long.class)
	// public ConfigChannel<Long> reactivePower = new ConfigChannel<Long>("ReactivePower", this);
	@ChannelInfo(title = "ActivePowerGeneratorConfig", type = JsonObject.class)
	public ConfigChannel<JsonObject> activePowerGeneratorConfig = new ConfigChannel<JsonObject>(
			"activePowerGeneratorConfig", this).addChangeListener(this);
	@ChannelInfo(title = "ReactivePowerGeneratorConfig", type = JsonObject.class)
	public ConfigChannel<JsonObject> reactivePowerGeneratorConfig = new ConfigChannel<JsonObject>(
			"reactivePowerGeneratorConfig", this).addChangeListener(this);
	private SimulatorReadChannel<Long> activePower = new SimulatorReadChannel<>("ActivePower", this);
	private SimulatorReadChannel<Long> reactivePower = new SimulatorReadChannel<>("ReactivePower", this);
	private FunctionalReadChannel<Long> apparentPower;
	private LoadGenerator activePowerGenerator;
	private LoadGenerator reactivePowerGenerator;
	private ThingStateChannels thingState;

	public SimulatorProductionMeter(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
		this.apparentPower = new FunctionalReadChannel<Long>("ApparentPower", this, (channels) -> {
			return ControllerUtils.calculateApparentPower(channels[0].valueOptional().orElse(0L),
					channels[1].valueOptional().orElse(0L));
		}, activePower, reactivePower);
		this.thingState = new ThingStateChannels(this);
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
	protected void update() {
		super.update();
		if (activePowerGenerator != null) {
			activePower.updateValue(activePowerGenerator.getLoad());
		} else {
			activePower.updateValue(0L);
			log.error("activePowerGenerator is null");
		}
		if (reactivePowerGenerator != null) {
			reactivePower.updateValue(reactivePowerGenerator.getLoad());
		} else {
			reactivePower.updateValue(0L);
			log.error("reactivePowerGenerator is null");
		}
	}

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		if (channel.equals(activePowerGeneratorConfig)) {
			if (activePowerGeneratorConfig.valueOptional().isPresent()) {
				activePowerGenerator = getGenerator(activePowerGeneratorConfig.valueOptional().get());
			}
		} else if (channel.equals(reactivePowerGeneratorConfig)) {
			if (reactivePowerGeneratorConfig.valueOptional().isPresent()) {
				reactivePowerGenerator = getGenerator(reactivePowerGeneratorConfig.valueOptional().get());
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
				} catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException e) {
					log.error(e.getMessage());
				}
			}
			return (LoadGenerator) clazz.newInstance();

		} catch (SecurityException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			log.error(e.getMessage());
		}
		return null;
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}

}
