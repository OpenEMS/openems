package io.openems.edge.evcs.evtec.core;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface EvTecCore extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CHARGER_STATE(Doc.of(CoreChargerState.values()) //
				.persistencePriority(PersistencePriority.HIGH)), //
		CHARGER_VERISON(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		CHARGER_NO_OF_CONNECTORS(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE) //
				.persistencePriority(PersistencePriority.LOW)), //
		CHARGER_ERROR(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		CHARGER_SERIAL(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		CHARGER_MODEL(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		COM_TIMEOUT_ENABLED(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)
				//
				.persistencePriority(PersistencePriority.HIGH)), //
		COM_TIMEOUT_VALUE(Doc.of(OpenemsType.INTEGER).unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)
				//
				.persistencePriority(PersistencePriority.HIGH)), //
		FALLBACK_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)
				//
				.persistencePriority(PersistencePriority.HIGH)) //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}

	}

	public default BooleanWriteChannel getComTimeoutEnabledChannel() {
		return this.channel(ChannelId.COM_TIMEOUT_ENABLED);
	}

	public default IntegerWriteChannel getComTimeoutValueChannel() {
		return this.channel(ChannelId.COM_TIMEOUT_VALUE);
	}

	public default IntegerWriteChannel getFallbackPowerChannel() {
		return this.channel(ChannelId.FALLBACK_POWER);
	}

}