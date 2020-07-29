package io.openems.edge.controller.api.backend;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.types.ChannelAddress;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.controller.api.backend.BackendApi.ChannelId;

class ResendHistoricData extends AbstractCycleWorker {

	private final Logger log = LoggerFactory.getLogger(ResendHistoricData.class);

	private BackendApi parent;

	public ResendHistoricData(BackendApi parent) {
		this.parent = parent;
	}

	@Override
	public void activate(String name) {
		super.activate(name);
	}

	@Override
	public void deactivate() {
		super.deactivate();
	}

	@Override
	protected void forever() throws Throwable {

		BooleanReadChannel connectionStatus = this.parent.channel(ChannelId.BACKEND_CONNECTED);

		log.info("Backend Connected Channel: " + connectionStatus.getNextValue().get());

		if (connectionStatus.getNextValue().orElse(false)) {
			this.resendHistoricData();
		}
	}

	private void resendHistoricData() throws OpenemsNamedException {

		ZonedDateTime fromDate;
		ZonedDateTime toDate = ZonedDateTime.now();

		LongReadChannel lastResendData = this.parent.channel(ChannelId.LAST_RESEND_DATA);

		// store the channels that need to be queried.
		TreeSet<ChannelAddress> channels = new TreeSet<>();

		// Prepare message values and create JSON-RPC notification
		TimestampedDataNotification message = new TimestampedDataNotification();

		if (lastResendData.value().get() == null) {

			log.info("last Sent data is Null, and executing for the very forst time");

			// Executing for the first time.
			fromDate = ZonedDateTime.of(2020, 07, 01, 00, 00, 00, 00, ZoneId.of("UTC"));
			// toDate = ZonedDateTime.of(2020, 07, 14, 11, 00, 00, 00, ZoneId.of("UTC"));

			// Stores all the channels we want values. We are now concentrating only on
			// certain values.
			channels = this.getAddresses();

			/*
			 * Query all the data from July 1st till now.. Experimenting for now, later we
			 * change the from date
			 */

			this.parent.timedata.queryHistoricData("query", fromDate, toDate, channels, 10).entrySet()
					.forEach(entry -> {
						SortedMap<ChannelAddress, JsonElement> values = new TreeMap<>();
						entry.getValue().entrySet().forEach(entryMap -> {

							// Check for null and send only non null values.
							if (!entryMap.getValue().isJsonNull()) {
								values.put(entryMap.getKey(), entryMap.getValue());
								long timestamp = entry.getKey().toLocalDateTime().toInstant(ZoneOffset.UTC)
										.toEpochMilli();
								message.add(timestamp, values);
//									log.info(" timestamp " + timestamp + " Values " + values);
							}
						});
					});
			// Send the JsonRPC request
			this.sendMessage(message);

		} else {

			/**
			 * extract the values from queried data only during the time when the
			 * successfully sent is false
			 */
			BooleanReadChannel successfullySentChannel = this.parent.channel(ChannelId.SUCCESSFULLY_SENT);

			// List of blocks of timestamps when successfully sent is false.
			List<Map<ZonedDateTime, Boolean>> successfullyNotSent = new ArrayList<>();

			// add the successfullySentChannel channel that need to be queried from last
			// resend data timestamp.
			channels.add(successfullySentChannel.address());

			Instant istant = Instant.ofEpochMilli(lastResendData.value().get());
			fromDate = ZonedDateTime.ofInstant(istant, ZoneId.of("UTC"));

			// Get the blocks of time when successfully sent is false.
			Map<ZonedDateTime, Boolean> notSentList = new TreeMap<>();

			// Query the data for the succesfully sent channel to extract the false sent
			// values later.
			this.parent.timedata.queryHistoricData(null, fromDate, toDate, channels, 10).entrySet() //
					.forEach(entry -> {
						entry.getValue().entrySet().forEach(valueEntry -> {
							if (!valueEntry.getValue().isJsonNull()) {
								if (valueEntry.getValue().getAsBoolean() == false) {
									notSentList.put(entry.getKey(), valueEntry.getValue().getAsBoolean());
								} else {
									if (!notSentList.isEmpty()) {
										successfullyNotSent.add(notSentList);
										notSentList.clear();
									}
								}
							}
						});
					});

			// Stores all the channels we want values. We are now concentrating only on
			// certain values.
			// Note: It repalces the channels stored before (Successfully Sent).
			channels = this.getAddresses();

			for (Map<ZonedDateTime, Boolean> entry : successfullyNotSent) {

				// Each entry has a block of timestamps.

				// first Key -> start time of the time block.
				ZonedDateTime from = (ZonedDateTime) entry.entrySet().toArray()[0];

				// last key -> end time of the end block.
				ZonedDateTime to = (ZonedDateTime) entry.entrySet().toArray()[entry.size() - 1];

				// get the Historic Data of that certain block.
				this.parent.timedata.queryHistoricData(null, from, to, channels, 10).entrySet()
						.forEach(historicEntry -> {
							SortedMap<ChannelAddress, JsonElement> values = new TreeMap<>();
							historicEntry.getValue().entrySet().forEach(entryMap -> {

								// Check for null and send only non null values.
								if (!entryMap.getValue().isJsonNull()) {
									values.put(entryMap.getKey(), entryMap.getValue());
									long timestamp = historicEntry.getKey() //
											.toLocalDateTime() //
											.toInstant(ZoneOffset.UTC) //
											.toEpochMilli();
									message.add(timestamp, values);
								}
							});
							// Send the JsonRPC request
							this.sendMessage(message);
						});
			}
		}
	}

	/**
	 * 
	 * @return the set of channels that need to be queried.
	 */
	private TreeSet<ChannelAddress> getAddresses() {

		TreeSet<ChannelAddress> addresses = new TreeSet<>();

		this.parent.componentManager.getEnabledComponents().parallelStream() //
				.filter(c -> c.isEnabled()) //
				.flatMap(component -> component.channels().parallelStream()) //
				.filter(channel -> // Ignore WRITE_ONLY Channels
				channel.channelDoc().getAccessMode() == AccessMode.READ_ONLY
						|| channel.channelDoc().getAccessMode() == AccessMode.READ_WRITE) //
				.filter(name -> name.address().toString().contains("_sum")) //
				.filter(name -> (name.address().toString().contains("EssSoc")
						|| name.address().toString().contains("EssActivePower")
						|| name.address().toString().contains("ProductionActivePower")
						|| name.address().toString().contains("ConsumptionActivePower")
						|| name.address().toString().contains("GridActivePower")))//
//				.filter(name -> !name.address().toString().contains("EssActivePowerL")
//						&& !name.address().toString().contains("GridActivePowerL")
//						&& !name.address().toString().contains("ConsumptionActivePowerL")) //

				.forEach(channel -> {
					addresses.add(channel.address());
					log.info("addresses : " + channel.address());
				});

		return addresses;
	}

	/**
	 * Send the Json Rpc message.
	 * 
	 * @param message
	 */
	private void sendMessage(TimestampedDataNotification message) {
		if (!message.getData().isEmpty()) {
			log.info("message is not empty_______________________________");
//			log.info("Message is ==================================> " + message);
			boolean sent = this.parent.websocket.sendMessage(message);
			log.info("Sent_________________________________________________");
			if (sent) {
				this.parent.channel(ChannelId.LAST_RESEND_DATA).setNextValue(Instant.now().toEpochMilli());
			}
		}
	}

}
