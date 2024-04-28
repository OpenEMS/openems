package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.common.test.TestUtils.withValue;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateAutomaticMode;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateChargeGridPower;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateDelayDischargePower;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateEssChargeInChargeGridPowerFromParams;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateMaxChargeProductionPower;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.common.sum.DummySum;
import io.openems.edge.controller.ess.timeofusetariff.Utils.ApplyState;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.test.DummyHybridEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class UtilsTest {

	@Test
	public void testCalculateChargeGridPower() {
		assertEquals(-10000, calculateChargeGridPower(null, //
				new DummyManagedSymmetricEss("ess0") //
						.withCapacity(20000), //
				/* essActivePower */ -6000, //
				/* gridActivePower */ 10000, //
				/* maxChargePowerFromGrid */ 20000, //
				/* limitChargePowerFor14aEnWG */ false));

		assertEquals(-4200, calculateChargeGridPower(null, //
				new DummyManagedSymmetricEss("ess0") //
						.withCapacity(20000), //
				/* essActivePower */ -6000, //
				/* gridActivePower */ 10000, //
				/* maxChargePowerFromGrid */ 20000, //
				/* limitChargePowerFor14aEnWG */ true));

		assertEquals(-11000, calculateChargeGridPower(null, //
				new DummyManagedSymmetricEss("ess0") //
						.withCapacity(20000), //
				/* essActivePower */ -6000, //
				/* gridActivePower */ 5000, //
				/* maxChargePowerFromGrid */ 20000, //
				/* limitChargePowerFor14aEnWG */ false));

		assertEquals(-5860, calculateChargeGridPower(1340, //
				new DummyManagedSymmetricEss("ess0"), //
				/* essActivePower */ -1000, //
				/* gridActivePower */ 500, //
				/* maxChargePowerFromGrid */ 24000, //
				/* limitChargePowerFor14aEnWG */ false));

		// Would be -3584, but limited to 5000 which is already surpassed
		// TODO if this should actually serve as blackout-protection, a positive value
		// would have to be returned
		assertEquals(0, calculateChargeGridPower(1000, //
				new DummyManagedSymmetricEss("ess0"), //
				/* essActivePower */ 1000, //
				/* gridActivePower */ 9000, //
				/* maxChargePowerFromGrid */ 5000, //
				/* limitChargePowerFor14aEnWG */ false));

		assertEquals(-8360, calculateChargeGridPower(1340, //
				new DummyHybridEss("ess0") //
						.withDcDischargePower(-1500), //
				/* essActivePower */ -1000, //
				/* gridActivePower */ -2000, //
				/* maxChargePowerFromGrid */ 24000, //
				/* limitChargePowerFor14aEnWG */ false));
	}

	@Test
	public void testCalculateChargeProduction() {
		assertEquals(-500, calculateMaxChargeProductionPower(//
				new DummySum() //
						.withProductionAcActivePower(500)) //
				.intValue());

		assertEquals(0, calculateMaxChargeProductionPower(//
				new DummySum()) //
				.intValue());

		assertEquals(0, calculateMaxChargeProductionPower(//
				new DummySum() //
						.withProductionAcActivePower(-100 /* wrong */)) //
				.intValue());
	}

	@Test
	public void testCalculateDelayDischarge() {
		// DC-PV
		assertEquals(500, calculateDelayDischargePower(//
				new DummyHybridEss("ess0") //
						.withActivePower(-500) //
						.withDcDischargePower(-1000)));

		// Never negative
		assertEquals(0, calculateDelayDischargePower(//
				new DummyHybridEss("ess0") //
						.withActivePower(-1500) //
						.withDcDischargePower(-1000)));

		// AC-PV
		assertEquals(0, calculateDelayDischargePower(//
				new DummyManagedSymmetricEss("ess0") //
						.withActivePower(-1500)));
	}

	@Test
	public void testCalculateMaxChargeGridPowerFromParams() {
		final var ess = new DummyManagedSymmetricEss("ess0");

		// No params, initial ESS
		assertEquals(0, calculateEssChargeInChargeGridPowerFromParams(null, ess));

		// No params, ESS with MaxApparentPower
		withValue(ess, SymmetricEss.ChannelId.MAX_APPARENT_POWER, 1000);
		assertEquals(250, calculateEssChargeInChargeGridPowerFromParams(null, ess));

		// No params, ESS with Capacity
		withValue(ess, SymmetricEss.ChannelId.CAPACITY, 15000);
		assertEquals(7500, calculateEssChargeInChargeGridPowerFromParams(null, ess));

		// With params (22 kWh; but few Consumption)
		assertEquals(5360, calculateEssChargeInChargeGridPowerFromParams(1340, ess));
	}

	@Test
	public void testCalculateAutomaticMode() {
		assertEquals("Null-Check", new ApplyState(BALANCING, null), //
				calculateAutomaticMode(//
						new DummySum(), //
						new DummyManagedSymmetricEss("ess0"), //
						/* essChargeInChargeGrid */ 1000, //
						/* maxChargePowerFromGrid */ 2000, //
						/* limitChargePowerFor14aEnWG */ true, //
						BALANCING));
		assertEquals("Null-Check", new ApplyState(BALANCING, null), //
				calculateAutomaticMode(//
						new DummySum() //
								.withGridActivePower(100), //
						new DummyManagedSymmetricEss("ess0"), //
						/* essChargeInChargeGrid */ 1000, //
						/* maxChargePowerFromGrid */ 2000, //
						/* limitChargePowerFor14aEnWG */ true, //
						BALANCING));

		assertEquals("BALANCING", new ApplyState(BALANCING, null), //
				calculateAutomaticMode(//
						new DummySum() //
								.withGridActivePower(100), //
						new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* essChargeInChargeGrid */ 1000, //
						/* maxChargePowerFromGrid */ 2000, //
						/* limitChargePowerFor14aEnWG */ true, //
						BALANCING));

		assertEquals("DELAY_DISCHARGE stays DELAY_DISCHARGE", new ApplyState(DELAY_DISCHARGE, 0), //
				calculateAutomaticMode(//
						new DummySum() //
								.withGridActivePower(100), //
						new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* essChargeInChargeGrid */ 1000, //
						/* maxChargePowerFromGrid */ 2000, //
						/* limitChargePowerFor14aEnWG */ true, //
						DELAY_DISCHARGE));
		assertEquals("DELAY_DISCHARGE to BALANCING", new ApplyState(BALANCING, null), //
				calculateAutomaticMode(//
						new DummySum() //
								.withGridActivePower(-500), //
						new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* essChargeInChargeGrid */ 1000, //
						/* maxChargePowerFromGrid */ 2000, //
						/* limitChargePowerFor14aEnWG */ true, //
						DELAY_DISCHARGE));

		assertEquals("CHARGE_GRID stays CHARGE_GRID", new ApplyState(CHARGE_GRID, -1400), //
				calculateAutomaticMode(//
						new DummySum() //
								.withGridActivePower(100), //
						new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* essChargeInChargeGrid */ 1000, //
						/* maxChargePowerFromGrid */ 2000, //
						/* limitChargePowerFor14aEnWG */ true, //
						CHARGE_GRID));
		assertEquals("CHARGE_GRID to DELAY_DISCHARGE", new ApplyState(DELAY_DISCHARGE, 0), //
				calculateAutomaticMode(//
						new DummySum() //
								.withGridActivePower(100), //
						new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* essChargeInChargeGrid */ 1000, //
						/* maxChargePowerFromGrid */ 400, //
						/* limitChargePowerFor14aEnWG */ true, //
						CHARGE_GRID));
		assertEquals("CHARGE_GRID to BALANCING", new ApplyState(BALANCING, null), //
				calculateAutomaticMode(//
						new DummySum() //
								.withGridActivePower(-500), //
						new DummyManagedSymmetricEss("ess0") //
								.withActivePower(500), //
						/* essChargeInChargeGrid */ 1000, //
						/* maxChargePowerFromGrid */ 0, //
						/* limitChargePowerFor14aEnWG */ true, //
						CHARGE_GRID));
	}
}
