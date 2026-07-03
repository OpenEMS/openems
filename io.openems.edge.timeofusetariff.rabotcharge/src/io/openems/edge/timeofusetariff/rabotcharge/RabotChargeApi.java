package io.openems.edge.timeofusetariff.rabotcharge;

import java.util.List;
import java.util.function.Function;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

public class RabotChargeApi {

	private RabotChargeApi() {
	}

	public record LinkResponse(String authorizationUrl) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link LinkResponse}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<LinkResponse> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(LinkResponse.class, json -> {

				return new LinkResponse(//
						json.getString("authorizationUrl") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("authorizationUrl", obj.authorizationUrl()) //
						.build();
			});
		}

	}

	public record Contracts(List<Contract> contracts) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Contracts}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Contracts> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(Contracts.class,
					json -> new Contracts(json.getList("data", Contract.serializer())),
					obj -> JsonUtils.buildJsonObject() //
							.add("data", Contract.serializer().toListSerializer().serialize(obj.contracts())) //
							.build());
		}

	}

	public record Contract(String contractNumber, String contractState) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Contract}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Contract> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(Contract.class,
					json -> new Contract(json.getString("contractNumber"), json.getString("contractState")),
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("contractNumber", obj.contractNumber()) //
							.addProperty("contractState", obj.contractState()) //
							.build());
		}

	}

	public record Customers(List<Customer> customers) {
		/**
		 * Returns a {@link JsonSerializer} for a {@link Customers}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Customers> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(Customers.class,
					json -> new Customers(json.getList("data", Customer.serializer())),
					obj -> JsonUtils.buildJsonObject() //
							.add("data", Customer.serializer().toListSerializer().serialize(obj.customers())) //
							.build());
		}
	}

	public record Customer(String customerNumber, String firstName, String lastName, String emailAddress) {
		/**
		 * Returns a {@link JsonSerializer} for a {@link Customer}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Customer> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(Customer.class, //
					json -> new Customer(//
							json.getString("customerNumber"), //
							json.getString("firstName"), //
							json.getString("lastName"), //
							json.getString("emailAddress")), //
					obj -> JsonUtils.buildJsonObject().addProperty("customerNumber", obj.customerNumber()) //
							.addProperty("firstName", obj.firstName()) //
							.addProperty("lastName", obj.lastName()) //
							.addProperty("emailAddress", obj.emailAddress()) //
							.build());
		}
	}

	public record PriceComponents(//
			double variableGridFee, //
			double concessionFee, //
			double kwkgFee, //
			double electricityTax, //
			double offshoreFee, //
			double p19Fee, //
			double exchangeFee, //
			double guaranteeOfOrigin, //
			double variableServiceFee //
	) {

		/**
		 * A default instance with all price components set to 0.0. Used as a fallback
		 * when cost data cannot be fetched.
		 */
		public static final PriceComponents DEFAULT = new PriceComponents(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

		/**
		 * Returns a {@link JsonSerializer} for a {@link PriceComponents}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<PriceComponents> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(PriceComponents.class, json -> {
				// TODO check nullable values
				final var data = json.getJsonObject("data");
				

				Function<String, Double> getNestedValue = (key) -> {
					var nestedOpt = JsonUtils.getAsOptionalJsonObject(data, key);
					if (nestedOpt.isPresent()) {
						return JsonUtils.getAsOptionalDouble(nestedOpt.get(), "value").orElse(0.0);
					}
					
					return 0.0;
				};

				return new PriceComponents(//
						getNestedValue.apply("variableGridFee"), //
						getNestedValue.apply("concessionFee"), //
						getNestedValue.apply("kwkgFee"), //
						getNestedValue.apply("electricityTax"), //
						getNestedValue.apply("offshoreFee"), //
						getNestedValue.apply("p19Fee"), //
						getNestedValue.apply("exchangeFee"), //
						getNestedValue.apply("guaranteeOfOrigin"), //
						getNestedValue.apply("variableServiceFee") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.add("data", JsonUtils.buildJsonObject() //
								.add("variableGridFee", JsonUtils.buildJsonObject() //
										.addProperty("value", obj.variableGridFee())//
										.build()) //
								.add("concessionFee", JsonUtils.buildJsonObject()//
										.addProperty("value", obj.concessionFee())//
										.build()) //
								.add("kwkgFee", JsonUtils.buildJsonObject()//
										.addProperty("value", obj.kwkgFee())//
										.build()) //
								.add("electricityTax", JsonUtils.buildJsonObject()//
										.addProperty("value", obj.electricityTax())//
										.build()) //
								.add("offshoreFee", JsonUtils.buildJsonObject()//
										.addProperty("value", obj.offshoreFee())//
										.build()) //
								.add("p19Fee", JsonUtils.buildJsonObject()//
										.addProperty("value", obj.p19Fee())//
										.build()) //
								.add("exchangeFee", JsonUtils.buildJsonObject()//
										.addProperty("value", obj.exchangeFee())//
										.build()) //
								.add("guaranteeOfOrigin", JsonUtils.buildJsonObject()//
										.addProperty("value", obj.guaranteeOfOrigin())//
										.build()) //
								.add("variableServiceFee",
										JsonUtils.buildJsonObject()//
										.addProperty("value", obj.variableServiceFee())//
												.build()) //
								.build())
						.build();
			});
		}

		public double getVariableFeesGross() {
			return this.variableGridFee() + this.concessionFee() + this.kwkgFee() + this.electricityTax()
					+ this.offshoreFee() + this.p19Fee() + this.exchangeFee() + this.guaranteeOfOrigin()
					+ this.variableServiceFee();
		}

	}

}