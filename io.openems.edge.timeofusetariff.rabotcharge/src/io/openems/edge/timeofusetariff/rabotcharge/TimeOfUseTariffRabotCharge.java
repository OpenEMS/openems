package io.openems.edge.timeofusetariff.rabotcharge;

import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

public interface TimeOfUseTariffRabotCharge extends TimeOfUseTariff, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		HTTP_STATUS_CODE(Doc.of(OpenemsType.INTEGER)//
				.translationKey(TimeOfUseTariffRabotCharge.class, "httpStatusCode")), //
		STATUS_AUTHENTICATION_FAILED(Doc.of(Level.WARNING) //
				.translationKey(TimeOfUseTariffRabotCharge.class, "statusAuthenticationFailed")), //
		/**
		 * Should never happen. Only happens if the request has missing fields or wrong
		 * format of timestamps.
		 */
		STATUS_BAD_REQUEST(Doc.of(Level.FAULT) //
				.translationKey(TimeOfUseTariffRabotCharge.class, "statusBadRequest")), //
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
	 * Internal method to set the 'nextValue' on {@link ChannelId#HTTP_STATUS_CODE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setHttpStatusCode(int value) {
		this.getHttpStatusCodeChannel().setNextValue(value);
	}

	/**
	 * Gets the HttpStatusCode value. See {@link ChannelId#HTTP_STATUS_CODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHttpStatusCode() {
		return this.getHttpStatusCodeChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#HTTP_STATUS_CODE}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getHttpStatusCodeChannel() {
		return this.channel(ChannelId.HTTP_STATUS_CODE);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#STATUS_AUTHENTICATION_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setStatusAuthenticationFailed(boolean value) {
		this.getStatusAuthenticationFailedChannel().setNextValue(value);
	}

	/**
	 * Gets the status value. See {@link ChannelId#STATUS_AUTHENTICATION_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getStatusAuthenticationFailed() {
		return this.getStatusAuthenticationFailedChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#STATUS_AUTHENTICATION_FAILED}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getStatusAuthenticationFailedChannel() {
		return this.channel(ChannelId.STATUS_AUTHENTICATION_FAILED);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#STATUS_BAD_REQUEST} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setStatusBadRequest(boolean value) {
		this.getStatusBadRequestChannel().setNextValue(value);
	}

	/**
	 * Gets the status value. See {@link ChannelId#STATUS_BAD_REQUEST}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getStatusBadRequest() {
		return this.getStatusBadRequestChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#STATUS_BAD_REQUEST}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getStatusBadRequestChannel() {
		return this.channel(ChannelId.STATUS_BAD_REQUEST);
	}

}
