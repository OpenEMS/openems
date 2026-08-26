package io.openems.backend.alerting;

import java.lang.annotation.Annotation;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import io.openems.backend.alerting.scheduler.MessageScheduler;
import io.openems.backend.alerting.scheduler.MessageSchedulerService;
import io.openems.backend.alerting.scheduler.MinuteTimer;
import io.openems.backend.common.alerting.OfflineEdgeAlertingSetting;
import io.openems.backend.common.alerting.SumStateAlertingSetting;
import io.openems.backend.common.mail.MailContext;
import io.openems.backend.common.mail.Mailer;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.test.DummyMetadata;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.test.TimeLeapClock;

public class Dummy {

	/**
	 * Dummy ThreadPoolExecutor, that executes tasks in the caller Thread.
	 *
	 * @return Dummy {@link ThreadPoolExecutor}
	 */
	public static ThreadPoolExecutor executor() {
		return new ThreadPoolExecutor(1, 1, 0, TimeUnit.MINUTES, new LinkedBlockingQueue<>()) {
			@Override
			public void execute(Runnable command) {
				command.run();
			}
		};
	}

	public static class MailerImpl implements Mailer {
		public record Mail(ZonedDateTime sentAt, String template) {
		}

		public final List<Mail> sentMails = new LinkedList<>();

		@Override
		public synchronized CompletableFuture<Integer> sendMail(ZonedDateTime sendAt, String template,
				List<MailContext> context) {
			this.sentMails.add(new Mail(sendAt, template));
			return CompletableFuture.completedFuture(context.size());
		}

		public int getMailsCount() {
			return this.sentMails.size();
		}
	}

	public static class MessageSchedulerServiceImpl implements MessageSchedulerService {
		public final List<MessageScheduler<? extends Message>> msgScheduler = new ArrayList<>();

		/**
		 * Find handler in MessageSchedulerService handler-list.
		 *
		 * @param <T>     type of handler
		 * @param handler to search for
		 * @return handler if found, else null
		 */
		@SuppressWarnings("unchecked")
		public <T extends Message> MessageScheduler<T> find(Handler<T> handler) {
			return (MessageScheduler<T>) this.msgScheduler.stream() //
					.filter(s -> s.isFor(handler)) //
					.findFirst() //
					.orElse(null);
		}

		@Override
		public <T extends Message> MessageScheduler<T> register(Handler<T> handler) {
			var msgSch = new MessageScheduler<>(handler);
			this.msgScheduler.add(msgSch);
			return msgSch;
		}

		@Override
		public <T extends Message> void unregister(Handler<T> handler) {
			this.msgScheduler.removeIf(msgs -> msgs.isFor(handler));
		}
	}

	public static class SimpleMetadataImpl extends DummyMetadata {
		@Override
		public EventAdmin getEventAdmin() {
			return new EventAdminImpl();
		}
	}

	public static class AlertingMetadataImpl extends SimpleMetadataImpl {
		private Collection<Edge> edges;
		private Map<String, List<OfflineEdgeAlertingSetting>> offlineSettings;
		private Map<String, List<SumStateAlertingSetting>> sumStateSettings;

		/**
		 * Initialize Metadata with test data for Offline-Alerting.
		 *
		 * @param edges    to add
		 * @param settings to add
		 */
		public void initializeOffline(Collection<Edge> edges, Map<String, List<OfflineEdgeAlertingSetting>> settings) {
			this.edges = edges;
			this.offlineSettings = settings;
			this.sumStateSettings = Map.of();
		}

		/**
		 * Initialize Metadata with test data for SumState-Alerting.
		 *
		 * @param edges    to add
		 * @param settings to add
		 */
		public void initializeSumState(Collection<Edge> edges, Map<String, List<SumStateAlertingSetting>> settings) {
			this.edges = edges;
			this.sumStateSettings = settings;
			this.offlineSettings = Map.of();
		}

		@Override
		public boolean isInitialized() {
			return true;
		}

		@Override
		public Optional<Edge> getEdge(String edgeId) {
			return this.edges.stream().filter(e -> Objects.equals(e.getId(), edgeId)).findFirst();
		}

		@Override
		public Collection<Edge> getAllOfflineEdges() {
			return this.edges.stream().filter(Edge::isOffline).toList();
		}

		@Override
		public List<OfflineEdgeAlertingSetting> getEdgeOfflineAlertingSettings(String edgeId) throws OpenemsException {
			return this.offlineSettings.get(edgeId);
		}

		@Override
		public List<SumStateAlertingSetting> getSumStateAlertingSettings(String edgeId) throws OpenemsException {
			return this.sumStateSettings.get(edgeId);
		}

		@Override
		public Optional<Level> getSumState(String edgeId) {
			return this.getEdge(edgeId).map(Edge::getSumState);
		}

		public void setSumState(String edgeId, Level sumState) {
			this.getEdge(edgeId).ifPresent(e -> e.setSumState(sumState));
		}

		public Map<String, List<OfflineEdgeAlertingSetting>> getOfflineSettings() {
			return this.offlineSettings;
		}
	}

	public static class EventAdminImpl implements EventAdmin {
		private final List<Event> lastEvents;

		public EventAdminImpl() {
			this.lastEvents = new ArrayList<>();
		}

		@Override
		public void postEvent(Event event) {
			this.lastEvents.add(event);
		}

		@Override
		public void sendEvent(Event event) {
			this.lastEvents.add(event);
		}
	}

	public static class TimeLeapMinuteTimer extends MinuteTimer {

		private final TimeLeapClock timeLeapClock;

		public TimeLeapMinuteTimer(Instant instant) {
			this(new TimeLeapClock(instant));
		}

		private TimeLeapMinuteTimer(TimeLeapClock clock) {
			super(clock);
			this.timeLeapClock = clock;
		}

		/**
		 * Leap the given amount in minutes. executing cycle method every time.
		 *
		 * @param amount to leap in minutes
		 */
		public void leap(long amount) {
			for (int i = 0; i < amount; i++) {
				this.timeLeapClock.leap(1, ChronoUnit.MINUTES);
				this.cycle();
			}
		}
	}

	protected static Config testConfig(int initialDelay, boolean onOffline, boolean onSumState) {
		return new Config() {

			@Override
			public Class<? extends Annotation> annotationType() {
				return Config.class;
			}

			@Override
			public String webconsole_configurationFactory_nameHint() {
				return "Alerting";
			}

			@Override
			public boolean notifyOnSumStateChange() {
				return onSumState;
			}

			@Override
			public boolean notifyOnOffline() {
				return onOffline;
			}

			@Override
			public int initialDelay() {
				return initialDelay;
			}
		};
	}
}
