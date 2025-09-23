package io.openems.edge.scheduler.jscalendar;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jscalendar.JSCalendar.Task;
import io.openems.common.jsonrpc.serialization.JsonElementPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;

public class Utils {

	private Utils() {
	}

	protected static record HighPeriod(Instant from, Instant to, String[] controllerIds) {
	}

	public record Payload(String[] controllerIds) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Payload}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Payload> serializer() {
			return jsonObjectSerializer(Payload.class, json -> {

				final var result = json.getArray("controllerIds", String[]::new, JsonElementPath::getAsString);
				return new Payload(result);

			}, obj -> {
				return obj == null //
						? JsonNull.INSTANCE //
						: buildJsonObject() //
								.add("controllerIds", Stream.of(obj.controllerIds()) //
										.map(JsonPrimitive::new) //
										.collect(JsonUtils.toJsonArray())) //
								.build();
			});
		}
	}

	protected static HighPeriod getNextPeriod(ZonedDateTime now, ImmutableList<Task<Payload>> schedule) {
		return JSCalendar.Tasks.getNextOccurence(schedule, now) //
				.map(ot -> {
					if (ot.payload() == null) {
						return null;
					}

					return new HighPeriod(ot.start().toInstant(), ot.start().plus(ot.duration()).toInstant(),
							ot.payload().controllerIds());
				}) //
				.orElse(null);
	}

	protected static ImmutableList<Task<Payload>> parseConfig(String config) {
		try {
			return JSCalendar.Tasks.serializer(Payload.serializer()) //
					.deserialize(config);
		} catch (Exception e) {
			e.printStackTrace();
			return ImmutableList.of();
		}
	}
}
