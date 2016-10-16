package io.openems.api.channel;

import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.exception.InvalidValueException;
import io.openems.core.databus.DataBus;

public class Channel {
	private final static Logger log = LoggerFactory.getLogger(Channel.class);
	private DataBus dataBus = null;
	private boolean isValid = false;
	private BigInteger value;

	public BigInteger getValue() throws InvalidValueException {
		if (this.isValid) {
			return value;
		} else {
			throw new InvalidValueException("Channel value is invalid.");
		}
	}

	public BigInteger getValueOrNull() {
		return value;
	}

	public void setValue(BigInteger value) {
		if (value == null) {
			this.isValid = false;
		} else {
			this.isValid = true;
		}
		this.value = value;
		try {
			log.info("Channel value updated: " + getValue());
		} catch (InvalidValueException e) {
			log.info("Channel value updated: INVALID");
		}
		// TODOdataBus.channelValueUpdated(this);
	}
}
