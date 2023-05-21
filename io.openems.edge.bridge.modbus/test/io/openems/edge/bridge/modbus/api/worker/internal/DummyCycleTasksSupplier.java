package io.openems.edge.bridge.modbus.api.worker.internal;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DummyCycleTasksSupplier implements Function<DefectiveComponents, CycleTasks> {

	private final Queue<CycleTasks> records;

	public DummyCycleTasksSupplier(CycleTasks... records) {
		this.records = Stream.of(records) //
				.collect(Collectors.toCollection(LinkedList::new));
	}

	@Override
	public CycleTasks apply(DefectiveComponents defectiveComponents) {
		return this.records.poll();
	}

}
