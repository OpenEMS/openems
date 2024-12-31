package io.openems.edge.controller.evcs;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSortedSet.toImmutableSortedSet;
import static io.openems.common.utils.JsonUtils.getAsEnum;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.getAsLocalDateTime;
import static io.openems.common.utils.JsonUtils.getAsString;
import static io.openems.common.utils.JsonUtils.getAsUUID;
import static io.openems.common.utils.JsonUtils.getAsZonedDateTime;
import static io.openems.common.utils.JsonUtils.stream;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static io.openems.edge.controller.evcs.JSCalendar.RecurrenceFrequency.WEEKLY;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.temporal.ChronoField.NANO_OF_DAY;
import static java.time.temporal.TemporalAdjusters.nextOrSame;
import static java.util.Arrays.stream;
import static java.util.UUID.randomUUID;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.utils.JsonUtils;

/**
 * This implementation is based on RFC 8984 "JSCalendar: A JSON Representation
 * of Calendar Data".
 * 
 * <p>
 * 
 * @link <a href=
 *       "https://www.rfc-editor.org/rfc/rfc8984.html">https://www.rfc-editor.org/rfc/rfc8984.html</a>
 */
// CHECKSTYLE:OFF
public class JSCalendar<PAYLOAD> {
	// CHECKSTYLE:ON

