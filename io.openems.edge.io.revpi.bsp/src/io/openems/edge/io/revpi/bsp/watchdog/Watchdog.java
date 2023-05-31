package io.openems.edge.io.revpi.bsp.watchdog;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface Watchdog extends OpenemsComponent {

    public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {

	/**
	 * Watchdog status bit.
	 * 
	 * <ul>
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	WATCHDOG(Doc.of(OpenemsType.BOOLEAN) //
		.unit(Unit.NONE) //
		.accessMode(AccessMode.READ_ONLY) //
	), //
	;

	private final Doc doc;

	private ChannelId(Doc doc) {
	    this.doc = doc;
	}

	public Doc doc() {
	    return this.doc;
	}

    }

}
