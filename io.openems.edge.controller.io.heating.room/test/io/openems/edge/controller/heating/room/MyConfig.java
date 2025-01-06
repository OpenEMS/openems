package io.openems.edge.controller.heating.room;

import static io.openems.common.utils.ConfigUtils.generateReferenceTargetFilter;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private Mode mode;
		private String schedule;
		private int lowFloorTemperature;
		private int lowAmbientTemperature;
		private int highFloorTemperature;
		private int highAmbientTemperature;
		private String floorThermometerId;
		private String ambientThermometerId;
		private String[] floorRelays;
		private String[] infraredRelays;
		private int floorPower;
		private int infraredPower;
		private boolean hasExternalAmbientHeating;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setMode(Mode mode) {
			this.mode = mode;
			return this;
		}

		public Builder setSchedule(String schedule) {
			this.schedule = schedule;
			return this;
		}

		public Builder setLowFloorTemperature(int lowFloorTemperature) {
			this.lowFloorTemperature = lowFloorTemperature;
			return this;
		}

		public Builder setLowAmbientTemperature(int lowAmbientTemperature) {
			this.lowAmbientTemperature = lowAmbientTemperature;
			return this;
		}

		public Builder setHighFloorTemperature(int highFloorTemperature) {
			this.highFloorTemperature = highFloorTemperature;
			return this;
		}

		public Builder setHighAmbientTemperature(int highAmbientTemperature) {
			this.highAmbientTemperature = highAmbientTemperature;
			return this;
		}

		public Builder setFloorThermometerId(String floorThermometerId) {
			this.floorThermometerId = floorThermometerId;
			return this;
		}

		public Builder setAmbientThermometerId(String ambientThermometerId) {
			this.ambientThermometerId = ambientThermometerId;
			return this;
		}

		public Builder setFloorRelays(String... floorRelays) {
			this.floorRelays = floorRelays;
			return this;
		}

		public Builder setInfraredRelays(String... infraredRelays) {
			this.infraredRelays = infraredRelays;
			return this;
		}

		public Builder setFloorPower(int floorPower) {
			this.floorPower = floorPower;
			return this;
		}

		public Builder setInfraredPower(int infraredPower) {
			this.infraredPower = infraredPower;
			return this;
		}

		public Builder setHasExternalAmbientHeating(boolean hasExternalAmbientHeating) {
			this.hasExternalAmbientHeating = hasExternalAmbientHeating;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
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

	private final Builder builder;

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public Mode mode() {
		return this.builder.mode;
	}

	@Override
	public String schedule() {
		return this.builder.schedule;
	}

	@Override
	public int lowFloorTemperature() {
		return this.builder.lowFloorTemperature;
	}

	@Override
	public int lowAmbientTemperature() {
		return this.builder.lowAmbientTemperature;
	}

	@Override
	public int highFloorTemperature() {
		return this.builder.highFloorTemperature;
	}

	@Override
	public int highAmbientTemperature() {
		return this.builder.highAmbientTemperature;
	}

	@Override
	public String floorThermometer_id() {
		return this.builder.floorThermometerId;
	}

	@Override
	public String ambientThermometer_id() {
		return this.builder.ambientThermometerId;
	}

	@Override
	public String[] floorRelays() {
		return this.builder.floorRelays;
	}

	@Override
	public String[] infraredRelays() {
		return this.builder.infraredRelays;
	}

	@Override
	public int floorPower() {
		return this.builder.floorPower;
	}

	@Override
	public int infraredPower() {
		return this.builder.infraredPower;
	}

	@Override
	public boolean hasExternalAmbientHeating() {
		return this.builder.hasExternalAmbientHeating;
	}

	@Override
	public String floorThermometer_target() {
		return generateReferenceTargetFilter(this.id(), this.floorThermometer_id());
	}

	@Override
	public String ambientThermometer_target() {
		return generateReferenceTargetFilter(this.id(), this.ambientThermometer_id());
	}

	@Override
	public String floorRelayComponents_target() {
		return generateReferenceTargetFilter(this.id(), this.floorRelays());
	}

	@Override
	public String infraredRelayComponents_target() {
		return generateReferenceTargetFilter(this.id(), this.infraredRelays());
	}
}