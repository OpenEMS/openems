package io.openems.edge.controller.ess.powersolvercluster;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyMetaEss;

public class MyControllerTest {

	private static final String CTRL_ID = "ctrl0";

	private static final String ESS_ID = "ess0";

	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS_ASSA1 = new ChannelAddress("essA1",
			"SetActivePowerEquals");	
	
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS_ASSA2 = new ChannelAddress("essA2",
			"SetActivePowerEquals");
	
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS_ASSA3 = new ChannelAddress("essA3",
			"SetActivePowerEquals");
	
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS_ASSA4 = new ChannelAddress("essA4",
			"SetActivePowerEquals");
	
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS_ASSA5 = new ChannelAddress("essA5",
			"SetActivePowerEquals");
	
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS_ASSA6 = new ChannelAddress("essA6",
			"SetActivePowerEquals");
	
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS_ASSA7 = new ChannelAddress("essA7",
			"SetActivePowerEquals");
	
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS_ASSA8 = new ChannelAddress("essA8",
			"SetActivePowerEquals");

	@Test
	public void testOn() throws OpenemsException, Exception {
		
		//PowerSolverCluster powerSolverCluster = new PowerSolverClusterImpl();
		
		//var essB1 = new DummyManagedSymmetricEss()
		
		var essA1 = new DummyManagedSymmetricEss("essA1") //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(50000) //
				.withSoc(11);
		var essA2 = new DummyManagedSymmetricEss("essA2") //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(50000) //
				.withSoc(12);
		var essA3 = new DummyManagedSymmetricEss("essA3") //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(50000) //
				.withSoc(13);
		var essA4 = new DummyManagedSymmetricEss("essA4") //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(50000) //
				.withSoc(14);
		var essA5 = new DummyManagedSymmetricEss("essA5") //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(50000) //
				.withSoc(15);
		var essA6 = new DummyManagedSymmetricEss("essA6") //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(50000) //
				.withSoc(16);
		var essA7 = new DummyManagedSymmetricEss("essA7") //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(50000) //
				.withSoc(16);
		var essA8 = new DummyManagedSymmetricEss("essA8") //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(50000) //
				.withSoc(16);
		var ess0 = new DummyMetaEss("ess0", essA1, essA2, essA3, essA4, essA5, essA6, essA7, essA8); //
		var componentManager = new DummyComponentManager();
		componentManager.addComponent(ess0);
		componentManager.addComponent(essA1);
		componentManager.addComponent(essA2);
		componentManager.addComponent(essA3);
		componentManager.addComponent(essA4);
		componentManager.addComponent(essA5);
		componentManager.addComponent(essA6);
		componentManager.addComponent(essA7);
		componentManager.addComponent(essA8);
		
		new ControllerTest(new PowerSolverClusterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				//.addReference("componentManager", componentManager) //
				.addReference("esss", ess0) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMode(Mode.MANUAL_ON) //
						.setPower(1234) //
						.build()) //
				.next(new TestCase() //
				 .output(ESS_SET_ACTIVE_POWER_EQUALS_ASSA1, 1234) 
				 .output(ESS_SET_ACTIVE_POWER_EQUALS_ASSA2, 1234)
				 .output(ESS_SET_ACTIVE_POWER_EQUALS_ASSA3, 1234)
				 .output(ESS_SET_ACTIVE_POWER_EQUALS_ASSA4, 1234)
				 .output(ESS_SET_ACTIVE_POWER_EQUALS_ASSA5, 1234)
				 .output(ESS_SET_ACTIVE_POWER_EQUALS_ASSA6, 1234)
				 .output(ESS_SET_ACTIVE_POWER_EQUALS_ASSA7, 1234)
				 .output(ESS_SET_ACTIVE_POWER_EQUALS_ASSA8, 1234));
	}

}
