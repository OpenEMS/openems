package io.openems.edge.iooffgridswitch;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.io.test.DummyInputOutput;

public class IoOffGridSwitchTest {

	private static final String COMPONENT_ID = "ioOffGridSwitch0";

	private static final String IO_ID = "io0";
	private static final ChannelAddress INPUT_MAIN_CONTACTOR = new ChannelAddress(IO_ID, "InputOutput0");
	private static final ChannelAddress INPUT_GRID_STATUS = new ChannelAddress(IO_ID, "InputOutput1");
	private static final ChannelAddress INPUT_GROUNDING_CONTACTOR = new ChannelAddress(IO_ID, "InputOutput2");
	private static final ChannelAddress OUTPUT_MAIN_CONTACTOR = new ChannelAddress(IO_ID, "InputOutput3");
	private static final ChannelAddress OUTPUT_GROUNDING_CONTACTOR = new ChannelAddress(IO_ID, "InputOutput4");

	@Test
	public void test() throws Exception {
		var io0 = new DummyInputOutput(IO_ID);

		new ComponentTest(new IoOffGridSwitchImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(io0) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setInputMainContactor(INPUT_MAIN_CONTACTOR.toString()) //
						.setInputGridStatus(INPUT_GRID_STATUS.toString()) //
						.setInputGroundingContactor(INPUT_GROUNDING_CONTACTOR.toString()) //
						.setOutputMainContactor(OUTPUT_MAIN_CONTACTOR.toString()) //
						.setOutputGroundingContactor(OUTPUT_GROUNDING_CONTACTOR.toString()) //
						.build())
				.next(new TestCase());
	}

}
