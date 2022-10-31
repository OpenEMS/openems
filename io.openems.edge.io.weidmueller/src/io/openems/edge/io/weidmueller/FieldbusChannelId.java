package io.openems.edge.io.weidmueller;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;

public class FieldbusChannelId implements io.openems.edge.common.channel.ChannelId {

	/**
	 * Factory for Digital-Input Channel-ID.
	 * 
	 * @param module the module number
	 * @param index  the index within the module
	 * @return the {@link FieldbusChannelId}
	 */
	public static final FieldbusChannelId forDigitalInput(int module, int index) {
		var doc = new BooleanDoc();
		doc.persistencePriority(PersistencePriority.MEDIUM);

		return new FieldbusChannelId(//
				String.format("DIGITAL_INPUT_M%d_C%d", module, index), //
				doc);
	}

	/**
	 * Factory for Digital-Output Channel-ID.
	 * 
	 * @param module the module number
	 * @param index  the index within the module
	 * @return the {@link FieldbusChannelId}
	 */
	public static final FieldbusChannelId forDigitalOutput(int module, int index) {
		var doc = new BooleanDoc();
		doc.persistencePriority(PersistencePriority.MEDIUM);
		doc.accessMode(AccessMode.READ_WRITE);

		return new FieldbusChannelId(//
				String.format("DIGITAL_OUTPUT_M%d_C%d", module, index), //
				doc);
	}

	private final String name;
	private final OpenemsTypeDoc<?> doc;

	public FieldbusChannelId(String name, OpenemsTypeDoc<?> doc) {
		this.name = name;
		this.doc = doc;
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public OpenemsTypeDoc<?> doc() {
		return this.doc;
	}
}
