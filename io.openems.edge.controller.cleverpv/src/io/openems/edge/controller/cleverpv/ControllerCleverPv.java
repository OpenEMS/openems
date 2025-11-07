package io.openems.edge.controller.cleverpv;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.Unit.CUMULATED_SECONDS;
import static io.openems.common.types.OpenemsType.LONG;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerCleverPv extends Controller, OpenemsComponent {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		UNABLE_TO_SEND(Doc.of(Level.WARNING)
				.translationKey(ControllerCleverPv.class, "unableToSend")),

		CONTROL_MODE(Doc.of(ControlMode.values()) //
				.persistencePriority(PersistencePriority.HIGH) //
				.accessMode(AccessMode.READ_ONLY)),

		CUMULATED_NO_DISCHARGE_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS) //
				.persistencePriority(HIGH)), //

		CUMULATED_INACTIVE_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS) //
				.persistencePriority(HIGH)), //

		CUMULATED_FORCE_CHARGE_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS) //
				.persistencePriority(HIGH)), //
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

	/**
	 * Gets the Channel for {@link ChannelId#CONTROL_MODE}.
	 *
	 * @return the Channel
	 */
	default Channel<ControlMode> getControlModeChannel() {
		return this.channel(ChannelId.CONTROL_MODE);
	}

	/**
	 * Gets the current mode of override. See {@link ChannelId#CONTROL_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	default ControlMode getControlMode() {
		return this.getControlModeChannel().value().asEnum();
	}

	/**
	 * Gets the current mode of control override. See
	 * {@link ChannelId#CONTROL_MODE}.
	 *
	 * @param controlMode the current control mode
	 */
	default void setControlMode(ControlMode controlMode) {
		this.getControlModeChannel().setNextValue(controlMode);
	}
}