package io.openems.edge.bridge.mccomms.api;

import io.openems.edge.bridge.mccomms.IMCCommsBridge;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractMCCommsComponent extends AbstractOpenemsComponent {
	private int mcCommsAddress;
	private AtomicReference<IMCCommsBridge> mcCommsBridgeAtomicReference;
	protected final Logger logger;
	
	protected AbstractMCCommsComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		logger = LoggerFactory.getLogger(getClass());
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
	 * @param mcCommsAddress  MCComms address of the target device
	 * @param cm              An instance of ConfigurationAdmin. Receive it
	 *                        using @Reference
	 * @param mcCommsBridgeID   The component ID of the MCComms bridge. Typically
	 *                        'config.mcCommsBridge_id()'
	 */
	protected void activate(ComponentContext context, String id, String alias, boolean enabled, int mcCommsAddress,
	                           ConfigurationAdmin cm, String mcCommsBridgeID) {
		super.activate(context, id, alias, enabled);
		this.mcCommsAddress = mcCommsAddress;
		// update filter for 'MCCommsBridge'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "mcCommsBridge", mcCommsBridgeID)) {
			logInfo(logger, "reference filter updated");
		}
	}
	
	@Override
	protected void activate(ComponentContext context, String id, String alias, boolean enabled) {
		throw new IllegalArgumentException("Use the other activate() for MCComms components");
	}
	
	@Override
	protected void deactivate() {
		super.deactivate();
	}
	
	public void setMCCommsBridge(IMCCommsBridge bridge) {
		this.mcCommsBridgeAtomicReference.set(bridge);
	}
	
	public int getMcCommsAddress() {
		return mcCommsAddress;
	}
	
	protected IMCCommsBridge getMCCommsBridge() {
		return this.mcCommsBridgeAtomicReference.get();
	}
}
