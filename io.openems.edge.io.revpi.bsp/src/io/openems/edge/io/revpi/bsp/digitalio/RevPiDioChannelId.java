package io.openems.edge.io.revpi.bsp.digitalio;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;

public enum RevPiDioChannelId implements ChannelId {

    LAST_INVALIDATED_CHANNEL(Doc.of(OpenemsType.INTEGER)), //

    /**
     * Holds writes to ioX/Out1 for debugging.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    DEBUG_OUT1(Doc.of(OpenemsType.BOOLEAN)), //
    /**
     * ioX/Out1.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    OUT_1(new BooleanDoc() //
	    .accessMode(AccessMode.READ_WRITE) //
	    .onChannelSetNextWriteMirrorToDebugChannel(RevPiDioChannelId.DEBUG_OUT1)), //
    /**
     * Holds writes to ioX/Out2 for debugging.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    DEBUG_OUT2(Doc.of(OpenemsType.BOOLEAN)), //
    /**
     * ioX/Out2.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    OUT_2(new BooleanDoc() //
	    .accessMode(AccessMode.READ_WRITE) //
	    .onChannelSetNextWriteMirrorToDebugChannel(RevPiDioChannelId.DEBUG_OUT2)), //
    /**
     * Holds writes to ioX/Out3 for debugging.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    DEBUG_OUT3(Doc.of(OpenemsType.BOOLEAN)), //
    /**
     * ioX/Out3.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    OUT_3(new BooleanDoc() //
	    .accessMode(AccessMode.READ_WRITE) //
	    .onChannelSetNextWriteMirrorToDebugChannel(RevPiDioChannelId.DEBUG_OUT3)), //
    /**
     * Holds writes to ioX/Out4 for debugging.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    DEBUG_OUT4(Doc.of(OpenemsType.BOOLEAN)),
    /**
     * ioX/Out4.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    OUT_4(new BooleanDoc() //
	    .accessMode(AccessMode.READ_WRITE) //
	    .onChannelSetNextWriteMirrorToDebugChannel(RevPiDioChannelId.DEBUG_OUT4)), //
    /**
     * Holds writes to ioX/Out5 for debugging.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    DEBUG_OUT5(Doc.of(OpenemsType.BOOLEAN)), //
    /**
     * ioX/Out5.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    OUT_5(new BooleanDoc() //
	    .accessMode(AccessMode.READ_WRITE) //
	    .onChannelSetNextWriteMirrorToDebugChannel(RevPiDioChannelId.DEBUG_OUT5)), //
    /**
     * Holds writes to ioX/Out6 for debugging.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    DEBUG_OUT6(Doc.of(OpenemsType.BOOLEAN)), //
    /**
     * ioX/Out6.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    OUT_6(new BooleanDoc() //
	    .accessMode(AccessMode.READ_WRITE) //
	    .onChannelSetNextWriteMirrorToDebugChannel(RevPiDioChannelId.DEBUG_OUT6)), //
    /**
     * Holds writes to ioX/Out7 for debugging.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    DEBUG_OUT7(Doc.of(OpenemsType.BOOLEAN)), //
    /**
     * ioX/Out7.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    OUT_7(new BooleanDoc() //
	    .accessMode(AccessMode.READ_WRITE) //
	    .onChannelSetNextWriteMirrorToDebugChannel(RevPiDioChannelId.DEBUG_OUT7)), //
    /**
     * Holds writes to ioX/Out8 for debugging.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    DEBUG_OUT8(Doc.of(OpenemsType.BOOLEAN)), //
    /**
     * ioX/Out8.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    OUT_8(new BooleanDoc() //
	    .accessMode(AccessMode.READ_WRITE) //
	    .onChannelSetNextWriteMirrorToDebugChannel(RevPiDioChannelId.DEBUG_OUT8)), //
    /**
     * Holds writes to ioX/Out9 for debugging.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    DEBUG_OUT9(Doc.of(OpenemsType.BOOLEAN)), //
    /**
     * ioX/Out9.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    OUT_9(new BooleanDoc() //
	    .accessMode(AccessMode.READ_WRITE) //
	    .onChannelSetNextWriteMirrorToDebugChannel(RevPiDioChannelId.DEBUG_OUT9)), //
    /**
     * Holds writes to ioX/Out10 for debugging.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    DEBUG_OUT10(Doc.of(OpenemsType.BOOLEAN)), //
    /**
     * ioX/Out10.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    OUT_10(new BooleanDoc() //
	    .accessMode(AccessMode.READ_WRITE) //
	    .onChannelSetNextWriteMirrorToDebugChannel(RevPiDioChannelId.DEBUG_OUT10)), //
    /**
     * Holds writes to ioX/Out11 for debugging.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    DEBUG_OUT11(Doc.of(OpenemsType.BOOLEAN)), //
    /**
     * ioX/Out11.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    OUT_11(new BooleanDoc() //
	    .accessMode(AccessMode.READ_WRITE) //
	    .onChannelSetNextWriteMirrorToDebugChannel(RevPiDioChannelId.DEBUG_OUT11)), //
    /**
     * Holds writes to ioX/Out12 for debugging.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    DEBUG_OUT12(Doc.of(OpenemsType.BOOLEAN)), //
    /**
     * ioX/Out12.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    OUT_12(new BooleanDoc() //
	    .accessMode(AccessMode.READ_WRITE) //
	    .onChannelSetNextWriteMirrorToDebugChannel(RevPiDioChannelId.DEBUG_OUT12)), //
    /**
     * Holds writes to ioX/Out13 for debugging.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    DEBUG_OUT13(Doc.of(OpenemsType.BOOLEAN)), //
    /**
     * ioX/Out13.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    OUT_13(new BooleanDoc() //
	    .accessMode(AccessMode.READ_WRITE) //
	    .onChannelSetNextWriteMirrorToDebugChannel(RevPiDioChannelId.DEBUG_OUT13)), //
    /**
     * Holds writes to ioX/Out14 for debugging.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    DEBUG_OUT14(Doc.of(OpenemsType.BOOLEAN)), //

    /**
     * ioX/Out14.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    OUT_14(new BooleanDoc() //
	    .accessMode(AccessMode.READ_WRITE) //
	    .onChannelSetNextWriteMirrorToDebugChannel(RevPiDioChannelId.DEBUG_OUT14)), //

    /**
     * Holds writes to ioX/Out15 for debugging.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    DEBUG_OUT15(Doc.of(OpenemsType.BOOLEAN)), //

    /**
     * ioX/Out15.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    OUT_15(new BooleanDoc() //
	    .accessMode(AccessMode.READ_WRITE) //
	    .onChannelSetNextWriteMirrorToDebugChannel(RevPiDioChannelId.DEBUG_OUT15)), //

    /**
     * Holds writes to ioX/Out16 for debugging.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    DEBUG_OUT16(Doc.of(OpenemsType.BOOLEAN)), //

    /**
     * ioX/Out16.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    OUT_16(new BooleanDoc() //
	    .accessMode(AccessMode.READ_WRITE) //
	    .onChannelSetNextWriteMirrorToDebugChannel(RevPiDioChannelId.DEBUG_OUT16)), //

    /**
     * ioX/In1.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    IN_1(Doc.of(OpenemsType.BOOLEAN) //
	    .unit(Unit.NONE) //
	    .accessMode(AccessMode.READ_ONLY)), //

    /**
     * ioX/In2.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    IN_2(Doc.of(OpenemsType.BOOLEAN) //
	    .unit(Unit.NONE) //
	    .accessMode(AccessMode.READ_ONLY)), //

    /**
     * ioX/In3.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    IN_3(Doc.of(OpenemsType.BOOLEAN) //
	    .unit(Unit.NONE) //
	    .accessMode(AccessMode.READ_ONLY)), //

    /**
     * ioX/In4.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    IN_4(Doc.of(OpenemsType.BOOLEAN) //
	    .unit(Unit.NONE) //
	    .accessMode(AccessMode.READ_ONLY)), //

    /**
     * ioX/In5.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    IN_5(Doc.of(OpenemsType.BOOLEAN) //
	    .unit(Unit.NONE) //
	    .accessMode(AccessMode.READ_ONLY)), //

    /**
     * ioX/In6.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    IN_6(Doc.of(OpenemsType.BOOLEAN) //
	    .unit(Unit.NONE) //
	    .accessMode(AccessMode.READ_ONLY)), //

    /**
     * ioX/In7.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    IN_7(Doc.of(OpenemsType.BOOLEAN) //
	    .unit(Unit.NONE) //
	    .accessMode(AccessMode.READ_ONLY)), //

    /**
     * ioX/In8.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    IN_8(Doc.of(OpenemsType.BOOLEAN) //
	    .unit(Unit.NONE) //
	    .accessMode(AccessMode.READ_ONLY)), //

    /**
     * ioX/In9.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    IN_9(Doc.of(OpenemsType.BOOLEAN) //
	    .unit(Unit.NONE) //
	    .accessMode(AccessMode.READ_ONLY)), //

    /**
     * ioX/In10.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    IN_10(Doc.of(OpenemsType.BOOLEAN) //
	    .unit(Unit.NONE) //
	    .accessMode(AccessMode.READ_ONLY)), //

    /**
     * ioX/In11.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    IN_11(Doc.of(OpenemsType.BOOLEAN) //
	    .unit(Unit.NONE) //
	    .accessMode(AccessMode.READ_ONLY)), //

    /**
     * ioX/In12.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    IN_12(Doc.of(OpenemsType.BOOLEAN) //
	    .unit(Unit.NONE) //
	    .accessMode(AccessMode.READ_ONLY)), //

    /**
     * ioX/In13.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    IN_13(Doc.of(OpenemsType.BOOLEAN) //
	    .unit(Unit.NONE) //
	    .accessMode(AccessMode.READ_ONLY)), //

    /**
     * ioX/In14.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    IN_14(Doc.of(OpenemsType.BOOLEAN) //
	    .unit(Unit.NONE) //
	    .accessMode(AccessMode.READ_ONLY)), //

    /**
     * ioX/In15.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    IN_15(Doc.of(OpenemsType.BOOLEAN) //
	    .unit(Unit.NONE) //
	    .accessMode(AccessMode.READ_ONLY)), //

    /**
     * ioX/In16.
     * 
     * <ul>
     * <li>Interface: KunbusRevPiDataIOModule
     * <li>Type: Boolean
     * <li>Range: On/Off
     * </ul>
     */
    IN_16(Doc.of(OpenemsType.BOOLEAN) //
	    .unit(Unit.NONE) //
	    .accessMode(AccessMode.READ_ONLY)) //

    ;

    private final Doc doc;

    private RevPiDioChannelId(Doc doc) {
	this.doc = doc;
    }

    public Doc doc() {
	return this.doc;
    }
}