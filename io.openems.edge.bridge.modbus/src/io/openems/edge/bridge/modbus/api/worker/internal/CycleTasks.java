package io.openems.edge.bridge.modbus.api.worker.internal;

import java.util.LinkedList;
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

		public Builder reads(ReadTask... tasks) {
			Stream.of(tasks).forEach(this.reads::add);
			return this;
		}

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
	 * Is any of the tasks belonging to a Component that is known to be defective?
	 * 
	 * @param defectiveComponents the {@link DefectiveComponents}
	 * @return
	 */
	public boolean containsDefectiveComponent(DefectiveComponents defectiveComponents) {
		return Stream.concat(this.reads.stream(), this.writes.stream()) //
				.map(t -> t.getParent().id()) //
				.anyMatch(c -> defectiveComponents.isKnown(c));
	}
}
