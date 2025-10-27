package io.openems.edge.predictor.api.prediction;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.common.utils.FunctionUtils.doNothing;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.OpenemsComponent;

public abstract class AbstractPredictor extends AbstractOpenemsComponent implements Predictor, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(AbstractPredictor.class);

	private final Map<ChannelAddress, Prediction> predictions = new HashMap<>();

	private ChannelAddress[] channelAdresses = new ChannelAddress[0];
	private LogVerbosity logVerbosity = LogVerbosity.NONE;

	protected abstract ClockProvider getClockProvider();

	protected abstract Prediction createNewPrediction(ChannelAddress channelAddress);

	protected AbstractPredictor(//
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	@Override
	protected final void activate(ComponentContext context, String id, String alias, boolean enabled) {
		throw new IllegalArgumentException("use the other activate method!");
	}

	/**
	 * Activate with a String-Array of ChannelAddresses.
	 * 
	 * @param context          the {@link ComponentContext}
	 * @param id               the Component-ID
	 * @param alias            the Component-Alias
	 * @param enabled          is the Component enabled?
	 * @param logVerbosity     the {@link LogVerbosity}
	 * @param channelAddresses the ChannelAddresses as String-Array
	 * @throws OpenemsNamedException on error
	 */
	protected void activate(ComponentContext context, String id, String alias, boolean enabled,
			LogVerbosity logVerbosity, String... channelAddresses) throws OpenemsNamedException {
		this.activate(context, id, alias, enabled, logVerbosity, toChannelAddresses(channelAddresses));
	}

	/**
	 * Activate with a {@link ChannelAddress}-Array.
	 * 
	 * @param context          the {@link ComponentContext}
	 * @param id               the Component-ID
	 * @param alias            the Component-Alias
	 * @param enabled          is the Component enabled?
	 * @param logVerbosity     the {@link LogVerbosity}
	 * @param channelAddresses the {@link ChannelAddress}es
	 */
	protected void activate(ComponentContext context, String id, String alias, boolean enabled,
			LogVerbosity logVerbosity, ChannelAddress... channelAddresses) {
		super.activate(context, id, alias, enabled);
		this.logVerbosity = logVerbosity;
		this.channelAdresses = channelAddresses;
	}

	@Override
	public ChannelAddress[] getChannelAddresses() {
		return this.channelAdresses;
	}

	@Override
	public Prediction getPrediction(ChannelAddress channelAddress) {
		var now = roundDownToQuarter(ZonedDateTime.now(this.getClockProvider().getClock()));
		var prediction = this.predictions.get(channelAddress);
		if (Optional.ofNullable(prediction) // handle first-request or unsupported channelAddress
				.map(p -> p.getFirstTime()) // handle prediction is EMPTY_PREDICTION
				.map(t -> now.isAfter(t)) // handle prediction is outdated
				.orElse(true /* any null? */)) {
			// Create new prediction
			prediction = this.createNewPrediction(channelAddress);
			this.predictions.put(channelAddress, prediction);
		} else {
			// Reuse existing prediction
		}
		switch (this.logVerbosity) {
		case NONE -> doNothing();
		case REQUESTED_PREDICTIONS, ARCHIVE_LOCALLY ->
			this.logInfo(this.log, "Prediction for [" + channelAddress + "]: " + prediction);
		}
		return prediction;
	}

	protected LogVerbosity getLogVerbosity() {
		return this.logVerbosity;
	}

	private static ChannelAddress[] toChannelAddresses(String[] strings) throws OpenemsNamedException {
		final var result = new ChannelAddress[strings.length];
		for (var i = 0; i < strings.length; i++) {
			result[i] = ChannelAddress.fromString(strings[i]);
		}
		return result;
	}
}
