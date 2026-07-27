package io.openems.edge.ess.saxpower;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

    protected static class Builder {
        private String id;
        private String alias = "";
        private boolean enabled = true;
        private String modbusId;
        private int modbusUnitId;
        private SingleOrAllPhase phase = SingleOrAllPhase.L1;
        private int capacity;

        private Builder() {
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setAlias(String alias) {
            this.alias = alias;
            return this;
        }

        public Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder setModbusId(String modbusId) {
            this.modbusId = modbusId;
            return this;
        }

        public Builder setModbusUnitId(int modbusUnitId) {
            this.modbusUnitId = modbusUnitId;
            return this;
        }

        public Builder setPhase(SingleOrAllPhase phase) {
            this.phase = phase;
            return this;
        }

        public Builder setCapacity(int capacity) {
            this.capacity = capacity;
            return this;
        }

        public MyConfig build() {
            return new MyConfig(this);
        }
    }

    public static Builder create() {
        return new Builder();
    }

    private final Builder builder;

    private MyConfig(Builder builder) {
        super(Config.class, builder.id);
        this.builder = builder;
    }

    @Override
    public String alias() {
        return this.builder.alias;
    }

    @Override
    public boolean enabled() {
        return this.builder.enabled;
    }

    @Override
    public String modbus_id() {
        return this.builder.modbusId;
    }

    @Override
    public int modbusUnitId() {
        return this.builder.modbusUnitId;
    }

    @Override
    public SingleOrAllPhase phase() {
        return this.builder.phase;
    }

    @Override
    public int capacity() {
        return this.builder.capacity;
    }

    @Override
    public String Modbus_target() {
        return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id());
    }
}