
package io.openems.edge.bridge.sml;

import org.openmuc.jrxtx.DataBits;
import org.openmuc.jrxtx.FlowControl;
import org.openmuc.jrxtx.Parity;
import org.openmuc.jrxtx.StopBits;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.bridge.sml.api.BridgeSml;

@SuppressWarnings("all")
public class MyConfigSerial extends AbstractComponentConfig implements BridgeSmlSerialConfig {

	protected static class Builder {
		private String id = null;
		public String portName;
		public int baudRate;
		public DataBits databits;
		public StopBits stopbits;
		public Parity parity;
		public int timeout;
		public FlowControl flowControl;
		public int invalidateElementsAfterReadErrors;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setPortName(String portName) {
			this.portName = portName;
			return this;
		}

		public Builder setBaudRate(int baudRate) {
			this.baudRate = baudRate;
			return this;
		}

		public Builder setDatabits(DataBits databits) {
			this.databits = databits;
			return this;
		}

		public Builder setStopbits(StopBits stopbits) {
			this.stopbits = stopbits;
			return this;
		}

		public Builder setParity(Parity parity) {
			this.parity = parity;
			return this;
		}
		
		public Builder setFlowControl(FlowControl flowControl) {
			this.flowControl = flowControl;
			return this;
		}
		
		public Builder setTimeout(int timeout) {
			this.timeout = timeout;
			return this;
		}
		
		public Builder setInvalidateElementsAfterReadErrors(int errors) {
			this.invalidateElementsAfterReadErrors = errors;
			return this;
		}

		public MyConfigSerial build() {
			return new MyConfigSerial(this);
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

	private MyConfigSerial(Builder builder) {
		super(BridgeSmlSerialConfig.class, builder.id);
		this.builder = builder;
	}

	@Override
	public String portName() {
		return this.builder.portName;
	}

	@Override
	public int baudRate() {
		return this.builder.baudRate;
	}

	@Override
	public DataBits databits() {
		return this.builder.databits;
	}

	@Override
	public StopBits stopbits() {
		return this.builder.stopbits;
	}

	@Override
	public Parity parity() {
		return this.builder.parity;
	}

	@Override
	public int timeout() {
		return this.builder.timeout;
	}

	@Override
	public FlowControl flowControl() {
		return this.builder.flowControl;
	}

	@Override
	public int invalidateElementsAfterReadErrors() {
		return this.builder.invalidateElementsAfterReadErrors;
	}
}