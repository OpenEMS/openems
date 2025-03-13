/*
 *   OpenEMS Meter B+G E-Tech DS100 bundle
 *
 *   Written by Christian Poulter.
 *   Copyright (C) 2025 Christian Poulter <devel(at)poulter.de>
 *
 *   This program and the accompanying materials are made available under
 *   the terms of the Eclipse Public License v2.0 which accompanies this
 *   distribution, and is available at
 *
 *   https://www.eclipse.org/legal/epl-2.0
 *
 *   SPDX-License-Identifier: EPL-2.0
 *
 */package io.openems.edge.meter.bgetech.ds100;

import io.openems.common.utils.ConfigUtils;
import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.MeterType;

@SuppressWarnings("all")
public class MeterBgeTechDS100TestConfig extends AbstractComponentConfig implements Config {

    protected static class Builder {
    	private String id;
    	private String modbusId = null;
    	private int modbusUnitId;
        private MeterType type;
        private boolean invert;
    
    	private Builder() {}
    
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
    	
    	public Builder setType(MeterType type) {
            this.type = type;
            return this;
        }
    	
        public Builder setInvert(boolean invert) {
            this.invert = invert;
            return this;
        }
    
    	public MeterBgeTechDS100TestConfig build() {
	        return new MeterBgeTechDS100TestConfig(this);
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
    
    private MeterBgeTechDS100TestConfig(Builder builder) {
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
    public MeterType type() {
        return this.builder.type;
    }

    @Override
    public boolean invert() {
        return this.builder.invert;
    }

}
