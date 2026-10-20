package io.openems.edge.common.meta;

import java.time.Clock;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

public record GridBuyLimit(//
		/**
		 * The continuous Hard-Limit for Grid-Buy Power.
		 * 
		 * <p>
		 * This value is derived from GridConnectionPointFuseLimit and
		 * {@link ChannelId#MAXIMUM_GRID_FEED_IN_LIMIT}; reduced by a safety buffer.
		 */
		Hard hard, //

		/**
		 * A Schedule for Grid-Buy Soft-Limits.
		 * 
		 * <p>
		 * Controllers will try to achieve this Soft-Limit, e.g. via Peak-Shaving with
		 * an ESS.
		 */
		JSCalendar.Tasks<Soft> soft) {

	public static final class Builder {
		private Hard hard;
		private JSCalendar.Tasks<Soft> soft;

		private Builder() {
		}

		public Builder setHard(Hard hard) {
			this.hard = hard;
			return this;
		}

		public Builder setSoft(JSCalendar.Tasks<Soft> soft) {
			this.soft = soft;
			return this;
		}

		public GridBuyLimit build() {
			return new GridBuyLimit(this.hard, this.soft);
		}
	}

	/**
	 * Create a {@link GridBuyLimit} builder.
	 *
	 * @return a {@link Builder}
	 */
	public static GridBuyLimit.Builder create() {
		return new Builder();
	}

	public record Hard(//
			/** Power in [W] */
			int power) {
	}

	public record Soft(//
			/** Power in [W] */
			int power) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GridBuyLimit.Soft}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Soft> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(Soft.class, json -> {
				return new Soft(json.getInt("power"));
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("power", obj.power()).build();
			});
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link JSCalendar.Tasks} of
		 * {@link Soft GridBuySoftLimits}.
		 *
		 * @param clock the {@link Clock}
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<JSCalendar.Tasks<Soft>> tasksSerializer(Clock clock) {
			return JSCalendar.Tasks.serializer(clock, Soft.serializer());
		}
	}
}
