package io.openems.edge.bridge.mccomms.api;

import io.openems.edge.bridge.mccomms.MCCommsBridge;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;

import java.util.concurrent.atomic.AtomicReference;

public class AbstractMCCommsComponent extends AbstractOpenemsComponent {
	private int mcCommsAddress;
	private AtomicReference<MCCommsBridge> mcCommsBridgeAtomicReference;
	
	protected AbstractMCCommsComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		mcCommsBridgeAtomicReference = new AtomicReference<>();
	}
	
	protected void activate(String id) {
		throw new IllegalArgumentException("Use the other activate() method.");
	}
	
	/**
	 * Call this method from Component implementations activate().
	 *
	 * @param context         ComponentContext of this component. Receive it from
	 *                        parameter for @Activate
	 * @param id              ID of this component. Typically 'config.id()'
	 * @param alias           Human-readable name of this Component. Typically
	 *                        'config.alias()'. Defaults to 'id' if empty
	 * @param enabled         Whether the component should be enabled. Typically
	 *                        'config.enabled()'
	 * @param mcCommsAddress  Unit-ID of the Modbus target
	 * @param cm              An instance of ConfigurationAdmin. Receive it
	 *                        using @Reference
	 * @param mcCommsBridgeComponentID   The component ID of the MCComms bridge. Typically
	 *                        'config.mcCommsBridgeComponentID()'
	 * @return true if the target filter was updated. You may use it to abort the
	 *         activate() method.
	 */
	@Activate
	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int mcCommsAddress,
	                           ConfigurationAdmin cm, String mcCommsBridgeComponentID) {
		super.activate(context, id, alias, enabled);
		// update filter for 'MCCommsBridge'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "MCCommsBridge", mcCommsBridgeComponentID)) {
			return true;
		}
		this.mcCommsAddress = mcCommsAddress;
		return false;
	}
	
	@Override
	protected void activate(ComponentContext context, String id, String alias, boolean enabled) {
		throw new IllegalArgumentException("Use the other activate() for MCComms components");
	}
	
	public void setMCCommsBridge(MCCommsBridge bridge) {
		this.mcCommsBridgeAtomicReference.set(bridge);
	}
	
	public int getMcCommsAddress() {
		return mcCommsAddress;
	}
	
	public MCCommsBridge getMCCommsBridge() {
		return this.mcCommsBridgeAtomicReference.get();
	}
}
