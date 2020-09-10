package io.openems.edge.battery.bmw;

import java.lang.annotation.Annotation;

import org.osgi.service.metatype.annotations.AttributeDefinition;

import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private String modbusId = null;
		private String batteryId = null;
		private StartStopConfig startStop = null;

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

		public Builder setBatteryId(String batteryId) {
			this.batteryId = batteryId;
			return this;
		}

		public Builder setStartStop(StartStopConfig startStop) {
			this.startStop = startStop;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}

	}

//--------------------------------------------------
	public MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	@Override
	public String modbus_id() {
		return this.builder.modbusId;
	}

	@Override
	public int modbusUnitId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public StartStopConfig startStop() {
		return this.builder.startStop;
	}

	@Override
	public String Modbus_target() {
		return "(&(enabled=true)(!(service.pid=" + this.id() + "))(|(id=" + this.modbus_id() + ")))";
	}

}
