package io.openems.edge.core.meta.geocoding;

import java.time.DateTimeException;
import java.time.ZoneId;

import io.openems.common.jsonrpc.serialization.JsonElementPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.currency.Currency;
import io.openems.edge.common.meta.types.CountryCode;
import io.openems.edge.common.meta.types.SubdivisionCode;

public record GeoResult(//
		String country, //
		CountryCode countryCode, //
		String subdivision, //
		SubdivisionCode subdivisionCode, //
		String placeName, //
		String postcode, //
		String road, //
		String houseNumber, //
		double latitude, //
		double longitude, //
		ZoneId timezone, //
		Currency currency, //
		String openStreetMapUrl) {

	/**
	 * Returns a {@link JsonSerializer} for deserializing OpenCage API JSON into a
	 * {@link GeoResult}.
	 * 
	 * <p>
	 * This serializer supports only deserialization. Serialization is not supported
	 * and will throw an {@link UnsupportedOperationException}.
	 *
	 * @return the {@link JsonSerializer} for OpenCage API JSON
	 */
	public static JsonSerializer<GeoResult> fromOpenCageApiJsonDeserializer() {
		return JsonSerializerUtil.jsonObjectSerializer(json -> {
			var components = json.getJsonObjectPath("components");
			var annotations = json.getNullableJsonObjectPath("annotations");

			var countryCode = components.getOptionalString("ISO_3166-1_alpha-2")//
					.map(CountryCode::fromCode)//
					.orElse(CountryCode.UNDEFINED);

			var subdivisionCode = components.getNullableJsonArrayPath("ISO_3166-2").mapToOptional(sc -> {
				var code = sc.getAsList(JsonElementPath::getAsString).getFirst();
				return SubdivisionCode.fromCode(code);
			}).orElse(SubdivisionCode.UNDEFINED);

			var placeName = components.getOptionalString("hamlet")//
					.or(() -> components.getOptionalString("village"))//
					.or(() -> components.getOptionalString("suburb"))//
					.or(() -> components.getOptionalString("town"))//
					.or(() -> components.getOptionalString("neighbourhood"))//
					.or(() -> components.getOptionalString("quarter"))//
					.or(() -> components.getOptionalString("city_district"))//
					.or(() -> components.getOptionalString("city"))//
					.orElse(null);

			var currency = annotations.mapIfPresent(ann -> {
				return ann.getNullableJsonObjectPath("currency").mapIfPresent(curr -> {
					return curr.getOptionalString("iso_code")//
							.map(Currency::fromCode)//
							.orElse(Currency.UNDEFINED);
				});
			});
			currency = (currency == null) ? Currency.UNDEFINED : currency;

			var timezone = annotations.mapIfPresent(ann -> {
				return ann.getNullableJsonObjectPath("timezone").mapIfPresent(tz -> {
					return tz.getOptionalString("name")//
							.map(name -> {
								try {
									return ZoneId.of(name);
								} catch (DateTimeException e) {
									return null;
								}
							}).orElse(null);
				});
			});

			var openStreetMapUrl = annotations.mapIfPresent(ann -> {
				return ann.getNullableJsonObjectPath("OSM").mapIfPresent(osm -> {
					return osm.getOptionalString("url")//
							.orElse(null);
				});
			});

			return new GeoResult(//
					components.getStringOrNull("country"), //
					countryCode, //
					components.getStringOrNull("state"), //
					subdivisionCode, //
					placeName, //
					components.getStringOrNull("postcode"), //
					components.getStringOrNull("road"), //
					components.getStringOrNull("house_number"), //
					json.getJsonObjectPath("geometry").getDouble("lat"), //
					json.getJsonObjectPath("geometry").getDouble("lng"), //
					timezone, //
					currency, //
					openStreetMapUrl);
		}, obj -> {
			throw new UnsupportedOperationException(
					"Serialization is not supported by this deserializer, which only handles parsing OpenCage API JSON into GeoResult");
		});
	}

	/**
	 * Returns a {@link JsonSerializer} for a {@link GeoResult}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<GeoResult> serializer() {
		return JsonSerializerUtil.jsonObjectSerializer(json -> {
			var countryCode = json.getOptionalString("countryCode")//
					.map(CountryCode::fromCode)//
					.orElse(CountryCode.UNDEFINED);

			var subdivisionCode = json.getOptionalString("subdivisionCode")//
					.map(SubdivisionCode::fromCode)//
					.orElse(SubdivisionCode.UNDEFINED);

			var timezone = json.getOptionalString("timezone")//
					.map(tz -> {
						try {
							return ZoneId.of(tz);
						} catch (DateTimeException e) {
							return null;
						}
					}).orElse(null);

			var currency = json.getOptionalString("currency")//
					.map(Currency::fromCode)//
					.orElse(Currency.UNDEFINED);

			return new GeoResult(//
					json.getStringOrNull("country"), //
					countryCode, //
					json.getStringOrNull("subdivision"), //
					subdivisionCode, //
					json.getStringOrNull("placeName"), //
					json.getStringOrNull("postcode"), //
					json.getStringOrNull("road"), //
					json.getStringOrNull("houseNumber"), //
					json.getDouble("latitude"), //
					json.getDouble("longitude"), //
					timezone, //
					currency, //
					json.getStringOrNull("openStreetMapUrl"));
		}, obj -> JsonUtils.buildJsonObject()//
				.addProperty("country", obj.country())//
				.addProperty("countryCode", (obj.countryCode() == null || obj.countryCode() == CountryCode.UNDEFINED) //
						? null //
						: obj.countryCode().toString())
				.addProperty("subdivision", obj.subdivision())//
				.addProperty("subdivisionCode",
						(obj.subdivisionCode() == null || obj.subdivisionCode() == SubdivisionCode.UNDEFINED) //
								? null //
								: obj.subdivisionCode().toString())
				.addProperty("placeName", obj.placeName())//
				.addProperty("postcode", obj.postcode())//
				.addProperty("road", obj.road())//
				.addProperty("houseNumber", obj.houseNumber())//
				.addProperty("latitude", obj.latitude())//
				.addProperty("longitude", obj.longitude())//
				.addProperty("timezone", obj.timezone() == null //
						? null //
						: obj.timezone().toString())//
				.addProperty("currency", (obj.currency() == null || obj.currency() == Currency.UNDEFINED) //
						? null //
						: obj.currency().toString())
				.addProperty("openStreetMapUrl", obj.openStreetMapUrl())//
				.build());
	}
}
