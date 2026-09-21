package io.openems.edge.evse.chargepoint.hardybarth.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DeviceRoleTest {

	static Stream<Arguments> fromModelNameAndProductCases() {
		return Stream.of(//
				Arguments.of("Salia PLCC Master", "2310006", DeviceRole.MASTER), //
				Arguments.of("Salia PLCC Slave", "2310007", DeviceRole.SLAVE), //
				Arguments.of("Salia PLCC Master", "2310007", DeviceRole.UNKNOWN), //
				Arguments.of("Salia PLCC Slave", "2310006", DeviceRole.UNKNOWN), //
				Arguments.of("Salia PLCC", "2310006", DeviceRole.UNKNOWN), //
				Arguments.of("Salia PLCC Master", "unknown", DeviceRole.UNKNOWN), //
				Arguments.of(null, "2310006", DeviceRole.UNKNOWN), //
				Arguments.of("Salia PLCC Master", null, DeviceRole.UNKNOWN), //
				Arguments.of(null, null, DeviceRole.UNKNOWN)); //
	}

	@ParameterizedTest(name = "[{index}] modelName={0}, product={1} -> {2}")
	@MethodSource("fromModelNameAndProductCases")
	void fromModelNameAndProduct(String modelName, String product, DeviceRole expected) {
		assertEquals(expected, DeviceRole.fromModelNameAndProduct(modelName, product));
	}

}