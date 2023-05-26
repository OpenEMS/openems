package io.openems.common.types;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.ops4j.pax.logging.spi.PaxLevel;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;

/**
 * Holds a System-Log line.
 */
public class SystemLog {

	private static final Logger log = LoggerFactory.getLogger(SystemLog.class);

	public static enum Level {
		INFO, WARN, ERROR;

		/**
		 * Converts a PaxLevel to this Level via Syslog codes:
		 * {@link org.apache.log4j.Level}.
		 *
		 * @param paxLevel the PaxLevel
		 * @return a Level enum
		 */
		public static Level fromPaxLevel(PaxLevel paxLevel) {
			return switch (paxLevel.getSyslogEquivalent()) {
			case 0, // FATAL/OFF
			3 ->  // ERROR
				 ERROR;
			case 4 -> // WARN
				 WARN;
			case 6, // INFO
			     7 -> // DEBUG/TRACE/ALL
				 INFO;
			default -> {
				 SystemLog.log.warn("Undefined PaxLevel [" + paxLevel.toString() + "/" + paxLevel.getSyslogEquivalent()
						+ "] . Falling back to [INFO].");
				yield  INFO;
			}
			};			
		}
	}

	/**
	 * Creates a SystemLog object from a PaxLoggingEvent.
	 *
	 * @param event the PaxLoggingEvent
	 * @return the SystemLog object
	 */
	public static SystemLog fromPaxLoggingEvent(PaxLoggingEvent event) {
		var level = Level.fromPaxLevel(event.getLevel());
		var time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(event.getTimeStamp()), ZoneId.systemDefault());
		var source = event.getLoggerName();
		var message = event.getRenderedMessage();
		return new SystemLog(time, level, source, message);
	}

	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	private final ZonedDateTime time;
	private final Level level;
	private final String source;
	private final String message;

	public SystemLog(ZonedDateTime time, Level level, String source, String message) {
		this.time = time;
		this.level = level;
		this.source = source;
		this.message = message;
	}

	/**
	 * Returns the SystemLog as a JSON Object.
	 *
	 * <pre>
	 * {
	 *   "time": date,
	 *   "level": string,
	 *   "source": string,
	 *   "message": string
	 * }
	 * </pre>
	 *
	 * @return SystemLog as a JSON Object
	 */
	public JsonObject toJson() {
		return JsonUtils.buildJsonObject() //
				.addProperty("time", SystemLog.FORMAT.format(this.time)) //
				.addProperty("level", this.level.toString()) //
				.addProperty("source", this.source) //
				.addProperty("message", this.message) //
				.build();
	}

	/**
	 * Parses a JSON-Object to a SystemLog.
	 *
	 * @param j the JSON-Object
	 * @return the SystemLog
	 * @throws OpenemsNamedException on error
	 */
	public static SystemLog fromJsonObject(JsonObject j) throws OpenemsNamedException {
		var time = ZonedDateTime.parse(JsonUtils.getAsString(j, "time"), SystemLog.FORMAT);
		var level = Level.valueOf(JsonUtils.getAsString(j, "level").toUpperCase());
		var source = JsonUtils.getAsString(j, "source");
		var message = JsonUtils.getAsString(j, "message");
		return new SystemLog(time, level, source, message);
	}

}
