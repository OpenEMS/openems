package io.openems.edge.meter.tibber.pulse.decoder;

import java.util.List;

public class SmlMeterData {

	public static class Reading {
		protected String obisCode;
		protected String name;
		protected Number value;
		protected String unit;

		public Reading() {
		}

		public Reading(String obisCode, String name, Number value, String unit) {
			super();
			this.obisCode = obisCode;
			this.name = name;
			this.value = value;
			this.unit = unit;
		}

		public String getName() {
			return this.name;
		}

		public String getObisCode() {
			return this.obisCode;
		}

		public String getUnit() {
			return this.unit;
		}

		public Number getValue() {
			return this.value;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(this.obisCode);
			if (this.name != null) {
				sb.append(" / ").append(this.name);
			}
			sb.append(" = ").append(this.value);
			if (this.unit != null) {
				sb.append(" ").append(this.unit);
			}
			return sb.toString();
		}
	}

	public SmlMeterData() {
	}

	public SmlMeterData(String meterId, List<Reading> readings) {
		this.meterId = meterId;
		this.readings = readings;
	}

	protected String meterId;
	protected List<Reading> readings;

	public String getMeterId() {
		return this.meterId;
	}

	public List<Reading> getReadings() {
		return this.readings;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Meter ").append(this.meterId);
		if (this.readings != null) {
			for (Reading r : this.readings) {
				sb.append("\n ").append(r);
			}
		}
		return sb.toString();
	}

}
