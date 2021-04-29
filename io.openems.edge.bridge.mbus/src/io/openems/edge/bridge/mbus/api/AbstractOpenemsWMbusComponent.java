package io.openems.edge.bridge.mbus.api;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import org.openmuc.jmbus.VariableDataStructure;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is the template class for a Wireless M-Bus device. For an example of how to implement a device using this class,
 * look in io.openems.edge.meter.watermeter.
 */

public abstract class AbstractOpenemsWMbusComponent extends AbstractOpenemsComponent {

	private String radioAddress;
	protected boolean dynamicDataAddress = false; // For WM-Bus devices that have dynamic addresses of their data.

	/*
	 * The protocol. Consume via 'getWMbusProtocol()'
	 */
	private WMbusProtocol protocol = null;

	/**
	 * Default constructor for AbstractOpenemsWMbusComponent.
	 *
	 * <p>
	 * Automatically initializes (i.e. creates {@link Channel} instances for each
	 * given {@link ChannelId} using the Channel-{@link Doc}.
	 *
	 * <p>
	 * It is important to list all Channel-ID enums of all inherited
	 * OpenEMS-Natures, i.e. for every OpenEMS Java interface you are implementing,
	 * you need to list the interface' ChannelID-enum here like
	 * Interface.ChannelId.values().
	 *
	 * <p>
	 * Use as follows:
	 *
	 * <pre>
	 * public YourPhantasticOpenemsComponent() {
	 * 	super(//
	 * 			OpenemsComponent.ChannelId.values(), //
	 * 			YourPhantasticOpenemsComponent.ChannelId.values());
	 * }
	 * </pre>
	 *
	 * <p>
	 * Note: the separation in firstInitialChannelIds and furtherInitialChannelIds
	 * is only there to enforce that calling the constructor cannot be forgotten.
	 * This way it needs to be called with at least one parameter - which is always
	 * at least "OpenemsComponent.ChannelId.values()". Just use it as if it was:
	 *
	 * <pre>
	 * AbstractOpenemsComponent(ChannelId[]... channelIds)
	 * </pre>
	 *
	 * @param firstInitialChannelIds   the Channel-IDs to initialize.
	 * @param furtherInitialChannelIds the Channel-IDs to initialize.
	 */
	protected AbstractOpenemsWMbusComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
                                            io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	/**
	 * Call this method from Component implementations activate().
	 * 
	 * @param context        	ComponentContext of this component. Receive it from parameter for @Activate.
	 * @param id             	ID of this component. Typically 'config.id()'.
	 * @param alias          	Human-readable name of this Component. Typically 'config.alias()'. Defaults to 'id' if
	 *                          empty.
	 * @param enabled        	Whether the component should be enabled. Typically 'config.enabled()'.
	 * @param radioAddress   	Device Id of the M-Bus device, usually printed on the casing. Typically
	 *                          'config.radioAddress'.
	 * @param cm             	An instance of ConfigurationAdmin. Receive it using @Reference.
	 * @param wmbusReference 	The name of the @Reference setter method for the M-Bus bridge.
	 * @param wmbusId         	The ID of the M-Bus bridge. Typically 'config.wmbusBridgeId()'.
	 * @param key         	 	The decryption key for the encrypted data sent by the device.
	 * @return true if the target filter was updated. You may use it to abort the
	 *         activate() method.
	 */
	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, String radioAddress,
			ConfigurationAdmin cm, String wmbusReference, String wmbusId, String key) {
		super.activate(context, id, alias, enabled);
		// update filter for 'WirelessMbus'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "WirelessMbus", wmbusId)) {
			return true;
		}
		this.radioAddress = radioAddress;
		if (radioAddress.length() != 8) {
			// A valid radio address is 8 chars long.
			return true;
		}
		BridgeWMbus wmbus = this.wmbusId.get();
		if (this.isEnabled() && wmbus != null) {
			wmbus.addProtocol(this.id(), this.getWMbusProtocol(key));
		}
		return false;
	}

	@Override
	protected void deactivate() {
		super.deactivate();
		BridgeWMbus wmbus = this.wmbusId.getAndSet(null);
		if (wmbus != null) {
			wmbus.removeProtocol(this.id());
		}
	}

	/**
	 * Get the Wireless M-Bus radio address of this device.
	 *
	 * @return the radio address.
	 */
	public String getRadioAddress() {
		return this.radioAddress;
	}

	private AtomicReference<BridgeWMbus> wmbusId = new AtomicReference<BridgeWMbus>(null);

	/**
	 * Set the WMbus bridge. Should be called by @Reference
	 *
	 * @param wmbus the BridgeWMbus Reference
	 */
	protected void setWMbus(BridgeWMbus wmbus) {
		this.wmbusId.set(wmbus);
	}

	/**
	 * Unset the WMbus bridge. Should be called by @Reference
	 *
	 * @param wmbus the BridgeWMbus Reference
	 */
	protected void unsetWMbus(BridgeWMbus wmbus) {
		this.wmbusId.compareAndSet(wmbus, null);
		if (wmbus != null) {
			wmbus.removeProtocol(this.id());
		}
	}

	/**
	 * Gets the WMbus protocol.
	 *
	 * @param key      The decryption key for the encrypted data sent by the device.
	 *
	 * @return the WMbusProtocol
	 */
	private WMbusProtocol getWMbusProtocol(String key) {
		WMbusProtocol protocol = this.protocol;
		if (protocol != null) {
			return protocol;
		}
		this.protocol = this.defineWMbusProtocol(key);
		return this.protocol;
	}

	/**
	 * Defines the WMbus protocol.
	 * Here you define channels of the wmbus device and the record position of its
	 * corresponding values or the datatype if the channel displays secondary
	 * address values.
	 *
	 * @param key      The decryption key for the encrypted data sent by the device.
	 *
	 * @return the WMbusProtocol
	 */
	protected abstract WMbusProtocol defineWMbusProtocol(String key);

	/**
	 * Some meters change the record positions in their data during runtime. This option accounts for that. When enabled,
	 * it checks for the correctness of the record position by comparing the unit of the channel with the unit of the
	 * data on that record position. If it is a mismatch, the records are searched to find a data item with matching unit.
	 *
	 * @return use dynamicDataAddress true/false
	 */
	public boolean isDynamicDataAddress() {
		return this.dynamicDataAddress;
	}

	/**
	 * If "dynamicDataAddress" is true, this method is called. It checks for the correctness of the record position by
	 * comparing the unit of the channel with the unit of the data on that record position. If it is a mismatch, the
	 * records are searched to find a data item with matching unit.
	 * For an example of how to implement this, look at io.openems.edge.meter.watermeter.
	 *
	 * @param data         				The data received from the WM-Bus device.
	 * @param channelDataRecordsList	The list of channelDataRecords, where the addresses are stored. The method
	 *                                  should modify the addresses in this list.
	 */
	public abstract void findRecordPositions(VariableDataStructure data, List<ChannelRecord> channelDataRecordsList);

	/**
	 * Log the signal strength of the received message.
	 *
	 * @param signalStrength The signal strength.
	 */
	public abstract void logSignalStrength(int signalStrength);
}
