package io.openems.edge.core.meta;

import static io.openems.edge.common.currency.CurrencyConfig.EUR;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class MetaImplTest {

	@Test
	public void test() throws OpenemsException, Exception {
		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(ComponentManager.SINGLETON_SERVICE_PID);
		new ComponentTest(new MetaImpl()) //
				.addReference("cm", cm) //
				.addReference("oem", new DummyOpenemsEdgeOem()) //
				.activate(MyConfig.create() //
						.setCurrency(EUR) //
						.build());
	}

}
