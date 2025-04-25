package io.openems.edge.controller.api.modbus;

import io.openems.edge.bridge.modbus.api.Parity;
import io.openems.edge.bridge.modbus.api.Stopbit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.controller.api.Controller;

public abstract class AbstractModbusRtuApi extends AbstractModbusApi
		implements ModbusApi, Controller, OpenemsComponent, ComponentJsonApi {

	public AbstractModbusRtuApi(String implementationName,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	public static final class RtuConfig extends AbstractModbusConfig {
		private final String portName;
		private final int baudRate;
		private final int databits;
		private final Stopbit stopbits;
		private final Parity parity;

		public RtuConfig(String id, String alias, boolean enabled, Meta metaComponent, String[] componentIds,
				int apiTimeout, String portName, int baudRate, int databits, Stopbit stopbits, Parity parity,
				int maxConcurrentConnections) {
			super(id, alias, enabled, metaComponent, componentIds, apiTimeout, maxConcurrentConnections);
			this.portName = portName;
			this.baudRate = baudRate;
			this.databits = databits;
			this.stopbits = stopbits;
			this.parity = parity;
		}

		/**
		 * Returns the portName.
		 *
		 * @return the portName
		 */
		public String portName() {
			return this.portName;
		}

		/**
		 * Returns the baudRate.
		 *
		 * @return the baudRate
		 */
		public int baudRate() {
			return this.baudRate;
		}

		/**
		 * Returns the databits.
		 *
		 * @return databits
		 */
		public int databits() {
			return this.databits;
		}

		/**
		 * Returns the stopbits.
		 *
		 * @return the stopbits
		 */
		public Stopbit stopbits() {
			return this.stopbits;
		}

		/**
		 * Returns the parity.
		 *
		 * @return the parity
		 */
		public Parity parity() {
			return this.parity;
		}

		@Override
		public boolean equals(Object other) {
			if (!super.equals(other)) {
				return false;
			}
			if (!(other instanceof RtuConfig config)) {
				return false;
			}
			return this.baudRate == config.baudRate //
					&& this.databits == config.databits //
					&& this.stopbits == config.stopbits //
					&& this.parity == config.parity //
					&& this.portName.equals(config.portName);
		}

	}

}
