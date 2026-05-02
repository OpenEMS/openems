package io.openems.edge.system.fenecon.masterbox2v0.utils;

import java.util.List;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.system.fenecon.masterbox2v0.MasterBox2v0;

/**
 * This is an interface for a MasterBox component that has its modbus communication
 * outsourced in the base component {@link MasterBox2v0}. It maps the read value
 * of the copies of the channels from the ioc component to the actual channel of
 * the component.
 */
public interface MasterBoxModbusComponent extends OpenemsComponent {

	/**
	 * Sets the read value of the channels from the values of the channels of the
	 * ioc component. Call this method during the handleEvent method.
	 * 
	 * @throws IllegalArgumentException on error
	 */
	default void setChannels() throws IllegalArgumentException {
		if (!this.hasIoc()) {
			return;
		}
		this.applyReadValueMappings();
	}

	/**
	 * Applies the read value mappings.
	 *
	 * @throws IllegalArgumentException on error
	 */
	default void applyReadValueMappings() throws IllegalArgumentException {
		for (IocReadValueMapping<?> mapping : this.getReadValueMappings()) {
			mapping.apply(this);
		}
	}

	/**
	 * Gets the read value mappings.
	 * 
	 * @return a {@link List} of {@link IocReadValueMapping}
	 */
	List<IocReadValueMapping<?>> getReadValueMappings();

	/**
	 * Has the component an ioc reference.
	 * 
	 * @return true if it has one
	 */
	boolean hasIoc();
}
