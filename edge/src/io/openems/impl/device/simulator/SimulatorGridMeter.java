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
import io.openems.api.device.Device;
import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.device.nature.meter.AsymmetricMeterNature;
import io.openems.api.device.nature.meter.MeterNature;
import io.openems.api.device.nature.meter.SymmetricMeterNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.thing.Thing;
import io.openems.core.ThingRepository;
import io.openems.core.ThingsChangedListener;
import io.openems.core.utilities.ControllerUtils;
import io.openems.impl.protocol.simulator.SimulatorReadChannel;

@ThingInfo(title = "Simulated Grid Meter")
public class SimulatorGridMeter extends SimulatorMeter implements ChannelChangeListener, AsymmetricMeterNature {

	private SimulatorReadChannel<Long> activePower = new SimulatorReadChannel<>("ActivePower", this);
	private SimulatorReadChannel<Long> activePowerL1 = new SimulatorReadChannel<>("ActivePowerL1", this);
	private SimulatorReadChannel<Long> activePowerL2 = new SimulatorReadChannel<>("ActivePowerL2", this);
	private SimulatorReadChannel<Long> activePowerL3 = new SimulatorReadChannel<>("ActivePowerL3", this);
	private FunctionalReadChannel<Long> apparentPower;
	private SimulatorReadChannel<Long> reactivePower = new SimulatorReadChannel<>("ReactivePower", this);
	private SimulatorReadChannel<Long> reactivePowerL1 = new SimulatorReadChannel<>("ReactivePowerL1", this);
	private SimulatorReadChannel<Long> reactivePowerL2 = new SimulatorReadChannel<>("ReactivePowerL2", this);
	private SimulatorReadChannel<Long> reactivePowerL3 = new SimulatorReadChannel<>("ReactivePowerL3", this);
	@ChannelInfo(title = "esss", type = JsonArray.class)
	public ConfigChannel<JsonArray> esss = new ConfigChannel<JsonArray>("esss", this).addChangeListener(this);
	@ChannelInfo(title = "producer", type = JsonArray.class)
	public ConfigChannel<JsonArray> producer = new ConfigChannel<JsonArray>("producer", this).addChangeListener(this);
	@ChannelInfo(title = "ActivePowerGeneratorConfig", type = JsonObject.class)
	public ConfigChannel<JsonObject> activePowerGeneratorConfig = new ConfigChannel<JsonObject>(
			"activePowerGeneratorConfig", this).addChangeListener(this);
	@ChannelInfo(title = "ReactivePowerGeneratorConfig", type = JsonObject.class)
	public ConfigChannel<JsonObject> reactivePowerGeneratorConfig = new ConfigChannel<JsonObject>(
			"reactivePowerGeneratorConfig", this).addChangeListener(this);

	private ThingRepository repo = ThingRepository.getInstance();
	private List<EssNature> essNatures = new ArrayList<>();
	private List<MeterNature> meterNatures = new ArrayList<>();
	private LoadGenerator activePowerLoad;
	private LoadGenerator reactivePowerLoad;

