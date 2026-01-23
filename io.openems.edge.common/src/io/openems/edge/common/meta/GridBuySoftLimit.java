package io.openems.edge.common.meta;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

public record GridBuySoftLimit(int power) {

	/**
	 * Returns a {@link JsonSerializer} for a {@link GridBuySoftLimit}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<GridBuySoftLimit> serializer() {
		return JsonSerializerUtil.jsonObjectSerializer(GridBuySoftLimit.class, json -> {
			return new GridBuySoftLimit(json.getInt("power"));
		}, obj -> {
			return JsonUtils.buildJsonObject() //
					.addProperty("power", obj.power()).build();
		});
	}

	/**
	 * Returns a {@link JsonSerializer} for a {@link JSCalendar.Tasks} of
	 * {@link GridBuySoftLimit GridBuySoftLimits}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<JSCalendar.Tasks<GridBuySoftLimit>> tasksSerializer() {
		return JSCalendar.Tasks.serializer(GridBuySoftLimit.serializer());
	}
}