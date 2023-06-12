package io.openems.edge.controller.generic.jsonlogic;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

public class ControllerGenericJsonLogicImplTest2 {

	private static final ChannelAddress SUM_PRODUCTION_POWER = new ChannelAddress(Sum.SINGLETON_COMPONENT_ID,
			Sum.ChannelId.PRODUCTION_ACTIVE_POWER.id());
	private static final ChannelAddress SUM_SOC = new ChannelAddress(Sum.SINGLETON_COMPONENT_ID,
			Sum.ChannelId.ESS_SOC.id());

	private static final String IO_ID = "io0";
	private static final ChannelAddress INPUT0 = new ChannelAddress(IO_ID, "InputOutput0");
	private static final ChannelAddress OUTPUT0 = new ChannelAddress(IO_ID, "InputOutput1");
	private static final ChannelAddress OUTPUT1 = new ChannelAddress(IO_ID, "InputOutput2");

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerGenericJsonLogicImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummySum()) //
				.addComponent(new DummyInputOutput(IO_ID)) //
				.activate(MyConfig.create() //
						.setRule("{"//
								+ "    \"if\": ["//
								+ "        {"//
								+ "            \">\": ["//
								+ "                {"//
								+ "                    \"var\": \"" + SUM_PRODUCTION_POWER + "\""//
								+ "                },"//
								+ "                2000"//
								+ "            ]"//
								+ "        },"//
								+ "        {"//
								+ "            \"if\": ["//
								+ "                {"//
								+ "                    \">\": ["//
								+ "                        {"//
								+ "                            \"var\": \"" + SUM_SOC + "\""//
								+ "                        },"//
								+ "                        70"//
								+ "                    ]"//
								+ "                },"//
								+ "                {"//
								+ "                    \"if\": ["//
								+ "                        {"//
								+ "                            \"var\": \"" + INPUT0 + "\""//
								+ "                        },"//
								+ "                        ["//
								+ "                        ],"//
								+ "                        {"//
								+ "                            \"if\": ["//
								+ "                                {"//
								+ "                                    \"var\": \"" + OUTPUT0 + "\""//
								+ "                                },"//
								+ "                                ["//
								+ "                                    ["//
								+ "                                        \"" + OUTPUT0 + "\","//
								+ "                                        false"//
								+ "                                    ]"//
								+ "                                ],"//
								+ "                                ["//
								+ "                                    ["//
								+ "                                        \"" + OUTPUT1 + "\","//
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
								+ "                            \"var\": \"" + SUM_SOC + "\""//
								+ "                        },"//
								+ "                        40"//
								+ "                    ]"//
								+ "                },"//
								+ "                {"//
								+ "                    \"if\": ["//
								+ "                        {"//
								+ "                            \"var\": \"" + INPUT0 + "\""//
								+ "                        },"//
								+ "                        ["//
								+ "                        ],"//
								+ "                        {"//
								+ "                            \"if\": ["//
								+ "                                {"//
								+ "                                    \"var\": \"" + OUTPUT1 + "\""//
								+ "                                },"//
								+ "                                ["//
								+ "                                    ["//
								+ "                                        \"" + OUTPUT1 + "\","//
								+ "                                        false"//
								+ "                                    ]"//
								+ "                                ],"//
								+ "                                ["//
								+ "                                    ["//
								+ "                                        \"" + OUTPUT0 + "\","//
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
						.input(SUM_PRODUCTION_POWER, 2001) //
						.input(SUM_SOC, 71) //
						.input(INPUT0, false) //
						.input(OUTPUT0, true) //
						.output(OUTPUT0, false)) //
				.next(new TestCase() //
						.input(SUM_PRODUCTION_POWER, 2001) //
						.input(SUM_SOC, 71) //
						.input(INPUT0, false) //
						.input(OUTPUT0, false) //
						.output(OUTPUT1, true)) //
				.next(new TestCase() //
						.input(SUM_PRODUCTION_POWER, 1999) //
						.input(SUM_SOC, 39) //
						.input(INPUT0, false) //
						.input(OUTPUT1, true) //
						.output(OUTPUT1, false)) //
				.next(new TestCase() //
						.input(SUM_PRODUCTION_POWER, 1999) //
						.input(SUM_SOC, 39) //
						.input(INPUT0, false) //
						.input(OUTPUT1, false) //
						.output(OUTPUT0, true)) //
		;
	}

}
