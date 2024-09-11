package io.openems.edge.controller.ess.timeofusetariff.v1;

import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.v1.UtilsV1.calculateAutomaticMode;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.common.sum.DummySum;
import io.openems.edge.controller.ess.timeofusetariff.Utils.ApplyState;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class UtilsTest {

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