	public static record Task<PAYLOAD>(UUID uid, ZonedDateTime updated, LocalDateTime start,
			ImmutableList<RecurrenceRule> recurrenceRules, PAYLOAD payload) {

		/**
		 * Parse a List of {@link Task}s from a {@link JsonArray}.
		 * 
		 * @param <PAYLOAD>     the type of the Payload
		 * @param json          the {@link JsonArray}
		 * @param payloadParser a parser for a Payload
		 * @return the List of {@link Task}s
		 * @throws OpenemsNamedException on error
		 */
		public static <PAYLOAD> ImmutableList<Task<PAYLOAD>> fromJson(JsonArray json,
				ThrowingFunction<JsonObject, PAYLOAD, OpenemsNamedException> payloadParser)
				throws OpenemsNamedException {
			return stream(json) //
					.map(j -> {
						try {
							return fromJson(JsonUtils.getAsJsonObject(j), payloadParser);
						} catch (OpenemsNamedException e) {
							e.printStackTrace();
							throw new NoSuchElementException(e.getMessage());
						}
					}) //
					.collect(toImmutableList());
		}

		/**
		 * Parse a {@link Task} from a {@link JsonObject}.
		 * 
		 * @param <PAYLOAD>     the type of the Payload
		 * @param json          the {@link JsonObject}
		 * @param payloadParser a parser for a Payload
		 * @return the {@link Task}
		 * @throws OpenemsNamedException on error
		 */
		public static <PAYLOAD> Task<PAYLOAD> fromJson(JsonObject json, Function<JsonObject, PAYLOAD> payloadParser)
				throws OpenemsNamedException {
			return fromJson(json, new ThrowingFunction<JsonObject, PAYLOAD, OpenemsNamedException>() {

				@Override
				public PAYLOAD apply(JsonObject json) throws OpenemsNamedException {
					return payloadParser.apply(json);
				}
			});
		}

		/**
		 * Parse a {@link Task} from a {@link JsonObject}.
		 * 
		 * @param <PAYLOAD>     the type of the Payload
		 * @param json          the {@link JsonObject}
		 * @param payloadParser a parser for a Payload
		 * @return the {@link Task}
		 * @throws OpenemsNamedException on error
		 */
		public static <PAYLOAD> Task<PAYLOAD> fromJson(JsonObject json,
				ThrowingFunction<JsonObject, PAYLOAD, OpenemsNamedException> payloadParser)
				throws OpenemsNamedException {
			var type = getAsString(json, "@type");
			if (!type.equalsIgnoreCase("Task")) {
				throw new OpenemsException("This is not a 'Task': " + type);
			}
			try {
				var uid = getAsUUID(json, "uid");
				var updated = getAsZonedDateTime(json, "updated");
				var start = getAsLocalDateTime(json, "start");
				var recurrenceRules = stream(getAsJsonArray(json, "recurrenceRules")) //
						.map(r -> {
							try {
								return RecurrenceRule.fromJson(r);
							} catch (OpenemsNamedException e) {
								e.printStackTrace();
								throw new NoSuchElementException(e.getMessage());
							}
						}) //
						.collect(toImmutableList());
				var payload = payloadParser.apply(getAsJsonObject(json, "payload"));
				return new Task<PAYLOAD>(uid, updated, start, recurrenceRules, payload);

			} catch (NoSuchElementException e) {
				throw new OpenemsException("NoSuchElementException: " + e.getMessage());
			}
		}

		public static class Builder<PAYLOAD> {
			private final UUID uid;
			private final ZonedDateTime updated;

			private LocalDateTime start = null;
			private ImmutableList.Builder<RecurrenceRule> recurrenceRules = ImmutableList.builder();
			private PAYLOAD payload = null;

			protected Builder() {
				this(randomUUID(), ZonedDateTime.now());
			}

			protected Builder(UUID uid, ZonedDateTime updated) {
				this.uid = uid;
				this.updated = updated;
			}

			public Builder<PAYLOAD> setStart(LocalDateTime start) {
				this.start = start;
				return this;
			}

			protected Builder<PAYLOAD> setStart(String start) {
				this.setStart(LocalDateTime.parse(start));
				return this;
			}

			/**
			 * Adds a {@link RecurrenceRule}.
			 * 
			 * @param recurrenceRule the {@link RecurrenceRule}
			 * @return myself
			 */
			public Builder<PAYLOAD> addRecurrenceRule(RecurrenceRule recurrenceRule) {
				this.recurrenceRules.add(recurrenceRule);
				return this;
			}

			/**
			 * Adds a {@link RecurrenceRule}.
			 * 
			 * @param consumer a RecurrenceRule Builder
			 * @return myself
			 */
			public Builder<PAYLOAD> addRecurrenceRule(Consumer<RecurrenceRule.Builder> consumer) {
				var builder = RecurrenceRule.create();
				consumer.accept(builder);
				this.recurrenceRules.add(builder.build());
				return this;
			}

			public Builder<PAYLOAD> setPayload(PAYLOAD payload) {
				this.payload = payload;
				return this;
			}

			public Task<PAYLOAD> build() {
				return new Task<PAYLOAD>(this.uid, this.updated, this.start, this.recurrenceRules.build(),
						this.payload);
			}
		}

		/**
		 * Create a {@link CalendarEvent} {@link Builder}.
		 * 
		 * @param <PAYLOAD> the type of the Payload
		 * @return a {@link Builder}
		 */
		public static <PAYLOAD> Builder<PAYLOAD> create() {
			return new Builder<PAYLOAD>();
		}

		/**
		 * Convert to {@link JsonObject}.
		 * 
		 * @param payloadConverter a converter for a Payload
		 * @return a {@link JsonObject}
		 */
		public JsonObject toJson(Function<PAYLOAD, JsonObject> payloadConverter) {
			var j = JsonUtils.buildJsonObject() //
					.addProperty("@type", "Task") //
					.addProperty("uid", this.uid.toString()) //
					.addProperty("updated", this.updated.format(ISO_INSTANT));
			if (this.start != null) {
				j.addProperty("start", this.start.format(ISO_LOCAL_DATE_TIME));
			}
			if (!this.recurrenceRules.isEmpty()) {
				j.add("recurrenceRules", this.recurrenceRules.stream() //
						.map(RecurrenceRule::toJson) //
						.collect(toJsonArray()));
			}
			if (this.payload != null) {
				j.add("payload", payloadConverter.apply(this.payload));
			}
			return j.build();
		}

		/**
		 * Gets the next occurence of the {@link Task} at or after a date.
		 * 
		 * @param from the from timestamp
		 * @return a {@link ZonedDateTime}
		 */
		public ZonedDateTime getNextOccurence(ZonedDateTime from) {
			var start = this.start.atZone(from.getZone());
			return this.recurrenceRules.stream() //
					.map(rr -> rr.getNextOccurence(from.isBefore(start) ? start : from, start)) //
					.min((o1, o2) -> o1.toInstant().compareTo(o2.toInstant())) //
					.orElse(null);
		}
	}

