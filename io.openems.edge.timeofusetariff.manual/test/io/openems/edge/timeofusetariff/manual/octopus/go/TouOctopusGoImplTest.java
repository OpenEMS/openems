package io.openems.edge.timeofusetariff.manual.octopus.go;

import static io.openems.common.test.TestUtils.createDummyClock;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

public class TouOctopusGoImplTest {

	@Test
	public void test() throws Exception {
		final var clock = createDummyClock();
		new ComponentTest(new TouOctopusGoImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("tou0") //
						.setStandardPrice(0.30) //
						.setLowPrice(0.20) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
