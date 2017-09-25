package io.openems.impl.device.system.asymmetrictosymmetricess;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.FunctionalReadChannel;
import io.openems.api.channel.FunctionalWriteChannel;
import io.openems.api.channel.FunctionalWriteChannelFunction;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StatusBitChannels;
import io.openems.api.channel.WriteChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.api.thing.ThingChannelsUpdatedListener;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.ControllerUtils;
import io.openems.impl.protocol.system.SystemDeviceNature;

@ThingInfo(title = "Asymmetric to Symmetric Ess")
public class AsymmetricToSymmetricEssNature extends SystemDeviceNature
		implements SymmetricEssNature, ChannelChangeListener {

	private final Logger log = LoggerFactory.getLogger(AsymmetricToSymmetricEssNature.class);

	private List<ThingChannelsUpdatedListener> listeners;

	private static ThingRepository repo = ThingRepository.getInstance();

	@ChannelInfo(title = "Ess", description = "Sets the Ess devices for the proxy.", type = String.class)
	public ConfigChannel<String> essId = new ConfigChannel<String>("essId", this).addChangeListener(this);
	private ConfigChannel<Integer> minSoc = new ConfigChannel<>("minSoc", this);
	private ConfigChannel<Integer> chargeSoc = new ConfigChannel<Integer>("chargeSoc", this);
	private AsymmetricEssNature ess;

	private FunctionalReadChannel<Long> soc = new FunctionalReadChannel<Long>("Soc", this, (channels) -> {
		if (channels.length > 0) {
			return channels[0].value();
		}
		return null;
	}).unit("%");

	private FunctionalReadChannel<Long> allowedCharge = new FunctionalReadChannel<Long>("AllowedCharge", this,
			(channels) -> {
				if (channels.length > 0) {
					return channels[0].value();
				}
				return null;
			}).unit("W");
	private FunctionalReadChannel<Long> allowedDischarge = new FunctionalReadChannel<Long>("AllowedDischarge", this,
			(channels) -> {
				if (channels.length > 0) {
					return channels[0].value();
				}
				return null;
			}).unit("W");
	private FunctionalReadChannel<Long> allowedApparent = new FunctionalReadChannel<Long>("AllowedApparent", this,
			(channels) -> {
				if (channels.length > 0) {
					return channels[0].value();
				}
				return null;
			}).unit("VA");

	private FunctionalReadChannel<Long> activePower = new FunctionalReadChannel<Long>("ActivePower", this,
			(channels) -> {
				long sum = 0L;
				try {
					sum += ess.activePowerL1().value();
				} catch (InvalidValueException e) {
					log.error("Can't read values of " + ess.id(), e);
				}
				try {
					sum += ess.activePowerL2().value();
				} catch (InvalidValueException e) {
					log.error("Can't read values of " + ess.id(), e);
				}
				try {
					sum += ess.activePowerL3().value();
				} catch (InvalidValueException e) {
					log.error("Can't read values of " + ess.id(), e);
				}
				return sum;
			}).unit("W");
	private FunctionalReadChannel<Long> reactivePower = new FunctionalReadChannel<Long>("ReactivePower", this,
			(channels) -> {
				long sum = 0L;
				try {
					sum += ess.reactivePowerL1().value();
				} catch (InvalidValueException e) {
					log.error("Can't read values of " + ess.id(), e);
				}
				try {
					sum += ess.reactivePowerL2().value();
				} catch (InvalidValueException e) {
					log.error("Can't read values of " + ess.id(), e);
				}
				try {
					sum += ess.reactivePowerL3().value();
				} catch (InvalidValueException e) {
					log.error("Can't read values of " + ess.id(), e);
				}
				return sum;
			}).unit("Var");
	private FunctionalReadChannel<Long> apparentPower = new FunctionalReadChannel<Long>("ApparentPower", this,
			(channels) -> {
				long sum = 0L;
				try {
					sum = ControllerUtils.calculateApparentPower(channels[0].value(), channels[1].value());
				} catch (InvalidValueException e) {
					log.error("Can't read values of " + ess.id(), e);
				}
				return sum;
			}, activePower, reactivePower).unit("VA");

	private FunctionalWriteChannel<Long> setActivePower = new FunctionalWriteChannel<Long>("SetActivePower", this,
			new FunctionalWriteChannelFunction<Long>() {

				@Override
				public void setValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					long power = newValue / 3;
					for (WriteChannel<Long> channel : channels) {
						try {
							channel.pushWrite(power);
						} catch (WriteChannelException e) {
							log.error("Failed to write " + power + " to " + channel.address(), e);
						}
					}
				}

				@Override
				public Long getValue(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) {
					long sum = 0L;
					for (ReadChannel<Long> channel : channels) {
						try {
							sum += channel.value();
						} catch (InvalidValueException e) {
							log.error("Can't read ActivePower from " + channel.address());
						}
					}
					return sum;
				}

				@Override
				public Long getMinValue(@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					long min = Long.MIN_VALUE;
					boolean isPresent = false;
					for (WriteChannel<Long> channelMin : channels) {
						if (channelMin.writeMin().isPresent() && channelMin.writeMin().get() > min) {
							min = channelMin.writeMin().get();
							isPresent = true;
						}
					}
					if (isPresent) {
						return min * 3;
					}
					return null;
				}

				@Override
				public Long getMaxValue(@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					long max = Long.MAX_VALUE;
					boolean isPresent = false;
					for (WriteChannel<Long> channelMax : channels) {
						if (channelMax.writeMax().isPresent() && channelMax.writeMax().get() < max) {
							max = channelMax.writeMax().get();
							isPresent = true;
						}
					}
					if (isPresent) {
						return max * 3;
					}
					return null;
				}

				@Override
				public void setMinValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					long min = 0L;
					min = newValue / 3;
					if (min < getMinValue(channels)) {
						throw new WriteChannelException("Value [" + newValue
								+ "] for [ SetActivePower ] is out of boundaries. Different min value ["
								+ getMinValue(channels) + "] had already been set");
					}
					for (WriteChannel<Long> channel : channels) {
						try {
							channel.pushWriteMin(min);
						} catch (WriteChannelException e) {
							log.error("Failed to write " + min + " to " + channel.address(), e);
						}
					}
				}

				@Override
				public void setMaxValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					long max = 0L;
					max = newValue / 3;
					if (max > getMaxValue(channels)) {
						throw new WriteChannelException("Value [" + newValue
								+ "] for [ SetActivePower ] is out of boundaries. Different max value ["
								+ getMaxValue(channels) + "] had already been set");
					}
					for (WriteChannel<Long> channel : channels) {
						try {
							channel.pushWriteMax(max);
						} catch (WriteChannelException e) {
							log.error("Failed to write " + max + " to " + channel.address(), e);
						}
					}
				}

			});
	private FunctionalWriteChannel<Long> setReactivePower = new FunctionalWriteChannel<Long>("SetReactivePower", this,
			new FunctionalWriteChannelFunction<Long>() {

				@Override
				public void setValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					long power = 0L;
					if (channels.length > 0) {
						power = newValue / channels.length;
					}
					for (WriteChannel<Long> channel : channels) {
						try {
							channel.pushWrite(power);
						} catch (WriteChannelException e) {
							log.error("Failed to write " + power + " to " + channel.address(), e);
						}
					}
				}

				@Override
				public Long getValue(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) {
					long sum = 0L;
					for (ReadChannel<Long> channel : channels) {
						try {
							sum += channel.value();
						} catch (InvalidValueException e) {
							log.error("Can't read ReactivePower from " + channel.address());
						}
					}
					return sum;
				}

				@Override
				public Long getMinValue(@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					long min = Long.MIN_VALUE;
					boolean isPresent = false;
					for (WriteChannel<Long> channelMin : channels) {
						if (channelMin.writeMin().isPresent() && channelMin.writeMin().get() > min) {
							min = channelMin.writeMin().get();
							isPresent = true;
						}
					}
					if (isPresent) {
						return min * 3;
					}
					return null;
				}

				@Override
				public Long getMaxValue(@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					long max = Long.MAX_VALUE;
					boolean isPresent = false;
					for (WriteChannel<Long> channelMax : channels) {
						if (channelMax.writeMax().isPresent() && channelMax.writeMax().get() < max) {
							max = channelMax.writeMax().get();
							isPresent = true;
						}
					}
					if (isPresent) {
						return max * 3;
					}
					return null;
				}

				@Override
				public void setMinValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					long min = 0L;
					min = newValue / 3;
					if (getMinValue(channels) != null && min < getMinValue(channels)) {
						throw new WriteChannelException("Value [" + newValue
								+ "] for [ SetReactivePower ] is out of boundaries. Different min value ["
								+ getMinValue(channels) + "] had already been set");
					}
					for (WriteChannel<Long> channel : channels) {
						try {
							channel.pushWriteMin(min);
						} catch (WriteChannelException e) {
							log.error("Failed to write " + min + " to " + channel.address(), e);
						}
					}
				}

				@Override
				public void setMaxValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					long max = 0L;
					max = newValue / 3;
					if (getMaxValue(channels) != null && max > getMaxValue(channels)) {
						throw new WriteChannelException("Value [" + newValue
								+ "] for [ SetReactivePower ] is out of boundaries. Different max value ["
								+ getMaxValue(channels) + "] had already been set");
					}
					for (WriteChannel<Long> channel : channels) {
						try {
							channel.pushWriteMax(max);
						} catch (WriteChannelException e) {
							log.error("Failed to write " + max + " to " + channel.address(), e);
						}
					}
				}

			});

	private FunctionalReadChannel<Long> maxNominalPower = new FunctionalReadChannel<Long>("MaxNominalPower", this,
			(channels) -> {
				if (channels.length > 0) {
					return channels[0].value();
				}
				return null;
			}).unit("VA");
	private FunctionalReadChannel<Long> capacity = new FunctionalReadChannel<Long>("Capacity", this, (channels) -> {
		if (channels.length > 0) {
			return channels[0].value();
		}
		return null;
	}).unit("Wh");

	private FunctionalReadChannel<Long> gridMode = new FunctionalReadChannel<Long>("GridMode", this, (channels) -> {
		if (channels[0].labelOptional().equals(Optional.of(EssNature.ON_GRID))) {
			return 1L;
		}
		return 0L;
	}).label(0L, EssNature.OFF_GRID).label(1L, EssNature.ON_GRID);
	private FunctionalReadChannel<Long> systemState = new FunctionalReadChannel<Long>("SystemState", this,
			(channels) -> {
				if (channels[0].labelOptional().equals(Optional.of(EssNature.ON))) {
					return 1L;
				} else if (channels[0].labelOptional().equals(Optional.of(EssNature.OFF))) {
					return 0L;
				} else if (channels[0].labelOptional().equals(Optional.of(EssNature.FAULT))) {
					return 2L;
				} else {
					return 3L;
				}
			}).label(0L, EssNature.OFF).label(1L, EssNature.ON).label(2L, EssNature.FAULT).label(3L, "UNDEFINED");
	private StatusBitChannels warning = new StatusBitChannels("Warning", this);

	private FunctionalWriteChannel<Long> setWorkState = new FunctionalWriteChannel<Long>("SetWorkState", this,
			new FunctionalWriteChannelFunction<Long>() {

				@Override
				public void setValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					for (WriteChannel<Long> channel : channels) {
						try {
							channel.pushWriteFromLabel(newLabel);
						} catch (WriteChannelException e) {
							log.error("Can't set value for channel " + channel.address(), e);
						}
					}
				}

				@Override
				public Long getValue(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) {
					if (channels.length > 0) {
						if (channels[0].labelOptional().equals(Optional.of(EssNature.ON))) {
							return 1L;
						}
					}
					return 0L;
				}

				@Override
				public Long getMinValue(@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					if (channels.length > 0 && channels[0].writeMin().isPresent()) {
						return channels[0].writeMin().get();
					}
					return null;
				}

				@Override
				public Long getMaxValue(@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
					if (channels.length > 0 && channels[0].writeMax().isPresent()) {
						return channels[0].writeMax().get();
					}
					return null;
				}

				@Override
				public void setMinValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (getMinValue(channels) != null && newValue < getMinValue(channels)) {
						throw new WriteChannelException(
								"Value [" + newValue + "] for [ GridMode ] is out of boundaries. Different min value ["
										+ getMinValue(channels) + "] had already been set");
					}
					for (WriteChannel<Long> channel : channels) {
						try {
							channel.pushWriteMax(newValue);
						} catch (WriteChannelException e) {
							log.error("Failed to write " + newValue + " to " + channel.address(), e);
						}
					}
				}

				@Override
				public void setMaxValue(Long newValue, String newLabel,
						@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
					if (getMaxValue(channels) != null && newValue < getMaxValue(channels)) {
						throw new WriteChannelException(
								"Value [" + newValue + "] for [ GridMode ] is out of boundaries. Different max value ["
										+ getMaxValue(channels) + "] had already been set");
					}
					for (WriteChannel<Long> channel : channels) {
						try {
							channel.pushWriteMax(newValue);
						} catch (WriteChannelException e) {
							log.error("Failed to write " + newValue + " to " + channel.address(), e);
						}
					}
				}

			}).label(0L, EssNature.OFF).label(1L, EssNature.ON);

	public AsymmetricToSymmetricEssNature(String id, Device parent) throws ConfigException {
		super(id, parent);
		this.listeners = new ArrayList<>();
	}

	@Override
	public void setAsRequired(Channel channel) {
		// unused
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
	public StatusBitChannels warning() {
		return warning;
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
	public ReadChannel<Long> maxNominalPower() {
		return maxNominalPower;
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
	public ReadChannel<Long> capacity() {
		return capacity;
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
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		if (channel.equals(essId)) {
			loadEss();
		}
	}

	private void loadEss() {
		Set<DeviceNature> natures = repo.getDeviceNatures();
		String essId;
		try {
			essId = this.essId.value();
			if (ess != null) {
				// remove old ess
				activePower.removeChannel(ess.activePowerL1());
				activePower.removeChannel(ess.activePowerL2());
				activePower.removeChannel(ess.activePowerL3());
				reactivePower.removeChannel(ess.reactivePowerL1());
				reactivePower.removeChannel(ess.reactivePowerL2());
				reactivePower.removeChannel(ess.reactivePowerL3());
				setActivePower.removeChannel(ess.setActivePowerL1());
				setActivePower.removeChannel(ess.setActivePowerL2());
				setActivePower.removeChannel(ess.setActivePowerL3());
				setReactivePower.removeChannel(ess.setReactivePowerL1());
				setReactivePower.removeChannel(ess.setReactivePowerL2());
				setReactivePower.removeChannel(ess.setReactivePowerL3());
				allowedCharge.removeChannel(ess.allowedCharge());
				allowedDischarge.removeChannel(ess.allowedDischarge());
				allowedApparent.removeChannel(ess.allowedApparent());
				systemState.removeChannel(ess.systemState());
				setWorkState.removeChannel(ess.setWorkState());
				capacity.removeChannel(ess.capacity());
				maxNominalPower.removeChannel(ess.maxNominalPower());
				gridMode.removeChannel(ess.gridMode());
				soc.removeChannel(ess.soc());
				ess = null;
			}
			for (DeviceNature nature : natures) {
				if (nature instanceof AsymmetricEssNature) {
					if (essId.contains(nature.id())) {
						AsymmetricEssNature ess = (AsymmetricEssNature) nature;
						this.ess = ess;
						activePower.addChannel(ess.activePowerL1());
						activePower.addChannel(ess.activePowerL2());
						activePower.addChannel(ess.activePowerL3());
						reactivePower.addChannel(ess.reactivePowerL1());
						reactivePower.addChannel(ess.reactivePowerL2());
						reactivePower.addChannel(ess.reactivePowerL3());
						setActivePower.addChannel(ess.setActivePowerL1());
						setActivePower.addChannel(ess.setActivePowerL2());
						setActivePower.addChannel(ess.setActivePowerL3());
						setReactivePower.addChannel(ess.setReactivePowerL1());
						setReactivePower.addChannel(ess.setReactivePowerL2());
						setReactivePower.addChannel(ess.setReactivePowerL3());
						allowedCharge.addChannel(ess.allowedCharge());
						allowedDischarge.addChannel(ess.allowedDischarge());
						allowedApparent.addChannel(ess.allowedApparent());
						systemState.addChannel(ess.systemState());
						setWorkState.addChannel(ess.setWorkState());
						capacity.addChannel(ess.capacity());
						maxNominalPower.addChannel(ess.maxNominalPower());
						gridMode.addChannel(ess.gridMode());
						soc.addChannel(ess.soc());
					}
				}
			}
		} catch (InvalidValueException e) {
			log.error("esss value is invalid!", e);
		}
	}

	@Override
	public void init() {
		for (ThingChannelsUpdatedListener listener : this.listeners) {
			listener.thingChannelsUpdated(this);
		}
	}

	@Override
	protected void update() {
		if (ess == null) {
			loadEss();
		}
	}

}
