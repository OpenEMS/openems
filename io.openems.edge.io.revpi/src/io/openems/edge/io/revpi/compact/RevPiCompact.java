package io.openems.edge.io.revpi.compact;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;

public interface RevPiCompact extends DigitalInput, DigitalOutput, OpenemsComponent, EventHandler {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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
		 * Holds writes to ioX/Out1 for debugging.
		 *
		 * <ul>
		 * <li>Interface: KunbusRevPiDataIOModule
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_OUT_1(Doc.of(OpenemsType.BOOLEAN)), //

		/**
		 * Holds writes to ioX/Out2 for debugging.
		 *
		 * <ul>
		 * <li>Interface: KunbusRevPiDataIOModule
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_OUT_2(Doc.of(OpenemsType.BOOLEAN)), //

		/**
		 * Holds writes to ioX/Out3 for debugging.
		 *
		 * <ul>
		 * <li>Interface: KunbusRevPiDataIOModule
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_OUT_3(Doc.of(OpenemsType.BOOLEAN)), //

		/**
		 * Holds writes to ioX/Out4 for debugging.
		 *
		 * <ul>
		 * <li>Interface: KunbusRevPiDataIOModule
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_OUT_4(Doc.of(OpenemsType.BOOLEAN)),

		/**
		 * Holds writes to ioX/Out5 for debugging.
		 *
		 * <ul>
		 * <li>Interface: KunbusRevPiDataIOModule
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_OUT_5(Doc.of(OpenemsType.BOOLEAN)), //

		/**
		 * Holds writes to ioX/Out6 for debugging.
		 *
		 * <ul>
		 * <li>Interface: KunbusRevPiDataIOModule
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_OUT_6(Doc.of(OpenemsType.BOOLEAN)), //

		/**
		 * Holds writes to ioX/Out7 for debugging.
		 *
		 * <ul>
		 * <li>Interface: KunbusRevPiDataIOModule
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_OUT_7(Doc.of(OpenemsType.BOOLEAN)), //

		/**
		 * Holds writes to ioX/Out8 for debugging.
		 *
		 * <ul>
		 * <li>Interface: KunbusRevPiDataIOModule
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_OUT_8(Doc.of(OpenemsType.BOOLEAN)), //

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
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(RevPiCompact.ChannelId.DEBUG_OUT_1))), //

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
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(RevPiCompact.ChannelId.DEBUG_OUT_2))), //

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
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(RevPiCompact.ChannelId.DEBUG_OUT_3))), //

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
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(RevPiCompact.ChannelId.DEBUG_OUT_4))), //

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
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(RevPiCompact.ChannelId.DEBUG_OUT_5))), //

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
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(RevPiCompact.ChannelId.DEBUG_OUT_6))), //

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
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(RevPiCompact.ChannelId.DEBUG_OUT_7))), //

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
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(RevPiCompact.ChannelId.DEBUG_OUT_8))); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public static Boolean isReadChannel(RevPiCompact.ChannelId id) {
		return id.name().startsWith("IN");
	}

	public static Boolean isDebugChannel(RevPiCompact.ChannelId id) {
		return id.name().startsWith("DEBUG");
	}

	public static Boolean isWriteChannel(RevPiCompact.ChannelId id) {
		return id.name().startsWith("OUT");
	}
}