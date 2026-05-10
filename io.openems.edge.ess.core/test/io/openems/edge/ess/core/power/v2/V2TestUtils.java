package io.openems.edge.ess.core.power.v2;

import static io.openems.edge.ess.power.api.SolverStrategy.BALANCE;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.core.power.EssPower;
import io.openems.edge.ess.core.power.EssPowerImpl;
import io.openems.edge.ess.core.power.MyConfig;

/**
 * Shared test utilities for PowerDistributionHandlerV2 integration tests.
 */
final class V2TestUtils {

	private V2TestUtils() {
	}

	/**
	 * Creates a fully activated {@link ComponentTest} for the given ESS instances.
	 * Handles the ConfigAdmin and MyConfig boilerplate common to all integration
	 * tests.
	 *
	 * @param power the {@link EssPowerImpl} under test
	 * @param esss  all ESS to register (clusters and leaf members)
	 * @return an activated {@link ComponentTest}
	 * @throws Exception on activation failure
	 */
	static ComponentTest createComponentTest(EssPowerImpl power, ManagedSymmetricEss... esss) throws Exception {
		var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(EssPower.SINGLETON_SERVICE_PID);
		var test = new ComponentTest(power).addReference("cm", cm);
		for (var ess : esss) {
			test.addReference("addEss", ess);
		}
		return test.activate(MyConfig.create() //
				.setStrategy(BALANCE) //
				.setSymmetricMode(true) //
				.setDebugMode(false) //
				.setEnablePid(false) //
				.build());
	}
}
