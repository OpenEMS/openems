package io.openems.edge.bridge.modbus.sunspec;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.sunspec.dummy.DummySunSpecComponent;
import io.openems.edge.common.taskmanager.Priority;

public class SunSpecComponentTest {

	@Test
	public void test() throws OpenemsException {
		final Map<SunSpecModel, Priority> allModels = Stream.of(DefaultSunSpecModel.values()) //
				.collect(Collectors.toMap(//
						model -> model, //
						model -> Priority.LOW, //
						(a, b) -> a, TreeMap::new));

		var component = new DummySunSpecComponent(allModels);
		assertTrue(component.maximumTaskLenghth() <= 126);
	}

}
