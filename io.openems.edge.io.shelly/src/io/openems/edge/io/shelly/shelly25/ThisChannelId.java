package io.openems.edge.io.shelly.shelly25;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;

public enum ThisChannelId implements io.openems.edge.common.channel.ChannelId {
	/**
	 * Holds writes to Relay Output 1 for debugging
	 * 
	 * <ul>
	 * <li>Interface: KmtronicRelayOutput
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	DEBUG_RELAY_1(Doc.of(OpenemsType.BOOLEAN)), //
	/**
	 * Relay Output 1
	 * 
	 * <ul>
	 * <li>Interface: KmtronicRelayOutput
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	RELAY_1(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(ThisChannelId.DEBUG_RELAY_1))), //
	/**
	 * Holds writes to Relay Output 2 for debugging
	 * 
	 * <ul>
	 * <li>Interface: KmtronicRelayOutput
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	DEBUG_RELAY_2(Doc.of(OpenemsType.BOOLEAN)), //
	/**
	 * Relay Output 2
	 * 
	 * <ul>
	 * <li>Interface: KmtronicRelayOutput
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	RELAY_2(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(ThisChannelId.DEBUG_RELAY_2))), //

	SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT)); //

	private final Doc doc;

	private ThisChannelId(Doc doc) {
		this.doc = doc;
	}

	public Doc doc() {
		return this.doc;
	}
}