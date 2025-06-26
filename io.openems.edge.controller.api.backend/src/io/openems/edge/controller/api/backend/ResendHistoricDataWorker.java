package io.openems.edge.controller.api.backend;

import static io.openems.common.utils.CollectorUtils.toTreeBasedTable;
import static java.util.stream.Collectors.toSet;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.notification.ResendDataNotification;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.worker.AbstractWorker;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.timedata.api.Timedata;

@Component(//
		scope = ServiceScope.PROTOTYPE, //
		service = { ResendHistoricDataWorker.class } //
)
public final class ResendHistoricDataWorker extends AbstractWorker {

	public record Config(//
			ChannelAddress addressForSuccessfulSend, //
			ChannelAddress addressForSuccessfulResend, //
			PersistencePriority resendPriority, //
			Consumer<? super Long> onLastSuccessfulResendUpdated, //
			Function<? super JsonrpcMessage, Boolean> onSendData //
	) {

	}

	protected enum TriggerState {
		INIT, //
		AFTER_TRIGGER, //
		SKIP_FIRST_FOREVER, //
		WAITING_FOR_TIMEDATA, //
		WAITING_FOR_CONFIG, //
	}

	/**
	 * This worker only starts resending data, after it got explicit triggered with
	 * {@link AbstractWorker#triggerNextRun()} and also after this delay time.
	 */
	protected static final int DELAY_TRIGGER_TIME = 300_000; // [milliseconds] 5 min
	private static final int MAX_RANDOM_DELAY = 3_600_000; // [milliseconds] 1 h
	private static final int BUFFER_SECONDS = 300; // [seconds] 5 min
	private static final int MAX_RESEND_TIMESPAN_SECONDS = 300; // [seconds] 5 min

	private final Logger log = LoggerFactory.getLogger(ResendHistoricDataWorker.class);

	@Reference
	private ComponentManager componentManager;
	private volatile Timedata timedata;

	private Config config;

	/**
	 * Trigger helper variable to delay execution of the forever method by
	 * DELAY_TRIGGER_TIME. If during the forever method timedata or the
	 * configuration is not set the state changes into either
	 * {@link TriggerState#WAITING_FOR_TIMEDATA} or
	 * {@link TriggerState#WAITING_FOR_CONFIG} and continues with the execution when
	 * the one missing got set.
	 * 
	 * <pre>
	 * triggerState = INIT
	 * tiggerNextRun();
	 * -> triggerState = AFTER_TRIGGER; 
	 * -> forever();
	 * -> if(triggerState == 1) triggerState = SKIP_FIRST_FOREVER;
	 * -> getCycleTime() = DELAY_TRIGGER_TIME; triggerState = INIT;
	 * -> forever();
	 * </pre>
	 */
	protected final AtomicReference<TriggerState> triggerState = new AtomicReference<>(TriggerState.INIT);

	@Activate
	public ResendHistoricDataWorker() {
	}

	@Override
	@Deactivate
	public void deactivate() {
		super.deactivate();
	}

