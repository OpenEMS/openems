package io.openems.edge.controller.evse.single;

import static io.openems.common.test.TestUtils.createDummyClock;
import static org.junit.Assert.assertEquals;

import java.time.Clock;
import java.util.function.Consumer;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsRuntimeException;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.dummy.DummyEvseChargePoint;
import io.openems.edge.evse.api.chargepoint.test.DummyElectricVehicle;

public class ControllerEvseSingleImplTest {

	@Test
	public void test() throws OpenemsException, Exception {
		generateSingleSut(FunctionUtils::doNothing).test //
				.next(new TestCase()) //
				.deactivate();
	}

	@Test
	public void smartTest() throws Exception {
		final var sut = generateSingleSut(config -> config //
				.setMode(Mode.SMART) //
				.setSmartConfig("""
						[{
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
						    "sessionEnergy": 10001
						  }
						}]"""));

		sut.test //
				.next(new TestCase()) //
				.deactivate();

		assertEquals(Mode.Actual.ZERO, sut.ctrlSingle.getParams().actualMode());
	}

	public record SingleSut(ControllerTest test, ControllerEvseSingleImpl ctrlSingle, DummyEvseChargePoint chargePoint,
			DummyElectricVehicle electricVehicle) {
	}

	private static SingleSut generateSingleSut(Consumer<MyConfig.Builder> config) throws OpenemsException, Exception {
		return generateSingleSut(createDummyClock(), 0, config);
	}

	/**
	 * Generates a {@link SingleSut}.
	 * 
	 * @param clock  the {@link Clock}
	 * @param count  sets the Component-ID
	 * @param config a MyConfig callback
	 * @return {@link SingleSut}
	 * @throws OpenemsRuntimeException on error
	 */
	public static SingleSut generateSingleSut(Clock clock, int count, Consumer<MyConfig.Builder> config)
			throws OpenemsRuntimeException {
		final var ctrlSingle = new ControllerEvseSingleImpl(clock);
		final var chargePoint = new DummyEvseChargePoint("chargePoint0");
		final var electricVehicle = new DummyElectricVehicle("electricVehicle0");
		final var myConfig = MyConfig.create() //
				.setId("ctrlEvseSingle" + count) //
				.setMode(Mode.MINIMUM) //
				.setChargePointId("chargePoint0") //
				.setElectricVehicleId("electricVehicle0") //
				.setPhaseSwitching(PhaseSwitching.DISABLE) //
				.setSmartConfig("") //
				.setManualEnergySessionLimit(10_000) //
				.setLogVerbosity(LogVerbosity.NONE);
		config.accept(myConfig);

		try {
			final var test = new ControllerTest(ctrlSingle) //
					.addReference("componentManager", new DummyComponentManager(clock)) //
					.addReference("cm", new DummyConfigurationAdmin()) //
					.addReference("chargePoint", chargePoint) //
					.addReference("electricVehicle", electricVehicle) //
					.activate(myConfig.build());
			return new SingleSut(test, ctrlSingle, chargePoint, electricVehicle);
		} catch (Exception e) {
			throw new OpenemsRuntimeException(e);
		}
	}
}
