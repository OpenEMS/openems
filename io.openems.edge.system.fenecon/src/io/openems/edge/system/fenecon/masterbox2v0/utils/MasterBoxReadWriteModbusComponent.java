package io.openems.edge.system.fenecon.masterbox2v0.utils;

import java.util.List;

/**
 * This is an interface extends from the {@link MasterBoxModbusComponent}. If
 * the component also has ReadWrite Channels, implement this interface instead.
 * It adds listener to the channels to set the write value of the copies of the
 * channels from the ioc component.
 */
public interface MasterBoxReadWriteModbusComponent extends MasterBoxModbusComponent {

	/**
	 * Adds the listener to its channels. Call this method during the activate
	 * method.
	 */
	default void addListenerToChannels() {
		if (!this.hasIoc()) {
			return;
		}
		this.getWriteValueMappings().forEach(IocWriteValueMapping::addListener);
	}

	/**
	 * Removes the listener to its channels. Call this method during the deactivate
	 * method.
	 */
	default void removeListenerFromChannels() {
		this.getWriteValueMappings().forEach(IocWriteValueMapping::removeListener);
	}

	/**
	 * Gets the write value mappings. Note, that the returned list should always be
	 * the same instance, as the mappings are added and removed by reference.
	 * 
	 * @return a {@link List} of {@link IocWriteValueMapping}.
	 */
	List<IocWriteValueMapping<?>> getWriteValueMappings();
}