	@Override
	protected void forever() throws Throwable {
		if (this.triggerState.compareAndSet(TriggerState.AFTER_TRIGGER, TriggerState.SKIP_FIRST_FOREVER)) {
			return;
		}

		Timedata timedata;
		final Config config;
		synchronized (this.triggerState) {
			config = this.config;
			if (config == null) {
				this.triggerState.set(TriggerState.WAITING_FOR_CONFIG);
				this.log.warn("ResendHistoricDataWorker configuration is not set!");
				return;
			}

			timedata = this.timedata;
			if (timedata == null) {
				this.triggerState.set(TriggerState.WAITING_FOR_TIMEDATA);
				this.log.info("Missing timedata reference!");
				return;
			}
		}

		final var latestResendTimestamp = timedata.getLatestValue(config.addressForSuccessfulResend()).get() //
				.map(t -> TypeUtils.<Long>getAsType(OpenemsType.LONG, t)) //
				.orElse(-1L);

		final var now = ZonedDateTime.now(this.componentManager.getClock()) //
				.minus(DELAY_TRIGGER_TIME, ChronoUnit.MILLIS);
		final var timeranges = timedata.getResendTimeranges(config.addressForSuccessfulSend(), latestResendTimestamp) //
				.withBuffer(BUFFER_SECONDS, BUFFER_SECONDS);

		final var channelsToResend = this.getChannelsToResend(config.resendPriority());

		// maximum of 5 minutes range of resend data
		for (var timerange : timeranges.maxDataInTime(MAX_RESEND_TIMESPAN_SECONDS)) {
			final var from = Instant.ofEpochSecond(timerange.getMinTimestamp()).atZone(now.getZone());
			final var to = Instant.ofEpochSecond(timerange.getMaxTimestamp()).atZone(now.getZone());

			timedata = this.timedata;
			if (timedata == null) {
				synchronized (this.triggerState) {
					timedata = this.timedata;
					if (timedata == null) {
						this.triggerState.set(TriggerState.WAITING_FOR_TIMEDATA);
						this.log.info("Missing timedata reference!");
						return;
					}
				}
			}

			final var data = timedata.queryResendData(from, to, channelsToResend);

			final var successful = config.onSendData().apply(new ResendDataNotification(mapResendData(data)));

			if (successful) {
				config.onLastSuccessfulResendUpdated().accept(timerange.getMaxTimestamp());
			} else {
				// if data can not be send wait for next trigger
				this.log.warn("Unable to resend data!");
				return;
			}
		}

	}

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.OPTIONAL, //
			bind = "bindTimedata", unbind = "unbindTimedata" //
	)
	protected void bindTimedata(Timedata timedata) {
		synchronized (this.triggerState) {
			this.timedata = timedata;
			if (this.triggerState.get() == TriggerState.WAITING_FOR_TIMEDATA) {
				this.triggerNextRun();
			}
		}
	}

	protected void unbindTimedata(Timedata timedata) {
		this.timedata = null;
	}

	private Set<ChannelAddress> getChannelsToResend(PersistencePriority resendPriority) {
		return this.componentManager.getEnabledComponents().stream() //
				.flatMap(component -> component.channels().stream()) //
				.filter(channel -> //
				channel.channelDoc().getAccessMode() != AccessMode.WRITE_ONLY //
						&& channel.channelDoc().getPersistencePriority() //
								.isAtLeast(resendPriority))
				.map(t -> t.address()) //
				.collect(toSet());
	}

	@Override
	protected int getCycleTime() {
		if (this.triggerState.compareAndSet(TriggerState.SKIP_FIRST_FOREVER, TriggerState.INIT)) {
			return DELAY_TRIGGER_TIME + new Random().nextInt(MAX_RANDOM_DELAY);
		}
		return AbstractWorker.ALWAYS_WAIT_FOR_TRIGGER_NEXT_RUN;
	}

	@Override
	public void triggerNextRun() {
		this.triggerState.set(TriggerState.AFTER_TRIGGER);
		super.triggerNextRun();
	}

	public void setConfig(Config config) {
		if (Objects.equals(config, this.config)) {
			return;
		}
		synchronized (this.triggerState) {
			this.config = config;
			if (this.config == null) {
				return;
			}
			if (this.triggerState.get() == TriggerState.WAITING_FOR_CONFIG) {
				this.triggerNextRun();
			}
		}
	}

	protected static TreeBasedTable<Long, String, JsonElement> mapResendData(//
			final SortedMap<Long, SortedMap<ChannelAddress, JsonElement>> resendData //
	) {
		return resendData.entrySet().stream() //
				.collect(Collectors.toMap(Entry::getKey,
						entry -> entry.getValue().entrySet().stream()
								.collect(Collectors.<Entry<ChannelAddress, JsonElement>, String, JsonElement>toMap(
										t -> t.getKey().toString(), Entry::getValue)))) //
				.entrySet().stream().collect(toTreeBasedTable());
	}

}