	public enum RecurrenceFrequency {
		// SECONDLY("secondly"),
		// MINUTELY("minutely"),
		// HOURLY("hourly"),
		DAILY("daily"), //
		WEEKLY("weekly"), //
		MONTHLY("monthly"), //
		YEARLY("yearly");

		public final String name;

		private RecurrenceFrequency(String name) {
			this.name = name;
		}
	}

	public record RecurrenceRule(RecurrenceFrequency frequency, ImmutableSortedSet<DayOfWeek> byDay) {

		/**
		 * Parse a {@link RecurrenceRule} from a {@link JsonObject}.
		 * 
		 * @param json the {@link JsonObject}
		 * @return the {@link RecurrenceRule}
		 * @throws OpenemsNamedException on error
		 */
		public static RecurrenceRule fromJson(JsonElement json) throws OpenemsNamedException, NoSuchElementException {
			var frequency = getAsEnum(RecurrenceFrequency.class, json, "frequency");
			var byDay = stream(getAsJsonArray(json, "byDay")) //
					.map(j -> switch (JsonUtils.getAsOptionalString(j).orElseThrow()) {
					case "mo" -> MONDAY;
					case "tu" -> TUESDAY;
					case "we" -> WEDNESDAY;
					case "th" -> THURSDAY;
					case "fr" -> FRIDAY;
					case "sa" -> SATURDAY;
					case "su" -> SUNDAY;
					default -> throw new NoSuchElementException("");
					}) //
					.collect(toImmutableSortedSet(Ordering.natural()));
			return new RecurrenceRule(frequency, byDay);
		}

		public static class Builder {
			private RecurrenceFrequency frequency;
			private ImmutableSortedSet.Builder<DayOfWeek> byDay = ImmutableSortedSet.naturalOrder();

			public Builder() {
			}

			public Builder setFrequency(RecurrenceFrequency frequency) {
				this.frequency = frequency;
				return this;
			}

			/**
			 * Adds a `byDay`rule.
			 * 
			 * @param byDay the {@link DayOfWeek}s
			 * @return myself
			 */
			public Builder addByDay(DayOfWeek... byDay) {
				stream(byDay).forEach(this.byDay::add);
				return this;
			}

			public RecurrenceRule build() {
				return new RecurrenceRule(this.frequency, this.byDay.build());
			}
		}

		/**
		 * Create a {@link RecurrenceRule} {@link Builder}.
		 * 
		 * @return a {@link Builder}
		 */
		public static Builder create() {
			return new Builder();
		}

		/**
		 * Gets the next occurence of the {@link RecurrenceRule} at or after a date.
		 * 
		 * @param from  the from date
		 * @param start the start timestamp of the {@link Task}
		 * @return a {@link ZonedDateTime}
		 */
		public ZonedDateTime getNextOccurence(ZonedDateTime from, ZonedDateTime start) {
			if (this.frequency == WEEKLY) {
				if (!this.byDay.isEmpty()) {
					var startTime = start.toLocalTime();
					var nextByDay = this.byDay.ceiling(from.toLocalTime().isAfter(startTime) //
							? from.getDayOfWeek().plus(1) // next day
							: from.getDayOfWeek()); // same day
					if (nextByDay == null) {
						nextByDay = this.byDay.first();
					}
					return from //
							.with(nextOrSame(nextByDay)) //
							.with(NANO_OF_DAY, startTime.toNanoOfDay());
				}
			}
			return null;
		}

		/**
		 * Convert to {@link JsonObject}.
		 * 
		 * @return a {@link JsonObject}
		 */
		public JsonObject toJson() {
			var j = JsonUtils.buildJsonObject();
			if (this.frequency != null) {
				j.addProperty("frequency", this.frequency.name);
			}
			if (!this.byDay.isEmpty()) {
				j.add("byDay", this.byDay.stream() //
						.map(d -> switch (d) {
						case MONDAY -> "mo";
						case TUESDAY -> "tu";
						case WEDNESDAY -> "we";
						case THURSDAY -> "th";
						case FRIDAY -> "fr";
						case SATURDAY -> "sa";
						case SUNDAY -> "su";
						}) //
						.map(JsonUtils::toJson) //
						.collect(toJsonArray()));
			}
			return j.build();
		}
	}

}
