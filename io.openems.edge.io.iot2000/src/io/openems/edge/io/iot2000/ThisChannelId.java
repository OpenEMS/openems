package io.openems.edge.io.iot2000;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public enum ThisChannelId implements io.openems.edge.common.channel.ChannelId {

	/**
	 * Digital Output 1
	 * 
	 * <ul>
	 * <li>Interface: IOT2000
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	OUT_1(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_WRITE)), //

	/**
	 * Digital Output 2
	 * 
	 * <ul>
	 * <li>Interface: IOT2000
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	OUT_2(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_WRITE)), //

	/**
	 * Digital Input 1
	 * 
	 * <ul>
	 * <li>Interface: IOT2000
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	IN_1(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_ONLY)), //
	
	/**
	 * Digital Input 2
	 * 
	 * <ul>
	 * <li>Interface: IOT2000
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	IN_2(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_ONLY)), //
	
	/**
	 * Digital Input 3
	 * 
	 * <ul>
	 * <li>Interface: IOT2000
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	IN_3(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_ONLY)), //
	
	/**
	 * Digital Input 4
	 * 
	 * <ul>
	 * <li>Interface: IOT2000
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	IN_4(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_ONLY)), //
	
	/**
	 * Digital Input 5
	 * 
	 * <ul>
	 * <li>Interface: IOT2000
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	IN_5(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_ONLY)), //
	
	; //

	private final Doc doc;

	private ThisChannelId(Doc doc) {
		this.doc = doc;
	}

	public Doc doc() {
		return this.doc;
	}
}