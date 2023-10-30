package io.openems.edge.bridge.can.linux;

import java.io.IOException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.can.AbstractCanBridge;
import io.openems.edge.bridge.can.CanHardwareType;
import io.openems.edge.bridge.can.LogVerbosity;
import io.openems.edge.bridge.can.api.BridgeCan;
import io.openems.edge.bridge.can.api.CanConnection;
import io.openems.edge.bridge.can.api.CanIoException;
import io.openems.edge.bridge.can.api.CanUtils;
import io.openems.edge.bridge.can.io.CanDevice;
import io.openems.edge.bridge.can.io.CanSimulator;
import io.openems.edge.bridge.can.linux.io.hw.CanSocketHardwareLinux;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Bridge.CAN.linux", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class BridgeCanImpl extends AbstractCanBridge implements BridgeCan, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(BridgeCanImpl.class);
	private Integer baudrate;
	private CanConnection _connection = null;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public BridgeCanImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeCan.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	/**
	 * Gets the baud rate.
	 * 
	 * @return Baud rate
	 */
	public int getBaudRate() {
		return this.baudrate;
	}

	/**
	 * Sets the baud rate.
	 * 
	 * @param newBaudRate set Baud rate
	 */
	public void setBaudRate(int newBaudRate) {
		this.baudrate = newBaudRate;
	}

	@Activate
	protected void activate(ComponentContext context, Config config) throws IOException {
		this.setSelectedHardware(config.selected_hardware());

		var b = config.can_baudrate();
		this.setBaudRate(CanUtils.parseHexInteger(b));
		super.activate(context, config.id(), config.alias(), config.enabled(), config.logVerbosity(),
				config.invalidateElementsAfterReadErrors());

		this.log.warn("Activate -----------");
		try {
			this.getCanConnection();
		} catch (OpenemsException e) {
			this.log.error("Activation failed: " + e.getMessage());
			throw new IOException("Activation failed: " + e.getMessage());
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.log.warn("Deactivate -----------");
		super.deactivate();
	}

	@Override
	public void closeCanConnection() {
		if (this._connection != null) {
			try {
				this._connection.close();
			} catch (CanIoException e) {
				;
			}
			this._connection = null;
		}
	}

	@Override
	public synchronized CanConnection getCanConnection() throws OpenemsException {
		if (this._connection == null) {
			CanDevice canDevice = null;
			if (this.getSelectedHardware() == CanHardwareType.SIMULATOR) {
				canDevice = new CanSimulator();
			} else {
				canDevice = new CanSocketHardwareLinux();
			}
			var connection = new CanConnection(this, this.getSelectedHardware(), canDevice);
			this._connection = connection;
		}
		if (!this._connection.isOpen()) {
			try {
				this._connection.connect(this.getBaudRate());

			} catch (Exception e) {
				throw new OpenemsException(
						"CAN Connection open [" + this.getBaudRate() + "] failed: " + e.getMessage());
			}
		}
		return this._connection;
	}

	@Override
	public String debugLog() {
		if (this.getLogVerbosity() != LogVerbosity.ALL) {
			return null;
		}
		return ((this._connection != null && this._connection.isOpen()) ? "open" : "close") + "," + this.getBaudRate()
				+ "kBaud" + "," + this.canWorkerDebugStats();
	}

}
