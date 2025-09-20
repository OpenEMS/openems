package io.openems.edge.timedata.api;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;

import org.osgi.annotation.versioning.ProviderType;

import com.google.gson.JsonElement;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.CommonTimedataService;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface Timedata extends CommonTimedataService, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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
	 * Gets the latest known value for the given {@link ChannelAddress}.
	 *
	 * @param channelAddress the ChannelAddress to be queried
	 * @return the latest known value or Empty
	 */
	public CompletableFuture<Optional<Object>> getLatestValue(ChannelAddress channelAddress);

	/**
	 * Gets the latest known value for the given {@link ChannelAddress} of a not
	 * existing channel.
	 *
	 * <p>
	 * Gets the latest known value even if the ChannelAddress is not longer
	 * existing.
	 * 
	 * @param channelAddress the ChannelAddress to be queried
	 * @param unit           unit
	 * @return the latest known value or Empty
	 */
	public default CompletableFuture<Optional<Object>> getLatestValueOfNotExistingChannel(ChannelAddress channelAddress,
			Unit unit) {
		return CompletableFuture.completedFuture(Optional.empty());
	}

	/**
	 * Gets the {@link Timeranges} to data which got not send. The not send data
	 * gets determined with the notSendChannel and the lastResendTimestamp.
	 * 
	 * @param notSendChannel      the channel with the timestamps where the data got
	 *                            not send
	 * @param lastResendTimestamp the timestamp of the last resend; negativ if there
	 *                            is no lastResendTimestamp
	 * @return the {@link Timeranges}
	 * @throws OpenemsNamedException on error
	 */
	public Timeranges getResendTimeranges(//
			final ChannelAddress notSendChannel, //
			final long lastResendTimestamp //
	) throws OpenemsNamedException;

	/**
	 * Queries data to resend.
	 * 
	 * @param fromDate the start date
	 * @param toDate   the end date
	 * @param channels the channels to resend
	 * @return the query result; possibly null
	 * @throws OpenemsNamedException on error
	 */
	public SortedMap<Long, SortedMap<ChannelAddress, JsonElement>> queryResendData(//
			final ZonedDateTime fromDate, //
			final ZonedDateTime toDate, //
			final Set<ChannelAddress> channels //
	) throws OpenemsNamedException;

}
