package io.openems.backend.alerting;

import java.time.ZonedDateTime;
import java.util.Objects;

import com.google.gson.JsonObject;

/**
 * Properties for one notification.
 */
public abstract class Message implements Comparable<Message> {
	private final String id;

	protected Message(String id) {
		this.id = id;
	}

	/**
	 * Returns the unique identifier for a Message.
	 * 
	 * @return identifier as {@link String}
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Returns the time stamp at which this message is supposed to be sent.
	 *
	 * @return {@link ZonedDateTime} at which to send this message
	 */
	public abstract ZonedDateTime getNotifyStamp();

	/**
	 * Get attributes as JsonObject for Mailer.
	 *
	 * @return JsonObject
	 */
	public abstract JsonObject getParams();

	@Override
	public int hashCode() {
		return Objects.hash(this.getId());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof Message other) {
			return Objects.equals(this.getId(), other.getId());
		} else {
			return false;
		}
	}

	@Override
	public int compareTo(Message o) {
		if (o == null) {
			return -1;
		}
		return this.getNotifyStamp().compareTo(o.getNotifyStamp());
	}
}
