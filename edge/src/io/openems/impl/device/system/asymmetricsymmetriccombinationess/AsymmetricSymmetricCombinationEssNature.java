package io.openems.impl.device.system.asymmetricsymmetriccombinationess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

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
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.api.thing.Thing;
import io.openems.api.thing.ThingChannelsUpdatedListener;
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
	private List<ThingChannelsUpdatedListener> listeners;

	private ProxyReadChannel<Long> gridMode = new ProxyReadChannel<>("GridMode", this);
	private ProxyReadChannel<Long> soc = new ProxyReadChannel<>("Soc", this);
	private ProxyReadChannel<Long> allowedCharge = new ProxyReadChannel<Long>("AllowedCharge", this);
	private ProxyReadChannel<Long> allowedDischarge = new ProxyReadChannel<Long>("AllowedDischarge", this);
	private ProxyReadChannel<Long> allowedApparent = new ProxyReadChannel<Long>("AllowedApparent", this);
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
				public Long handle(@SuppressWarnings("unchecked") ReadChannel<Long>... channels)
						throws InvalidValueException {
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
				public Long handle(@SuppressWarnings("unchecked") ReadChannel<Long>... channels)
						throws InvalidValueException {
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
				public Long handle(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) {
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
				public Long setValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 1) {
						channels[0].checkIntervalBoundaries(newValue);
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

				@Override
				public Long getValue(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) {
					return channels[0].valueOptional().orElse(null);
				}

				@Override
				public Long getMinValue(Optional<Long> minValue,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					return getMinActivePowerPhase(1, minValue);
				}

				@Override
				public Long getMaxValue(Optional<Long> maxValue,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					return getMaxActivePowerPhase(1, maxValue);
				}

				@Override
				public Long setMinValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 1) {
						channels[0].checkIntervalBoundaries(newValue);
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

				@Override
				public Long setMaxValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 1) {
						channels[0].checkIntervalBoundaries(newValue);
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

			});
	private FunctionalWriteChannel<Long> setActivePowerL2 = new FunctionalWriteChannel<Long>("SetActivePowerL2", this,
			new FunctionalWriteChannelFunction<Long>() {

				@Override
				public Long setValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 1) {
						channels[0].checkIntervalBoundaries(newValue);
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

				@Override
				public Long getValue(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) {
					return channels[0].valueOptional().orElse(null);
				}

				@Override
				public Long getMinValue(Optional<Long> minValue,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					return getMinActivePowerPhase(2, minValue);
				}

				@Override
				public Long getMaxValue(Optional<Long> maxValue,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					return getMaxActivePowerPhase(2, maxValue);
				}

				@Override
				public Long setMinValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 1) {
						channels[0].checkIntervalBoundaries(newValue);
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

				@Override
				public Long setMaxValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 1) {
						channels[0].checkIntervalBoundaries(newValue);
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

			});
	private FunctionalWriteChannel<Long> setActivePowerL3 = new FunctionalWriteChannel<Long>("SetActivePowerL3", this,
			new FunctionalWriteChannelFunction<Long>() {

				@Override
				public Long setValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 1) {
						channels[0].checkIntervalBoundaries(newValue);
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

				@Override
				public Long getValue(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) {
					return channels[0].valueOptional().orElse(null);
				}

				@Override
				public Long getMinValue(Optional<Long> minValue,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					return getMinActivePowerPhase(3, minValue);
				}

				@Override
				public Long getMaxValue(Optional<Long> maxValue,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					return getMaxActivePowerPhase(3, maxValue);
				}

				@Override
				public Long setMinValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 1) {
						channels[0].checkIntervalBoundaries(newValue);
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

				@Override
				public Long setMaxValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 1) {
						channels[0].checkIntervalBoundaries(newValue);
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

			});

	private FunctionalWriteChannel<Long> setActivePower = new FunctionalWriteChannel<Long>("SetActivePower", this,
			new FunctionalWriteChannelFunction<Long>() {

				@Override
				public Long setValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 3) {
						for (int i = 0; i < 3; i++) {
							channels[i].checkIntervalBoundaries(newValue / 3);
						}
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

				@Override
				public Long getValue(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) {
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
				public Long getMinValue(Optional<Long> minValue,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					Long minActivePower = getMinActivePower();
					if (minActivePower == null) {
						return null;
					}
					return minActivePower;
				}

				@Override
				public Long getMaxValue(Optional<Long> maxValue,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					Long maxActivePower = getMaxActivePower();
					if (maxActivePower == null) {
						return null;
					}
					return maxActivePower;
				}

				@Override
				public Long setMinValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 3) {
						for (int i = 0; i < 3; i++) {
							channels[i].checkIntervalBoundaries(newValue / 3);
						}
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

				@Override
				public Long setMaxValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 3) {
						for (int i = 0; i < 3; i++) {
							channels[i].checkIntervalBoundaries(newValue / 3);
						}
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

			});
	private FunctionalWriteChannel<Long> setReactivePowerL1 = new FunctionalWriteChannel<Long>("SetReactivePowerL1",
			this, new FunctionalWriteChannelFunction<Long>() {

				@Override
				public Long setValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 3) {
						for (int i = 0; i < 3; i++) {
							channels[i].checkIntervalBoundaries(newValue / 3);
						}
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

				@Override
				public Long getValue(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) {
					return channels[0].valueOptional().orElse(null);
				}

				@Override
				public Long getMinValue(Optional<Long> minValue,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					Long minReactivePower = getMinReactivePowerPhase(1, minValue);
					if (minReactivePower == null) {
						return null;
					}
					return minReactivePower / 3;
				}

				@Override
				public Long getMaxValue(Optional<Long> maxValue,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					Long maxReactivePower = getMaxReactivePowerPhase(1, maxValue);
					if (maxReactivePower == null) {
						return null;
					}
					return maxReactivePower / 3;
				}

				@Override
				public Long setMinValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 1) {
						channels[0].checkIntervalBoundaries(newValue);
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

				@Override
				public Long setMaxValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 1) {
						channels[0].checkIntervalBoundaries(newValue);
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

			});
	private FunctionalWriteChannel<Long> setReactivePowerL2 = new FunctionalWriteChannel<Long>("SetReactivePowerL2",
			this, new FunctionalWriteChannelFunction<Long>() {

				@Override
				public Long setValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 1) {
						channels[0].checkIntervalBoundaries(newValue);
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

				@Override
				public Long getValue(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) {
					return channels[0].valueOptional().orElse(null);
				}

				@Override
				public Long getMinValue(Optional<Long> minValue,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					Long minReactivePower = getMinReactivePowerPhase(2, minValue);
					if (minReactivePower == null) {
						return null;
					}
					return minReactivePower / 3;
				}

				@Override
				public Long getMaxValue(Optional<Long> maxValue,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					Long maxReactivePower = getMaxReactivePowerPhase(2, maxValue);
					if (maxReactivePower == null) {
						return null;
					}
					return maxReactivePower / 3;
				}

				@Override
				public Long setMinValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 1) {
						channels[0].checkIntervalBoundaries(newValue);
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

				@Override
				public Long setMaxValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 1) {
						channels[0].checkIntervalBoundaries(newValue);
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

			});
	private FunctionalWriteChannel<Long> setReactivePowerL3 = new FunctionalWriteChannel<Long>("SetReactivePowerL3",
			this, new FunctionalWriteChannelFunction<Long>() {

				@Override
				public Long setValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 1) {
						channels[0].checkIntervalBoundaries(newValue);
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

				@Override
				public Long getValue(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) {
					return channels[0].valueOptional().orElse(null);
				}

				@Override
				public Long getMinValue(Optional<Long> minValue,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					Long minReactivePower = getMinReactivePowerPhase(3, minValue);
					if (minReactivePower == null) {
						return null;
					}
					return minReactivePower / 3;
				}

				@Override
				public Long getMaxValue(Optional<Long> maxValue,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					Long maxReactivePower = getMaxReactivePowerPhase(3, maxValue);
					if (maxReactivePower == null) {
						return null;
					}
					return maxReactivePower / 3;
				}

				@Override
				public Long setMinValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 1) {
						channels[0].checkIntervalBoundaries(newValue);
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

				@Override
				public Long setMaxValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 1) {
						channels[0].checkIntervalBoundaries(newValue);
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

			});
	private FunctionalWriteChannel<Long> setReactivePower = new FunctionalWriteChannel<Long>("SetReactivePower", this,
			new FunctionalWriteChannelFunction<Long>() {

				@Override
				public Long setValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 3) {
						for (int i = 0; i < 3; i++) {
							channels[i].checkIntervalBoundaries(newValue / 3);
						}
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

				@Override
				public Long getValue(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) {
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
				public Long getMinValue(Optional<Long> minValue,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					Long minActivePower = getMinReactivePower();
					if (minActivePower == null) {
						return null;
					}
					return minActivePower;
				}

				@Override
				public Long getMaxValue(Optional<Long> maxValue,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					Long maxActivePower = getMaxReactivePower();
					if (maxActivePower == null) {
						return null;
					}
					return maxActivePower;
				}

				@Override
				public Long setMinValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 3) {
						for (int i = 0; i < 3; i++) {
							channels[i].checkIntervalBoundaries(newValue / 3);
						}
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

				@Override
				public Long setMaxValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (channels.length == 3) {
						for (int i = 0; i < 3; i++) {
							channels[i].checkIntervalBoundaries(newValue / 3);
						}
						return newValue;
					} else {
						throw new WriteChannelException("no essNature to control available");
					}
				}

			});
	private FunctionalWriteChannel<Long> setWorkState = new FunctionalWriteChannel<Long>("SetWorkState", this,
			new FunctionalWriteChannelFunction<Long>() {

				@Override
				public Long setValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					if (channels.length == 1) {
						try {
							channels[0].pushWriteFromLabel(newLabel);
						} catch (WriteChannelException e) {
							log.error("Can't set value for channel " + channels[0].address(), e);
						}
					}
					return newValue;
				}

				@Override
				public Long getValue(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) {
					if (channels.length == 1 && channels[0].labelOptional().equals(Optional.of(EssNature.START))) {
						return 1L;
					}
					return 0L;
				}

				@Override
				public Long getMinValue(Optional<Long> minValue,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					long min = Long.MIN_VALUE;
					if (channels.length == 1 && channels[0].writeMin().isPresent()
							&& channels[0].writeMin().get() > min) {
						min = channels[0].writeMin().get();
					}
					if (min == Long.MIN_VALUE) {
						return null;
					} else {
						return min;
					}
				}

				@Override
				public Long getMaxValue(Optional<Long> maxValue,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					long max = Long.MAX_VALUE;
					if (channels.length == 1 && channels[0].writeMax().isPresent()
							&& channels[0].writeMax().get() < max) {
						max = channels[0].writeMax().get();
					}
					if (max == Long.MAX_VALUE) {
						return null;
					} else {
						return max;
					}
				}

				@Override
				public Long setMinValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					if (channels.length == 1) {
						try {
							channels[0].pushWriteMin(newValue);
						} catch (WriteChannelException e) {
							log.error("Can't set value for channel " + channels[0].address(), e);
						}
					}
					return newValue;
				}

				@Override
				public Long setMaxValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					if (channels.length == 1) {
						try {
							channels[0].pushWriteMax(newValue);
						} catch (WriteChannelException e) {
							log.error("Can't set value for channel " + channels[0].address(), e);
						}
					}
					return newValue;
				}

			}).label(0L, EssNature.STOP).label(1L, EssNature.START);

	public AsymmetricSymmetricCombinationEssNature(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
		this.repo = ThingRepository.getInstance();
		this.listeners = new LinkedList<>();
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
	public void addListener(ThingChannelsUpdatedListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void removeListener(ThingChannelsUpdatedListener listener) {
		this.listeners.remove(listener);
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
			// remove old ess
			if (essNature != null) {
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
				setActivePower.removeChannel(essNature.setActivePowerL1());
				setActivePower.removeChannel(essNature.setActivePowerL2());
				setActivePower.removeChannel(essNature.setActivePowerL3());
				setReactivePowerL1.removeChannel(essNature.setReactivePowerL1());
				setReactivePowerL2.removeChannel(essNature.setReactivePowerL2());
				setReactivePowerL3.removeChannel(essNature.setReactivePowerL3());
				setReactivePower.removeChannel(essNature.setReactivePowerL1());
				setReactivePower.removeChannel(essNature.setReactivePowerL2());
				setReactivePower.removeChannel(essNature.setReactivePowerL3());
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
							allowedApparent.setChannel(essNature.allowedApparent());
							allowedDischarge.setChannel(essNature.allowedDischarge());
							allowedCharge.setChannel(essNature.allowedCharge());
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
							setActivePower.addChannel(essNature.setActivePowerL1());
							setActivePower.addChannel(essNature.setActivePowerL2());
							setActivePower.addChannel(essNature.setActivePowerL3());
							setReactivePowerL1.addChannel(essNature.setReactivePowerL1());
							setReactivePowerL2.addChannel(essNature.setReactivePowerL2());
							setReactivePowerL3.addChannel(essNature.setReactivePowerL3());
							setReactivePower.addChannel(essNature.setReactivePowerL1());
							setReactivePower.addChannel(essNature.setReactivePowerL2());
							setReactivePower.addChannel(essNature.setReactivePowerL3());
							setWorkState.addChannel(essNature.setWorkState());
						}
					} else {
						log.error("ThingID: " + essId + " is no AsymmetricEss!");
					}
				} else {
					log.warn("meter: " + essId + " not found!");
				}
			}
		} catch (InvalidValueException e) {
			log.error("esss value is invalid!", e);
		}
	}

	public void runCalculation() throws WriteChannelException {
		long L1 = 0;
		long L2 = 0;
		long L3 = 0;
		if (setActivePower.getWriteValue().isPresent()) {
			long activePowerWriteValue = setActivePower.getWriteValue().get();
			L1 += activePowerWriteValue / 3;
			L2 += activePowerWriteValue / 3;
			L3 += activePowerWriteValue / 3;
		}
		if (setActivePowerL1.getWriteValue().isPresent()) {
			L1 += setActivePowerL1.getWriteValue().get();
		}
		if (setActivePowerL2.getWriteValue().isPresent()) {
			L2 += setActivePowerL2.getWriteValue().get();
		}
		if (setActivePowerL3.getWriteValue().isPresent()) {
			L3 += setActivePowerL3.getWriteValue().get();
		}
		essNature.setActivePowerL1().pushWrite(L1);
		essNature.setActivePowerL2().pushWrite(L2);
		essNature.setActivePowerL3().pushWrite(L3);
	}

	private Long getMinActivePowerPhase(int phase, Optional<Long> thisMinPower) {
		Long minValue = null;
		WriteChannel<Long> nativeSetPower = getNativeSetPower(phase);
		WriteChannel<Long> thisSetPower = getThisSetPower(phase);
		if (nativeSetPower != null && nativeSetPower.getWriteValue().isPresent()) {
			minValue = 0L;
		} else if (thisSetPower.getWriteValue().isPresent()) {
			minValue = thisSetPower.getWriteValue().get();
		} else {
			List<Long> minValues = new ArrayList<>();
			if (thisMinPower.isPresent()) {
				if (setActivePower.getWriteValue().isPresent()) {
					minValues.add(thisMinPower.get() - setActivePower.getWriteValue().get() / 3);
				} else {
					minValues.add(thisMinPower.get());
				}
			}
			if (nativeSetPower != null && nativeSetPower.writeMin().isPresent()) {
				if (setActivePower.getWriteValue().isPresent()) {
					minValues.add(nativeSetPower.writeMin().get() - setActivePower.getWriteValue().get() / 3);
				} else {
					minValues.add(nativeSetPower.writeMin().get());
				}
			}
			if (minValues.size() > 0) {
				minValue = Collections.max(minValues);
			}
		}
		return minValue;
	}

	private Long getMaxActivePowerPhase(int phase, Optional<Long> thisMaxPower) {
		Long maxValue = null;
		WriteChannel<Long> nativeSetPower = getNativeSetPower(phase);
		WriteChannel<Long> thisSetPower = getThisSetPower(phase);
		if (nativeSetPower != null && nativeSetPower.getWriteValue().isPresent()) {
			maxValue = 0L;
		} else if (thisSetPower.getWriteValue().isPresent()) {
			maxValue = thisSetPower.getWriteValue().get();
		} else {
			List<Long> maxValues = new ArrayList<>();
			if (thisMaxPower.isPresent()) {
				if (setActivePower.getWriteValue().isPresent()) {
					maxValues.add(thisMaxPower.get() - setActivePower.getWriteValue().get() / 3);
				} else {
					maxValues.add(thisMaxPower.get());
				}
			}
			if (nativeSetPower != null && nativeSetPower.writeMax().isPresent()) {
				if (setActivePower.getWriteValue().isPresent()) {
					maxValues.add(nativeSetPower.writeMax().get() - setActivePower.getWriteValue().get() / 3);
				} else {
					maxValues.add(nativeSetPower.writeMax().get());
				}
			}
			if (maxValues.size() > 0) {
				maxValue = Collections.min(maxValues);
			}
		}
		return maxValue;
	}

	private WriteChannel<Long> getNativeSetPower(int phase) {
		if (essNature != null) {
			switch (phase) {
			case 1:
				return essNature.setActivePowerL1();
			case 2:
				return essNature.setActivePowerL2();
			case 3:
				return essNature.setActivePowerL3();
			}
			return null;
		} else {
			return null;
		}
	}

	private WriteChannel<Long> getThisSetPower(int phase) {
		switch (phase) {
		case 1:
			return setActivePowerL1;
		case 2:
			return setActivePowerL2;
		case 3:
			return setActivePowerL3;
		}
		return null;
	}

	private Long getMinActivePower() {
		Long minValue = null;
		if (essNature != null && (essNature.setActivePowerL1().getWriteValue().isPresent()
				|| essNature.setActivePowerL2().getWriteValue().isPresent()
				|| essNature.setActivePowerL3().getWriteValue().isPresent())) {
			minValue = 0L;
		} else if (setActivePower.getWriteValue().isPresent()) {
			minValue = setActivePower.getWriteValue().get();
		} else {
			long baseL1 = 0L;
			long baseL2 = 0L;
			long baseL3 = 0L;
			List<Long> minValues = new ArrayList<>();
			if (setActivePowerL1.getWriteValue().isPresent()) {
				baseL1 = setActivePowerL1.getWriteValue().get();
			}
			if (setActivePowerL2.getWriteValue().isPresent()) {
				baseL2 = setActivePowerL2.getWriteValue().get();
			}
			if (setActivePowerL3.getWriteValue().isPresent()) {
				baseL3 = setActivePowerL3.getWriteValue().get();
			}
			if (essNature != null && essNature.allowedApparent().valueOptional().isPresent()) {
				minValues.add(essNature.setActivePowerL1().writeMin()
						.orElse(essNature.allowedApparent().valueOptional().get() / -3) - baseL1);
				minValues.add(essNature.setActivePowerL2().writeMin()
						.orElse(essNature.allowedApparent().valueOptional().get() / -3) - baseL2);
				minValues.add(essNature.setActivePowerL3().writeMin()
						.orElse(essNature.allowedApparent().valueOptional().get() / -3) - baseL3);
				minValue = Collections.min(minValues) * 3;
			}
		}
		return minValue;
	}

	private Long getMaxActivePower() {
		Long maxValue = null;
		if (essNature != null && (essNature.setActivePowerL1().getWriteValue().isPresent()
				|| essNature.setActivePowerL2().getWriteValue().isPresent()
				|| essNature.setActivePowerL3().getWriteValue().isPresent())) {
			maxValue = 0L;
		} else if (setActivePower.getWriteValue().isPresent()) {
			maxValue = setActivePower.getWriteValue().get();
		} else {
			long baseL1 = 0L;
			long baseL2 = 0L;
			long baseL3 = 0L;
			List<Long> maxValues = new ArrayList<>();
			if (setActivePowerL1.getWriteValue().isPresent()) {
				baseL1 = setActivePowerL1.getWriteValue().get();
			}
			if (setActivePowerL2.getWriteValue().isPresent()) {
				baseL2 = setActivePowerL2.getWriteValue().get();
			}
			if (setActivePowerL3.getWriteValue().isPresent()) {
				baseL3 = setActivePowerL3.getWriteValue().get();
			}
			if (essNature != null && essNature.allowedApparent().valueOptional().isPresent()) {
				maxValues.add(essNature.setActivePowerL1().writeMax()
						.orElse(essNature.allowedApparent().valueOptional().get() / 3) - baseL1);
				maxValues.add(essNature.setActivePowerL2().writeMax()
						.orElse(essNature.allowedApparent().valueOptional().get() / 3) - baseL2);
				maxValues.add(essNature.setActivePowerL3().writeMax()
						.orElse(essNature.allowedApparent().valueOptional().get() / 3) - baseL3);
				maxValue = Collections.min(maxValues) * 3;
			}
		}
		return maxValue;
	}

	private Long getMinReactivePowerPhase(int phase, Optional<Long> thisMinPower) {
		Long minValue = null;
		WriteChannel<Long> nativeSetPower = getNativeSetReactivePower(phase);
		WriteChannel<Long> thisSetPower = getThisSetReactivePower(phase);
		if (nativeSetPower != null && nativeSetPower.getWriteValue().isPresent()) {
			minValue = 0L;
		} else if (thisSetPower.getWriteValue().isPresent()) {
			minValue = thisSetPower.getWriteValue().get();
		} else {
			List<Long> minValues = new ArrayList<>();
			if (thisMinPower.isPresent()) {
				if (setReactivePower.getWriteValue().isPresent()) {
					minValues.add(thisMinPower.get() - setReactivePower.getWriteValue().get() / 3);
				} else {
					minValues.add(thisMinPower.get());
				}
			}
			if (nativeSetPower != null && nativeSetPower.writeMin().isPresent()) {
				if (setReactivePower.getWriteValue().isPresent()) {
					minValues.add(nativeSetPower.writeMin().get() - setReactivePower.getWriteValue().get() / 3);
				} else {
					minValues.add(nativeSetPower.writeMin().get());
				}
			}
			if (minValues.size() > 0) {
				minValue = Collections.max(minValues);
			}
		}
		return minValue;
	}

	private Long getMaxReactivePowerPhase(int phase, Optional<Long> thisMaxPower) {
		Long maxValue = null;
		WriteChannel<Long> nativeSetPower = getNativeSetPower(phase);
		WriteChannel<Long> thisSetPower = getThisSetPower(phase);
		if (nativeSetPower != null && nativeSetPower.getWriteValue().isPresent()) {
			maxValue = 0L;
		} else if (thisSetPower.getWriteValue().isPresent()) {
			maxValue = thisSetPower.getWriteValue().get();
		} else {
			List<Long> maxValues = new ArrayList<>();
			if (thisMaxPower.isPresent()) {
				if (setReactivePower.getWriteValue().isPresent()) {
					maxValues.add(thisMaxPower.get() - setReactivePower.getWriteValue().get() / 3);
				} else {
					maxValues.add(thisMaxPower.get());
				}
			}
			if (nativeSetPower != null && nativeSetPower.writeMax().isPresent()) {
				if (setReactivePower.getWriteValue().isPresent()) {
					maxValues.add(nativeSetPower.writeMax().get() - setReactivePower.getWriteValue().get() / 3);
				} else {
					maxValues.add(nativeSetPower.writeMax().get());
				}
			}
			if (maxValues.size() > 0) {
				maxValue = Collections.min(maxValues);
			}
		}
		return maxValue;
	}

	private WriteChannel<Long> getNativeSetReactivePower(int phase) {
		if (essNature != null) {
			switch (phase) {
			case 1:
				return essNature.setReactivePowerL1();
			case 2:
				return essNature.setReactivePowerL2();
			case 3:
				return essNature.setReactivePowerL3();
			}
			return null;
		} else {
			return null;
		}
	}

	private WriteChannel<Long> getThisSetReactivePower(int phase) {
		switch (phase) {
		case 1:
			return setReactivePowerL1;
		case 2:
			return setReactivePowerL2;
		case 3:
			return setReactivePowerL3;
		}
		return null;
	}

	private Long getMinReactivePower() {
		Long minValue = null;
		if (essNature != null && (essNature.setReactivePowerL1().getWriteValue().isPresent()
				|| essNature.setReactivePowerL2().getWriteValue().isPresent()
				|| essNature.setReactivePowerL3().getWriteValue().isPresent())) {
			minValue = 0L;
		} else if (setReactivePower.getWriteValue().isPresent()) {
			minValue = setReactivePower.getWriteValue().get();
		} else {
			long baseL1 = 0L;
			long baseL2 = 0L;
			long baseL3 = 0L;
			List<Long> minValues = new ArrayList<>();
			if (setReactivePowerL1.getWriteValue().isPresent()) {
				baseL1 = setReactivePowerL1.getWriteValue().get();
			}
			if (setReactivePowerL2.getWriteValue().isPresent()) {
				baseL2 = setReactivePowerL2.getWriteValue().get();
			}
			if (setReactivePowerL3.getWriteValue().isPresent()) {
				baseL3 = setReactivePowerL3.getWriteValue().get();
			}
			if (essNature != null && essNature.allowedApparent().valueOptional().isPresent()) {
				minValues.add(essNature.setReactivePowerL1().writeMin()
						.orElse(essNature.allowedApparent().valueOptional().get() / -3) - baseL1);
				minValues.add(essNature.setReactivePowerL2().writeMin()
						.orElse(essNature.allowedApparent().valueOptional().get() / -3) - baseL2);
				minValues.add(essNature.setReactivePowerL3().writeMin()
						.orElse(essNature.allowedApparent().valueOptional().get() / -3) - baseL3);
				minValue = Collections.min(minValues) * 3;
			}
		}
		return minValue;
	}

	private Long getMaxReactivePower() {
		Long maxValue = null;
		if (essNature != null && (essNature.setReactivePowerL1().getWriteValue().isPresent()
				|| essNature.setReactivePowerL2().getWriteValue().isPresent()
				|| essNature.setReactivePowerL3().getWriteValue().isPresent())) {
			maxValue = 0L;
		} else if (setReactivePower.getWriteValue().isPresent()) {
			maxValue = setReactivePower.getWriteValue().get();
		} else {
			long baseL1 = 0L;
			long baseL2 = 0L;
			long baseL3 = 0L;
			List<Long> maxValues = new ArrayList<>();
			if (setReactivePowerL1.getWriteValue().isPresent()) {
				baseL1 = setReactivePowerL1.getWriteValue().get();
			}
			if (setReactivePowerL2.getWriteValue().isPresent()) {
				baseL2 = setReactivePowerL2.getWriteValue().get();
			}
			if (setReactivePowerL3.getWriteValue().isPresent()) {
				baseL3 = setReactivePowerL3.getWriteValue().get();
			}
			if (essNature != null && essNature.allowedApparent().valueOptional().isPresent()) {
				maxValues.add(essNature.setReactivePowerL1().writeMax()
						.orElse(essNature.allowedApparent().valueOptional().get() / 3) - baseL1);
				maxValues.add(essNature.setReactivePowerL2().writeMax()
						.orElse(essNature.allowedApparent().valueOptional().get() / 3) - baseL2);
				maxValues.add(essNature.setReactivePowerL3().writeMax()
						.orElse(essNature.allowedApparent().valueOptional().get() / 3) - baseL3);
				maxValue = Collections.min(maxValues) * 3;
			}
		}
		return maxValue;
	}

	@Override
	public void init() {
		for (ThingChannelsUpdatedListener listener : this.listeners) {
			listener.thingChannelsUpdated(this);
		}
	}

}
