package io.openems.edge.bridge.eebus.usecase.powerlimitation;

import static io.openems.edge.common.channel.ChannelUtils.setValue;

import java.util.ArrayList;
import java.util.List;

import org.openmuc.jeebus.spine.spi.UseCase;
import org.openmuc.jeebus.spine.utils.datatypes.ScaledNumberWrapper;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.ActiveLimit;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.LimitationConfig;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.SimpleLimitationConfig;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.lpc.LpcCs;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.states.Event;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.states.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.bridge.eebus.api.BridgeEebus;
import io.openems.edge.bridge.eebus.usecase.EebusUseCase;
import io.openems.edge.bridge.eebus.usecase.powerlimitation.api.ILimitPowerConsumptionHandler;
import io.openems.edge.bridge.eebus.usecase.powerlimitation.api.LimitPowerState;

public class LimitPowerConsumptionUseCase extends EebusUseCase {
	private final BridgeEebus bridge;
	private final List<ILimitPowerConsumptionHandler> handlers = new ArrayList<ILimitPowerConsumptionHandler>();
	private final Logger log = LoggerFactory.getLogger(LimitPowerConsumptionUseCase.class);

	private LimitationConfig configSnapshot;

	public LimitPowerConsumptionUseCase(BridgeEebus bridge) {
		this.bridge = bridge;
	}

	public long getNominalMax() {
		return this.handlers.stream() //
				.mapToLong(ILimitPowerConsumptionHandler::getNominalMaxConsumption) //
				.max() //
				.getAsLong();
	}

	private LimitationConfig createConfig() {
		return new SimpleLimitationConfig(//
				LimitationConfig.DEFAULT_FAILSAFE_DURATION_MIN, //
				LimitationConfig.DEFAULT_BIG_POWER, //
				LimitationConfig.DEFAULT_BIG_POWER.negate(), //
				new ScaledNumberWrapper(this.getNominalMax(), 0));
	}

	private LimitPowerState mapState(State state) {
		return LimitPowerState.valueOf(state.name());
	}

	public void handleLimit(LimitPowerState state, Double activeLimit) {
		setValue(this.bridge, BridgeEebus.ChannelId.LPC_CURRENT_LIMIT, activeLimit);

		for (var eventHandler : this.handlers) {
			try {
				eventHandler.handleLimitPowerConsumption(state, activeLimit);
			} catch (Exception ex) {
				this.log.error("An exception occurred while calling handleLimitPowerConsumption() for listener "
						+ eventHandler.getClass().getName(), ex);
			}
		}
	}

	protected void handleEebusEvent(Event event, State state, ActiveLimit activeLimit) {
		this.log.info("Received eebus LPC signal | State: {}, ActiveLimit: {}", state, activeLimit);

		var mappedState = this.mapState(state);
		this.handleLimit(mappedState, activeLimit != null ? activeLimit.getResultingValue() : null);
	}

	public void addHandler(ILimitPowerConsumptionHandler handler) {
		this.handlers.add(handler);
	}

	public void removeHandler(ILimitPowerConsumptionHandler handler) {
		this.handlers.remove(handler);
	}

	@Override
	public boolean isInUse() {
		return !this.handlers.isEmpty();
	}

	@Override
	public UseCase createUseCase() {
		this.configSnapshot = this.createConfig();

		var useCase = new LpcCs(this.configSnapshot);
		useCase.addListener(this::handleEebusEvent);
		return useCase;
	}

	@Override
	public boolean requiresReInit() {
		return this.configSnapshot == null
				|| !this.createConfig().getNominalMax().isEqualTo(this.configSnapshot.getNominalMax());
	}
}
