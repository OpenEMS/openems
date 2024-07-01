package io.openems.edge.simulator.datasource.api;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.simulator.DataContainer;

public abstract class AbstractCsvDatasource extends AbstractOpenemsComponent
		implements SimulatorDatasource, EventHandler {

	private int timeDelta;
	private LocalDateTime lastIteration = LocalDateTime.MIN;
	private DataContainer data;

	protected abstract ComponentManager getComponentManager();

	protected abstract DataContainer getData() throws NumberFormatException, IOException;

	protected AbstractCsvDatasource(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	protected void activate(ComponentContext context, String id, String alias, boolean enabled, int timeDelta)
			throws NumberFormatException, IOException {
		super.activate(context, id, alias, enabled);
		this.timeDelta = timeDelta;
		this.data = this.getData();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE:
			var now = LocalDateTime.now(this.getComponentManager().getClock());
			if (this.timeDelta > 0 && Duration.between(this.lastIteration, now).getSeconds() < this.timeDelta) {
				// don't change record, if timeDetla is active and has not been passed yet
				return;
			}

			this.lastIteration = now;
			this.data.nextRecord();
			break;
		}
	}

	@Override
	public <T> T getValue(OpenemsType type, ChannelAddress channelAddress) {
		// First: try full ChannelAddress
		var valueOpt = this.data.getValue(channelAddress.toString());
		if (!valueOpt.isPresent()) {
			// Not found: try Channel-ID only (without Component-ID)
			valueOpt = this.data.getValue(channelAddress.getChannelId());
		}
		return TypeUtils.getAsType(type, valueOpt);
	}

	@Override
	public Set<String> getKeys() {
		return this.data.getKeys();
	}

	@Override
	public int getTimeDelta() {
		return this.timeDelta;
	}
}
