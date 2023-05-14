package io.openems.edge.bridge.modbus.api.worker.internal;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DummyCycleTasksSupplier implements Supplier<CycleTasks> {

	private final Queue<CycleTasks> records;

	public DummyCycleTasksSupplier(CycleTasks... records) {
		this.records = Stream.of(records) //
				.collect(Collectors.toCollection(LinkedList::new));
	}

	@Override
	public CycleTasks get() {
		return this.records.poll();
	}

}
