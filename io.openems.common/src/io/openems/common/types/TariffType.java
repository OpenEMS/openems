package io.openems.common.types;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.util.function.Supplier;

import com.google.gson.JsonNull;

import io.openems.common.channel.Unit;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.PolymorphicSerializer;

public sealed interface TariffType {

	default Unit getUnit() {
		return Unit.MONEY_PER_KILOWATT_HOUR;
	}

	record DayAheadEpexSpot(BiddingZone biddingZone) implements TariffType {

		@Override
		public Unit getUnit() {
			return Unit.MONEY_PER_MEGAWATT_HOUR;
		}

		public static JsonSerializer<DayAheadEpexSpot> serializer() {
			return jsonObjectSerializer(//
					json -> new DayAheadEpexSpot(json.getEnum("biddingZone", BiddingZone.class)), //
					obj -> buildJsonObject() //
							.addProperty("biddingZone", obj.biddingZone()).build());
		}
	}

	record BuyGross() implements TariffType {
	}

	record BuyNet() implements TariffType {
	}

	record SellGross() implements TariffType {
	}

	record SellNet() implements TariffType {
	}

	record BuyGridFees() implements TariffType {
	}

	record SellGridFees() implements TariffType {
	}

	static JsonSerializer<TariffType> serializer() {
		final var polymorphicSerializer = PolymorphicSerializer.<TariffType>create() //
				.add(DayAheadEpexSpot.class, DayAheadEpexSpot.serializer(), DayAheadEpexSpot.class.getSimpleName()) //
				.add(BuyGross.class, emptySerializer(BuyGross::new), BuyGross.class.getSimpleName()) //
				.add(BuyNet.class, emptySerializer(BuyNet::new), BuyNet.class.getSimpleName()) //
				.add(SellGross.class, emptySerializer(SellGross::new), SellGross.class.getSimpleName()) //
				.add(SellNet.class, emptySerializer(SellNet::new), SellNet.class.getSimpleName()) //
				.add(BuyGridFees.class, emptySerializer(BuyGridFees::new), BuyGridFees.class.getSimpleName()) //
				.add(SellGridFees.class, emptySerializer(SellGridFees::new), SellGridFees.class.getSimpleName()) //
				.build();

		return jsonSerializer(TariffType.class,
				json -> json.polymorphic(polymorphicSerializer, t -> t.getAsJsonObjectPath().getStringPath("class")), //
				obj -> {
					if (obj == null) {
						return JsonNull.INSTANCE;
					}
					var serialized = polymorphicSerializer.serialize(obj).getAsJsonObject();
					serialized.addProperty("class", obj.getClass().getSimpleName());
					return serialized;
				});
	}

	private static <T> JsonSerializer<T> emptySerializer(Supplier<T> createInstanceFunc) {
		return jsonObjectSerializer(//
				json -> createInstanceFunc.get(), //
				obj -> buildJsonObject() //
						.build());
	}
}
