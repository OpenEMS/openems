package io.openems.edge.controller.evse.single;

import static io.openems.edge.controller.evse.TestUtils.generateSingleSut;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.controller.evse.single.Types.Hysteresis;
import io.openems.edge.evse.api.chargepoint.Mode;

public class ControllerEvseSingleImplTest {

	@Test
	public void test() throws OpenemsException, Exception {
		var sut = generateSingleSut(c -> c //
				.setLogVerbosity(LogVerbosity.DEBUG_LOG) //
				.setJsCalendar("""
						[{
						  "@type": "Task",
						  "updated": "2020-01-01T00:00:00Z",
						  "start": "2024-06-17T00:00:00",
						  "recurrenceRules": [
						    {
						      "frequency": "weekly",
						      "byDay": [
						        "sa",
						        "su"
						      ]
						    }
						  ],
						  "openems.io:payload": {
						    "sessionEnergyMinimum": 10001
						  }
						}]"""));

		sut.test() //
				.next(new TestCase()) //
				.deactivate();

		final var ctrl = sut.ctrlSingle();
		assertEquals("Mode:Minimum|Undefined", ctrl.debugLog());

		var params = sut.ctrlSingle().getParams();
		assertEquals("ctrlEvseSingle0", params.componentId());
		assertEquals(Mode.MINIMUM, params.mode());
		assertNull(params.activePower());
		assertEquals(0, params.sessionEnergy());
		assertEquals(10000, params.sessionEnergyLimit());
		assertEquals(0, params.history().streamAll().count());
		assertEquals(Hysteresis.INACTIVE, params.hysteresis());
		assertEquals(PhaseSwitching.DISABLE, params.phaseSwitching());
		assertFalse(params.appearsToBeFullyCharged());
	}
}