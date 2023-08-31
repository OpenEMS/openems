package io.openems.edge.bridge.modbus.api.worker.internal;

import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Stream;

import io.openems.edge.bridge.modbus.api.task.ReadTask;
import io.openems.edge.bridge.modbus.api.task.WriteTask;

/**
 * Holds the Read- and Write-Tasks for one Cycle.
 */
public record CycleTasks(LinkedList<ReadTask> reads, LinkedList<WriteTask> writes) {

	public static class Builder {
		private final LinkedList<ReadTask> reads = new LinkedList<>();
		private final LinkedList<WriteTask> writes = new LinkedList<>();

		private Builder() {
		}

		/**
		 * Adds {@link ReadTask}s.
		 * 
		 * @param tasks the tasks
		 * @return myself
		 */
		public Builder reads(ReadTask... tasks) {
			Stream.of(tasks).forEach(this.reads::add);
			return this;
		}

		/**
		 * Adds {@link WriteTask}s.
		 * 
		 * @param tasks the tasks
		 * @return myself
		 */
		public Builder writes(WriteTask... tasks) {
			Stream.of(tasks).forEach(this.writes::add);
			return this;
		}

		public CycleTasks build() {
			return new CycleTasks(this.reads, this.writes);
		}
	}

	/**
	 * Create a Config builder.
	 *
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Is any of the tasks belonging to a Component that is known to be defective?.
	 * 
	 * @param defectiveComponents the {@link DefectiveComponents}
	 * @return true for defective; false otherwise
	 */
	public boolean containsDefectiveComponent(DefectiveComponents defectiveComponents) {
		return Stream.concat(this.reads.stream(), this.writes.stream()) //
				.map(t -> t.getParent()) //
				.filter(Objects::nonNull) //
				.map(p -> p.id()) //
				.anyMatch(c -> defectiveComponents.isKnown(c));
	}
}
