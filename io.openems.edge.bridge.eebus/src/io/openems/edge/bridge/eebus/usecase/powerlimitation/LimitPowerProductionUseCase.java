package io.openems.edge.bridge.eebus.usecase.powerlimitation;

import static io.openems.edge.common.channel.ChannelUtils.setValue;

import java.util.ArrayList;
import java.util.List;

import org.openmuc.jeebus.spine.spi.UseCase;
import org.openmuc.jeebus.spine.utils.datatypes.ScaledNumberWrapper;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.ActiveLimit;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.LimitationConfig;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.SimpleLimitationConfig;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.lpp.LppCs;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.states.Event;
import org.openmuc.jeebus.usecase.powerlimitation.controllablesystem.states.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.bridge.eebus.api.BridgeEebus;
import io.openems.edge.bridge.eebus.usecase.EebusUseCase;
import io.openems.edge.bridge.eebus.usecase.powerlimitation.api.ILimitPowerProductionHandler;
import io.openems.edge.bridge.eebus.usecase.powerlimitation.api.LimitPowerState;

public class LimitPowerProductionUseCase extends EebusUseCase {
	private final BridgeEebus bridge;
	private final List<ILimitPowerProductionHandler> handlers = new ArrayList<>();
	private final Logger log = LoggerFactory.getLogger(LimitPowerProductionUseCase.class);

	private LimitationConfig configSnapshot;

	public LimitPowerProductionUseCase(BridgeEebus bridge) {
		this.bridge = bridge;
	}

	private long getNominalMax() {
		return this.handlers.stream() //
				.mapToLong(ILimitPowerProductionHandler::getNominalMaxProduction) //
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
		setValue(this.bridge, BridgeEebus.ChannelId.LPP_CURRENT_LIMIT, activeLimit);

		for (var eventHandler : this.handlers) {
			try {
				eventHandler.handleLimitPowerProduction(state, activeLimit);
			} catch (Exception ex) {
				this.log.error("An exception occurred while calling handleLimitPowerProduction() for listener "
						+ eventHandler.getClass().getName(), ex);
			}
		}
	}

	protected void handleEebusEvent(Event event, State state, ActiveLimit activeLimit) {
		this.log.info("Received eebus LPP signal | State: {}, ActiveLimit: {}", state, activeLimit);

		var mappedState = this.mapState(state);
		this.handleLimit(mappedState, activeLimit.getResultingValue());
	}

	public void addHandler(ILimitPowerProductionHandler handler) {
		this.handlers.add(handler);
	}

	public void removeHandler(ILimitPowerProductionHandler handler) {
		this.handlers.remove(handler);
	}

	@Override
	public boolean isInUse() {
		return !this.handlers.isEmpty();
	}

	@Override
	public UseCase createUseCase() {
		this.configSnapshot = this.createConfig();

		var useCase = new LppCs(this.configSnapshot);
		useCase.addListener(this::handleEebusEvent);
		return useCase;
	}

	@Override
	public boolean requiresReInit() {
		return this.configSnapshot == null
				|| !this.createConfig().getNominalMax().isEqualTo(this.configSnapshot.getNominalMax());
	}
}
