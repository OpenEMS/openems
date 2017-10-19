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
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.api.thing.Thing;
import io.openems.api.thing.ThingChannelsUpdatedListener;
import io.openems.core.BridgeInitializedEventListener;
import io.openems.core.Config;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.ControllerUtils;
import io.openems.impl.protocol.system.SystemDeviceNature;

@ThingInfo(title = "Ess Asymmetric-Symmetric-Combination")
public class AsymmetricSymmetricCombinationEssNature extends SystemDeviceNature
implements SymmetricEssNature, AsymmetricEssNature, ChannelChangeListener, BridgeInitializedEventListener {

	private ConfigChannel<Integer> minSoc = new ConfigChannel<>("minSoc", this);
	private ConfigChannel<Integer> chargeSoc = new ConfigChannel<Integer>("chargeSoc", this);

	@ChannelInfo(title = "Ess", description = "Sets the ess device for the combinationEss.", type = String.class)
	public ConfigChannel<String> ess = new ConfigChannel<String>("ess", this);
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
	private FunctionalReadChannel<Long> activePowerL1 = new FunctionalReadChannel<Long>("ActivePowerL1", this,
			new FunctionalReadChannelFunction<Long>() {

		@Override
		public Long handle(@SuppressWarnings("unchecked") ReadChannel<Long>... channels)
				throws InvalidValueException {
			Long value = null;
			if (channels.length == 3 && channels[0].valueOptional().isPresent()
					&& channels[1].valueOptional().isPresent() && channels[2].valueOptional().isPresent()) {
				long sum = channels[0].valueOptional().get() + channels[1].valueOptional().get()
						+ channels[2].valueOptional().get();
				value = channels[0].valueOptional().get() - sum / 3;
			}
			return value;
		}

	});
	private FunctionalReadChannel<Long> activePowerL2 = new FunctionalReadChannel<Long>("ActivePowerL2", this,
			new FunctionalReadChannelFunction<Long>() {

		@Override
		public Long handle(@SuppressWarnings("unchecked") ReadChannel<Long>... channels)
				throws InvalidValueException {
			Long value = null;
			if (channels.length == 3 && channels[0].valueOptional().isPresent()
					&& channels[1].valueOptional().isPresent() && channels[2].valueOptional().isPresent()) {
				long sum = channels[0].valueOptional().get() + channels[1].valueOptional().get()
						+ channels[2].valueOptional().get();
				value = channels[1].valueOptional().get() - sum / 3;
			}
			return value;
		}

	});
	private FunctionalReadChannel<Long> activePowerL3 = new FunctionalReadChannel<Long>("ActivePowerL3", this,
			new FunctionalReadChannelFunction<Long>() {

		@Override
		public Long handle(@SuppressWarnings("unchecked") ReadChannel<Long>... channels)
				throws InvalidValueException {
			Long value = null;
			if (channels.length == 3 && channels[0].valueOptional().isPresent()
					&& channels[1].valueOptional().isPresent() && channels[2].valueOptional().isPresent()) {
				long sum = channels[0].valueOptional().get() + channels[1].valueOptional().get()
						+ channels[2].valueOptional().get();
				value = channels[2].valueOptional().get() - sum / 3;
			}
			return value;
		}

	});
	private FunctionalReadChannel<Long> reactivePowerL1 = new FunctionalReadChannel<Long>("ReactivePowerL1", this,
			new FunctionalReadChannelFunction<Long>() {

		@Override
		public Long handle(@SuppressWarnings("unchecked") ReadChannel<Long>... channels)
				throws InvalidValueException {
			Long value = null;
			if (channels.length == 3 && channels[0].valueOptional().isPresent()
					&& channels[1].valueOptional().isPresent() && channels[2].valueOptional().isPresent()) {
				long sum = channels[0].valueOptional().get() + channels[1].valueOptional().get()
						+ channels[2].valueOptional().get();
				value = channels[0].valueOptional().get() - sum / 3;
			}
			return value;
		}

	});
	private FunctionalReadChannel<Long> reactivePowerL2 = new FunctionalReadChannel<Long>("ReactivePowerL2", this,
			new FunctionalReadChannelFunction<Long>() {

		@Override
		public Long handle(@SuppressWarnings("unchecked") ReadChannel<Long>... channels)
				throws InvalidValueException {
			Long value = null;
			if (channels.length == 3 && channels[0].valueOptional().isPresent()
					&& channels[1].valueOptional().isPresent() && channels[2].valueOptional().isPresent()) {
				long sum = channels[0].valueOptional().get() + channels[1].valueOptional().get()
						+ channels[2].valueOptional().get();
				value = channels[1].valueOptional().get() - sum / 3;
			}
			return value;
		}

	});
	private FunctionalReadChannel<Long> reactivePowerL3 = new FunctionalReadChannel<Long>("ReactivePowerL3", this,
			new FunctionalReadChannelFunction<Long>() {

		@Override
		public Long handle(@SuppressWarnings("unchecked") ReadChannel<Long>... channels)
				throws InvalidValueException {
			Long value = null;
			if (channels.length == 3 && channels[0].valueOptional().isPresent()
					&& channels[1].valueOptional().isPresent() && channels[2].valueOptional().isPresent()) {
				long sum = channels[0].valueOptional().get() + channels[1].valueOptional().get()
						+ channels[2].valueOptional().get();
				value = channels[2].valueOptional().get() - sum / 3;
			}
			return value;
		}

	});
	private FunctionalReadChannel<Long> activePower = new FunctionalReadChannel<Long>("ActivePower", this,
			new FunctionalReadChannelFunction<Long>() {

		@Override
		public Long handle(@SuppressWarnings("unchecked") ReadChannel<Long>... channels)
				throws InvalidValueException {
			Long value = null;
			if (channels.length == 3 && channels[0].valueOptional().isPresent()
					&& channels[1].valueOptional().isPresent() && channels[2].valueOptional().isPresent()) {
				value = channels[0].valueOptional().get() + channels[1].valueOptional().get()
						+ channels[2].valueOptional().get();
			}
			return value;
		}

	});
	private FunctionalReadChannel<Long> reactivePower = new FunctionalReadChannel<Long>("ReactivePower", this,
			new FunctionalReadChannelFunction<Long>() {

		@Override
		public Long handle(@SuppressWarnings("unchecked") ReadChannel<Long>... channels)
				throws InvalidValueException {
			Long value = null;
			if (channels.length == 3 && channels[0].valueOptional().isPresent()
					&& channels[1].valueOptional().isPresent() && channels[2].valueOptional().isPresent()) {
				value = channels[0].valueOptional().get() + channels[1].valueOptional().get()
						+ channels[2].valueOptional().get();
			}
			return value;
		}

	});
	private FunctionalReadChannel<Long> apparentPower = new FunctionalReadChannel<Long>("ApparentPower", this,
			new FunctionalReadChannelFunction<Long>() {

		@Override
		public Long handle(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) {
			Long sum = 0L;
			try {
				sum = ControllerUtils.calculateApparentPower(channels[0].value(), channels[1].value());
			} catch (InvalidValueException e) {
				sum = null;
			}
			return sum;
		}

	}, activePower, reactivePower).unit("VA");
	private FunctionalWriteChannel<Long> setActivePowerL1 = new FunctionalWriteChannel<Long>("SetActivePowerL1", this,
			new FunctionalWriteChannelFunction<Long>() {

		@Override
		public Long setValue(Long newValue, String newLabel,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
			if (channels.length >= 1) {
				channels[0].checkIntervalBoundaries(newValue);
				return newValue;
			} else {
				throw new WriteChannelException("no essNature to control available");
			}
		}

		@Override
		public Long getValue(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) {
			Long value = null;
			if (channels.length == 3 && channels[0].valueOptional().isPresent()
					&& channels[1].valueOptional().isPresent() && channels[2].valueOptional().isPresent()) {
				long sum = channels[0].valueOptional().get() + channels[1].valueOptional().get()
						+ channels[2].valueOptional().get();
				value = channels[0].valueOptional().get() - sum / 3;
			}
			return value;
		}

		@Override
		public Long getMinValue(Optional<Long> minValue,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
			return getMinPowerPhase(1, minValue, getNativeSetPower(1), getThisSetPower(1),
					getThisSetReactivePower(1), setActivePower, setReactivePower);
		}

		@Override
		public Long getMaxValue(Optional<Long> maxValue,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
			return getMaxPowerPhase(1, maxValue, getNativeSetPower(1), getThisSetPower(1),
					getThisSetReactivePower(1), setActivePower, setReactivePower);
		}

		@Override
		public Long setMinValue(Long newValue, String newLabel,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
			if (channels.length >= 1) {
				channels[0].checkIntervalBoundaries(newValue);
				return newValue;
			} else {
				throw new WriteChannelException("no essNature to control available");
			}
		}

		@Override
		public Long setMaxValue(Long newValue, String newLabel,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
			if (channels.length >= 1) {
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
			if (channels.length >= 1) {
				channels[0].checkIntervalBoundaries(newValue);
				return newValue;
			} else {
				throw new WriteChannelException("no essNature to control available");
			}
		}

		@Override
		public Long getValue(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) {
			Long value = null;
			if (channels.length == 3 && channels[0].valueOptional().isPresent()
					&& channels[1].valueOptional().isPresent() && channels[2].valueOptional().isPresent()) {
				long sum = channels[0].valueOptional().get() + channels[1].valueOptional().get()
						+ channels[2].valueOptional().get();
				value = channels[1].valueOptional().get() - sum / 3;
			}
			return value;
		}

		@Override
		public Long getMinValue(Optional<Long> minValue,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
			return getMinPowerPhase(2, minValue, getNativeSetPower(2), getThisSetPower(2),
					getThisSetReactivePower(2), setActivePower, setReactivePower);
		}

		@Override
		public Long getMaxValue(Optional<Long> maxValue,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
			return getMaxPowerPhase(2, maxValue, getNativeSetPower(2), getThisSetPower(2),
					getThisSetReactivePower(2), setActivePower, setReactivePower);
		}

		@Override
		public Long setMinValue(Long newValue, String newLabel,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
			if (channels.length >= 1) {
				channels[0].checkIntervalBoundaries(newValue);
				return newValue;
			} else {
				throw new WriteChannelException("no essNature to control available");
			}
		}

		@Override
		public Long setMaxValue(Long newValue, String newLabel,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
			if (channels.length >= 1) {
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
			if (channels.length >= 1) {
				channels[0].checkIntervalBoundaries(newValue);
				return newValue;
			} else {
				throw new WriteChannelException("no essNature to control available");
			}
		}

		@Override
		public Long getValue(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) {
			Long value = null;
			if (channels.length == 3 && channels[0].valueOptional().isPresent()
					&& channels[1].valueOptional().isPresent() && channels[2].valueOptional().isPresent()) {
				long sum = channels[0].valueOptional().get() + channels[1].valueOptional().get()
						+ channels[2].valueOptional().get();
				value = channels[2].valueOptional().get() - sum / 3;
			}
			return value;
		}

		@Override
		public Long getMinValue(Optional<Long> minValue,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
			return getMinPowerPhase(3, minValue, getNativeSetPower(3), getThisSetPower(3),
					getThisSetReactivePower(3), setActivePower, setReactivePower);
		}

		@Override
		public Long getMaxValue(Optional<Long> maxValue,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
			return getMaxPowerPhase(3, maxValue, getNativeSetPower(3), getThisSetPower(3),
					getThisSetReactivePower(3), setActivePower, setReactivePower);
		}

		@Override
		public Long setMinValue(Long newValue, String newLabel,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
			if (channels.length >= 1) {
				channels[0].checkIntervalBoundaries(newValue);
				return newValue;
			} else {
				throw new WriteChannelException("no essNature to control available");
			}
		}

		@Override
		public Long setMaxValue(Long newValue, String newLabel,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
			if (channels.length >= 1) {
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

		@SuppressWarnings("unchecked")
		@Override
		public Long getMinValue(Optional<Long> minValue, WriteChannel<Long>... channels) {
			WriteChannel<Long>[] nativeSetPrimaryPowerPhase = new WriteChannel[3];
			WriteChannel<Long>[] thisSetPrimaryPowerPhase = new WriteChannel[] { setActivePowerL1,
					setActivePowerL2, setActivePowerL3 };
			WriteChannel<Long>[] thisSetSecundaryPowerPhase = new WriteChannel[] { setReactivePowerL1,
					setReactivePowerL2, setReactivePowerL3 };
			if (channels.length == 3) {
				nativeSetPrimaryPowerPhase = channels;
			}
			Long minActivePower = getMinPower(nativeSetPrimaryPowerPhase, thisSetPrimaryPowerPhase,
					thisSetSecundaryPowerPhase, setActivePower, setReactivePower);
			return minActivePower;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Long getMaxValue(Optional<Long> maxValue, WriteChannel<Long>... channels) {
			WriteChannel<Long>[] nativeSetPrimaryPowerPhase = new WriteChannel[3];
			WriteChannel<Long>[] thisSetPrimaryPowerPhase = new WriteChannel[] { setActivePowerL1,
					setActivePowerL2, setActivePowerL3 };
			WriteChannel<Long>[] thisSetSecundaryPowerPhase = new WriteChannel[] { setReactivePowerL1,
					setReactivePowerL2, setReactivePowerL3 };
			if (channels.length == 3) {
				nativeSetPrimaryPowerPhase = channels;
			}
			Long maxActivePower = getMaxPower(nativeSetPrimaryPowerPhase, thisSetPrimaryPowerPhase,
					thisSetSecundaryPowerPhase, setActivePower, setReactivePower);
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
			if (channels.length >= 1) {
				channels[0].checkIntervalBoundaries(newValue);
				return newValue;
			} else {
				throw new WriteChannelException("no essNature to control available");
			}
		}

		@Override
		public Long getValue(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) {
			Long value = null;
			if (channels.length == 3 && channels[0].valueOptional().isPresent()
					&& channels[1].valueOptional().isPresent() && channels[2].valueOptional().isPresent()) {
				long sum = channels[0].valueOptional().get() + channels[1].valueOptional().get()
						+ channels[2].valueOptional().get();
				value = channels[0].valueOptional().get() - sum / 3;
			}
			return value;
		}

		@Override
		public Long getMinValue(Optional<Long> minValue,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
			return getMinPowerPhase(1, minValue, getNativeSetReactivePower(1), getThisSetReactivePower(1),
					getThisSetPower(1), setReactivePower, setActivePower);
		}

		@Override
		public Long getMaxValue(Optional<Long> maxValue,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
			return getMaxPowerPhase(1, maxValue, getNativeSetReactivePower(1), getThisSetReactivePower(1),
					getThisSetPower(1), setReactivePower, setActivePower);
		}

		@Override
		public Long setMinValue(Long newValue, String newLabel,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
			if (channels.length >= 1) {
				channels[0].checkIntervalBoundaries(newValue);
				return newValue;
			} else {
				throw new WriteChannelException("no essNature to control available");
			}
		}

		@Override
		public Long setMaxValue(Long newValue, String newLabel,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
			if (channels.length >= 1) {
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
			if (channels.length >= 1) {
				channels[0].checkIntervalBoundaries(newValue);
				return newValue;
			} else {
				throw new WriteChannelException("no essNature to control available");
			}
		}

		@Override
		public Long getValue(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) {
			Long value = null;
			if (channels.length == 3 && channels[0].valueOptional().isPresent()
					&& channels[1].valueOptional().isPresent() && channels[2].valueOptional().isPresent()) {
				long sum = channels[0].valueOptional().get() + channels[1].valueOptional().get()
						+ channels[2].valueOptional().get();
				value = channels[1].valueOptional().get() - sum / 3;
			}
			return value;
		}

		@Override
		public Long getMinValue(Optional<Long> minValue,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
			return getMinPowerPhase(2, minValue, getNativeSetReactivePower(2), getThisSetReactivePower(2),
					getThisSetPower(2), setReactivePower, setActivePower);
		}

		@Override
		public Long getMaxValue(Optional<Long> maxValue,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
			return getMaxPowerPhase(2, maxValue, getNativeSetReactivePower(2), getThisSetReactivePower(2),
					getThisSetPower(2), setReactivePower, setActivePower);
		}

		@Override
		public Long setMinValue(Long newValue, String newLabel,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
			if (channels.length >= 1) {
				channels[0].checkIntervalBoundaries(newValue);
				return newValue;
			} else {
				throw new WriteChannelException("no essNature to control available");
			}
		}

		@Override
		public Long setMaxValue(Long newValue, String newLabel,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
			if (channels.length >= 1) {
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
			if (channels.length >= 1) {
				channels[0].checkIntervalBoundaries(newValue);
				return newValue;
			} else {
				throw new WriteChannelException("no essNature to control available");
			}
		}

		@Override
		public Long getValue(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) {
			Long value = null;
			if (channels.length == 3 && channels[0].valueOptional().isPresent()
					&& channels[1].valueOptional().isPresent() && channels[2].valueOptional().isPresent()) {
				long sum = channels[0].valueOptional().get() + channels[1].valueOptional().get()
						+ channels[2].valueOptional().get();
				value = channels[2].valueOptional().get() - sum / 3;
			}
			return value;
		}

		@Override
		public Long getMinValue(Optional<Long> minValue,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
			return getMinPowerPhase(3, minValue, getNativeSetReactivePower(3), getThisSetReactivePower(3),
					getThisSetPower(3), setReactivePower, setActivePower);
		}

		@Override
		public Long getMaxValue(Optional<Long> maxValue,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) {
			return getMaxPowerPhase(3, maxValue, getNativeSetReactivePower(3), getThisSetReactivePower(3),
					getThisSetPower(3), setReactivePower, setActivePower);
		}

		@Override
		public Long setMinValue(Long newValue, String newLabel,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
			if (channels.length >= 1) {
				channels[0].checkIntervalBoundaries(newValue);
				return newValue;
			} else {
				throw new WriteChannelException("no essNature to control available");
			}
		}

		@Override
		public Long setMaxValue(Long newValue, String newLabel,
				@SuppressWarnings("unchecked") WriteChannel<Long>... channels) throws WriteChannelException {
			if (channels.length >= 1) {
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

		@SuppressWarnings("unchecked")
		@Override
		public Long getMinValue(Optional<Long> minValue, WriteChannel<Long>... channels) {
			WriteChannel<Long>[] nativeSetPrimaryPowerPhase = new WriteChannel[3];
			WriteChannel<Long>[] thisSetSecundaryPowerPhase = new WriteChannel[] { setActivePowerL1,
					setActivePowerL2, setActivePowerL3 };
			WriteChannel<Long>[] thisSetPrimaryPowerPhase = new WriteChannel[] { setReactivePowerL1,
					setReactivePowerL2, setReactivePowerL3 };
			if (channels.length == 3) {
				nativeSetPrimaryPowerPhase = channels;
			}
			Long minReactivePower = getMinPower(nativeSetPrimaryPowerPhase, thisSetPrimaryPowerPhase,
					thisSetSecundaryPowerPhase, setReactivePower, setActivePower);
			if (minReactivePower == null) {
				return null;
			}
			return minReactivePower;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Long getMaxValue(Optional<Long> maxValue, WriteChannel<Long>... channels) {
			WriteChannel<Long>[] nativeSetPrimaryPowerPhase = new WriteChannel[3];
			WriteChannel<Long>[] thisSetSecundaryPowerPhase = new WriteChannel[] { setActivePowerL1,
					setActivePowerL2, setActivePowerL3 };
			WriteChannel<Long>[] thisSetPrimaryPowerPhase = new WriteChannel[] { setReactivePowerL1,
					setReactivePowerL2, setReactivePowerL3 };
			if (channels.length == 3) {
				nativeSetPrimaryPowerPhase = channels;
			}
			Long maxReactivePower = getMaxPower(nativeSetPrimaryPowerPhase, thisSetPrimaryPowerPhase,
					thisSetSecundaryPowerPhase, setReactivePower, setActivePower);
			if (maxReactivePower == null) {
				return null;
			}
			return maxReactivePower;
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
		Config.getInstance().addBridgeInitializedEventListener(this);
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
	protected void update() {}

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
				activePowerL1.removeChannel(essNature.activePowerL2());
				activePowerL1.removeChannel(essNature.activePowerL3());
				activePowerL2.removeChannel(essNature.activePowerL1());
				activePowerL2.removeChannel(essNature.activePowerL2());
				activePowerL2.removeChannel(essNature.activePowerL3());
				activePowerL3.removeChannel(essNature.activePowerL1());
				activePowerL3.removeChannel(essNature.activePowerL2());
				activePowerL3.removeChannel(essNature.activePowerL3());
				activePower.removeChannel(essNature.activePowerL1());
				activePower.removeChannel(essNature.activePowerL2());
				activePower.removeChannel(essNature.activePowerL3());
				reactivePowerL1.removeChannel(essNature.reactivePowerL1());
				reactivePowerL1.removeChannel(essNature.reactivePowerL2());
				reactivePowerL1.removeChannel(essNature.reactivePowerL3());
				reactivePowerL2.removeChannel(essNature.reactivePowerL1());
				reactivePowerL2.removeChannel(essNature.reactivePowerL2());
				reactivePowerL2.removeChannel(essNature.reactivePowerL3());
				reactivePowerL3.removeChannel(essNature.reactivePowerL1());
				reactivePowerL3.removeChannel(essNature.reactivePowerL2());
				reactivePowerL3.removeChannel(essNature.reactivePowerL3());
				reactivePower.removeChannel(essNature.reactivePowerL1());
				reactivePower.removeChannel(essNature.reactivePowerL2());
				reactivePower.removeChannel(essNature.reactivePowerL3());
				setActivePowerL1.removeChannel(essNature.setActivePowerL1());
				setActivePowerL1.removeChannel(essNature.setActivePowerL2());
				setActivePowerL1.removeChannel(essNature.setActivePowerL3());
				setActivePowerL2.removeChannel(essNature.setActivePowerL1());
				setActivePowerL2.removeChannel(essNature.setActivePowerL2());
				setActivePowerL2.removeChannel(essNature.setActivePowerL3());
				setActivePowerL3.removeChannel(essNature.setActivePowerL1());
				setActivePowerL3.removeChannel(essNature.setActivePowerL2());
				setActivePowerL3.removeChannel(essNature.setActivePowerL3());
				setActivePower.removeChannel(essNature.setActivePowerL1());
				setActivePower.removeChannel(essNature.setActivePowerL2());
				setActivePower.removeChannel(essNature.setActivePowerL3());
				setReactivePowerL1.removeChannel(essNature.setReactivePowerL1());
				setReactivePowerL1.removeChannel(essNature.setReactivePowerL2());
				setReactivePowerL1.removeChannel(essNature.setReactivePowerL3());
				setReactivePowerL2.removeChannel(essNature.setReactivePowerL1());
				setReactivePowerL2.removeChannel(essNature.setReactivePowerL2());
				setReactivePowerL2.removeChannel(essNature.setReactivePowerL3());
				setReactivePowerL3.removeChannel(essNature.setReactivePowerL1());
				setReactivePowerL3.removeChannel(essNature.setReactivePowerL2());
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
							activePowerL1.addChannel(essNature.activePowerL1());
							activePowerL1.addChannel(essNature.activePowerL2());
							activePowerL1.addChannel(essNature.activePowerL3());
							activePowerL2.addChannel(essNature.activePowerL1());
							activePowerL2.addChannel(essNature.activePowerL2());
							activePowerL2.addChannel(essNature.activePowerL3());
							activePowerL3.addChannel(essNature.activePowerL1());
							activePowerL3.addChannel(essNature.activePowerL2());
							activePowerL3.addChannel(essNature.activePowerL3());
							activePower.addChannel(essNature.activePowerL1());
							activePower.addChannel(essNature.activePowerL2());
							activePower.addChannel(essNature.activePowerL3());
							reactivePowerL1.addChannel(essNature.reactivePowerL1());
							reactivePowerL1.addChannel(essNature.reactivePowerL2());
							reactivePowerL1.addChannel(essNature.reactivePowerL3());
							reactivePowerL2.addChannel(essNature.reactivePowerL1());
							reactivePowerL2.addChannel(essNature.reactivePowerL2());
							reactivePowerL2.addChannel(essNature.reactivePowerL3());
							reactivePowerL3.addChannel(essNature.reactivePowerL1());
							reactivePowerL3.addChannel(essNature.reactivePowerL2());
							reactivePowerL3.addChannel(essNature.reactivePowerL3());
							reactivePower.addChannel(essNature.reactivePowerL1());
							reactivePower.addChannel(essNature.reactivePowerL2());
							reactivePower.addChannel(essNature.reactivePowerL3());
							setActivePowerL1.addChannel(essNature.setActivePowerL1());
							setActivePowerL1.addChannel(essNature.setActivePowerL2());
							setActivePowerL1.addChannel(essNature.setActivePowerL3());
							setActivePowerL2.addChannel(essNature.setActivePowerL1());
							setActivePowerL2.addChannel(essNature.setActivePowerL2());
							setActivePowerL2.addChannel(essNature.setActivePowerL3());
							setActivePowerL3.addChannel(essNature.setActivePowerL1());
							setActivePowerL3.addChannel(essNature.setActivePowerL2());
							setActivePowerL3.addChannel(essNature.setActivePowerL3());
							setActivePower.addChannel(essNature.setActivePowerL1());
							setActivePower.addChannel(essNature.setActivePowerL2());
							setActivePower.addChannel(essNature.setActivePowerL3());
							setReactivePowerL1.addChannel(essNature.setReactivePowerL1());
							setReactivePowerL1.addChannel(essNature.setReactivePowerL2());
							setReactivePowerL1.addChannel(essNature.setReactivePowerL3());
							setReactivePowerL2.addChannel(essNature.setReactivePowerL1());
							setReactivePowerL2.addChannel(essNature.setReactivePowerL2());
							setReactivePowerL2.addChannel(essNature.setReactivePowerL3());
							setReactivePowerL3.addChannel(essNature.setReactivePowerL1());
							setReactivePowerL3.addChannel(essNature.setReactivePowerL2());
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
		long activePowerL1 = 0;
		long activePowerL2 = 0;
		long activePowerL3 = 0;
		if (setActivePower.getWriteValue().isPresent()) {
			long activePowerWriteValue = setActivePower.getWriteValue().get();
			activePowerL1 += activePowerWriteValue / 3;
			activePowerL2 += activePowerWriteValue / 3;
			activePowerL3 += activePowerWriteValue / 3;
		}
		if (setActivePowerL1.getWriteValue().isPresent() && setActivePowerL2.getWriteValue().isPresent()
				&& setActivePowerL3.getWriteValue().isPresent()) {
			activePowerL1 += setActivePowerL1.getWriteValue().get();
			activePowerL2 += setActivePowerL2.getWriteValue().get();
			activePowerL3 += setActivePowerL3.getWriteValue().get();
		}
		essNature.setActivePowerL1().pushWrite(activePowerL1);
		essNature.setActivePowerL2().pushWrite(activePowerL2);
		essNature.setActivePowerL3().pushWrite(activePowerL3);
		long reactivePowerL1 = 0;
		long reactivePowerL2 = 0;
		long reactivePowerL3 = 0;
		if (setReactivePower.getWriteValue().isPresent()) {
			long reactivePowerWriteValue = setReactivePower.getWriteValue().get();
			reactivePowerL1 += reactivePowerWriteValue / 3;
			reactivePowerL2 += reactivePowerWriteValue / 3;
			reactivePowerL3 += reactivePowerWriteValue / 3;
		}
		if (setReactivePowerL1.getWriteValue().isPresent()) {
			reactivePowerL1 += setReactivePowerL1.getWriteValue().get();
		}
		if (setReactivePowerL2.getWriteValue().isPresent()) {
			reactivePowerL2 += setReactivePowerL2.getWriteValue().get();
		}
		if (setReactivePowerL3.getWriteValue().isPresent()) {
			reactivePowerL3 += setReactivePowerL3.getWriteValue().get();
		}
		essNature.setReactivePowerL1().pushWrite(reactivePowerL1);
		essNature.setReactivePowerL2().pushWrite(reactivePowerL2);
		essNature.setReactivePowerL3().pushWrite(reactivePowerL3);
	}

	private Long getMinPowerPhase(int phase, Optional<Long> thisMinPower, WriteChannel<Long> nativeSetPrimaryPowerPhase,
			WriteChannel<Long> thisSetPrimaryPowerPhase, WriteChannel<Long> thisSetSecundaryPowerPhase,
			WriteChannel<Long> thisSetPrimaryPower, WriteChannel<Long> thisSetSecundaryPower) {
		Long minValue = null;
		if (nativeSetPrimaryPowerPhase != null && nativeSetPrimaryPowerPhase.getWriteValue().isPresent()) {
			minValue = 0L;
		} else if (thisSetPrimaryPowerPhase.getWriteValue().isPresent()) {
			minValue = thisSetPrimaryPowerPhase.getWriteValue().get();
		} else {
			List<Long> minValues = new ArrayList<>();
			if (thisMinPower.isPresent()) {
				if (thisSetPrimaryPower.getWriteValue().isPresent()) {
					minValues.add(thisMinPower.get() - thisSetPrimaryPower.getWriteValue().get() / 3);
				} else {
					minValues.add(thisMinPower.get());
				}
			}
			if (allowedApparent.valueOptional().isPresent()) {
				long primaryPowerSum = 0L;
				long scundaryPowerSum = 0L;
				if (thisSetPrimaryPowerPhase.getWriteValue().isPresent()) {
					primaryPowerSum += thisSetPrimaryPowerPhase.getWriteValue().get();
				}
				if (thisSetSecundaryPowerPhase.getWriteValue().isPresent()) {
					scundaryPowerSum += thisSetSecundaryPowerPhase.getWriteValue().get();
				}
				if (thisSetSecundaryPower.getWriteValue().isPresent()) {
					scundaryPowerSum += thisSetSecundaryPower.getWriteValue().get() / 3;
				}
				if (thisSetPrimaryPower.getWriteValue().isPresent()) {
					primaryPowerSum += thisSetPrimaryPower.getWriteValue().get() / 3;
				}
				long allowedPrimaryPowerPhase = ControllerUtils.calculateActivePower(scundaryPowerSum,
						allowedApparent.valueOptional().get() / 3);
				if (primaryPowerSum > 0) {
					minValues.add(allowedPrimaryPowerPhase * -1);
				} else {
					minValues.add(allowedPrimaryPowerPhase * -1 - primaryPowerSum);
				}
			}
			if (nativeSetPrimaryPowerPhase != null && nativeSetPrimaryPowerPhase.writeMin().isPresent()) {
				if (thisSetPrimaryPower.getWriteValue().isPresent()) {
					minValues.add(nativeSetPrimaryPowerPhase.writeMin().get()
							- thisSetPrimaryPower.getWriteValue().get() / 3);
				} else {
					minValues.add(nativeSetPrimaryPowerPhase.writeMin().get());
				}
			}
			if (minValues.size() > 0) {
				minValue = Collections.max(minValues);
			}
		}
		return minValue;

	}

	private Long getMaxPowerPhase(int phase, Optional<Long> thisMaxPower, WriteChannel<Long> nativeSetPrimaryPowerPhase,
			WriteChannel<Long> thisSetPrimaryPowerPhase, WriteChannel<Long> thisSetSecundaryPowerPhase,
			WriteChannel<Long> thisSetPrimaryPower, WriteChannel<Long> thisSetSecundaryPower) {
		Long maxValue = null;
		if (nativeSetPrimaryPowerPhase != null && nativeSetPrimaryPowerPhase.getWriteValue().isPresent()) {
			maxValue = 0L;
		} else if (thisSetPrimaryPowerPhase.getWriteValue().isPresent()) {
			maxValue = thisSetPrimaryPowerPhase.getWriteValue().get();
		} else {
			List<Long> maxValues = new ArrayList<>();
			if (thisMaxPower.isPresent()) {
				if (thisSetPrimaryPower.getWriteValue().isPresent()) {
					maxValues.add(thisMaxPower.get() - thisSetPrimaryPower.getWriteValue().get() / 3);
				} else {
					maxValues.add(thisMaxPower.get());
				}
			}
			if (allowedApparent.valueOptional().isPresent()) {
				long primaryPowerSum = 0L;
				long secundaryPowerSum = 0L;
				if (thisSetPrimaryPowerPhase.getWriteValue().isPresent()) {
					primaryPowerSum += thisSetPrimaryPowerPhase.getWriteValue().get();
				}
				if (thisSetSecundaryPowerPhase.getWriteValue().isPresent()) {
					secundaryPowerSum += thisSetSecundaryPowerPhase.getWriteValue().get();
				}
				if (thisSetSecundaryPower.getWriteValue().isPresent()) {
					secundaryPowerSum += thisSetSecundaryPower.getWriteValue().get() / 3;
				}
				if (thisSetPrimaryPower.getWriteValue().isPresent()) {
					primaryPowerSum += thisSetPrimaryPower.getWriteValue().get() / 3;
				}
				long allowedPowerPhase = ControllerUtils.calculateActivePower(secundaryPowerSum,
						allowedApparent.valueOptional().get() / 3);
				if (primaryPowerSum > 0) {
					maxValues.add(allowedPowerPhase - primaryPowerSum);
				} else {
					maxValues.add(allowedPowerPhase);
				}
			}
			if (nativeSetPrimaryPowerPhase != null && nativeSetPrimaryPowerPhase.writeMax().isPresent()) {
				if (thisSetPrimaryPower.getWriteValue().isPresent()) {
					maxValues.add(nativeSetPrimaryPowerPhase.writeMax().get()
							- thisSetPrimaryPower.getWriteValue().get() / 3);
				} else {
					maxValues.add(nativeSetPrimaryPowerPhase.writeMax().get());
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

	private Long getMinPower(WriteChannel<Long>[] nativeSetPrimaryPowerPhases,
			WriteChannel<Long>[] thisSetPrimaryPowerPhases, WriteChannel<Long>[] thisSetSecundaryPowerPhases,
			WriteChannel<Long> thisSetPrimaryPower, WriteChannel<Long> thisSetSecundaryPower) {
		Long minValue = null;
		for (WriteChannel<Long> nativeSetPower : nativeSetPrimaryPowerPhases) {
			if (nativeSetPower != null && nativeSetPower.getWriteValue().isPresent()) {
				return 0L;
			}
		}
		if (thisSetPrimaryPower.getWriteValue().isPresent()) {
			minValue = thisSetPrimaryPower.getWriteValue().get();
		} else {
			List<Long> minValues = new ArrayList<>();
			if (essNature != null && essNature.allowedApparent().valueOptional().isPresent()) {
				for (int i = 0; i < 3; i++) {
					WriteChannel<Long> thisSetPrimaryPowerPhase = thisSetPrimaryPowerPhases[i];
					WriteChannel<Long> thisSetSecundarzPowerPhase = thisSetSecundaryPowerPhases[i];
					long primaryPowerSum = 0L;
					long secundaryPowerSum = 0L;
					if (thisSetPrimaryPowerPhase.getWriteValue().isPresent()) {
						primaryPowerSum += thisSetPrimaryPowerPhase.getWriteValue().get();
					}
					if (thisSetSecundarzPowerPhase.getWriteValue().isPresent()) {
						secundaryPowerSum += thisSetSecundarzPowerPhase.getWriteValue().get();
					}
					if (thisSetSecundaryPower.getWriteValue().isPresent()) {
						secundaryPowerSum += thisSetSecundaryPower.getWriteValue().get() / 3;
					}
					if (thisSetPrimaryPower.getWriteValue().isPresent()) {
						primaryPowerSum += thisSetPrimaryPower.getWriteValue().get() / 3;
					}
					long allowedPrimaryPowerPhase = ControllerUtils.calculateActivePower(secundaryPowerSum,
							allowedApparent.valueOptional().get() / 3);
					if (primaryPowerSum > 0) {
						minValues.add(allowedPrimaryPowerPhase * -1);
					} else {
						minValues.add(allowedPrimaryPowerPhase * -1 - primaryPowerSum);
					}
					if (nativeSetPrimaryPowerPhases[i].writeMin().isPresent()) {
						minValues.add(nativeSetPrimaryPowerPhases[i].writeMin().get());
					}
				}
				minValue = Collections.max(minValues) * 3;
			}
		}
		return minValue;
	}

	private Long getMaxPower(WriteChannel<Long>[] nativeSetPrimaryPowerPhases,
			WriteChannel<Long>[] thisSetPrimaryPowerPhases, WriteChannel<Long>[] thisSetSecundaryPowerPhases,
			WriteChannel<Long> thisSetPrimaryPower, WriteChannel<Long> thisSetSecundaryPower) {
		Long maxValue = null;
		for (WriteChannel<Long> nativeSetPower : nativeSetPrimaryPowerPhases) {
			if (nativeSetPower != null && nativeSetPower.getWriteValue().isPresent()) {
				return 0L;
			}
		}
		if (thisSetPrimaryPower.getWriteValue().isPresent()) {
			maxValue = thisSetPrimaryPower.getWriteValue().get();
		} else {
			List<Long> maxValues = new ArrayList<>();
			if (essNature != null && essNature.allowedApparent().valueOptional().isPresent()) {
				for (int i = 0; i < 3; i++) {
					WriteChannel<Long> thisSetPrimaryPowerPhase = thisSetPrimaryPowerPhases[i];
					WriteChannel<Long> thisSetSecundaryPowerPhase = thisSetSecundaryPowerPhases[i];
					long primaryPowerSum = 0L;
					long secundaryPowerSum = 0L;
					if (thisSetPrimaryPowerPhase.getWriteValue().isPresent()) {
						primaryPowerSum += thisSetPrimaryPowerPhase.getWriteValue().get();
					}
					if (thisSetSecundaryPowerPhase.getWriteValue().isPresent()) {
						secundaryPowerSum += thisSetSecundaryPowerPhase.getWriteValue().get();
					}
					if (thisSetSecundaryPower.getWriteValue().isPresent()) {
						secundaryPowerSum += thisSetSecundaryPower.getWriteValue().get() / 3;
					}
					if (thisSetPrimaryPower.getWriteValue().isPresent()) {
						primaryPowerSum += thisSetPrimaryPower.getWriteValue().get() / 3;
					}
					long allowedPrimaryPowerPhase = ControllerUtils.calculateActivePower(secundaryPowerSum,
							allowedApparent.valueOptional().get() / 3);
					if (primaryPowerSum > 0) {
						maxValues.add(allowedPrimaryPowerPhase - primaryPowerSum);
					} else {
						maxValues.add(allowedPrimaryPowerPhase);
					}
					if (nativeSetPrimaryPowerPhases[i].writeMax().isPresent()) {
						maxValues.add(nativeSetPrimaryPowerPhases[i].writeMax().get());
					}
				}
				maxValue = Collections.min(maxValues) * 3;
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

	@Override
	public void init() {
		this.ess.addChangeListener(this);
		for (ThingChannelsUpdatedListener listener : this.listeners) {
			listener.thingChannelsUpdated(this);
		}
	}

	@Override
	public void onBridgeInitialized() {
		loadEss();
	}

}
