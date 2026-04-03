package io.openems.edge.goodwe.genset;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface GoodWeStsBoxGensetMeter extends OpenemsComponent {
	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		BACKUP_POWER(Doc.of(OpenemsType.LONG)//
				.unit(Unit.WATT)),
		GENSET_OPERATING_MODE(Doc.of(OpenemsType.BOOLEAN)//
				.persistencePriority(PersistencePriority.HIGH));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}

	}

	public default LongReadChannel getBackUpPowerChannel() {
		return this.channel(ChannelId.BACKUP_POWER);
	}

	public default Value<Long> getBackupPower() {
		return this.getBackUpPowerChannel().value();
	}

	public default void setBackupPower(long backupPower) {
		this.getBackUpPowerChannel().setNextValue(backupPower);
	}

	public default BooleanReadChannel getGensetOperatingModeChannel() {
		return this.channel(ChannelId.GENSET_OPERATING_MODE);
	}

	public default Value<Boolean> getGensetOperatingMode() {
		return this.getGensetOperatingModeChannel().value();
	}
}
