package io.openems.edge.braiinsos.api;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.util.Optional;

import io.openems.common.jsonrpc.serialization.JsonSerializer;

public record MinerStats(//
		double realHashRateLast15s /* gigahash_per_second */, //
		int approximatedConsumption /* [W] */, //
		double efficiency /* joule_per_terahash */) {

	/**
	 * Returns a {@link JsonSerializer} for a {@link MinerStats}.
	 * 
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<MinerStats> serializer() {
		return jsonObjectSerializer(MinerStats.class, json -> {
			final var gigahashPerSecond = Optional.ofNullable(//
					json.getNullableJsonObjectPath("miner_stats") //
							.mapIfPresent(ms -> ms.getNullableJsonObjectPath("real_hashrate")) //
							.mapIfPresent(rhr -> rhr.getNullableJsonObjectPath("last_15s")) //
							.mapIfPresent(l15 -> l15.getDoubleOrDefault("gigahash_per_second", 0.))) //
					.orElse(0.);

			final var powerStats = json.getNullableJsonObjectPath("power_stats");
			final var approximatedConsumption = Optional.ofNullable(//
					powerStats //
							.mapIfPresent(ps -> ps.getNullableJsonObjectPath("approximated_consumption")) //
							.mapIfPresent(ac -> ac.getIntOrDefault("watt", 0))) //
					.orElse(0);
			final var efficiency = Optional.ofNullable(//
					powerStats //
							.mapIfPresent(ps -> ps.getNullableJsonObjectPath("efficiency")) //
							.mapIfPresent(ac -> ac.getDoubleOrDefault("joule_per_terahash", 0.))) //
					.orElse(0.);

			return new MinerStats(gigahashPerSecond, approximatedConsumption, efficiency);
		}, obj -> {
			return buildJsonObject() //
					.add("miner_stats", buildJsonObject() //
							.add("real_hashrate", buildJsonObject() //
									.add("last_15s", buildJsonObject() //
											.addProperty("gigahash_per_second", obj.realHashRateLast15s) //
											.build()) //
									.build()) //
							.build()) //
					.add("power_stats", buildJsonObject() //
							.add("approximated_consumption", buildJsonObject() //
									.addProperty("watt", obj.approximatedConsumption) //
									.build()) //
							.add("efficiency", buildJsonObject() //
									.addProperty("joule_per_terahash", obj.efficiency) //
									.build()) //
							.build()) //
					.build();
		});
	}
}