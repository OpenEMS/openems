package io.openems.edge.controller.ess.fixactivepower;

import static io.openems.edge.controller.ess.fixactivepower.ControllerEssFixActivePowerImpl.getAcPower;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.ess.test.DummyHybridEss;
import io.openems.edge.ess.test.DummyManagedAsymmetricEss;

public class ControllerEssFixActivePowerImplTest {

	@Test
	public void testOn() throws OpenemsException, Exception {
		final var ess = new DummyManagedAsymmetricEss("ess0");
		new ControllerTest(new ControllerEssFixActivePowerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ess) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMode(Mode.MANUAL_ON) //
						.setHybridEssMode(HybridEssMode.TARGET_DC) //
						.setPower(1234) //
						.setPhase(Phase.ALL) //
						.setRelationship(Relationship.EQUALS) //
						.build()) //
				.deactivate();
	}

	@Test
	public void testOff() throws OpenemsException, Exception {
		new ControllerTest(new ControllerEssFixActivePowerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedAsymmetricEss("ess0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMode(Mode.MANUAL_OFF) //
						.setHybridEssMode(HybridEssMode.TARGET_DC) //
						.setPower(1234) //
						.setPhase(Phase.ALL) //
						.setRelationship(Relationship.EQUALS) //
						.build()) //
				.deactivate();
	}

	@Test
	public void testGetAcPower() throws OpenemsException, Exception {
		var hybridEss = new DummyHybridEss("ess0") //
				.withActivePower(7000) //
				.withMaxApparentPower(10000) //
				.withAllowedChargePower(-5000) //
				.withAllowedDischargePower(5000) //
				.withDcDischargePower(3000); //

		assertEquals(Integer.valueOf(5000), //
				getAcPower(hybridEss, HybridEssMode.TARGET_AC, 5000));

		assertEquals(Integer.valueOf(9000), //
				getAcPower(hybridEss, HybridEssMode.TARGET_DC, 5000));
	}
}
