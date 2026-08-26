package io.openems.edge.meter.abb.b32;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.openems.common.types.MeterType;
import io.openems.common.utils.ReflectionUtils.ReflectionException;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class MeterAbbB23ImplTest {

	@Test
	public void test() throws Exception {
		assertThrows(ReflectionException.class, () -> {
			new ComponentTest(new MeterAbbB23Impl()) //
					.addReference("mbus", null) // TODO create DummyMbusBridge
					.activate(MyConfig.create() //
							.setId("meter0") //
							.setMbusId("bridge0") //
							.setPrimaryAddress(10) //
							.setType(MeterType.PRODUCTION) //
							.build()) //
					.next(new TestCase()) //
					.deactivate();
		});
	}
}
