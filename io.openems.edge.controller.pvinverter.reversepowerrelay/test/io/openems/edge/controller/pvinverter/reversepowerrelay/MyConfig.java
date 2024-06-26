package io.openems.edge.controller.pvinverter.reversepowerrelay;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

    protected static class Builder {
        private String id;
        private String pvInverterId;
        private String inputChannelAddress0Percent;
        private String inputChannelAddress30Percent;
        private String inputChannelAddress60Percent;
        private String inputChannelAddress100Percent;
        private int powerLimit30;
        private int powerLimit60;

        private Builder() {
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setPvInverterId(String pvInverterId) {
            this.pvInverterId = pvInverterId;
            return this;
        }

        public Builder setInputChannelAddress0Percent(String inputChannelAddress0Percent) {
            this.inputChannelAddress0Percent = inputChannelAddress0Percent;
            return this;
        }

        public Builder setInputChannelAddress30Percent(String inputChannelAddress30Percent) {
            this.inputChannelAddress30Percent = inputChannelAddress30Percent;
            return this;
        }

        public Builder setInputChannelAddress60Percent(String inputChannelAddress60Percent) {
            this.inputChannelAddress60Percent = inputChannelAddress60Percent;
            return this;
        }

        public Builder setInputChannelAddress100Percent(String inputChannelAddress100Percent) {
            this.inputChannelAddress100Percent = inputChannelAddress100Percent;
            return this;
        }

        public Builder setPowerLimit30(int powerLimit30) {
            this.powerLimit30 = powerLimit30;
            return this;
        }

        public Builder setPowerLimit60(int powerLimit60) {
            this.powerLimit60 = powerLimit60;
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
    public String pvInverter_id() {
        return builder.pvInverterId;
    }

    @Override
    public String inputChannelAddress0Percent() {
        return builder.inputChannelAddress0Percent;
    }

    @Override
    public String inputChannelAddress30Percent() {
        return builder.inputChannelAddress30Percent;
    }

    @Override
    public String inputChannelAddress60Percent() {
        return builder.inputChannelAddress60Percent;
    }

    @Override
    public String inputChannelAddress100Percent() {
        return builder.inputChannelAddress100Percent;
    }

    @Override
    public int powerLimit30() {
        return builder.powerLimit30;
    }

    @Override
    public int powerLimit60() {
        return builder.powerLimit60;
    }
}
