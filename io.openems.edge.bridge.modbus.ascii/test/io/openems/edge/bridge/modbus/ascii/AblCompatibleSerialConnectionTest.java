package io.openems.edge.bridge.modbus.ascii;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class AblCompatibleSerialConnectionTest {

	@Test
	public void testReplaceAblFrameStart_noReplacement() {
		byte[] buffer = { ':', '0', '1', '0', '3', '\r', '\n' };
		byte[] expected = buffer.clone();
		AblCompatibleSerialConnection.replaceAblFrameStart(buffer, buffer.length);
		assertArrayEquals(expected, buffer);
	}

	@Test
	public void testReplaceAblFrameStart_replacesAblStart() {
		byte[] buffer = { '>', '0', '1', '0', '3', '\r', '\n' };
		AblCompatibleSerialConnection.replaceAblFrameStart(buffer, buffer.length);
		assertArrayEquals(new byte[] { ':', '0', '1', '0', '3', '\r', '\n' }, buffer);
	}

	@Test
	public void testReplaceAblFrameStart_multipleOccurrences() {
		byte[] buffer = { '>', 'A', '>', 'B' };
		AblCompatibleSerialConnection.replaceAblFrameStart(buffer, buffer.length);
		assertArrayEquals(new byte[] { ':', 'A', ':', 'B' }, buffer);
	}

	@Test
	public void testReplaceAblFrameStart_respectsCount() {
		byte[] buffer = { '>', 'A', '>', 'B' };
		AblCompatibleSerialConnection.replaceAblFrameStart(buffer, 2);
		assertArrayEquals(new byte[] { ':', 'A', '>', 'B' }, buffer);
	}

	@Test
	public void testReplaceAblFrameStart_emptyBuffer() {
		byte[] buffer = {};
		AblCompatibleSerialConnection.replaceAblFrameStart(buffer, 0); // must not throw
	}
}
