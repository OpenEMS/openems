package io.openems.edge.bridge.modbus.api.worker.internal;

import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Streams;

public class DummyTasksSupplier implements TasksSupplier {

	private final Queue<CycleTasks> records;
	private final int totalNumberOfTasks;

	public DummyTasksSupplier(CycleTasks... records) {
		this.records = Stream.of(records) //
				.collect(Collectors.toCollection(LinkedList::new));
		this.totalNumberOfTasks = (int) Streams.concat(//
				Stream.of(records).flatMap(t -> t.reads().stream()),
				Stream.of(records).flatMap(t -> t.writes().stream())) //
				.distinct() //
				.count();
	}

	@Override
	public CycleTasks getCycleTasks(DefectiveComponents defectiveComponents) {
		return this.records.poll();
	}

	@Override
	public int getTotalNumberOfTasks() {
		return this.totalNumberOfTasks;
	}

}
