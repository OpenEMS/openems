package io.openems.edge.evcs.hardybarth;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class EvcsHardyBarthImplTest {

	private static final String COMPONENT_ID = "evcs0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvcsHardyBarthImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setIp("192.168.8.101") //
						.setMaxHwCurrent(32_000) //
						.setMinHwCurrent(6_000) //
						.build())
				.next(new TestCase());
	}
}
