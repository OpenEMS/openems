package io.openems.edge.io.revpi.dio;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class RevPiDioTestConfig extends AbstractComponentConfig implements RevPiDioConfig {

	protected static class Builder {
		private String id = null;
		private boolean initOutputFromHardware = false;
		private String prefixDigitalIn = "DigitalInput_";
		private String prefixDigitalOut = "DigitalOutput_";
		private int firstInputIndex = 1;
		private int firstOutputIndex = 1;
		private String inputUsed = "1|1|1|1|1|1|1|1|1|1|1|1|1|1|1|1";
		private String outputUsed = "1|1|1|1|1|1|1|1|1|1|1|1|1|1|1|1";

		public RevPiDioTestConfig build() {
			return new RevPiDioTestConfig(this);
		}

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setInitOutputFromHardware(boolean init) {
			this.initOutputFromHardware = init;
			return this;
		}

		public Builder setPrefixDigitalIn(String prefix) {
			this.prefixDigitalIn = prefix;
			return this;
		}

		public Builder setPrefixDigitalOut(String prefix) {
			this.prefixDigitalOut = prefix;
			return this;
		}

		public Builder setFirstInputIndex(int index) {
			this.firstInputIndex = index;
			return this;
		}

		public Builder setFirstOutputIndex(int index) {
			this.firstOutputIndex = index;
			return this;
		}

		public Builder setInputUsed(String used) {
			this.inputUsed = used;
			return this;
		}

		public Builder setOutputUsed(String used) {
			this.outputUsed = used;
			return this;
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

	private RevPiDioTestConfig(Builder builder) {
		super(RevPiDioConfig.class, builder.id);
		this.builder = builder;
	}

	@Override
	public boolean initOutputFromHardware() {
		return this.builder.initOutputFromHardware;
	}

	@Override
	public String prefixDigitalIn() {
		return this.builder.prefixDigitalIn;
	}

	@Override
	public String prefixDigitalOut() {
		return this.builder.prefixDigitalOut;
	}

	@Override
	public int firstInputIndex() {
		return this.builder.firstInputIndex;
	}

	@Override
	public int firstOutputIndex() {
		return this.builder.firstOutputIndex;
	}

	@Override
	public String inputUsed() {
		return this.builder.inputUsed;
	}

	@Override
	public String outputUsed() {
		return this.builder.outputUsed;
	}
}