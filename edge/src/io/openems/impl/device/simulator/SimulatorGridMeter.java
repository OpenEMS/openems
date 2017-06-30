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
import io.openems.api.channel.FunctionalReadChannelFunction;
import io.openems.api.channel.ReadChannel;
import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.device.nature.meter.AsymmetricMeterNature;
import io.openems.api.device.nature.meter.MeterNature;
import io.openems.api.device.nature.meter.SymmetricMeterNature;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.thing.Thing;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.ControllerUtils;
import io.openems.impl.protocol.simulator.SimulatorReadChannel;

public class SimulatorGridMeter extends SimulatorMeter implements ChannelChangeListener {

	private SimulatorReadChannel activePower = new SimulatorReadChannel("ActivePower", this);
	private FunctionalReadChannel<Long> apparentPower;
	private SimulatorReadChannel reactivePower = new SimulatorReadChannel("ReactivePower", this);
	@ConfigInfo(title = "esss", type = JsonArray.class)
	public ConfigChannel<JsonArray> esss = new ConfigChannel<JsonArray>("esss", this).addChangeListener(this);
	@ConfigInfo(title = "producer", type = JsonArray.class)
	public ConfigChannel<JsonArray> producer = new ConfigChannel<JsonArray>("producer", this).addChangeListener(this);
	@ConfigInfo(title = "ActivePowerGeneratorConfig", type = JsonObject.class)
	public ConfigChannel<JsonObject> activePowerGeneratorConfig = new ConfigChannel<JsonObject>(
			"activePowerGeneratorConfig", this).addChangeListener(this);
	@ConfigInfo(title = "ReactivePowerGeneratorConfig", type = JsonObject.class)
	public ConfigChannel<JsonObject> reactivePowerGeneratorConfig = new ConfigChannel<JsonObject>(
			"reactivePowerGeneratorConfig", this).addChangeListener(this);

	private ThingRepository repo = ThingRepository.getInstance();
	private List<EssNature> essNatures;
	private List<MeterNature> meterNatures = new ArrayList<>();
	private LoadGenerator activePowerLoad;
	private LoadGenerator reactivePowerLoad;

	public SimulatorGridMeter(String thingId) throws ConfigException {
		super(thingId);
		this.apparentPower = new FunctionalReadChannel<Long>("ApparentPower", this,
				new FunctionalReadChannelFunction<Long>() {

					@Override
					public Long handle(ReadChannel<Long>... channels) {
						return ControllerUtils.calculateApparentPower(channels[0].valueOptional().orElse(0L),
								channels[1].valueOptional().orElse(0L));
					}

				}, activePower, reactivePower);

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
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		if (channel.equals(esss)) {
			if (essNatures != null) {
				getEssNatures();
			}
		} else if (channel.equals(producer)) {
			if (meterNatures != null) {
				getMeterNatures();
			}
		} else if (channel.equals(activePowerGeneratorConfig)) {
			if (activePowerGeneratorConfig.valueOptional().isPresent()) {
				activePowerLoad = getGenerator(activePowerGeneratorConfig.valueOptional().get());
			}
		} else if (channel.equals(reactivePowerGeneratorConfig)) {
			if (reactivePowerGeneratorConfig.valueOptional().isPresent()) {
				reactivePowerLoad = getGenerator(reactivePowerGeneratorConfig.valueOptional().get());
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

	private void getEssNatures() {
		essNatures.clear();
		if (esss.valueOptional().isPresent()) {
			JsonArray ids = esss.valueOptional().get();
			for (JsonElement e : ids) {
				Optional<Thing> t = repo.getThingById(e.getAsString());
				if (t.isPresent()) {
					if (t.get() instanceof EssNature) {
						essNatures.add((EssNature) t.get());
					}
				}
			}
		}
	}

	private void getMeterNatures() {
		meterNatures.clear();
		if (producer.valueOptional().isPresent()) {
			JsonArray ids = producer.valueOptional().get();
			for (JsonElement e : ids) {
				Optional<Thing> t = repo.getThingById(e.getAsString());
				if (t.isPresent()) {
					if (t.get() instanceof MeterNature) {
						meterNatures.add((MeterNature) t.get());
					}
				}
			}
		}
	}

	@Override
	protected void update() {
		if (essNatures == null) {
			essNatures = new ArrayList<>();
			getEssNatures();
		}
		if (meterNatures == null) {
			meterNatures = new ArrayList<>();
			getMeterNatures();
		}
		super.update();
		long activePower = 0;
		if (activePowerLoad != null) {
			activePower = activePowerLoad.getLoad();
		}
		activePower = activePower
				+ SimulatorTools.getRandomLong((int) Math.abs(activePower) / -10, (int) Math.abs(activePower) / 10);
		long reactivePower = 0;
		if (reactivePowerLoad != null) {
			reactivePower = reactivePowerLoad.getLoad();
		}
		for (EssNature entry : essNatures) {
			if (entry instanceof SymmetricEssNature) {
				SymmetricEssNature ess = (SymmetricEssNature) entry;
				activePower -= ess.activePower().valueOptional().orElse(0L);
				reactivePower -= ess.reactivePower().valueOptional().orElse(0L);
			} else if (entry instanceof AsymmetricEssNature) {
				AsymmetricEssNature ess = (AsymmetricEssNature) entry;
				activePower -= ess.activePowerL1().valueOptional().orElse(0L)
						+ ess.activePowerL2().valueOptional().orElse(0L)
						+ ess.activePowerL3().valueOptional().orElse(0L);
				reactivePower -= ess.reactivePowerL1().valueOptional().orElse(0L)
						+ ess.reactivePowerL2().valueOptional().orElse(0L)
						+ ess.reactivePowerL3().valueOptional().orElse(0L);
			}
		}
		for (MeterNature entry : meterNatures) {
			if (entry instanceof SymmetricMeterNature) {
				SymmetricMeterNature meter = (SymmetricMeterNature) entry;
				activePower -= meter.activePower().valueOptional().orElse(0L);
				reactivePower -= meter.reactivePower().valueOptional().orElse(0L);
			} else if (entry instanceof AsymmetricMeterNature) {
				AsymmetricMeterNature meter = (AsymmetricMeterNature) entry;
				activePower += meter.activePowerL1().valueOptional().orElse(0L)
						+ meter.activePowerL2().valueOptional().orElse(0L)
						+ meter.activePowerL3().valueOptional().orElse(0L);
				reactivePower += meter.reactivePowerL1().valueOptional().orElse(0L)
						+ meter.reactivePowerL2().valueOptional().orElse(0L)
						+ meter.reactivePowerL3().valueOptional().orElse(0L);
			}
		}
		if (isOffGrid(essNatures)) {
			this.activePower.updateValue(null);
			this.reactivePower.updateValue(null);
		} else {
			this.activePower.updateValue(activePower);
			this.reactivePower.updateValue(reactivePower);
		}
	}

	private boolean isOffGrid(List<EssNature> esss) {
		for (EssNature ess : esss) {
			if (ess.gridMode().labelOptional().equals(Optional.of(EssNature.OFF_GRID))) {
				return true;
			}
		}
		return false;
	}

}
