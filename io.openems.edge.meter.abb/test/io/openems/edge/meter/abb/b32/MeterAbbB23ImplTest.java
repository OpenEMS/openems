package io.openems.edge.meter.abb.b32;

import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.api.MeterType;

public class MeterAbbB23ImplTest {

	private static final String COMPONENT_ID = "meter0";

	@Test(expected = InvocationTargetException.class)
	public void test() throws Exception {
		new ComponentTest(new MeterAbbB23Impl()) //
				.addReference("cm", new DummyConfigurationAdmin()) // #
				.addReference("mbus", null) // TODO create DummyMbusBridge
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setMbusId("bridge0") //
						.setPrimaryAddress(10) //
						.setType(MeterType.PRODUCTION) //
						.build()) //
		;
	}

}
