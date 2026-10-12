package io.openems.edge.controller.ess.ripplecontrolreceiver.eebus;

import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.controller.ess.ripplecontrolreceiver.PowerProductionLimiterComponent;

import static io.openems.common.channel.PersistencePriority.HIGH;

public interface EebusPowerProductionLimiter extends PowerProductionLimiterComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		RESTRICTION_REASON(Doc.of(RestrictionModeReason.values())//),
				.persistencePriority(HIGH)), //

		FAILSAFE_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.persistencePriority(HIGH)), //

		UPDATE_FAILURE(Doc.of(Level.FAULT)), //
		;

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public enum RestrictionModeReason implements OptionsEnum {
		NO_LIMIT(-1, "NoLimit"), //
		ACTIVE_FAILSAFE(1, "ActiveFailsafe"), //
		LIMITED(2, "Limited"), //
		;

		private final int value;
		private final String name;

		private RestrictionModeReason(int value, String name) {
			this.value = value;
			this.name = name;
		}

		@Override
		public int getValue() {
			return this.value;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public OptionsEnum getUndefined() {
			return NO_LIMIT;
		}
	}

}
