package io.openems.edge.bridge.can;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.can.api.BridgeCan;
import io.openems.edge.bridge.can.api.CanProtocol;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

/**
 * Abstract component for connecting to, querying and writing to a CAN device.
 */
public abstract class AbstractCanBridge extends AbstractOpenemsComponent implements BridgeCan, EventHandler {

	private LogVerbosity logVerbosity = LogVerbosity.NONE;
	private int invalidateElementsAfterReadErrors = 1;
	private final CanWorker worker;
	private final CanReadWorker canReadWorker;
	private CanHardwareType selectedHardware;

	protected AbstractCanBridge(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		this.canReadWorker = new CanReadWorker(this);
		this.worker = new CanWorker(this, this.canReadWorker);
	}

	protected void activate(ComponentContext context, String id, String alias, boolean enabled,
			LogVerbosity logVerbosity, int invalidateElementsAfterReadErrors) {
		super.activate(context, id, alias, enabled);
		this.logVerbosity = logVerbosity;
		this.invalidateElementsAfterReadErrors = invalidateElementsAfterReadErrors;
		if (this.isEnabled()) {
			this.worker.activate(id);
			this.canReadWorker.activate(id);
		}
	}

	@Override
	protected void deactivate() {
		super.deactivate();
		this.worker.deactivate();
		this.canReadWorker.deactivate();
		this.closeCanConnection();
	}

	public void setSelectedHardware(CanHardwareType hardware) {
		this.selectedHardware = hardware;
	}

	public CanHardwareType getSelectedHardware() {
		return this.selectedHardware;
	}

	/**
	 * Adds the protocol.
	 *
	 * @param sourceId Component-ID of the source
	 * @param protocol the CanProtocol
	 */
	@Override
	public void addProtocol(String sourceId, CanProtocol protocol) {
		this.worker.addProtocol(sourceId, protocol);
	}

	/**
	 * Removes the protocol.
	 *
	 * @param sourceId Component-ID of the source
	 */
	@Override
	public void removeProtocol(String sourceId) {
		this.worker.removeProtocol(sourceId);
	}

	@Override
	public boolean isSimulationMode() {
		return this.selectedHardware == CanHardwareType.SIMULATOR;
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.worker.onBeforeProcessImage();
			this.updateStats();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this.worker.onExecuteWrite();
			break;
		}
	}

	private void updateStats() {
		try {
			this.channel(BridgeCan.ChannelId.STATS_NATIVE_ERROR_COUNT_CYCLIC_SEND)
					.setNextValue(this.getCanConnection().statsGetCanFrameErrorCntrCyclicalSend());

			this.channel(BridgeCan.ChannelId.STATS_NATIVE_ERROR_COUNT_SEND)
					.setNextValue(this.getCanConnection().statsGetCanFrameErrorCntrSend());

			this.channel(BridgeCan.ChannelId.STATS_NATIVE_ERROR_COUNT_RECEIVE)
					.setNextValue(this.getCanConnection().statsGetCanFrameErrorCntrReceive());

			this.channel(BridgeCan.ChannelId.STATS_NATIVE_FRAMES_SEND_PER_CYCLE)
					.setNextValue(this.getCanConnection().statsGetCanFrameFramesSendPerCycle());

		} catch (OpenemsException ex) {
			;
		}
	}

	/**
	 * Gets the instance for Channel "SlaveCommunicationFailed".
	 *
	 * @return the Channel instance
	 */
	protected Channel<Boolean> getSlaveCommunicationFailedChannel() {
		return this.channel(BridgeCan.ChannelId.SLAVE_COMMUNICATION_FAILED);
	}

	@Override
	public LogVerbosity getLogVerbosity() {
		return this.logVerbosity;
	}

	@Override
	public void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

	@Override
	public int invalidateElementsAfterReadErrors() {
		return this.invalidateElementsAfterReadErrors;
	}

	/**
	 * Gets the debug stats of the worker.
	 *
	 * @return The stats of the worker as String or 'no worker', if there is no
	 *         worker present
	 */
	public String canWorkerDebugStats() {
		if (this.worker != null) {
			return this.worker.debugStats();
		}
		return "no worker";
	}

}
