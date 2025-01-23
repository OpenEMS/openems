package io.openems.edge.controller.api.modbus;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.controller.api.Controller;

public abstract class AbstractModbusTcpApi extends AbstractModbusApi
		implements ModbusApi, Controller, OpenemsComponent {

	public static final int DEFAULT_PORT = 502;

	public AbstractModbusTcpApi(String implementationName,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	public class TcpConfig extends AbstractModbusConfig {
		private final int port;

		public TcpConfig(String id, String alias, boolean enabled, Meta metaComponent, String[] componentIds,
				int apiTimeout, int port, int maxConcurrentConnections) {
			super(id, alias, enabled, metaComponent, componentIds, apiTimeout, maxConcurrentConnections);
			this.port = port;
		}

		public int getPort() {
			return this.port;
		}

		@Override
		public boolean equals(Object other) {
			if (!super.equals(other)) {
				return false;
			}
			if (!(other instanceof TcpConfig config)) {
				return false;
			}
			return this.port == config.port;
		}

	}
}
