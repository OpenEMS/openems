package io.openems.edge.meter.discovergy;

import static io.openems.common.types.MeterType.GRID;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;

public class MeterDiscovergyImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new MeterDiscovergyImpl()) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setType(GRID) //
						.setPassword("xxx") //
						.setEmail("x@y.z") //
						.setSerialNumber("12345678") //
						.setFullSerialNumber("1ESY1234567890") //
						.setMeterId("0123456789ABCDEF0123456789ABCDEF") //
						.build()) //
		;
	}
}
