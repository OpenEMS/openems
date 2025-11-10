package io.openems.edge.timeofusetariff.luox;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.primitives.Doubles;
import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.jsonrpc.serialization.StringParser;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

public final class LuoxApi {

	public static final float VAT_GERMANY = 1.19F; // German Mehrwehrtsteuer (MWSt)

	private LuoxApi() {
	}

	private static JsonSerializer<Double> serializerDoubleFromString() {
		return JsonSerializerUtil.jsonSerializer(Double.class, json -> {
			return json.getAsStringParsed(new StringParser<Double>() {

				@Override
				public Double parse(String value) {
					return Doubles.tryParse(value);
				}

				@Override
				public ExampleValues<Double> getExample() {
					return new ExampleValues<>("0", 0.0);
				}
			});
		}, JsonPrimitive::new);
	}

	public record PricesResponse(List<Prices> prices) {

		/**
		 * Convert to {@link TimeOfUsePrices}.
		 * 
		 * @return {@link TimeOfUsePrices}
		 */
		public TimeOfUsePrices toTimeOfUsePrices() {
			var validFrom = this.prices.stream().map(Prices::validFrom)
					.min(Comparator.comparing(ZonedDateTime::toInstant)).get();
			var validUntil = this.prices.stream().map(Prices::validUntil)
					.max(Comparator.comparing(ZonedDateTime::toInstant)).get();

			var result = IntStream.iterate(0, i -> i + 1) //
					.mapToObj(i -> validFrom.plusMinutes(i * 15)) //
					.takeWhile(time -> time.isBefore(validUntil)) //
					.collect(ImmutableSortedMap.<ZonedDateTime, ZonedDateTime, Double>toImmutableSortedMap(//
							Comparator.naturalOrder(), //
							Function.identity(), //
							time -> {
								var variablePrice = this.prices.stream() //
										.flatMap(p -> p.variablePrices.stream()) //
										.filter(p -> !time.isBefore(p.validFrom) && time.isBefore(p.validUntil)) //
										.findFirst().get();
								return variablePrice.totalPriceNet //
										* -10 // invert and convert to EUR/MWh
										* VAT_GERMANY; // convert to gross price
							}));
			return TimeOfUsePrices.from(result);
		}

		public record Prices(//
				ZonedDateTime validFrom, //
				ZonedDateTime validUntil, //
				FixedPrice fixedPrice, //
				List<VariablePrice> variablePrices //
		) {

			public record FixedPrice(double totalPriceNet, String unit) {
				/**
				 * Returns a {@link JsonSerializer} for a {@link FixedPrice}.
				 *
				 * @return the created {@link JsonSerializer}
				 */
				public static JsonSerializer<FixedPrice> serializer() {
					return JsonSerializerUtil.jsonObjectSerializer(FixedPrice.class, json -> {
						return new FixedPrice(json.getObject("total_price_net", serializerDoubleFromString()), //
								json.getString("unit"));
					}, obj -> {
						return JsonUtils.buildJsonObject() //
								.addProperty("total_price_net", obj.totalPriceNet()) //
								.addProperty("unit", obj.unit()) //
								.build();
					});
				}
			}

			public record VariablePrice(//
					ZonedDateTime validFrom, //
					ZonedDateTime validUntil, //
					double energyPriceNet, //
					double taxesAndLeviesNet, //
					double feesNet, //
					double gridCostsNet, //
					String unit, //
					double totalPriceNet //
			) {

				/**
				 * Returns a {@link JsonSerializer} for a {@link VariablePrice}.
				 *
				 * @return the created {@link JsonSerializer}
				 */
				public static JsonSerializer<VariablePrice> serializer() {
					return JsonSerializerUtil.jsonObjectSerializer(VariablePrice.class, json -> {
						return new VariablePrice(//
								json.getZonedDateTime("valid_from"), //
								json.getZonedDateTime("valid_until"), //
								json.getObject("energy_price_net", serializerDoubleFromString()), //
								json.getObject("taxes_and_levies_net", serializerDoubleFromString()), //
								json.getObject("fees_net", serializerDoubleFromString()), //
								json.getObject("grid_costs_net", serializerDoubleFromString()), //
								json.getString("unit"), //
								json.getObject("total_price_net", serializerDoubleFromString()) //
						);
					}, obj -> {
						return JsonUtils.buildJsonObject() //
								.addProperty("valid_from", obj.validFrom().toString()) //
								.addProperty("valid_until", obj.validUntil().toString()) //
								.addProperty("energy_price_net", obj.energyPriceNet()) //
								.addProperty("taxes_and_levies_net", obj.taxesAndLeviesNet()) //
								.addProperty("fees_net", obj.feesNet()) //
								.addProperty("grid_costs_net", obj.gridCostsNet()) //
								.addProperty("unit", obj.unit()) //
								.addProperty("total_price_net", obj.totalPriceNet()) //
								.build();
					});
				}

			}

			/**
			 * Returns a {@link JsonSerializer} for a {@link Prices}.
			 *
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<Prices> serializer() {
				return JsonSerializerUtil.jsonObjectSerializer(Prices.class, json -> {
					return new Prices(//
							json.getZonedDateTime("valid_from"), //
							json.getZonedDateTime("valid_until"), //
							json.getObject("fixed_price", FixedPrice.serializer()), //
							json.getList("variable_prices", VariablePrice.serializer()) //
					);
				}, obj -> {
					return JsonUtils.buildJsonObject() //
							.addProperty("valid_from", obj.validFrom().toString()) //
							.addProperty("valid_until", obj.validUntil().toString()) //
							.add("fixed_price", FixedPrice.serializer().serialize(obj.fixedPrice())) //
							.add("variable_prices",
									VariablePrice.serializer().toListSerializer().serialize(obj.variablePrices())) //
							.build();
				});
			}
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link PricesResponse}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<PricesResponse> serializer() {
			return JsonSerializerUtil.jsonSerializer(PricesResponse.class, json -> {
				return new PricesResponse(json.getAsObject(Prices.serializer().toListSerializer()));
			}, obj -> {
				return Prices.serializer().toListSerializer().serialize(obj.prices());
			});
		}

	}

	public record ContractDetailsResponse(List<ContractDetails> details) {
		public record ContractDetails(String saasContractId) {

			/**
			 * Returns a {@link JsonSerializer} for a {@link ContractDetails}.
			 *
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<ContractDetails> serializer() {
				return JsonSerializerUtil.jsonObjectSerializer(ContractDetails.class, json -> {
					return new ContractDetails(//
							json.getString("saas_contract_id") //
					);
				}, obj -> {
					return JsonUtils.buildJsonObject() //
							.addProperty("saas_contract_id", obj.saasContractId()) //
							.build();
				});
			}

		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link ContractDetailsResponse}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<ContractDetailsResponse> serializer() {
			return JsonSerializerUtil.jsonSerializer(ContractDetailsResponse.class, json -> {
				return new ContractDetailsResponse(json.getAsObject(ContractDetails.serializer().toListSerializer()));
			}, obj -> {
				return ContractDetails.serializer().toListSerializer().serialize(obj.details());
			});
		}

	}

}
