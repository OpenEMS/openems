package de.fenecon.femscore.monitoring.fenecon;

import java.io.Serializable;
import java.util.Calendar;

public class TimedElementValue implements Serializable {
	private Long time;
	private String name;
	private Object value;

	/**
	 * T ideally is a java.util class, which is directly supported by MapDB
	 * serializer
	 *
	 * @param name
	 * @param value
	 */
	public TimedElementValue(String name, Object value) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		this.time = calendar.getTimeInMillis() / 1000;
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public Object getValue() {
		return value;
	}

	public Long getTime() {
		return time;
	}
}
