package io.openems.backend.alerting;

import java.time.ZonedDateTime;
import java.util.Objects;

import com.google.gson.JsonObject;

/**
 * Properties for one notification.
 */
public abstract class Message implements Comparable<Message> {
	private final String id;

	public Message(String messageId) {
		this.id = messageId;
	}

	/**
	 * Returns the unique identifier for a Message.
	 *
	 * @return identifier as {@link String}
	 */
	public String getId() {
		return this.id;
	}

	public boolean isValid() {
		return this.id != null && this.getNotifyStamp() != null;
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
		return Objects.hash(this.id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		var other = (Message) obj;
		return Objects.equals(this.id, other.id);
	}

	@Override
	public int compareTo(Message o) {
		if (o == null) {
			return -1;
		}
		return this.getNotifyStamp().compareTo(o.getNotifyStamp());
	}
}
