package io.openems.common.event;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

import org.osgi.service.event.Event;

public class EventReader {

	private Event event;

	public EventReader(Event event) {
		this.event = event;
	}

	/**
	 * Get event-topic-String.
	 * 
	 * @return topic
	 */
	public String getTopic() {
		return this.event.getTopic();
	}

	/**
	 * Get argument as {@link Object}.
	 * 
	 * @param propertyId identifier of argument
	 * @return argument
	 */
	public Object getObject(String propertyId) {
		return this.event.getProperty(propertyId);
	}

	/**
	 * Try to auto cast argument. Get argument as {@link T}.
	 * 
	 * @param <T>        type of argument
	 * @param propertyId identifier of argument
	 * @return (T)argument
	 */
	@SuppressWarnings("unchecked")
	public <T> T getProperty(String propertyId) {
		return (T) this.getObject(propertyId);
	}

	/**
	 * Get argument as {@link String}.
	 * 
	 * @param propertyId identifier of argument
	 * @return argument
	 */
	public String getString(String propertyId) {
		return this.getProperty(propertyId);
	}

	/**
	 * Get argument as {@link Integer}.
	 * 
	 * @param propertyId identifier of argument
	 * @return argument
	 */
	public int getInteger(String propertyId) {
		return this.getProperty(propertyId);
	}

	/**
	 * Get argument as {@link Double}.
	 * 
	 * @param propertyId identifier of argument
	 * @return argument
	 */
	public double getDouble(String propertyId) {
		return this.getProperty(propertyId);
	}

	/**
	 * Get argument as {@link Boolean}.
	 * 
	 * @param propertyId identifier of argument
	 * @return argument
	 */
	public boolean getBoolean(String propertyId) {
		return this.getProperty(propertyId);
	}

	/**
	 * Get argument as {@link ZonedDateTime}.
	 * 
	 * @param propertyId identifier of argument
	 * @return argument
	 */
	public ZonedDateTime getZonedDateTime(String propertyId) {
		return this.getProperty(propertyId);
	}

	/**
	 * Get argument as {@link ZonedDateTime} converted to given timezone.
	 * 
	 * @param propertyId identifier of argument
	 * @param timeZone   to which to convert
	 * @return argument
	 */
	public ZonedDateTime getZonedDateTime(String propertyId, ZoneId timeZone) {
		return this.getZonedDateTime(propertyId).withZoneSameInstant(timeZone);
	}

	/**
	 * Get argument as {@link ZonedDateTime} converted to ZoneId.of(timeZone).
	 * 
	 * @param propertyId identifier of argument
	 * @param timeZone   to which to convert
	 * @return argument
	 */
	public ZonedDateTime getZonedDateTime(String propertyId, String timeZone) {
		return this.getZonedDateTime(propertyId, ZoneId.of(timeZone));
	}

	@Override
	public String toString() {
		StringBuilder asString = new StringBuilder("EventReader{Event={Topic=");
		asString.append(this.getTopic()).append(", Params={");
		for (var property : this.event.getPropertyNames()) {
			if (property.equals("event.topics")) {
				break;
			}
			asString.append(property).append("=").append(this.event.getProperty(property)).append(", ");
		}
		asString.delete(asString.length() - 2, asString.length());
		asString.append("}}}");
		return asString.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.event);
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj //
				|| obj instanceof EventReader other //
						&& Objects.equals(this.event, other.event);
	}

}
