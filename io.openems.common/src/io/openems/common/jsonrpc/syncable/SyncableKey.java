package io.openems.common.jsonrpc.syncable;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.emptyObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import com.google.gson.JsonNull;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.jsonrpc.serialization.PolymorphicSerializer;
import io.openems.common.jsonrpc.syncable.types.SyncableExchangeRateData;
import io.openems.common.types.BiddingZone;
import io.openems.common.types.TariffData;

public sealed interface SyncableKey<VALUE> {
	/**
	 * Gets the serializer for the value of this syncable key.
	 *
	 * @return the value serializer
	 */
	JsonSerializer<VALUE> getValueSerializer();

	/**
	 * Gets who is responsible to get the value for this syncable key.
	 *
	 * @return the provider type
	 */
	SyncableProviderType getProviderType();

	record CurrencyExchangeRates() implements SyncableKey<SyncableExchangeRateData> {

		@Override
		public JsonSerializer<SyncableExchangeRateData> getValueSerializer() {
			return SyncableExchangeRateData.serializer();
		}

		@Override
		public SyncableProviderType getProviderType() {
			return SyncableProviderType.BACKEND_EDGE_SERVER;
		}
	}

	record SpotTariff(BiddingZone biddingZone) implements SyncableKey<TariffData> {

		@Override
		public JsonSerializer<TariffData> getValueSerializer() {
			return TariffData.serializer();
		}

		@Override
		public SyncableProviderType getProviderType() {
			return SyncableProviderType.BACKEND_EDGE_SERVER;
		}

		static JsonSerializer<SpotTariff> keySerializer() {
			return JsonSerializerUtil.jsonSerializer(//
					json -> new SpotTariff(json.getAsJsonObjectPath().getEnum("biddingZone", BiddingZone.class)), //
					obj -> {
						return buildJsonObject() //
								.addProperty("biddingZone", obj.biddingZone()) //
								.build();
					});
		}
	}

	record CurrentTimeInUnixTimestamp() implements SyncableKey<Long> {
		@Override
		public JsonSerializer<Long> getValueSerializer() {
			return JsonSerializerUtil.jsonSerializer(//
					json -> json.getAsJsonObjectPath().getLong("time"), //
					obj -> {
						return buildJsonObject() //
								.addProperty("time", obj) //
								.build();
					});
		}

		@Override
		public SyncableProviderType getProviderType() {
			return SyncableProviderType.BACKEND_EDGE_SERVER;
		}
	}

	record TimeDataTransferAllowed() implements SyncableKey<Boolean> {
		@Override
		public JsonSerializer<Boolean> getValueSerializer() {
			return JsonSerializerUtil.jsonSerializer(//
					json -> json.getAsJsonObjectPath().getBoolean("allowed"), //
					obj -> {
						return buildJsonObject() //
								.addProperty("allowed", obj) //
								.build();
					});
		}

		@Override
		public SyncableProviderType getProviderType() {
			return SyncableProviderType.BACKEND_EDGE_SERVER;
		}
	}

	/**
	 * Creates the json serializer for {@link SyncableKey}.
	 *
	 * @return json serializer
	 */
	static JsonSerializer<SyncableKey<?>> keySerializer() {
		final var polymorphicSerializer = PolymorphicSerializer.<SyncableKey<?>>create()
				.add(CurrencyExchangeRates.class, emptyObjectSerializer(CurrencyExchangeRates::new),
						CurrencyExchangeRates.class.getSimpleName()) //
				.add(SpotTariff.class, SpotTariff.keySerializer(), SpotTariff.class.getSimpleName()) //
				.add(CurrentTimeInUnixTimestamp.class, emptyObjectSerializer(CurrentTimeInUnixTimestamp::new),
						CurrentTimeInUnixTimestamp.class.getSimpleName()) //
				.add(TimeDataTransferAllowed.class, emptyObjectSerializer(TimeDataTransferAllowed::new),
						TimeDataTransferAllowed.class.getSimpleName()) //
				.build();

		return JsonSerializerUtil.jsonSerializer(
				json -> json.polymorphic(polymorphicSerializer, t -> t.getAsJsonObjectPath().getStringPath("type")), //
				(SyncableKey<?> obj) -> {
					if (obj == null) {
						return JsonNull.INSTANCE;
					}
					var serialized = polymorphicSerializer.serialize(obj).getAsJsonObject();
					serialized.addProperty("type", obj.getClass().getSimpleName());
					return serialized;
				});
	}
}
