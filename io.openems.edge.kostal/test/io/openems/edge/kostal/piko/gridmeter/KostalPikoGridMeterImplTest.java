package io.openems.edge.kostal.piko.gridmeter;

import org.junit.jupiter.api.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.kostal.piko.core.impl.KostalPikoCoreImpl;

public class KostalPikoGridMeterImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new KostalPikoGridMeterImpl()) //
				.addReference("setCore", new KostalPikoCoreImpl()) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setCoreId("core0") //
						.build()); //
		// TODO This does not work because this.worker == null
		// .next(new TestCase()) //
		// deactivate();
	}

}
