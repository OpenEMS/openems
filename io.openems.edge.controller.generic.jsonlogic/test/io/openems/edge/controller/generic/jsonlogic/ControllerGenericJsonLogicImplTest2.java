package io.openems.edge.controller.generic.jsonlogic;

import static io.openems.edge.common.sum.Sum.ChannelId.ESS_SOC;
import static io.openems.edge.common.sum.Sum.ChannelId.PRODUCTION_ACTIVE_POWER;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT0;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT1;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT2;

import org.junit.Test;

import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

public class ControllerGenericJsonLogicImplTest2 {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerGenericJsonLogicImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setRule("{"//
								+ "    \"if\": ["//
								+ "        {"//
								+ "            \">\": ["//
								+ "                {"//
								+ "                    \"var\": \"_sum/ProductionActivePower\""//
								+ "                },"//
								+ "                2000"//
								+ "            ]"//
								+ "        },"//
								+ "        {"//
								+ "            \"if\": ["//
								+ "                {"//
								+ "                    \">\": ["//
								+ "                        {"//
								+ "                            \"var\": \"_sum/EssSoc\""//
								+ "                        },"//
								+ "                        70"//
								+ "                    ]"//
								+ "                },"//
								+ "                {"//
								+ "                    \"if\": ["//
								+ "                        {"//
								+ "                            \"var\": \"io0/InputOutput0\""//
								+ "                        },"//
								+ "                        ["//
								+ "                        ],"//
								+ "                        {"//
								+ "                            \"if\": ["//
								+ "                                {"//
								+ "                                    \"var\": \"io0/InputOutput1\""//
								+ "                                },"//
								+ "                                ["//
								+ "                                    ["//
								+ "                                        \"io0/InputOutput1\","//
								+ "                                        false"//
								+ "                                    ]"//
								+ "                                ],"//
								+ "                                ["//
								+ "                                    ["//
								+ "                                        \"io0/InputOutput2\","//
								+ "                                        true"//
								+ "                                    ]"//
								+ "                                ]"//
								+ "                            ]"//
								+ "                        }"//
								+ "                    ]"//
								+ "                },"//
								+ "                ["//
								+ "                ]"//
								+ "            ]"//
								+ "        },"//
								+ "        {"//
								+ "            \"if\": ["//
								+ "                {"//
								+ "                    \"<\": ["//
								+ "                        {"//
								+ "                            \"var\": \"_sum/EssSoc\""//
								+ "                        },"//
								+ "                        40"//
								+ "                    ]"//
								+ "                },"//
								+ "                {"//
								+ "                    \"if\": ["//
								+ "                        {"//
								+ "                            \"var\": \"io0/InputOutput0\""//
								+ "                        },"//
								+ "                        ["//
								+ "                        ],"//
								+ "                        {"//
								+ "                            \"if\": ["//
								+ "                                {"//
								+ "                                    \"var\": \"io0/InputOutput2\""//
								+ "                                },"//
								+ "                                ["//
								+ "                                    ["//
								+ "                                        \"io0/InputOutput2\","//
								+ "                                        false"//
								+ "                                    ]"//
								+ "                                ],"//
								+ "                                ["//
								+ "                                    ["//
								+ "                                        \"io0/InputOutput1\","//
								+ "                                        true"//
								+ "                                    ]"//
								+ "                                ]"//
								+ "                            ]"//
								+ "                        }"//
								+ "                    ]"//
								+ "                },"//
								+ "                ["//
								+ "                ]"//
								+ "            ]"//
								+ "        }"//
								+ "    ]"//
								+ "}") //
						.build())
				.next(new TestCase() //
						.input(PRODUCTION_ACTIVE_POWER, 2001) //
						.input(ESS_SOC, 71) //
						.input("io0", INPUT_OUTPUT0, false) //
						.input("io0", INPUT_OUTPUT1, true) //
						.output("io0", INPUT_OUTPUT1, false)) //
				.next(new TestCase() //
						.input(PRODUCTION_ACTIVE_POWER, 2001) //
						.input(ESS_SOC, 71) //
						.input("io0", INPUT_OUTPUT0, false) //
						.input("io0", INPUT_OUTPUT1, false) //
						.output("io0", INPUT_OUTPUT2, true)) //
				.next(new TestCase() //
						.input(PRODUCTION_ACTIVE_POWER, 1999) //
						.input(ESS_SOC, 39) //
						.input("io0", INPUT_OUTPUT0, false) //
						.input("io0", INPUT_OUTPUT2, true) //
						.output("io0", INPUT_OUTPUT2, false)) //
				.next(new TestCase() //
						.input(PRODUCTION_ACTIVE_POWER, 1999) //
						.input(ESS_SOC, 39) //
						.input("io0", INPUT_OUTPUT0, false) //
						.input("io0", INPUT_OUTPUT2, false) //
						.output("io0", INPUT_OUTPUT1, true)) //
				.deactivate();
	}

}
