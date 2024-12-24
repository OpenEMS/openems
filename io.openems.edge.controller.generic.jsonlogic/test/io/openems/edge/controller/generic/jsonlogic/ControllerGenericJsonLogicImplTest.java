package io.openems.edge.controller.generic.jsonlogic;

import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.SOC;

import org.junit.Test;

import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class ControllerGenericJsonLogicImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerGenericJsonLogicImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummySum()) //
				.addComponent(new DummyManagedSymmetricEss("ess0")) //
				.activate(MyConfig.create() //
						.setRule("{" //
								+ "   \"if\":["//
								+ "      {"//
								+ "         \"<\": ["//
								+ "            {"//
								+ "               \"var\": \"ess0/Soc\""//
								+ "            },"//
								+ "            50"//
								+ "         ]"//
								+ "      },"//
								+ "      ["//
								+ "        ["//
								+ "          \"ess0/SetActivePowerEquals\","//
								+ "          5000"//
								+ "        ]"//
								+ "      ],"//
								+ "      ["//
								+ "        ["//
								+ "          \"ess0/SetActivePowerEquals\","//
								+ "          -2000"//
								+ "        ]"//
								+ "      ]"//
								+ "   ]"//
								+ "}") //
						.build())
				.next(new TestCase() //
						.input("ess0", SOC, 40) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 5000)) //
				.next(new TestCase() //
						.input("ess0", SOC, 60) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -2000)) //
				.deactivate();
	}

}
