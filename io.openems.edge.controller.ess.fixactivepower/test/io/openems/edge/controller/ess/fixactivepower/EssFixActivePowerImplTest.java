package io.openems.edge.controller.ess.fixactivepower;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.ess.test.DummyHybridEss;
import io.openems.edge.ess.test.DummyManagedAsymmetricEss;

public class EssFixActivePowerImplTest {

	private static final String CTRL_ID = "ctrl0";
	private static final String ESS_ID = "ess0";

	@Test
	public void testOn() throws OpenemsException, Exception {
		final var ess = new DummyManagedAsymmetricEss(ESS_ID);
		new ControllerTest(new EssFixActivePowerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ess) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMode(Mode.MANUAL_ON) //
						.setHybridEssMode(HybridEssMode.TARGET_DC) //
						.setPower(1234) //
						.setPhase(Phase.ALL) //
						.setRelationship(Relationship.EQUALS) //
						.build()); //
	}

	@Test
	public void testOff() throws OpenemsException, Exception {
		new ControllerTest(new EssFixActivePowerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedAsymmetricEss(ESS_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMode(Mode.MANUAL_OFF) //
						.setHybridEssMode(HybridEssMode.TARGET_DC) //
						.setPower(1234) //
						.setPhase(Phase.ALL) //
						.setRelationship(Relationship.EQUALS) //
						.build()); //
	}

	@Test
	public void testGetAcPower() throws OpenemsException, Exception {
		var hybridEss = new DummyHybridEss(ESS_ID) //
				.withActivePower(7000) //
				.withMaxApparentPower(10000) //
				.withAllowedChargePower(-5000) //
				.withAllowedDischargePower(5000) //
				.withDcDischargePower(3000); //

		assertEquals(Integer.valueOf(5000), //
				EssFixActivePowerImpl.getAcPower(hybridEss, HybridEssMode.TARGET_AC, 5000));

		assertEquals(Integer.valueOf(9000), //
				EssFixActivePowerImpl.getAcPower(hybridEss, HybridEssMode.TARGET_DC, 5000));
	}
}
