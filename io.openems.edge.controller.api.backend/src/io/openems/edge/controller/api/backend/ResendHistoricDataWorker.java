package io.openems.edge.controller.api.backend;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.type.TypeUtils;

public class ResendHistoricDataWorker {

	private final Logger log = LoggerFactory.getLogger(ResendHistoricDataWorker.class);

	private static final int INTERVAL = 300;

	private final BackendApiImpl parent;
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private int tasks = 0;

	public ResendHistoricDataWorker(BackendApiImpl parent) {
		this.parent = parent;
	}

	/**
	 * This method is called at the {@link OnOpen} event of the Websocket
	 * connection.
	 */
	public void onWebsocketOpen() {
		final var lastSuccessfulResend = new ChannelAddress(this.parent.id(), "LastSuccessfulResend");

		this.parent.getTimedata().getLatestValue(lastSuccessfulResend).thenAccept(latestValue -> {
			System.out.println("123============================================123");
			System.out.println(lastSuccessfulResend);
			final ZonedDateTime toDate = ZonedDateTime.now();
			final ZonedDateTime fromDate = toDate;

			// TODO if diff between latestValue and now is <= 31 then only minus latestValue
			// days and resolution is 5 minutes
			// or
			// diff between latestValue and now is > 31 then
			// * minus 31 days and resolution is 5 minutes
			// * minus latestValue and resolution is 1 hour
			// or
			// latestValue is undefined then
			// * minus 31 days and resolution is 5 minutes
			// * minus 334 and resolution is 1 hour

			List<ZonedDateTime> datesToResend = new ArrayList<>();
//			if (latestValue.isPresent()) {
//				var epochSeconds = (Double) latestValue.get();
//				var latestInstant = Instant.ofEpochSecond(epochSeconds.longValue());
//				var now = toDate.toInstant();
//
//				var diffInDays = Duration.between(latestInstant, now).toDays();
//				if (diffInDays <= 31) {
//					// if diff between latestValue and now is <= 31 then only minus latestValue
//					// days and resolution is 5 minutes
//					datesToResend.addAll(collectDatesToResend(fromDate.minusDays(diffInDays + 1), toDate,
//							new Resolution(5, ChronoUnit.MINUTES)));
//				} else {
//					// diff between latestValue and now is > 31 then
//					// * minus 31 days and resolution is 5 minutes
//					// * minus latestValue in days and resolution is 1 hour
//					collectDatesToResend(fromDate.minusDays(31L), toDate, new Resolution(5, ChronoUnit.MINUTES));
//					collectDatesToResend(fromDate.minusDays(diffInDays + 1), toDate,
//							new Resolution(1, ChronoUnit.HOURS));
//				}
//			} else {
////			 latestValue is undefined then
////			 * minus 31 days and resolution is 5 minutes
////			 * minus 334 and resolution is 1 hour
//				datesToResend.addAll(
//						collectDatesToResend(fromDate.minusDays(31L), toDate, new Resolution(5, ChronoUnit.MINUTES)));
//				datesToResend.addAll(
//						collectDatesToResend(fromDate.minusDays(334L), toDate, new Resolution(1, ChronoUnit.HOURS)));
//			}

//			datesToResend.addAll(
//					collectDatesToResend(fromDate.minusDays(3L), toDate, new Resolution(5, ChronoUnit.MINUTES)));

			var channelsToResend = this.parent.componentManager.getEnabledComponents() //
					.parallelStream() //
					.flatMap(component -> component.channels().parallelStream()) //
					.map(channel -> channel.address()) //
					.collect(Collectors.toSet());

//			for (var date : datesToResend) {
//				this.buildTask(date, channelsToResend);
//			}

			final var unableToSend = new ChannelAddress(this.parent.id(), "UnableToSend");

			var diff = Duration.between(fromDate.minusDays(3), toDate).toSeconds();
			System.out.println(diff);

			var fromTaskDate = fromDate.minusDays(3);
			var toTaskDate = fromTaskDate;

			var counter = diff;
			while (counter > 0) {
				toTaskDate = fromTaskDate.plusSeconds(INTERVAL);
				System.out.println("========== " + fromTaskDate + " | " + toTaskDate + " ==========");

				try {
//					Set<ResendTask> tasksToSend = new HashSet<>();
					List<ResendTask> tasksToSend = new ArrayList<>();

					var result = this.parent.getTimedata().queryHistoricData(null, fromTaskDate, toTaskDate,
							channelsToResend);
					for (var entry : result.entrySet()) {
						final var date = entry.getKey();
//						System.out.println(date);

						if (entry.getValue().containsKey(unableToSend)) {
							var value = entry.getValue().get(unableToSend);
							if (value != null && value.getAsDouble() > 0) {
								System.out.println("=========== Add new task");

								var a = entry.getValue();
								for (var b : a.entrySet()) {
									var key = b.getKey();
									var json = b.getValue();

									if (json.isJsonNull()) {
										continue;
									}

									Channel<?> channel = this.parent.componentManager.getChannel(key);
									var newType = TypeUtils.convert(channel.getType(), json);
//									System.out.println(newType);
									b.setValue(newType);
								}

								tasksToSend.add(new ResendTask(this, date.toInstant(), entry.getValue()));
//								this.executor.execute(new ResendTask(this, date.toInstant(), entry.getValue()));
								tasks++;
							}
						}
					}

					for (ResendTask resendTask : tasksToSend) {
						this.executor.execute(resendTask);
					}
				} catch (OpenemsNamedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				fromTaskDate = fromTaskDate.plusSeconds(INTERVAL);
				counter -= INTERVAL;
			}

			System.out.println("----------------- created tasks: " + tasks);
		});
	}

//	private List<ZonedDateTime> collectDatesToResend(ZonedDateTime fromDate, ZonedDateTime toDate,
//			Resolution resolution) {
//		final var unableToSend = new ChannelAddress(this.parent.id(), "UnableToSend");
//
//		try {
//			final var unableToSendValues = this.parent.getTimedata().queryHistoricData(null, fromDate, toDate,
//					unableToSend, resolution);
//
//			return unableToSendValues.entrySet() //
//					.stream() //
//					.filter(entry -> entry.getValue().isPresent()) //
//					.filter(entry -> (Double) entry.getValue().get() > 0) //
//					.map(entry -> entry.getKey()) //
//					.collect(Collectors.toList());
//		} catch (OpenemsNamedException e) {
//			return Collections.emptyList();
//		}
//	}
//
//	private void buildTask(ZonedDateTime date, Set<ChannelAddress> channels) {
//		try {
//			var data = this.parent.getTimedata().queryHistoricData(null, date, channels);
//			this.executor.execute(new ResendTask(this, date.toInstant(), data.get(date)));
//			System.out.println("========= Added new task");
//		} catch (OpenemsNamedException ex) {
//
//		}
//	}

	private static class ResendTask implements Runnable {

		private final ResendHistoricDataWorker parent;
		private final Instant timestamp;
		private final Map<ChannelAddress, JsonElement> data;

		public ResendTask(ResendHistoricDataWorker parent, Instant timestamp, Map<ChannelAddress, JsonElement> data) {
			this.parent = parent;
			this.timestamp = timestamp;
			this.data = data;
		}

		@Override
		public void run() {
			System.out.println("========= Execute task for date " + this.timestamp.atZone(ZoneId.of("Europe/Berlin")));

			final var cycleTime = this.parent.parent.cycle.getCycleTime();
			final var timestampMillis = this.timestamp.toEpochMilli() / cycleTime * cycleTime;

			var message = new TimestampedDataNotification();
			message.add(timestampMillis, this.data);

//			System.out.println(message);

			// Debug-Log
			if (this.parent.parent.config.debugMode()) {
				this.parent.parent.logInfo(this.parent.log, "Sending [" + this.data.size() + " values]: " + this.data);
			}

			// Try to send
			var wasSent = this.parent.parent.websocket.sendMessage(message);
//			var wasSent = true;

			System.out.println("---------Send to backend was " + wasSent);

			if (wasSent) {
//				this.parent.parent.getLastSuccessFulResendChannel().setNextValue(this.timestamp.getEpochSecond());
			}
		}

	}

}
