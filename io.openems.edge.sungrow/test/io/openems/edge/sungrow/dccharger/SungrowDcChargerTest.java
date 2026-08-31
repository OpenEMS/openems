package io.openems.edge.sungrow.dccharger;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.sungrow.ess.EssSungrowImpl;

public class SungrowDcChargerTest {

	private static final String CHARGER_ID = "charger0";
	private static final String ESS_ID = "ess0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new SungrowDcChargerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new EssSungrowImpl()) //
				.activate(MyConfig.create() //
						.setId(CHARGER_ID) //
						.setEssId(ESS_ID) //
						.build());
	}

}