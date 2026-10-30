package io.openems.edge.sonnen.core;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.annotations.ReferenceCardinality;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.sonnen.ess.EssSonnen;
import io.openems.edge.sonnen.meter.MeterSonnen;
import io.openems.edge.sonnen.pv.PvSonnen;

/**
 * Service for communicating with a sonnenBatterie via its local JSON-API.
 *
 * <p>
 * Polls the read-API and distributes the values to the registered
 * {@link EssSonnen}, {@link MeterSonnen} and {@link PvSonnen} components.
 * Executes the write-API commands.
 */
public interface SonnenCore extends OpenemsComponent {

	/**
	 * Registers the {@link EssSonnen} component. Called by the
	 * {@link EssSonnen} implementation on activation. The reference cardinality
	 * is {@link ReferenceCardinality#OPTIONAL}.
	 *
	 * @param ess the {@link EssSonnen}; null on deactivation
	 */
	void setEss(EssSonnen ess);

	/**
	 * Registers the {@link MeterSonnen} component. Called by the
	 * {@link MeterSonnen} implementation on activation.
	 *
	 * @param meter the {@link MeterSonnen}; null on deactivation
	 */
	void setMeter(MeterSonnen meter);

	/**
	 * Registers the {@link PvSonnen} component. Called by the {@link PvSonnen}
	 * implementation on activation.
	 *
	 * @param pv the {@link PvSonnen}; null on deactivation
	 */
	void setPv(PvSonnen pv);

	/**
	 * Gets the registered {@link EssSonnen}.
	 *
	 * @return the {@link EssSonnen}
	 */
	Optional<EssSonnen> getEss();

	/**
	 * Gets the registered {@link MeterSonnen}.
	 *
	 * @return the {@link MeterSonnen}
	 */
	Optional<MeterSonnen> getMeter();

	/**
	 * Gets the registered {@link PvSonnen}.
	 *
	 * @return the {@link PvSonnen}
	 */
	Optional<PvSonnen> getPv();

	/**
	 * Sets the operating mode of the sonnenBatterie via
	 * 'PUT /api/v2/configurations'.
	 *
	 * <p>
	 * Redundant calls (same mode as the last successful one) are skipped.
	 *
	 * @param mode the {@link OperatingMode}
	 * @return the future; errors are signaled via the WRITE_COMMAND_FAILED channel
	 */
	CompletableFuture<Void> setOperatingMode(OperatingMode mode);

	/**
	 * Sets the charge set-point of the sonnenBatterie via
	 * 'POST /api/v2/setpoint/charge/{watts}'.
	 *
	 * <p>
	 * Redundant calls (same set-point as the last successful one) are skipped.
	 *
	 * @param watts the charge power in [W]; zero or positive
	 * @return the future; errors are signaled via the WRITE_COMMAND_FAILED channel
	 */
	CompletableFuture<Void> setChargeSetpoint(int watts);

	/**
	 * Sets the discharge set-point of the sonnenBatterie via
	 * 'POST /api/v2/setpoint/discharge/{watts}'.
	 *
	 * <p>
	 * Redundant calls (same set-point as the last successful one) are skipped.
	 *
	 * @param watts the discharge power in [W]; zero or positive
	 * @return the future; errors are signaled via the WRITE_COMMAND_FAILED channel
	 */
	CompletableFuture<Void> setDischargeSetpoint(int watts);

	public enum CoreChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * The communication with the sonnenBatterie read-API failed.
		 */
		SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT)),

		/**
		 * A write-API command failed, e.g. because the API token is missing or
		 * wrong, or a VPP is active.
		 */
		WRITE_COMMAND_FAILED(Doc.of(Level.FAULT));

		private final Doc doc;

		private CoreChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link CoreChannelId#SLAVE_COMMUNICATION_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getSlaveCommunicationFailedChannel() {
		return this.channel(CoreChannelId.SLAVE_COMMUNICATION_FAILED);
	}

	/**
	 * Gets the Slave Communication Failed State. See
	 * {@link CoreChannelId#SLAVE_COMMUNICATION_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getSlaveCommunicationFailed() {
		return this.getSlaveCommunicationFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link CoreChannelId#SLAVE_COMMUNICATION_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSlaveCommunicationFailed(boolean value) {
		this.getSlaveCommunicationFailedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link CoreChannelId#WRITE_COMMAND_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getWriteCommandFailedChannel() {
		return this.channel(CoreChannelId.WRITE_COMMAND_FAILED);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link CoreChannelId#WRITE_COMMAND_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setWriteCommandFailed(boolean value) {
		this.getWriteCommandFailedChannel().setNextValue(value);
	}
}
