package io.openems.common.jsonrpc.syncable.types;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.util.Arrays;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.services.currency.ExchangeRate;
import io.openems.common.utils.JsonUtils;

public record SyncableExchangeRateData(ExchangeRate[] exchangeRates) {
	/**
	 * Returns JSON Serializer for {@link SyncableExchangeRateData}.
	 * 
	 * @return {@link JsonSerializer} instance.
	 */
	public static JsonSerializer<SyncableExchangeRateData> serializer() {
		return jsonObjectSerializer(SyncableExchangeRateData.class, //
				json -> {
					return new SyncableExchangeRateData(//
							json.getArray("exchangeRates", ExchangeRate[]::new, ExchangeRate.serializer()) //
					);
				}, obj -> {
					return JsonUtils.buildJsonObject() //
							.add("exchangeRates", obj.exchangeRates(),
									ExchangeRate.serializer().toArraySerializer(ExchangeRate[]::new)) //
							.build();
				});
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof SyncableExchangeRateData(ExchangeRate[] rates)
				&& Arrays.equals(this.exchangeRates, rates);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.exchangeRates);
	}

	@Override
	public String toString() {
		return Arrays.toString(this.exchangeRates);
	}
}
