package io.openems.edge.evcs.compleo.eco20;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.evcs.api.PhaseRotation;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

    protected static class Builder {
	private String id;
	private String modbusId;
	private int modbusUnitId;
	private Model model;
	private int minHwCurrent;
	private int maxHwCurrent;
	private PhaseRotation phaseRotation;
	private int startStopDelay;
	private boolean hasMeter;
	private boolean restartPilotSignal;
	private boolean debugMode;

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
	
	public Builder setModel(Model model) {
	    this.model = model;
	    return this;
	}

	public Builder setMinHwCurrent(int current) {
	    this.minHwCurrent = current;
	    return this;
	}

	public Builder setMaxHwCurrent(int current) {
	    this.maxHwCurrent = current;
	    return this;
	}

	public Builder setPhaseRotation(PhaseRotation rot) {
	    this.phaseRotation = rot;
	    return this;
	}

	public Builder setStartStopDelay(int delay) {
	    this.startStopDelay = delay;
	    return this;
	}

	public Builder setHasIntegratedMeter(boolean hasMeter) {
	    this.hasMeter = hasMeter;
	    return this;
	}

	public Builder setRestartPilotSignal(boolean restart) {
	    this.restartPilotSignal = restart;
	    return this;
	}

	public Builder setDebugMode(boolean debugMode) {
	    this.debugMode = debugMode;
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
    public int modbusUnitId() {
	return this.builder.modbusUnitId;
    }

    @Override
    public String Modbus_target() {
	return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id());
    }

    @Override
    public Model model() {
	return this.builder.model;
    }
    
    @Override
    public int minHwCurrent() {
	return this.builder.minHwCurrent;
    }

    @Override
    public int maxHwCurrent() {
	return this.builder.maxHwCurrent;
    }

    @Override
    public PhaseRotation phaseRotation() {
	return this.builder.phaseRotation;
    }

    @Override
    public int commandStartStopDelay() {
	return this.builder.startStopDelay;
    }

    @Override
    public boolean hasIntegratedMeter() {
	return this.builder.hasMeter;
    }

    @Override
    public boolean restartPilotSignal() {
	return this.builder.restartPilotSignal;
    }

    @Override
    public boolean debugMode() {
	return this.builder.debugMode;
    }


}