package io.openems.common.jsonrpc.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.jsonrpc.type.ScheduleUpdate.EshData;
import io.openems.common.jsonrpc.type.ScheduleUpdate.OptimizationStatus;
import io.openems.common.jsonrpc.type.ScheduleUpdate.Period;
import io.openems.common.jsonrpc.type.ScheduleUpdate.PeriodType;
import io.openems.common.jsonrpc.type.ScheduleUpdate.SumData;

class ScheduleUpdateTest {

	@Test
	void testSerializer_shouldSerializeAndDeserializeCompleteScheduleUpdate_whenAllValuesArePresent() {
		final var expected = new ScheduleUpdate(//
				OptimizationStatus.OPTIMIZING_FUTURE, //
				Instant.parse("2026-09-17T08:00:00Z"), //
				"Start of new period", //
				ImmutableList.of(//
						new Period(//
								Instant.parse("2026-09-17T08:00:00Z"), //
								PeriodType.PREDICTION, //
								new SumData(//
										123.45, //
										45.67, //
										1000, //
										2000, //
										1500, //
										500, //
										70, //
										500), //
								ImmutableList.of(//
										new EshData(//
												"ctrlEssTimeOfUseTariff0", //
												1, //
												null), //
										new EshData(//
												"ctrlEvseSingle0", //
												2, //
												3200)))));

		final var json = ScheduleUpdate.serializer().serialize(expected);
		final var actual = ScheduleUpdate.serializer().deserialize(json);

		assertEquals(expected, actual);
	}

	@Test
	void testSerializer_shouldSerializeAndDeserializeInactiveScheduleUpdate_whenNoOptimizationDataIsAvailable() {
		final var expected = new ScheduleUpdate(//
				OptimizationStatus.INACTIVE, //
				null, //
				null, //
				ImmutableList.of());

		final var json = ScheduleUpdate.serializer().serialize(expected);
		final var actual = ScheduleUpdate.serializer().deserialize(json);

		assertEquals(expected, actual);

		final var jsonObject = json.getAsJsonObject();

		assertEquals("INACTIVE", jsonObject.get("status").getAsString());
		assertFalse(jsonObject.has("startedAt"));
		assertFalse(jsonObject.has("reason"));
		assertTrue(jsonObject.getAsJsonArray("data").isEmpty());
	}

	@Test
	void testPeriodSerializer_shouldSerializeAndDeserializeHistoryPeriod_whenValuesArePresent() {
		final var expected = new Period(//
				Instant.parse("2026-09-17T07:55:00Z"), //
				PeriodType.HISTORY, //
				new SumData(//
						100.0, //
						50.0, //
						500, //
						1000, //
						800, //
						200, //
						75, //
						300), //
				ImmutableList.of(//
						new EshData(//
								"ctrlEssTimeOfUseTariff0", //
								1, //
								null)));

		final var json = Period.serializer().serialize(expected);
		final var actual = Period.serializer().deserialize(json);

		assertEquals(expected, actual);

		final var jsonObject = json.getAsJsonObject();

		assertEquals("2026-09-17T07:55:00Z", jsonObject.get("timestamp").getAsString());
		assertEquals("HISTORY", jsonObject.get("type").getAsString());
		assertTrue(jsonObject.has("_sum"));
		assertTrue(jsonObject.has("eshs"));
	}

	@Test
	void testPeriodSerializer_shouldSerializeAndDeserializePredictionPeriod_whenValuesArePresent() {
		final var expected = new Period(//
				Instant.parse("2026-09-17T08:00:00Z"), //
				PeriodType.PREDICTION, //
				new SumData(//
						123.45, //
						null, //
						1200, //
						2300, //
						1800, //
						-500, //
						65, //
						600), //
				ImmutableList.of());

		final var json = Period.serializer().serialize(expected);
		final var actual = Period.serializer().deserialize(json);

		assertEquals(expected, actual);
		assertEquals(//
				"PREDICTION", //
				json.getAsJsonObject().get("type").getAsString());
	}

	@Test
	void testSumDataSerializer_shouldSerializeAndDeserializeAllValues_whenAllValuesArePresent() {
		final var expected = new SumData(//
				123.45, //
				45.67, //
				1000, //
				2000, //
				1500, //
				500, //
				70, //
				500);

		final var json = SumData.serializer().serialize(expected);
		final var actual = SumData.serializer().deserialize(json);

		assertEquals(expected, actual);

		final var jsonObject = json.getAsJsonObject();

		assertEquals(123.45, jsonObject.get("GridBuyPrice").getAsDouble());
		assertEquals(45.67, jsonObject.get("GridSellPrice").getAsDouble());
		assertEquals(1000, jsonObject.get("ProductionActivePower").getAsInt());
		assertEquals(2000, jsonObject.get("ConsumptionActivePower").getAsInt());
		assertEquals(1500, jsonObject.get("UnmanagedConsumptionActivePower").getAsInt());
		assertEquals(500, jsonObject.get("EssDischargePower").getAsInt());
		assertEquals(70, jsonObject.get("EssSoc").getAsInt());
		assertEquals(500, jsonObject.get("GridActivePower").getAsInt());
	}

	@Test
	void testSumDataSerializer_shouldOmitNullValues_whenOptionalValuesAreNull() {
		final var expected = new SumData(//
				123.45, //
				null, //
				null, //
				2000, //
				null, //
				null, //
				70, //
				null);

		final var json = SumData.serializer().serialize(expected);
		final var actual = SumData.serializer().deserialize(json);

		assertEquals(expected, actual);

		final var jsonObject = json.getAsJsonObject();

		assertEquals(123.45, jsonObject.get("GridBuyPrice").getAsDouble());
		assertEquals(2000, jsonObject.get("ConsumptionActivePower").getAsInt());
		assertEquals(70, jsonObject.get("EssSoc").getAsInt());

		assertFalse(jsonObject.has("GridSellPrice"));
		assertFalse(jsonObject.has("ProductionActivePower"));
		assertFalse(jsonObject.has("UnmanagedConsumptionActivePower"));
		assertFalse(jsonObject.has("EssDischargePower"));
		assertFalse(jsonObject.has("GridActivePower"));
	}

	@Test
	void testEshDataSerializer_shouldSerializeAndDeserializeAllValues_whenAllValuesArePresent() {
		final var expected = new EshData(//
				"ctrlEvseSingle0", //
				2, //
				3200);

		final var json = EshData.serializer().serialize(expected);
		final var actual = EshData.serializer().deserialize(json);

		assertEquals(expected, actual);

		final var jsonObject = json.getAsJsonObject();

		assertEquals("ctrlEvseSingle0", jsonObject.get("id").getAsString());
		assertEquals(2, jsonObject.get("mode").getAsInt());
		assertEquals(3200, jsonObject.get("managedConsumption").getAsInt());
	}

	@Test
	void testEshDataSerializer_shouldOmitOptionalValues_whenModeAndManagedConsumptionAreNull() {
		final var expected = new EshData(//
				"ctrlEssTimeOfUseTariff0", //
				null, //
				null);

		final var json = EshData.serializer().serialize(expected);
		final var actual = EshData.serializer().deserialize(json);

		assertEquals(expected, actual);

		final var jsonObject = json.getAsJsonObject();

		assertEquals("ctrlEssTimeOfUseTariff0", jsonObject.get("id").getAsString());
		assertFalse(jsonObject.has("mode"));
		assertFalse(jsonObject.has("managedConsumption"));
	}
}
