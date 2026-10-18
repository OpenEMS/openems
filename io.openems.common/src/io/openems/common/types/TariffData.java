package io.openems.common.types;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.JsonElementPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.services.currency.CurrencyExchange;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.TimeRangeValues;
import io.openems.common.utils.TimeSpan;

public record TariffData(TariffType tariffType, TimeRangeValues<Double> prices, Currency currency, Instant fetchTime,
		Instant lastUpdate, String copyright) {

	/**
	 * Combines multiple tariff data sets into a single tariff series.
	 *
	 * <p>
	 * Tariffs are processed in list order. Missing price values are filled from the
	 * next available tariff until the result is complete. If a target currency is
	 * provided, each tariff is converted before it is merged.
	 *
	 * <p>
	 * The resulting tariff uses the tariff type of the first entry, the latest
	 * fetch/update timestamps of all used entries, and a concatenated copyright
	 * string of all distinct non-empty copyrights.
	 *
	 * @param list             the tariff data sets to combine, in merge priority
	 *                         order
	 * @param currency         the target currency; if {@code null}, the currency of
	 *                         the first tariff is used
	 * @param currencyExchange the exchange service used for optional currency
	 *                         conversion
	 * @return the combined tariff data; or {@code null} if the list is empty
	 */
	public static TariffData byCombination(List<TariffData> list, Currency currency,
			CurrencyExchange currencyExchange) {
		if (list.isEmpty()) {
			return null;
		}

		final var first = list.getFirst();
		final var timeSpan = TimeSpan.rangeByTimeSpans(list.stream().map(t -> t.prices().getTimeSpan()).toList());
		final var builder = TimeRangeValues.builder(timeSpan, DurationUnit.ofMinutes(15), Double.class);
		final var usedData = new ArrayList<TariffData>();

		for (var data : list) {
			var dataWithCorrectCurrency = currency != null ? data.withCurrency(currency, currencyExchange) : data;
			builder.fillMissingDataFromData(dataWithCorrectCurrency.prices());
			usedData.add(dataWithCorrectCurrency);

			if (builder.isComplete()) {
				break;
			}
		}

		final var finalPrices = builder.build();
		final var finalCopyright = usedData.stream() //
				.map(TariffData::copyright) //
				.filter(x -> x != null && !x.isEmpty()) //
				.distinct() //
				.collect(Collectors.joining(", "));
		final var finalFetchTime = usedData.stream() //
				.map(TariffData::fetchTime) //
				.max(Comparator.naturalOrder()) //
				.get();
		final var finalLastUpdate = usedData.stream() //
				.map(TariffData::lastUpdate) //
				.max(Comparator.naturalOrder()) //
				.get();

		final var finalCurrency = currency != null ? currency : first.currency();
		return new TariffData(first.tariffType(), finalPrices, finalCurrency, finalFetchTime, finalLastUpdate,
				finalCopyright);
	}

	/**
	 * Converts tariff prices to a target currency using the provided currency
	 * exchange.
	 *
	 * <p>
	 * If the current currency already matches the target currency, this instance is
	 * returned unchanged. Prices that cannot be converted are skipped.
	 *
	 * @param targetCurrency   the currency to convert prices to
	 * @param currencyExchange the exchange service used for price conversion
	 * @return a {@link TariffData} in the target currency
	 */
	public TariffData withCurrency(Currency targetCurrency, CurrencyExchange currencyExchange) {
		if (this.currency.equals(targetCurrency)) {
			return this;
		}

		var priceBuilder = TimeRangeValues.builder(this.prices.getTimeSpan(), this.prices.getResolution(),
				Double.class);
		for (var priceEntry : this.prices.entries()) {
			currencyExchange.convertPrice(priceEntry.value(), this.currency, targetCurrency) //
					.ifPresent(newPrice -> priceBuilder.setByTime(priceEntry.timeSpan().getStartInclusive(), newPrice));
		}

		return new TariffData(this.tariffType, priceBuilder.build(), targetCurrency, this.fetchTime, this.lastUpdate,
				this.copyright);
	}

	@Override
	public String toString() {
		return new StringBuilder("TariffData[") //
				.append("tariffType=").append(this.tariffType) //
				.append(", priceRange=").append(this.prices.getTimeSpan()) //
				.append(", currency=").append(this.currency.getCurrencyCode()) //
				.append(", copyright=").append(this.copyright) //
				.append(", lastUpdate=").append(this.lastUpdate) //
				.append("]") //
				.toString();
	}

	public static JsonSerializer<TariffData> serializer() {
		var doubleSerializer = JsonSerializerUtil.jsonSerializer(Double.class, JsonElementPath::getAsDouble,
				JsonPrimitive::new);
		var valuesSerializer = TimeRangeValues.serializer(Double[]::new, doubleSerializer);

		return jsonObjectSerializer(TariffData.class, //
				json -> {
					return new TariffData(//
							json.getObject("type", TariffType.serializer()), //
							json.getObject("prices", valuesSerializer), //
							Currency.getInstance(json.getString("currency")), //
							json.getInstant("fetchTime"), //
							json.getInstant("lastUpdate"), //
							json.getStringOrNull("copyright") //
					);
				}, obj -> {
					return JsonUtils.buildJsonObject() //
							.add("type", obj.tariffType(), TariffType.serializer()) //
							.add("prices", obj.prices(), valuesSerializer) //
							.addProperty("currency", obj.currency().getCurrencyCode()) //
							.addProperty("fetchTime", obj.fetchTime()) //
							.addProperty("lastUpdate", obj.lastUpdate()) //
							.addProperty("copyright", obj.copyright()) //
							.build();
				});
	}
}
