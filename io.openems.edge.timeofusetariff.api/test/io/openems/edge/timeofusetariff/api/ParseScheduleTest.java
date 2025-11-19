package io.openems.edge.timeofusetariff.api;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.utils.JsonUtils.parseToJsonArray;
import static io.openems.edge.timeofusetariff.api.AncillaryCosts.parseSchedule;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;

public class ParseScheduleTest {

	@Test
	public void testValidSchedule_parsesCorrectly() throws Exception {
		final var json = """
				[
				  {
				    "year": 2025,
				    "tariffs": {
				      "low": 0.10,
				      "standard": 0.20,
				      "high": 0.30
				    },
				    "quarters": [
				      {
				        "quarter": 1,
				        "dailySchedule": [
				          { "tariff": "low", "from": "00:00", "to": "06:00" },
				          { "tariff": "standard", "from": "06:00", "to": "18:00" },
				          { "tariff": "high", "from": "18:00", "to": "00:00" }
				        ]
				      }
				    ]
				  }
				]
				""";

		final var clock = createDummyClock();
		final var schedule = parseSchedule(clock, parseToJsonArray(json));

		assertEquals(3, schedule.numberOfTasks());
		assertEquals(0.10, schedule.tasks.get(0).payload().doubleValue(), 0.001);
		assertEquals(0.20, schedule.tasks.get(1).payload().doubleValue(), 0.001);
		assertEquals(0.30, schedule.tasks.get(2).payload().doubleValue(), 0.001);
	}

	@Test
	public void testOverlapBetweenLowAndHighTariff_throwsException() throws OpenemsNamedException {
		final var json = """
				[
				  {
				    "year": 2025,
				    "tariffs": {
				      "low": 0.10,
				      "standard": 0.20,
				      "high": 0.30
				    },
				    "quarters": [
				      {
				        "quarter": 1,
				        "dailySchedule": [
				          { "tariff": "low", "from": "00:00", "to": "08:00" },
				          { "tariff": "high", "from": "07:00", "to": "10:00" }
				        ]
				      }
				    ]
				  }
				]
				""";

		final var schedule = parseToJsonArray(json);
		final var clock = createDummyClock();
		final var ex = assertThrows(OpenemsException.class, () -> parseSchedule(clock, schedule));

		assertTrue(ex.getMessage().contains("overlaps"));
	}

	@Test
	public void testInvalidTimeFormat_throwsException() throws OpenemsNamedException {
		final var json = """
				[
				  {
				    "year": 2025,
				    "tariffs": {
				      "low": 0.10,
				      "standard": 0.20,
				      "high": 0.30
				    },
				    "quarters": [
				      {
				        "quarter": 1,
				        "dailySchedule": [
				          { "tariff": "low", "from": "INVALID", "to": "06:00" }
				        ]
				      }
				    ]
				  }
				]
				""";
		final var schedule = parseToJsonArray(json);
		final var clock = createDummyClock();
		final var ex = assertThrows(OpenemsException.class, () -> parseSchedule(clock, schedule));
		assertTrue(ex.getMessage().contains("Invalid time format"));
	}

	@Test
	public void testFromAfterTo_throwsException() throws OpenemsNamedException {
		final var json = """
				[
				  {
				    "year": 2025,
				    "tariffs": {
				      "low": 0.10,
				      "standard": 0.20,
				      "high": 0.30
				    },
				    "quarters": [
				      {
				        "quarter": 1,
				        "dailySchedule": [
				          { "tariff": "low", "from": "10:00", "to": "08:00" }
				        ]
				      }
				    ]
				  }
				]
				""";

		final var schedule = parseToJsonArray(json);
		final var clock = createDummyClock();
		final var ex = assertThrows(OpenemsException.class, () -> parseSchedule(clock, schedule));
		assertTrue(ex.getMessage().contains("Invalid time range"));
	}

	@Test
	public void testUnexpectedTariff_throwsException() throws OpenemsNamedException {
		final var json = """
				[
				  {
				    "year": 2025,
				    "tariffs": {
				      "low": 0.10,
				      "standard": 0.20,
				      "high": 0.30
				    },
				    "quarters": [
				      {
				        "quarter": 1,
				        "dailySchedule": [
				          { "tariff": "midnight", "from": "00:00", "to": "06:00" }
				        ]
				      }
				    ]
				  }
				]
				""";

		final var schedule = parseToJsonArray(json);
		final var clock = createDummyClock();
		final var ex = assertThrows(OpenemsNamedException.class, () -> parseSchedule(clock, schedule));
		assertEquals(OpenemsError.JSON_NO_ENUM_MEMBER, ex.getError());
	}
}
