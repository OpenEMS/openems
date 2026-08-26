package io.openems.edge.bridge.mbus.api;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.ComponentContext;

import io.openems.edge.common.component.AbstractOpenemsComponent;

public abstract class AbstractOpenemsMbusComponent extends AbstractOpenemsComponent {

	protected final List<ChannelRecord> channelDataRecordsList = new ArrayList<>();

	private Integer primaryAddress = null;

	protected AbstractOpenemsMbusComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	public List<ChannelRecord> getChannelDataRecordsList() {
		return this.channelDataRecordsList;
	}

	public Integer getPrimaryAddress() {
		return this.primaryAddress;
	}

	/**
	 * Call this method from Component implementations activate().
	 *
	 * @param context        ComponentContext of this component. Receive it from
	 *                       parameter for @Activate
	 * @param id             ID of this component. Typically 'config.id()'
	 * @param alias          Human-readable name of this Component. Typically
	 *                       'config.alias()'. Defaults to 'id' if empty
	 * @param enabled        Whether the component should be enabled. Typically
	 *                       'config.enabled()'
	 * @param primaryAddress Primary address of the M-Bus device. Typically
	 *                       'config.primaryAddress'
	 */
	protected void activate(ComponentContext context, String id, String alias, boolean enabled, int primaryAddress) {
		super.activate(context, id, alias, enabled);
		this.primaryAddress = primaryAddress;
		this.addChannelDataRecords();
	}

	/**
	 * Define channels of the mbus device and the record position of its
	 * corresponding values or the datatype if the channel displays secondary
	 * address values.
	 */
	protected abstract void addChannelDataRecords();
}