	public SimulatorGridMeter(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
		repo.addThingChangedListener(new ThingsChangedListener() {

			@Override
			public void thingChanged(Thing thing, Action action) {
				if (esss.valueOptional().isPresent()) {
					JsonArray ids = esss.valueOptional().get();
					for (JsonElement id : ids) {
						if (id.getAsString().equals(thing.id())) {
							getEssNatures();
						}
					}
				}
				if (producer.valueOptional().isPresent()) {
					JsonArray ids = producer.valueOptional().get();
					for (JsonElement id : ids) {
						if (id.getAsString().equals(thing.id())) {
							getMeterNatures();
						}
					}
				}
			}
		});
		this.apparentPower = new FunctionalReadChannel<Long>("ApparentPower", this, (channels) -> {
			return ControllerUtils.calculateApparentPower(channels[0].valueOptional().orElse(0L),
					channels[1].valueOptional().orElse(0L));
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

	private double factorL1 = 0.5;
	private double factorL2 = 0.2;
	private double factorL3 = 0.3;

	@Override
	protected void update() {
		super.update();
		long activePower = 0;
		long activePowerL1 = 0;
		long activePowerL2 = 0;
		long activePowerL3 = 0;
		if (activePowerLoad != null) {
			factorL1 = SimulatorTools.addRandomDouble(factorL1, 0, 1, 0.01);
			factorL2 = SimulatorTools.addRandomDouble(factorL2, 0, 1 - factorL1, 0.01);
			factorL3 = 1 - factorL1 - factorL2;
			long load = activePowerLoad.getLoad();
			activePower = load;
			activePowerL1 = (long) (load * factorL1);
			activePowerL2 = (long) (load * factorL2);
			activePowerL3 = (long) (load * factorL3);
		}
		long reactivePower = 0;
		long reactivePowerL1 = 0;
		long reactivePowerL2 = 0;
		long reactivePowerL3 = 0;
		if (reactivePowerLoad != null) {
			reactivePower = reactivePowerLoad.getLoad();
			reactivePowerL1 = reactivePower / 3;
			reactivePowerL2 = reactivePower / 3;
			reactivePowerL3 = reactivePower / 3;
		}
		for (EssNature entry : essNatures) {
			if (entry instanceof SymmetricEssNature) {
				SymmetricEssNature ess = (SymmetricEssNature) entry;
				activePower -= ess.activePower().valueOptional().orElse(0L);
				activePowerL1 -= ess.activePower().valueOptional().orElse(0L) / 3;
				activePowerL2 -= ess.activePower().valueOptional().orElse(0L) / 3;
				activePowerL3 -= ess.activePower().valueOptional().orElse(0L) / 3;
				reactivePower -= ess.reactivePower().valueOptional().orElse(0L);
				reactivePowerL1 -= ess.reactivePower().valueOptional().orElse(0L) / 3;
				reactivePowerL2 -= ess.reactivePower().valueOptional().orElse(0L) / 3;
				reactivePowerL3 -= ess.reactivePower().valueOptional().orElse(0L) / 3;
			} else if (entry instanceof AsymmetricEssNature) {
				AsymmetricEssNature ess = (AsymmetricEssNature) entry;
				activePower -= ess.activePowerL1().valueOptional().orElse(0L)
						+ ess.activePowerL2().valueOptional().orElse(0L)
						+ ess.activePowerL3().valueOptional().orElse(0L);
				activePowerL1 -= ess.activePowerL1().valueOptional().orElse(0L);
				activePowerL2 -= ess.activePowerL2().valueOptional().orElse(0L);
				activePowerL3 -= ess.activePowerL3().valueOptional().orElse(0L);
				reactivePower -= ess.reactivePowerL1().valueOptional().orElse(0L)
						+ ess.reactivePowerL2().valueOptional().orElse(0L)
						+ ess.reactivePowerL3().valueOptional().orElse(0L);
				reactivePowerL1 -= ess.reactivePowerL1().valueOptional().orElse(0L);
				reactivePowerL2 -= ess.reactivePowerL2().valueOptional().orElse(0L);
				reactivePowerL3 -= ess.reactivePowerL3().valueOptional().orElse(0L);
			}
		}
		for (MeterNature entry : meterNatures) {
			if (entry instanceof SymmetricMeterNature) {
				SymmetricMeterNature meter = (SymmetricMeterNature) entry;
				activePower -= meter.activePower().valueOptional().orElse(0L);
				activePowerL1 -= meter.activePower().valueOptional().orElse(0L) / 3;
				activePowerL2 -= meter.activePower().valueOptional().orElse(0L) / 3;
				activePowerL3 -= meter.activePower().valueOptional().orElse(0L) / 3;
				reactivePower -= meter.reactivePower().valueOptional().orElse(0L);
				reactivePowerL1 -= meter.reactivePower().valueOptional().orElse(0L) / 3;
				reactivePowerL2 -= meter.reactivePower().valueOptional().orElse(0L) / 3;
				reactivePowerL3 -= meter.reactivePower().valueOptional().orElse(0L) / 3;
			} else if (entry instanceof AsymmetricMeterNature) {
				AsymmetricMeterNature meter = (AsymmetricMeterNature) entry;
				activePower -= meter.activePowerL1().valueOptional().orElse(0L)
						+ meter.activePowerL2().valueOptional().orElse(0L)
						+ meter.activePowerL3().valueOptional().orElse(0L);
				activePowerL1 -= meter.activePowerL1().valueOptional().orElse(0L);
				activePowerL2 -= meter.activePowerL2().valueOptional().orElse(0L);
				activePowerL3 -= meter.activePowerL3().valueOptional().orElse(0L);
				reactivePower -= meter.reactivePowerL1().valueOptional().orElse(0L)
						+ meter.reactivePowerL2().valueOptional().orElse(0L)
						+ meter.reactivePowerL3().valueOptional().orElse(0L);
				reactivePowerL1 -= meter.reactivePowerL1().valueOptional().orElse(0L);
				reactivePowerL2 -= meter.reactivePowerL2().valueOptional().orElse(0L);
				reactivePowerL3 -= meter.reactivePowerL3().valueOptional().orElse(0L);
			}
		}
		if (isOffGrid(essNatures)) {
			this.activePower.updateValue(null);
			this.activePowerL1.updateValue(null);
			this.activePowerL2.updateValue(null);
			this.activePowerL3.updateValue(null);
			this.reactivePower.updateValue(null);
			this.reactivePowerL1.updateValue(null);
			this.reactivePowerL2.updateValue(null);
			this.reactivePowerL3.updateValue(null);
		} else {
			this.activePower.updateValue(activePower);
			this.activePowerL1.updateValue(activePowerL1);
			this.activePowerL2.updateValue(activePowerL2);
			this.activePowerL3.updateValue(activePowerL3);
			this.reactivePower.updateValue(reactivePower);
			this.reactivePowerL1.updateValue(reactivePowerL1);
			this.reactivePowerL2.updateValue(reactivePowerL2);
			this.reactivePowerL3.updateValue(reactivePowerL3);
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReadChannel<Long> currentL2() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReadChannel<Long> currentL3() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReadChannel<Long> voltageL1() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReadChannel<Long> voltageL2() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReadChannel<Long> voltageL3() {
		// TODO Auto-generated method stub
		return null;
	}

}