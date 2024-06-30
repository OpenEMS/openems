package io.openems.edge.controller.ess.timeframe;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Relationship;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String essId;
		private Mode mode;
		private Phase phase;
		private Relationship relationship;

        private int ess_capacity;
        private int targetSoC;
        private String startTime;
        private String endTime;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setEssId(String essId) {
			this.essId = essId;
			return this;
		}

		public Builder setMode(Mode mode) {
			this.mode = mode;
			return this;
		}

		public Builder setPhase(Phase phase) {
			this.phase = phase;
			return this;
		}

		public Builder setRelationship(Relationship relationship) {
			this.relationship = relationship;
			return this;
		}

        public Builder setEss_capacity(int ess_capacity) {
            this.ess_capacity = ess_capacity;
            return this;
        }

        public Builder setTargetSoC(int targetSoC) {
            this.targetSoC = targetSoC;
            return this;
        }

        public Builder setStartTime(String startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder setEndTime(String endTime) {
            this.endTime = endTime;
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
	public String ess_id() {
		return this.builder.essId;
	}

    @Override
    public int ess_capacity() {
        return this.builder.ess_capacity;
    }

    @Override
    public int targetSoC() {
        return this.builder.targetSoC;
    }

    @Override
    public String startTime() {
        return this.builder.startTime;
    }

    @Override
    public String endTime() {
        return this.builder.endTime;
    }

    @Override
	public Mode mode() {
		return this.builder.mode;
	}


	@Override
	public Relationship relationship() {
		return this.builder.relationship;
	}

	@Override
	public Phase phase() {
		return this.builder.phase;
	}

}