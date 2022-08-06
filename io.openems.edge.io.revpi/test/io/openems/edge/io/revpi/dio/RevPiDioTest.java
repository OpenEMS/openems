package io.openems.edge.io.revpi.dio;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class RevPiDioTest extends RevPiDioImpl {
	private static final String COMPONENT_ID = "dio0";

	@Test
	public void testActivateWithInitFromHardwareFalseShouldPass() throws Exception {
		new ComponentTest(new RevPiDioImpl()) //
				.activate(RevPiDioTestConfig.create() //
						.setId(COMPONENT_ID) //
						.setInitOutputFromHardware(false) //
						.build())//
				.next(new TestCase() //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_1.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_2.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_3.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_4.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_5.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_6.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_7.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_8.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_9.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_10.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_11.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_12.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_13.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_14.id()), false));
	}

	@Test
	public void testActivateWithInitFromHardwareTrueShouldPass() throws Exception {
		var sut = new RevPiDioImpl();
		new ComponentTest(sut) //
				.activate(RevPiDioTestConfig.create() //
						.setId(COMPONENT_ID) //
						.setInitOutputFromHardware(true) //
						.build());
	}

	@Test
	public void testDeactivateShouldSetAllOutputsToFalse() throws Exception {
		var sut = new RevPiDioImpl();
		new ComponentTest(sut) //
				.activate(RevPiDioTestConfig.create() //
						.setId(COMPONENT_ID) //
						.setInitOutputFromHardware(false) //
						.build())
				.next(new TestCase() //
						.onAfterProcessImage(() -> sut.deactivate()) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_1.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_2.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_3.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_4.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_5.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_6.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_7.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_8.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_9.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_10.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_11.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_12.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_13.id()), false) //
						.output(new ChannelAddress(COMPONENT_ID, RevPiDio.ChannelId.DEBUG_OUT_14.id()), false));
	}

	@Test
	public void testReadChannelsShouldGetAllReadChannels() throws Exception {
		var sut = new RevPiDioImpl();
		new ComponentTest(sut) //
				.activate(RevPiDioTestConfig.create() //
						.setId(COMPONENT_ID) //
						.setInitOutputFromHardware(false) //
						.build())//
				.next(new TestCase() //
						.onAfterProcessImage(() -> {//
							var numChannels = sut.digitalInputChannels();
							assertTrue(14 == numChannels.length);//
						}))//
				.next(new TestCase() //
						.onAfterProcessImage(() -> {//
							var numChannels = sut.digitalOutputChannels();//
							assertTrue(14 == numChannels.length);//
						})); //
	}

	@org.junit.Test
	public void testIsReadChannelWithPrefixMismatchShouldReturnFalse() {
		assertFalse(RevPiDio.isReadChannel(RevPiDio.ChannelId.OUT_1));
	}

	@org.junit.Test
	public void testIsReadChannelWithPrefixMatchShouldReturnTrue() {
		assertTrue(RevPiDio.isReadChannel(RevPiDio.ChannelId.IN_1));
	}

	@org.junit.Test
	public void testIsDebugChannelWithPrefixMismatchShouldReturnFalse() {
		assertFalse(RevPiDio.isDebugChannel(RevPiDio.ChannelId.OUT_1));
	}

	@org.junit.Test
	public void testIsDebugChannelWithPrefixMatchShouldReturnTrue() {
		assertTrue(RevPiDio.isDebugChannel(RevPiDio.ChannelId.DEBUG_OUT_1));
	}

	@org.junit.Test
	public void testIsWriteChannelWithPrefixMismatchShouldReturnFalse() {
		assertFalse(RevPiDio.isWriteChannel(RevPiDio.ChannelId.IN_1));
	}

	@org.junit.Test
	public void testIsWriteChannelWithPrefixMatchShouldReturnTrue() {
		assertTrue(RevPiDio.isWriteChannel(RevPiDio.ChannelId.OUT_1));
	}
}
