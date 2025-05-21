package io.openems.edge.evse.api;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static java.lang.Math.round;

import com.google.gson.JsonNull;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;

public record Limit(SingleThreePhase phase, int minCurrent, int maxCurrent) {

	/**
	 * Gets the Min-Power in [W].
	 * 
	 * @return min power
	 */
	public int getMinPower() {
		return round(this.minCurrent / 1000f * this.phase.count * 230f);
	}

	/**
	 * Gets the Max-Power in [W].
	 * 
	 * @return max power
	 */
	public int getMaxPower() {
		return round(this.maxCurrent / 1000f * this.phase.count * 230f);
	}

	/**
	 * Calculates the Current from Power, considering {@link SingleThreePhase}.
	 * 
	 * <p>
	 * NOTE: result could be less then minCurrent or greater than maxCurrent
	 * 
	 * @param power the power
	 * @return the current
	 */
	public int calculateCurrent(int power) {
		return round((power * 1000) / this.phase.count / 230f);
	}

	/**
	 * Returns a {@link JsonSerializer} for a {@link Limit}.
	 * 
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<Limit> serializer() {
		return JsonSerializerUtil.jsonObjectSerializer(Limit.class, json -> {
			return new Limit(//
					json.getEnum("phase", SingleThreePhase.class), //
					json.getInt("minCurrent"), //
					json.getInt("maxCurrent"));
		}, obj -> {
			return obj == null //
					? JsonNull.INSTANCE //
					: buildJsonObject() //
							.addProperty("phase", obj.phase()) //
							.addProperty("minCurrent", obj.minCurrent()) //
							.addProperty("maxCurrent", obj.maxCurrent()) //
							.build();
		});
	}
}
