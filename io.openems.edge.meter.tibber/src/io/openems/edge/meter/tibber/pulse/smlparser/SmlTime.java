package io.openems.edge.meter.tibber.pulse.smlparser;

public abstract class SmlTime {

	public static class SmlTimeSecIndex extends SmlTime {
		protected Long secIndex;

		public Long getSecIndex() {
			return this.secIndex;
		}
	}

}
