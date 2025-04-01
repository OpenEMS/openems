package io.openems.edge.evse.api;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.getAsEnum;
import static io.openems.common.utils.JsonUtils.getAsInt;
import static java.lang.Math.round;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

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
	 * Serialize to {@link JsonElement}.
	 * 
	 * @param limit the {@link Limit}, possibly null
	 * @return the {@link JsonElement}
	 */
	public static JsonElement toJson(Limit limit) {
		if (limit == null) {
			return JsonNull.INSTANCE;
		}
		return buildJsonObject() //
				.addProperty("phase", limit.phase.name()) //
				.addProperty("minCurrent", limit.minCurrent) //
				.addProperty("maxCurrent", limit.maxCurrent) //
				.build();
	}

	/**
	 * Deserialize a {@link JsonObject} to a {@link Limit} instance.
	 * 
	 * @param j a {@link JsonObject}
	 * @throws OpenemsNamedException on error
	 */
	public static Limit fromJson(JsonObject j) throws OpenemsNamedException {
		return new Limit(//
				getAsEnum(SingleThreePhase.class, j, "phase"), //
				getAsInt(j, "minCurrent"), //
				getAsInt(j, "maxCurrent"));
	}
}
