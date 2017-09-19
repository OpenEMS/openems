package io.openems.impl.device.system.asymmetricsymmetriccombinationess;

import java.util.Optional;

import io.openems.api.bridge.BridgeEvent;
import io.openems.api.bridge.BridgeEvent.Position;
import io.openems.api.bridge.BridgeEventListener;
import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.FunctionalReadChannel;
import io.openems.api.channel.FunctionalReadChannelFunction;
import io.openems.api.channel.FunctionalWriteChannel;
import io.openems.api.channel.FunctionalWriteChannelFunction;
import io.openems.api.channel.ProxyReadChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StatusBitChannels;
import io.openems.api.channel.WriteChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.api.thing.Thing;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.ControllerUtils;
import io.openems.impl.protocol.system.SystemDeviceNature;

public class AsymmetricSymmetricCombinationEssNature extends SystemDeviceNature
		implements SymmetricEssNature, AsymmetricEssNature, ChannelChangeListener {

	private ConfigChannel<Integer> minSoc = new ConfigChannel<>("minSoc", this);
	private ConfigChannel<Integer> chargeSoc = new ConfigChannel<Integer>("chargeSoc", this);

	@ChannelInfo(title = "Ess", description = "Sets the ess device for the combinationEss.", type = String.class)
	public ConfigChannel<String> ess = new ConfigChannel<String>("ess", this).addChangeListener(this);
	private AsymmetricEssNature essNature;
	private ThingRepository repo;
	private BridgeEventListener bridgeEventListener;

	private ProxyReadChannel<Long> gridMode = new ProxyReadChannel<>("GridMode", this);
	private ProxyReadChannel<Long> soc = new ProxyReadChannel<>("Soc", this);
	private FunctionalReadChannel<Long> allowedCharge = new FunctionalReadChannel<Long>("AllowedCharge", this,
			new FunctionalReadChannelFunction<Long>() {

				@Override
				public Long handle(ReadChannel<Long>... channels) {
					if (channels.length > 0) {
						return channels[0].valueOptional().orElse(null);
					} else {
						return null;
					}
				}

			}).unit("W");
	private FunctionalReadChannel<Long> allowedDischarge = new FunctionalReadChannel<Long>("AllowedDischarge", this,
			new FunctionalReadChannelFunction<Long>() {

				@Override
				public Long handle(ReadChannel<Long>... channels) {
					if (channels.length > 0) {
						return channels[0].valueOptional().orElse(null);
					} else {
						return null;
					}
				}

			}).unit("W");
	private FunctionalReadChannel<Long> allowedApparent = new FunctionalReadChannel<Long>("AllowedApparent", this,
			new FunctionalReadChannelFunction<Long>() {

				@Override
				public Long handle(ReadChannel<Long>... channels) {
					if (channels.length > 0) {
						return channels[0].valueOptional().orElse(null);
					} else {
						return null;
					}
				}

			}).unit("VA");
	private ProxyReadChannel<Long> systemState = new ProxyReadChannel<Long>("SystemState", this);
	private ProxyReadChannel<Long> capacity = new ProxyReadChannel<Long>("Capacity", this);
	private ProxyReadChannel<Long> maxNominalPower = new ProxyReadChannel<Long>("MaxNominalPower", this);
	private ProxyReadChannel<Long> activePowerL1 = new ProxyReadChannel<Long>("ActivePowerL1", this);
	private ProxyReadChannel<Long> activePowerL2 = new ProxyReadChannel<Long>("ActivePowerL2", this);
	private ProxyReadChannel<Long> activePowerL3 = new ProxyReadChannel<Long>("ActivePowerL3", this);
	private ProxyReadChannel<Long> reactivePowerL1 = new ProxyReadChannel<Long>("ReactivePowerL1", this);
	private ProxyReadChannel<Long> reactivePowerL2 = new ProxyReadChannel<Long>("ReactivePowerL2", this);
	private ProxyReadChannel<Long> reactivePowerL3 = new ProxyReadChannel<Long>("ReactivePowerL3", this);
	private FunctionalReadChannel<Long> activePower = new FunctionalReadChannel<Long>("ActivePower", this,
			new FunctionalReadChannelFunction<Long>() {

				@Override
				public Long handle(ReadChannel<Long>... channels) throws InvalidValueException {
					long sum = 0;
					for (ReadChannel<Long> channel : channels) {
						sum += channel.value();
					}
					return sum;
				}

			}, activePowerL1, activePowerL2, activePowerL3);
	private FunctionalReadChannel<Long> reactivePower = new FunctionalReadChannel<Long>("ReactivePower", this,
			new FunctionalReadChannelFunction<Long>() {

				@Override
				public Long handle(ReadChannel<Long>... channels) throws InvalidValueException {
					long sum = 0;
					for (ReadChannel<Long> channel : channels) {
						sum += channel.value();
					}
					return sum;
				}

			}, reactivePowerL1, reactivePowerL2, reactivePowerL3);
	private FunctionalReadChannel<Long> apparentPower = new FunctionalReadChannel<Long>("ApparentPower", this,
			new FunctionalReadChannelFunction<Long>() {

				@Override
				public Long handle(ReadChannel<Long>... channels) {
					long sum = 0L;
					try {
						sum = ControllerUtils.calculateApparentPower(channels[0].value(), channels[1].value());
					} catch (InvalidValueException e) {
						log.error("Can't read values of " + ess.id(), e);
					}
					return sum;
				}

			}, activePower, reactivePower).unit("VA");
	private FunctionalWriteChannel<Long> setActivePowerL1 = new FunctionalWriteChannel<Long>("SetActivePowerL1", this,
			new FunctionalWriteChannelFunction<Long>() {

				@Override
				public void setValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

				@Override
				public Long getValue(ReadChannel<Long>... channels) {
					return channels[0].valueOptional().orElse(null);
				}

				@Override
				public Long getMinValue(WriteChannel<Long>... channels) {
					Long minActivePower = getMinActivePower();
					if (minActivePower == null) {
						return null;
					}
					return minActivePower / 3;
				}

				@Override
				public Long getMaxValue(WriteChannel<Long>... channels) {
					Long maxActivePower = getMaxActivePower();
					if (maxActivePower == null) {
						return null;
					}
					return maxActivePower / 3;
				}

				@Override
				public void setMinValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

				@Override
				public void setMaxValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

			});
	private FunctionalWriteChannel<Long> setActivePowerL2 = new FunctionalWriteChannel<Long>("SetActivePowerL2", this,
			new FunctionalWriteChannelFunction<Long>() {

				@Override
				public void setValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

				@Override
				public Long getValue(ReadChannel<Long>... channels) {
					return channels[0].valueOptional().orElse(null);
				}

				@Override
				public Long getMinValue(WriteChannel<Long>... channels) {
					Long minActivePower = getMinActivePower();
					if (minActivePower == null) {
						return null;
					}
					return minActivePower / 3;
				}

				@Override
				public Long getMaxValue(WriteChannel<Long>... channels) {
					Long maxActivePower = getMaxActivePower();
					if (maxActivePower == null) {
						return null;
					}
					return maxActivePower / 3;
				}

				@Override
				public void setMinValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

				@Override
				public void setMaxValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

			});
	private FunctionalWriteChannel<Long> setActivePowerL3 = new FunctionalWriteChannel<Long>("SetActivePowerL3", this,
			new FunctionalWriteChannelFunction<Long>() {

				@Override
				public void setValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

				@Override
				public Long getValue(ReadChannel<Long>... channels) {
					return channels[0].valueOptional().orElse(null);
				}

				@Override
				public Long getMinValue(WriteChannel<Long>... channels) {
					Long minActivePower = getMinActivePower();
					if (minActivePower == null) {
						return null;
					}
					return minActivePower / 3;
				}

				@Override
				public Long getMaxValue(WriteChannel<Long>... channels) {
					Long maxActivePower = getMaxActivePower();
					if (maxActivePower == null) {
						return null;
					}
					return maxActivePower / 3;
				}

				@Override
				public void setMinValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

				@Override
				public void setMaxValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

			});
	private FunctionalWriteChannel<Long> setActivePower = new FunctionalWriteChannel<Long>("SetActivePower", this,
			new FunctionalWriteChannelFunction<Long>() {

				@Override
				public void setValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

				@Override
				public Long getValue(ReadChannel<Long>... channels) {
					Long sum = 0L;
					for (ReadChannel<Long> channel : channels) {
						if (channel == null || !channel.valueOptional().isPresent()) {
							return null;
						}
						sum += channel.valueOptional().get();
					}
					return sum;
				}

				@Override
				public Long getMinValue(WriteChannel<Long>... channels) {
					Long minActivePower = getMinActivePower();
					if (minActivePower == null) {
						return null;
					}
					return minActivePower;
				}

				@Override
				public Long getMaxValue(WriteChannel<Long>... channels) {
					Long maxActivePower = getMaxActivePower();
					if (maxActivePower == null) {
						return null;
					}
					return maxActivePower;
				}

				@Override
				public void setMinValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

				@Override
				public void setMaxValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

			}, setActivePowerL1, setActivePowerL2, setActivePowerL3);
	private FunctionalWriteChannel<Long> setReactivePowerL1 = new FunctionalWriteChannel<Long>("SetReactivePowerL1",
			this, new FunctionalWriteChannelFunction<Long>() {

				@Override
				public void setValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

				@Override
				public Long getValue(ReadChannel<Long>... channels) {
					return channels[0].valueOptional().orElse(null);
				}

				@Override
				public Long getMinValue(WriteChannel<Long>... channels) {
					Long minReactivePower = getMinReactivePower();
					if (minReactivePower == null) {
						return null;
					}
					return minReactivePower / 3;
				}

				@Override
				public Long getMaxValue(WriteChannel<Long>... channels) {
					Long maxReactivePower = getMaxReactivePower();
					if (maxReactivePower == null) {
						return null;
					}
					return maxReactivePower / 3;
				}

				@Override
				public void setMinValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

				@Override
				public void setMaxValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

			});
	private FunctionalWriteChannel<Long> setReactivePowerL2 = new FunctionalWriteChannel<Long>("SetReactivePowerL2",
			this, new FunctionalWriteChannelFunction<Long>() {

				@Override
				public void setValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

				@Override
				public Long getValue(ReadChannel<Long>... channels) {
					return channels[0].valueOptional().orElse(null);
				}

				@Override
				public Long getMinValue(WriteChannel<Long>... channels) {
					Long minReactivePower = getMinReactivePower();
					if (minReactivePower == null) {
						return null;
					}
					return minReactivePower / 3;
				}

				@Override
				public Long getMaxValue(WriteChannel<Long>... channels) {
					Long maxReactivePower = getMaxReactivePower();
					if (maxReactivePower == null) {
						return null;
					}
					return maxReactivePower / 3;
				}

				@Override
				public void setMinValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

				@Override
				public void setMaxValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

			});
	private FunctionalWriteChannel<Long> setReactivePowerL3 = new FunctionalWriteChannel<Long>("SetReactivePowerL3",
			this, new FunctionalWriteChannelFunction<Long>() {

				@Override
				public void setValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

				@Override
				public Long getValue(ReadChannel<Long>... channels) {
					return channels[0].valueOptional().orElse(null);
				}

				@Override
				public Long getMinValue(WriteChannel<Long>... channels) {
					Long minReactivePower = getMinReactivePower();
					if (minReactivePower == null) {
						return null;
					}
					return minReactivePower / 3;
				}

				@Override
				public Long getMaxValue(WriteChannel<Long>... channels) {
					Long maxReactivePower = getMaxReactivePower();
					if (maxReactivePower == null) {
						return null;
					}
					return maxReactivePower / 3;
				}

				@Override
				public void setMinValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

				@Override
				public void setMaxValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

			});
	private FunctionalWriteChannel<Long> setReactivePower = new FunctionalWriteChannel<Long>("SetReactivePower", this,
			new FunctionalWriteChannelFunction<Long>() {

				@Override
				public void setValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

				@Override
				public Long getValue(ReadChannel<Long>... channels) {
					Long sum = 0L;
					for (ReadChannel<Long> channel : channels) {
						if (channel == null || !channel.valueOptional().isPresent()) {
							return null;
						}
						sum += channel.valueOptional().get();
					}
					return sum;
				}

				@Override
				public Long getMinValue(WriteChannel<Long>... channels) {
					Long minReactivePower = getMinReactivePower();
					if (minReactivePower == null) {
						return null;
					}
					return minReactivePower;
				}

				@Override
				public Long getMaxValue(WriteChannel<Long>... channels) {
					Long maxReactivePower = getMaxReactivePower();
					if (maxReactivePower == null) {
						return null;
					}
					return maxReactivePower;
				}

				@Override
				public void setMinValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

				@Override
				public void setMaxValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {}

			}, setReactivePowerL1, setReactivePowerL2, setReactivePowerL3);
	private FunctionalWriteChannel<Long> setWorkState = new FunctionalWriteChannel<Long>("SetWorkState", this,
			new FunctionalWriteChannelFunction<Long>() {

				@Override
				public void setValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {
					channels[0].pushWrite(newValue);
				}

				@Override
				public Long getValue(ReadChannel<Long>... channels) {
					return channels[0].valueOptional().orElse(null);
				}

				@Override
				public Long getMinValue(WriteChannel<Long>... channels) {
					return channels[0].writeMin().orElse(null);
				}

				@Override
				public Long getMaxValue(WriteChannel<Long>... channels) {
					return channels[0].writeMax().orElse(null);
				}

				@Override
				public void setMinValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {
					channels[0].pushWriteMin(newValue);
				}

				@Override
				public void setMaxValue(Long newValue, String newLabel, WriteChannel<Long>... channels)
						throws WriteChannelException {
					channels[0].pushWriteMax(newValue);
				}

			});

	public AsymmetricSymmetricCombinationEssNature(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
		this.repo = ThingRepository.getInstance();
		this.bridgeEventListener = new BridgeEventListener() {

			@Override
			protected void notify(BridgeEvent event) {
				if (event.getPosition().equals(Position.BEFOREWRITE)) {
					// TODO calculate power
				}
			}
		};
	}

	@Override
	public ConfigChannel<Integer> minSoc() {
		return minSoc;
	}

	@Override
	public ConfigChannel<Integer> chargeSoc() {
		return chargeSoc;
	}

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
	public ReadChannel<Long> allowedApparent() {
		return allowedApparent;
	}

	@Override
	public ReadChannel<Long> capacity() {
		return capacity;
	}

	@Override
	public ReadChannel<Long> maxNominalPower() {
		return maxNominalPower;
	}

	@Override
	public StatusBitChannels warning() {
		return null;
	}

	@Override
	public WriteChannel<Long> setWorkState() {
		return setWorkState;
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
	protected void update() {
		// TODO Auto-generated method stub

	}

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		if (channel.equals(ess)) {
			loadEss();
		}
	}

	private void loadEss() {
		String essId;
		try {
			essId = ess.value();
			synchronized (essNature) {
				// remove old ess
				if (essNature != null) {
					essNature.getParent().getBridge().removeListener(bridgeEventListener);
					gridMode.removeChannel(essNature.gridMode());
					soc.removeChannel(essNature.soc());
					allowedApparent.removeChannel(essNature.allowedApparent());
					allowedCharge.removeChannel(essNature.allowedCharge());
					allowedDischarge.removeChannel(essNature.allowedDischarge());
					systemState.removeChannel(essNature.systemState());
					capacity.removeChannel(essNature.capacity());
					maxNominalPower.removeChannel(essNature.maxNominalPower());
					activePowerL1.removeChannel(essNature.activePowerL1());
					activePowerL2.removeChannel(essNature.activePowerL2());
					activePowerL3.removeChannel(essNature.activePowerL3());
					reactivePowerL1.removeChannel(essNature.reactivePowerL1());
					reactivePowerL2.removeChannel(essNature.reactivePowerL2());
					reactivePowerL3.removeChannel(essNature.reactivePowerL3());
					setActivePowerL1.removeChannel(essNature.setActivePowerL1());
					setActivePowerL2.removeChannel(essNature.setActivePowerL2());
					setActivePowerL3.removeChannel(essNature.setActivePowerL3());
					setReactivePowerL1.removeChannel(essNature.setReactivePowerL1());
					setReactivePowerL2.removeChannel(essNature.setReactivePowerL2());
					setReactivePowerL3.removeChannel(essNature.setReactivePowerL3());
					setWorkState.removeChannel(essNature.setWorkState());
				}
				essNature = null;
				if (essId != null) {
					Optional<Thing> nature = repo.getThingById(essId);
					if (nature.isPresent()) {
						if (nature.get() instanceof AsymmetricEssNature) {
							AsymmetricEssNature ess = (AsymmetricEssNature) nature.get();
							essNature = ess;
							// Add channels to functionalChannels
							if (essNature != null) {
								gridMode.setChannel(essNature.gridMode());
								soc.setChannel(essNature.soc());
								allowedApparent.addChannel(essNature.allowedApparent());
								allowedDischarge.addChannel(essNature.allowedDischarge());
								allowedCharge.addChannel(essNature.allowedCharge());
								systemState.setChannel(essNature.systemState());
								capacity.setChannel(essNature.capacity());
								maxNominalPower.setChannel(essNature.maxNominalPower());
								activePowerL1.setChannel(essNature.activePowerL1());
								activePowerL2.setChannel(essNature.activePowerL2());
								activePowerL3.setChannel(essNature.activePowerL3());
								reactivePowerL1.setChannel(essNature.reactivePowerL1());
								reactivePowerL2.setChannel(essNature.reactivePowerL2());
								reactivePowerL3.setChannel(essNature.reactivePowerL3());
								setActivePowerL1.addChannel(essNature.setActivePowerL1());
								setActivePowerL2.addChannel(essNature.setActivePowerL2());
								setActivePowerL3.addChannel(essNature.setActivePowerL3());
								setReactivePowerL1.addChannel(essNature.setReactivePowerL1());
								setReactivePowerL2.addChannel(essNature.setReactivePowerL2());
								setReactivePowerL3.addChannel(essNature.setReactivePowerL3());
								setWorkState.addChannel(essNature.setWorkState());
								essNature.getParent().getBridge().addListener(bridgeEventListener);
							}
						} else {
							log.error("ThingID: " + essId + " is no AsymmetricEss!");
						}
					} else {
						log.warn("meter: " + essId + " not found!");
					}
				}
			}
		} catch (InvalidValueException e) {
			log.error("esss value is invalid!", e);
		}
	}

	private Long getMinActivePower() {
		setActivePowerL1.getWriteValue().isPresent();
		setActivePowerL1.writeMin().isPresent();
	}

	private Long getMaxActivePower() {

	}

	private Long getMinReactivePower() {

	}

	private Long getMaxReactivePower() {

	}

}
