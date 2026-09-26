package io.openems.edge.controller.ess.limiter14a.eebus;

import static io.openems.common.channel.PersistencePriority.HIGH;

import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.controller.ess.limiter14a.ControllerEssLimiter14a;

public interface ControllerEssLimiter14aEebus extends ControllerEssLimiter14a {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		FAILSAFE_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.persistencePriority(HIGH)), //

		RESTRICTION_MODE_REASON(Doc.of(RestrictionModeReason.values())//
				.persistencePriority(HIGH)), //

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
