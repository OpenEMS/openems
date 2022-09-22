package io.openems.edge.bridge.sml.api;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

public abstract class AbstractOpenemsSmlComponent extends AbstractOpenemsComponent {

	protected final List<ChannelRecord> channelDataRecords = new ArrayList<>();

	protected AbstractOpenemsSmlComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	public List<ChannelRecord> getChannelDataRecordsList() {
		return this.channelDataRecords;
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
	 * @param cm             An instance of ConfigurationAdmin. Receive it
	 *                       using @Reference
	 * @param mbusReference  The name of the @Reference setter method for the M-Bus
	 *                       bridge
	 * @param smlId         The ID of the M-Bus bridge. Typically
	 *                       'config.mbus_id()'
	 * @return true if the target filter was updated. You may use it to abort the
	 *         activate() method.
	 */
	protected boolean activate(ComponentContext context, String alias, String id, boolean enabled,
			ConfigurationAdmin cm, String smlId) {
		super.activate(context, id, alias, enabled);

		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "sml", smlId)) {
			return true;
		}
		this.addChannelDataRecords();
		return false;
	}

	/**
	 * Define channels of the mbus device and the record position of its
	 * corresponding values or the datatype if the channel displays secondary
	 * address values.
	 */
	protected abstract void addChannelDataRecords();
}
