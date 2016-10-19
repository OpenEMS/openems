package io.openems.api.channel;

import java.math.BigInteger;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.databus.Databus;

public class Channel {
	protected final BigInteger delta;
	protected final Map<BigInteger, String> labels;
	protected final Logger log;
	protected BigInteger maxValue = null;
	protected BigInteger minValue = null;
	protected final BigInteger multiplier;
	protected BigInteger value = null;
	private String channelId = null;
	private Databus databus = null;
	private DeviceNature nature = null;
	private final String unit;

	public Channel(DeviceNature nature, String unit, BigInteger minValue, BigInteger maxValue, BigInteger multiplier,
			BigInteger delta, Map<BigInteger, String> labels) {
		log = LoggerFactory.getLogger(this.getClass());
		this.nature = nature;
		this.unit = unit;
		this.multiplier = multiplier;
		this.delta = delta;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.labels = labels;
	}

	public String getAddress() {
		String natureId = null;
		if (nature != null) {
			natureId = nature.getThingId();
		}
		return natureId + "/" + channelId;
	}

	public String getChannelId() {
		return channelId;
	}

	public BigInteger getMaxValue() {
		return maxValue;
	}

	public BigInteger getMinValue() {
		return minValue;
	}

	public String getUnit() {
		return unit;
	}

	public BigInteger getValue() throws InvalidValueException {
		if (value != null) {
			return value;
		} else {
			throw new InvalidValueException("Channel value is invalid.");
		}
	};

	public String getValueLabel() throws InvalidValueException {
		String label = getValueLabelOrNull();
		if (label != null) {
			return label;
		} else {
			throw new InvalidValueException("Channel value is invalid.");
		}
	};

	public String getValueLabelOrNull() {
		if (value != null) {
			if (labels != null && labels.containsKey(value)) {
				return labels.get(value);
			} else {
				return "UNKNOWN";
			}
		}
		return null;
	}

	@Nullable
	public BigInteger getValueOrNull() {
		return value;
	};

	public void setAsRequired() throws ConfigException {
		if (this.nature == null) {
			throw new ConfigException("DeviceNature is not set [" + this + "]");
		} else {
			this.nature.setAsRequired(this);
		}
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public void setDatabus(Databus databus) {
		this.databus = databus;
	}

	@Override
	public String toString() {
		if (value != null) {
			if (labels != null) {
				return getValueLabelOrNull();
			} else {
				return value + " " + unit;
			}
		} else {
			return "INVALID";
		}
	}

	/**
	 * Update value from the underlying {@link DeviceNature} and send an update event to {@link Databus}.
	 *
	 * @param value
	 */
	protected void updateValue(BigInteger value) {
		updateValue(value, true);
	}

	/**
	 * Update value from the underlying {@link DeviceNature}
	 *
	 * @param value
	 * @param triggerDatabusEvent
	 *            true if an event should be forwarded to {@link Databus}
	 */
	protected void updateValue(BigInteger value, boolean triggerDatabusEvent) {
		if (value == null) {
			this.value = null;
		} else {
			this.value = value.multiply(multiplier).subtract(delta);
		}
		if (databus != null && triggerDatabusEvent) {
			databus.channelValueUpdated(this);
		}
	}
}
