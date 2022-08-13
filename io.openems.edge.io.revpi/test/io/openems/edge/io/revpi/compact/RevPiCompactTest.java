package io.openems.edge.io.revpi.compact;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class RevPiCompactTest extends RevPiCompactImpl {
	private static final String COMPONENT_ID = "dio0";

	@Test
	public void testActivateWithInitFromHardwareFalseShouldPass() throws Exception {
		new ComponentTest(new RevPiCompactImpl()) //
				.activate(RevPiCompactTestConfig.create() //
						.setId(COMPONENT_ID) //
						.setInitOutputFromHardware(false) //
						.build())//
				.next(new TestCase() //
						.output(new ChannelAddress(COMPONENT_ID, RevPiCompact.ChannelId.DEBUG_OUT_1.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiCompact.ChannelId.DEBUG_OUT_2.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiCompact.ChannelId.DEBUG_OUT_3.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiCompact.ChannelId.DEBUG_OUT_4.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiCompact.ChannelId.DEBUG_OUT_5.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiCompact.ChannelId.DEBUG_OUT_6.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiCompact.ChannelId.DEBUG_OUT_7.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiCompact.ChannelId.DEBUG_OUT_8.id()), false));
	}

	@Test
	public void testActivateWithInitFromHardwareTrueShouldPass() throws Exception {
		var sut = new RevPiCompactImpl();
		new ComponentTest(sut) //
				.activate(RevPiCompactTestConfig.create() //
						.setId(COMPONENT_ID) //
						.setInitOutputFromHardware(true) //
						.build());
	}

	@Test
	public void testDeactivateShouldSetAllOutputsToFalse() throws Exception {
		var sut = new RevPiCompactImpl();
		new ComponentTest(sut) //
				.activate(RevPiCompactTestConfig.create() //
						.setId(COMPONENT_ID) //
						.setInitOutputFromHardware(false) //
						.build())
				.next(new TestCase() //
						.onAfterProcessImage(() -> sut.deactivate()) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiCompact.ChannelId.DEBUG_OUT_1.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiCompact.ChannelId.DEBUG_OUT_2.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiCompact.ChannelId.DEBUG_OUT_3.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiCompact.ChannelId.DEBUG_OUT_4.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiCompact.ChannelId.DEBUG_OUT_5.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiCompact.ChannelId.DEBUG_OUT_6.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiCompact.ChannelId.DEBUG_OUT_7.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiCompact.ChannelId.DEBUG_OUT_8.id()), false));
	}

	@Test
	public void testReadChannelsShouldGetAllReadChannels() throws Exception {
		var sut = new RevPiCompactImpl();
		new ComponentTest(sut) //
				.activate(RevPiCompactTestConfig.create() //
						.setId(COMPONENT_ID) //
						.setInitOutputFromHardware(false) //
						.build())//
				.next(new TestCase() //
						.onAfterProcessImage(() -> {//
							var numChannels = sut.digitalInputChannels();
							assertTrue(8 == numChannels.length);//
						}))//
				.next(new TestCase() //
						.onAfterProcessImage(() -> {//
							var numChannels = sut.digitalOutputChannels();//
							assertTrue(8 == numChannels.length);//
						})); //
	}

	@org.junit.Test
	public void testIsReadChannelWithPrefixMismatchShouldReturnFalse() {
		assertFalse(RevPiCompact.isReadChannel(RevPiCompact.ChannelId.OUT_1));
	}

	@org.junit.Test
	public void testIsReadChannelWithPrefixMatchShouldReturnTrue() {
		assertTrue(RevPiCompact.isReadChannel(RevPiCompact.ChannelId.IN_1));
	}

	@org.junit.Test
	public void testIsDebugChannelWithPrefixMismatchShouldReturnFalse() {
		assertFalse(RevPiCompact.isDebugChannel(RevPiCompact.ChannelId.OUT_1));
	}

	@org.junit.Test
	public void testIsDebugChannelWithPrefixMatchShouldReturnTrue() {
		assertTrue(RevPiCompact.isDebugChannel(RevPiCompact.ChannelId.DEBUG_OUT_1));
	}

	@org.junit.Test
	public void testIsWriteChannelWithPrefixMismatchShouldReturnFalse() {
		assertFalse(RevPiCompact.isWriteChannel(RevPiCompact.ChannelId.IN_1));
	}

	@org.junit.Test
	public void testIsWriteChannelWithPrefixMatchShouldReturnTrue() {
		assertTrue(RevPiCompact.isWriteChannel(RevPiCompact.ChannelId.OUT_1));
	}
}
