package io.openems.edge.bridge.mccomms.api;

import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.bridge.mccomms.IMCCommsBridge;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Abstract class extended by components wanting to interface with a {@link io.openems.edge.bridge.mccomms.MCCommsBridge} for device communication
 */
public abstract class AbstractMCCommsComponent extends AbstractOpenemsComponent {
	/**
	 * the MCComms address of the device being communicated with TODO remove this field
	 */
	private int mcCommsAddress;
	/**
	 * Atomic reference to the {@link io.openems.edge.bridge.mccomms.MCCommsBridge} being used for device communication
	 */
	private AtomicReference<IMCCommsBridge> mcCommsBridgeAtomicReference;
	/**
	 * Logger for logging logs
	 */
	protected final Logger logger;
	
	/**
	 * @see AbstractOpenemsComponent#AbstractOpenemsComponent(io.openems.edge.common.channel.ChannelId[], io.openems.edge.common.channel.ChannelId[]...)
	 */
	protected AbstractMCCommsComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		logger = LoggerFactory.getLogger(getClass());
		mcCommsBridgeAtomicReference = new AtomicReference<>();
	}
	
	/**
	 * Not used for this type of component
	 * @param id not used
	 */
	protected void activate(String id) {
		throw new IllegalArgumentException("Use the other activate() method.");
	}
	
	/**
	 * Activate method overriden by descendants of this class
	 * @param context the OSGi ComponentContext
	 * @param id      the unique OpenEMS Component ID
	 * @param alias   Human-readable name of this Component. Typically
	 *                'config.alias()'. Defaults to 'id' if empty
	 * @param enabled is the Component enabled?
	 * @param mcCommsAddress the MCComms address of the device being communicated with TODO remove this field
	 * @param cm {@link ConfigurationAdmin} of this component; passed by OSGi {@link org.osgi.service.component.annotations.Reference}
	 * @param mcCommsBridgeID the OSGi component ID of the {@link io.openems.edge.bridge.mccomms.MCCommsBridge used for device communication by this component instance}
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
	
	/**
	 * Not used for this component type
	 * @param context the OSGi ComponentContext
	 * @param id      the unique OpenEMS Component ID
	 * @param alias   Human-readable name of this Component. Typically
	 *                'config.alias()'. Defaults to 'id' if empty
	 * @param enabled is the Component enabled?
	 */
	@Override
	protected void activate(ComponentContext context, String id, String alias, boolean enabled) {
		throw new IllegalArgumentException("Use the other activate() for MCComms components");
	}
	
	@Override
	protected void deactivate() {
		super.deactivate();
	}
	
	/**
	 * Overridden by implementing classes
	 * @param bridge the {@link io.openems.edge.bridge.mccomms.MCCommsBridge} to use to communicate with the device being controlled by this component instance
	 */
	public void setMCCommsBridge(IMCCommsBridge bridge) {
		this.mcCommsBridgeAtomicReference.set(bridge);
	}
	
	/**
	 * @return the MCComms address of the device being communicated with TODO remove this getter
	 */
	public int getMcCommsAddress() {
		return mcCommsAddress;
	}
	
	/**
	 * @return the {@link io.openems.edge.bridge.mccomms.MCCommsBridge} being used to communicate with the device this component instance controls
	 */
	protected IMCCommsBridge getMCCommsBridge() {
		return this.mcCommsBridgeAtomicReference.get();
	}
}
