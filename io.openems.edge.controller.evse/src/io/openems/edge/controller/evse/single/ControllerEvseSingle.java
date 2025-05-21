package io.openems.edge.controller.evse.single;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.Unit.WATT_HOURS;
import static io.openems.common.types.OpenemsType.INTEGER;

import com.google.common.collect.ImmutableList;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.Profile;

public interface ControllerEvseSingle extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		ACTUAL_MODE(Doc.of(Mode.Actual.values())), //
		SESSION_ENERGY(Doc.of(INTEGER) //
				.unit(WATT_HOURS) //
				.persistencePriority(HIGH)), //
		SESSION_LIMIT_REACHED(Doc.of(Level.INFO) //
				.text("Session Limit reached")) //
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

	/**
	 * Gets the Controller {@link Params}.
	 * 
	 * @return the {@link Params}
	 */
	public Params getParams();

	/**
	 * Apply Current in [mA] and optionally {@link Profile.Command}s.
	 * 
	 * @param current         the Current in [mA]
	 * @param profileCommands the {@link Profile.Command}s
	 */
	public void apply(int current, ImmutableList<Profile.Command> profileCommands);

	/**
	 * Gets the Channel for {@link ChannelId#SESSION_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getSessionEnergyChannel() {
		return this.channel(ChannelId.SESSION_ENERGY);
	}

	/**
	 * Gets the Energy that was charged during the current in [Wh]. See
	 * {@link ChannelId#SESSON_ENERGY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSessionEnergy() {
		return this.getSessionEnergyChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SESSON_ENERGY}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSessionEnergy(int value) {
		this.getSessionEnergyChannel().setNextValue(value);
	}
}
