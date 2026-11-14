package io.openems.common.services.currency;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.util.Currency;
import java.util.Objects;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.DoubleUtils;
import io.openems.common.utils.JsonUtils;

public class ExchangeRate {
	private final ExchangeRateKey key;
	private final double rate;

	public ExchangeRate(ExchangeRateKey key, double rate) {
		this.key = key;
		this.rate = rate;
	}

	public ExchangeRateKey getKey() {
		return this.key;
	}

	public Currency getSourceCurrency() {
		return this.key.sourceCurrency();
	}

	public Currency getTargetCurrency() {
		return this.key.targetCurrency();
	}

	public double getConversionRate() {
		return this.rate;
	}

	/**
	 * Converts the given sourcePrice to the target currency.
	 * 
	 * @param sourcePrice Source price to convert.
	 * @return Target price.
	 */
	public double convertSourcePriceToTarget(double sourcePrice) {
		return DoubleUtils.roundPrice(sourcePrice * this.rate);
	}

	/**
	 * Converts the given targetPrice to the source currency.
	 *
	 * @param targetPrice Target price to convert.
	 * @return Source price.
	 */
	public double convertTargetPriceToSource(double targetPrice) {
		return DoubleUtils.roundPrice(targetPrice / this.rate);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ExchangeRate other && this.key.equals(other.key) && this.rate == other.rate;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.key, this.rate);
	}

	@Override
	public String toString() {
		return String.format("ExchangeRate[%s - %s => %.2f]", this.key.sourceCurrency(), this.key.targetCurrency(),
				this.rate);
	}

	/**
	 * Returns a {@link JsonSerializer} for a {@link ExchangeRate}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<ExchangeRate> serializer() {
		return jsonObjectSerializer(ExchangeRate.class, json -> {
			return new ExchangeRate(//
					new ExchangeRateKey(//
							Currency.getInstance(json.getString("sourceCurrency")), //
							Currency.getInstance(json.getString("targetCurrency"))), //
					json.getDouble("rate"));
		}, obj -> {
			return JsonUtils.buildJsonObject()//
					.addProperty("sourceCurrency", obj.getKey().sourceCurrency().getCurrencyCode()) //
					.addProperty("targetCurrency", obj.getKey().targetCurrency().getCurrencyCode()) //
					.addProperty("rate", obj.getConversionRate()) //
					.build();
		});
	}

	public record ExchangeRateKey(Currency sourceCurrency, Currency targetCurrency) {
	}
}
