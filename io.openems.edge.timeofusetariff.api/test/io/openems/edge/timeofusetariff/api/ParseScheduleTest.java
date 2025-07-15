package io.openems.edge.timeofusetariff.api;

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

		final var schedule = parseToJsonArray(json);
		final var tasks = parseSchedule(schedule);

		assertEquals(3, tasks.size());
		assertEquals(0.10, tasks.get(0).payload().doubleValue(), 0.001);
		assertEquals(0.20, tasks.get(1).payload().doubleValue(), 0.001);
		assertEquals(0.30, tasks.get(2).payload().doubleValue(), 0.001);
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
		final var ex = assertThrows(OpenemsException.class, () -> parseSchedule(schedule));

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
		final var ex = assertThrows(OpenemsException.class, () -> parseSchedule(schedule));
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
		final var ex = assertThrows(OpenemsException.class, () -> parseSchedule(schedule));
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
		final var ex = assertThrows(OpenemsNamedException.class, () -> parseSchedule(schedule));
		assertEquals(OpenemsError.JSON_NO_ENUM_MEMBER, ex.getError());
	}
}
