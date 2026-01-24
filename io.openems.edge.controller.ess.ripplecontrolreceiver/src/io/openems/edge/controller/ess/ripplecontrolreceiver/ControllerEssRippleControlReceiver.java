package io.openems.edge.controller.ess.ripplecontrolreceiver;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.types.OpenemsType.LONG;

import java.util.OptionalInt;

import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerEssRippleControlReceiver extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		RESTRICTION_MODE(Doc.of(EssRestrictionLevel.values())//
				.persistencePriority(HIGH)), //

		CUMULATED_RESTRICTION_TIME(Doc.of(LONG)//
				.unit(Unit.CUMULATED_SECONDS)//
				.persistencePriority(HIGH)); //

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}

	}

	/**
	 * Gets the Channel for {@link ChannelId#RESTRICTION_MODE}.
	 *
	 * @return the Channel
	 */
	default Channel<Boolean> getRestrictionModeChannel() {
		return this.channel(ChannelId.RESTRICTION_MODE);
	}

	/**
	 * Gets the restriction mode. See {@link ChannelId#RESTRICTION_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	default EssRestrictionLevel getRestrictionMode() {
		return this.getRestrictionModeChannel().value().asEnum();
	}

	/**
	 * Sets the restriction mode. See {@link ChannelId#RESTRICTION_MODE}.
	 *
	 * @param value the value as {@link EssRestrictionLevel}.
	 */
	default void _setRestrictionMode(EssRestrictionLevel value) {
		this.getRestrictionModeChannel().setNextValue(value);
	}

	/**
	 * Returns the current limitation factor as a decimal value between 0 and 1.
	 * 
	 * <p>
	 * While 1 means no limitation, 0 means a full limitation (no feed-in at all).
	 * </p>
	 * 
	 * @return the limitation factor as a double.
	 */
	/**
	 * Represents the current {@link EssRestrictionLevel}.
	 * 
	 * <p>
	 * This is determined by the external ripple control signals.
	 * </p>
	 * 
	 * @return the current restriction level.
	 */
	EssRestrictionLevel essRestrictionLevel();

	/**
	 * Represents the raw grid feed in limitation value of the meta component.
	 * 
	 * @return the limitation value.
	 */
	OptionalInt maximumGridFeedInLimit();

	/**
	 * Calculates the currently allowed grid feed-in power. This is determined as
	 * the minimum of:
	 * <ul>
	 * <li>the maximum apparent power multiplied by the limitation factor</li>
	 * <li>the raw limitation value</li>
	 * </ul>
	 *
	 * @param maxApparentPower the maximum apparent power in VA.
	 * @return the allowed grid feed-in power in VA.
	 */
	default int getDynamicGridFeedInLimit(int maxApparentPower) {
		
		return (int) Math.min(maxApparentPower * this.essRestrictionLevel().getLimitationFactor(),
				this.maximumGridFeedInLimit().orElse(maxApparentPower));
	}
}
