package io.openems.edge.controller.api.backend;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
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

	// Counts the number of Cycles till data is sent to Backend.
	private int cycleCount = 0;

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

		// Increase CycleCount
		if (++this.cycleCount < this.parent.noOfCycles) {
			// Stop here if not reached CycleCount
			return;
		}

		/*
		 * Reached CycleCount -> Send data
		 */
		// Reset CycleCount
		this.cycleCount = 0;

		BooleanReadChannel connectionStatus = this.parent.channel(ChannelId.BACKEND_CONNECTED);

		log.info("Backend Connected Channel: " + connectionStatus.getNextValue().get());

		if (connectionStatus.getNextValue().orElse(false)) {
			this.resendHistoricData();
		}
	}

	private void resendHistoricData() throws OpenemsNamedException {

		// Time stamp last time when the data was resent to the backend.
		LongReadChannel lastResendtimeStamp = this.parent.channel(ChannelId.LAST_RESEND_DATA);
 
		ZonedDateTime fromDate;
		ZonedDateTime toDate = ZonedDateTime.now();

		//check if it is the first time the logic is working.
		if (lastResendtimeStamp.value().get() == null) {

			log.info("last Sent data is Null, and executing for the very forst time");

			fromDate = ZonedDateTime.of(2020, 07, 01, 00, 00, 00, 00, ZoneId.of("UTC"));
		} else {
			Instant istant = Instant.ofEpochMilli(lastResendtimeStamp.value().get());
			fromDate = ZonedDateTime.ofInstant(istant, ZoneId.of("UTC"));
		}

		/*
		 * Query all the data from July 1st till now.. Experimenting for now, later we
		 * change the from date
		 */
		queryHistoricData(fromDate, toDate);

	}

	private void queryHistoricData(ZonedDateTime fromDate, ZonedDateTime toDate) throws OpenemsNamedException {

		// Prepare message values and create JSON-RPC notification
		TimestampedDataNotification message = new TimestampedDataNotification();

		//
		List<TimestampedDataNotification> messageList = new ArrayList<>();

		// store the channels that need to be queried.
		TreeSet<ChannelAddress> channels = new TreeSet<>();

		// Stores all the channels we want values. We are now concentrating only on
		// certain values.
		channels = this.getAddresses();

		this.parent.timedata.queryHistoricData(null, fromDate, toDate, channels, 10).entrySet() //
				.forEach(entry -> {
					SortedMap<ChannelAddress, JsonElement> values = new TreeMap<>();

					entry.getValue().entrySet().forEach(valueEntry -> {
						if (!valueEntry.getValue().isJsonNull()) {
							values.put(valueEntry.getKey(), valueEntry.getValue());
							long timestamp = entry.getKey() //
									.toLocalDateTime() //
									.toInstant(ZoneOffset.UTC) //
									.toEpochMilli();
							message.add(timestamp, values);
						}
					});

					if (message.getData().size() > 24) {
						messageList.add(message);
						message.getData().clear();
					}
				});

		for (TimestampedDataNotification data : messageList) {

			// Send the JsonRPC request
			this.sendMessage(data);

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
	 * @param toDate
	 */
	private void sendMessage(TimestampedDataNotification message) {
		if (!message.getData().isEmpty()) {
			log.info("message is not empty_______________________________");
//			log.info("Message is ==================================> " + message);
			boolean sent = this.parent.websocket.sendMessage(message);
			log.info("Sent_________________________________________________");
			if (sent) {
				this.parent.channel(ChannelId.LAST_RESEND_DATA).setNextValue(message.getData().rowKeySet().first());
			}
		}
	}
}
