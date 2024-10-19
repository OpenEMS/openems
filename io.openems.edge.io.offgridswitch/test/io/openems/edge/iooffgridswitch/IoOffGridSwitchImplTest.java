package io.openems.edge.iooffgridswitch;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.io.test.DummyInputOutput;

public class IoOffGridSwitchImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new IoOffGridSwitchImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("ioOffGridSwitch0") //
						.setInputMainContactor("io0/InputOutput0") //
						.setInputGridStatus("io0/InputOutput1") //
						.setInputGroundingContactor("io0/InputOutput2") //
						.setOutputMainContactor("io0/InputOutput3") //
						.setOutputGroundingContactor("io0/InputOutput4") //
						.build())
				.next(new TestCase());
	}

}
