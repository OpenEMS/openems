package io.openems.edge.meter.abb.b32;

import org.junit.Test;

import io.openems.common.types.MeterType;
import io.openems.common.utils.ReflectionUtils.ReflectionException;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class MeterAbbB23ImplTest {

	@Test(expected = ReflectionException.class)
	public void test() throws Exception {
		new ComponentTest(new MeterAbbB23Impl()) //
				.addReference("cm", new DummyConfigurationAdmin()) // #
				.addReference("mbus", null) // TODO create DummyMbusBridge
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setMbusId("bridge0") //
						.setPrimaryAddress(10) //
						.setType(MeterType.PRODUCTION) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
