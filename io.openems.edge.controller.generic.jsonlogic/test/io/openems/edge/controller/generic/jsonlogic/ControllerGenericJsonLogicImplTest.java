package io.openems.edge.controller.generic.jsonlogic;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class ControllerGenericJsonLogicImplTest {

	private static final ChannelAddress ESS_SOC = new ChannelAddress(Sum.SINGLETON_COMPONENT_ID,
			Sum.ChannelId.ESS_SOC.id());

	private static final String ESS_ID = "ess0";

	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS = new ChannelAddress(ESS_ID,
			"SetActivePowerEquals");

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerGenericJsonLogicImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummySum()) //
				.addComponent(new DummyManagedSymmetricEss(ESS_ID)) //
				.activate(MyConfig.create() //
						.setRule("{" //
								+ "   \"if\":["//
								+ "      {"//
								+ "         \"<\": ["//
								+ "            {"//
								+ "               \"var\": \"" + ESS_SOC + "\""//
								+ "            },"//
								+ "            50"//
								+ "         ]"//
								+ "      },"//
								+ "      ["//
								+ "        ["//
								+ "          \"" + ESS_SET_ACTIVE_POWER_EQUALS + "\","//
								+ "          5000"//
								+ "        ]"//
								+ "      ],"//
								+ "      ["//
								+ "        ["//
								+ "          \"" + ESS_SET_ACTIVE_POWER_EQUALS + "\","//
								+ "          -2000"//
								+ "        ]"//
								+ "      ]"//
								+ "   ]"//
								+ "}") //
						.build())
				.next(new TestCase() //
						.input(ESS_SOC, 40) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 5000)) //
				.next(new TestCase() //
						.input(ESS_SOC, 60) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, -2000) //
				);
	}

}
