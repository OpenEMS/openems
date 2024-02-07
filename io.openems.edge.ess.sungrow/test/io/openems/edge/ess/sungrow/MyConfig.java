package io.openems.edge.ess.sungrow;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

    protected static class Builder {
	private String id;
	private String modbusId = null;
	public int modbusUnitId;
	private boolean readOnly;

	private Builder() {
	}

	public Builder setId(String id) {
	    this.id = id;
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

	public Builder setReadOnly(boolean ro) {
	    this.readOnly = ro;
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
    public String modbus_id() {
	return this.builder.modbusId;
    }

    @Override
    public String Modbus_target() {
	return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id());
    }

    @Override
    public int modbusUnitId() {
	return this.builder.modbusUnitId;
    }

    @Override
    public boolean readOnly() {
	return this.builder.readOnly;
    }

}