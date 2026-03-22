package io.openems.common.types;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.time.Instant;

import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.JsonElementPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.TimeRangeValues;

public class MarketPriceData {
	private final TimeRangeValues<Double> values;
	private final String currency;
	private final Instant reportedDateByDataProvider;
	private final Instant lastFetchTime;

	public MarketPriceData(TimeRangeValues<Double> values, String currency, Instant reportedDateByDataProvider,
			Instant lastFetchTime) {
		this.values = values;
		this.currency = currency;
		this.reportedDateByDataProvider = reportedDateByDataProvider;
		this.lastFetchTime = lastFetchTime;
	}

	public TimeRangeValues<Double> getValues() {
		return this.values;
	}

	public String getCurrency() {
		return this.currency;
	}

	public Instant getReportedDateByDataProvider() {
		return this.reportedDateByDataProvider;
	}

	public Instant getLastFetchTime() {
		return this.lastFetchTime;
	}

	/**
	 * Returns a {@link JsonSerializer} for a {@link MarketPriceData}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<MarketPriceData> serializer() {
		var doubleSerializer = JsonSerializerUtil.jsonSerializer(Double.class, JsonElementPath::getAsDouble,
				JsonPrimitive::new);
		var valuesSerializer = TimeRangeValues.serializer(Double[]::new, doubleSerializer);

		return jsonObjectSerializer(MarketPriceData.class, //
				json -> {
					return new MarketPriceData(//
							json.getObject("values", valuesSerializer), //
							json.getString("currency"), //
							json.getInstant("reportedDateByDataProvider"), //
							json.getInstant("lastFetchTime") //
					);
				}, obj -> {
					return JsonUtils.buildJsonObject() //
							.add("values", obj.getValues(), valuesSerializer) //
							.addProperty("currency", obj.getCurrency()) //
							.addProperty("reportedDateByDataProvider", obj.getReportedDateByDataProvider()) //
							.addProperty("lastFetchTime", obj.getLastFetchTime()) //
							.build();
				});
	}

}
