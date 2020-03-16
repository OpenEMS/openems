package io.openems.edge.io.revpi;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;

public enum RevPiDioChannelId implements ChannelId {
	
	/**
	 * ioX/Out1
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	OUT_1(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_WRITE)),  // 

	/**
	 * ioX/Out2
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	OUT_2(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_WRITE)),  // 

	/**
	 * ioX/Out3
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	OUT_3(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_WRITE)),  // 
	
	/**
	 * ioX/Out4
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	OUT_4(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_WRITE)),  // 

	/**
	 * ioX/Out5
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	OUT_5(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_WRITE)),  // 

	/**
	 * ioX/Out6
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	OUT_6(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_WRITE)),  // 

	/**
	 * ioX/Out7
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	OUT_7(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_WRITE)),  // 

	/**
	 * ioX/Out8
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	OUT_8(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_WRITE)),  // 

	/**
	 * ioX/Out9
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	OUT_9(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_WRITE)),  // 

	/**
	 * ioX/Out10
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	OUT_10(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_WRITE)),  // 

	/**
	 * ioX/Out11
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	OUT_11(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_WRITE)),  // 

	/**
	 * ioX/Out12
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	OUT_12(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_WRITE)),  // 

	/**
	 * ioX/Out13
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	OUT_13(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_WRITE)),  // 

	/**
	 * ioX/Out14
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	OUT_14(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_WRITE)),  // 

	
	/**
	 * ioX/In1
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	IN_1(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_ONLY)),  // 

	/**
	 * ioX/In2
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	IN_2(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_ONLY)),  // 

	/**
	 * ioX/In3
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	IN_3(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_ONLY)),  // 
	
	/**
	 * ioX/In4
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	IN_4(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_ONLY)),  // 

	/**
	 * ioX/In5
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	IN_5(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_ONLY)),  // 

	/**
	 * ioX/In6
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	IN_6(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_ONLY)),  // 

	/**
	 * ioX/In7
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	IN_7(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_ONLY)),  // 

	/**
	 * ioX/In8
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	IN_8(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_ONLY)),  // 

	/**
	 * ioX/In9
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	IN_9(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_ONLY)),  // 

	/**
	 * ioX/In10
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	IN_10(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_ONLY)),  // 

	/**
	 * ioX/In11
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	IN_11(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_ONLY)),  // 

	/**
	 * ioX/In12
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	IN_12(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_ONLY)),  // 

	/**
	 * ioX/In13
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	IN_13(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_ONLY)),  // 

	/**
	 * ioX/In14
	 * 
	 * <ul>
	 * <li>Interface: KunbusRevPiDataIOModule
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	IN_14(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_ONLY)),  // 
	
	
	;

	private final Doc doc;

	private RevPiDioChannelId(Doc doc) {
		this.doc = doc;
	}

	public Doc doc() {
		return this.doc;
	}
}