package io.openems.edge.bridge.eebus.usecase;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.openmuc.jeebus.spine.spi.UseCase;

import io.openems.edge.bridge.eebus.api.BridgeEebus;
import io.openems.edge.bridge.eebus.api.EebusUseCaseManager;
import io.openems.edge.bridge.eebus.usecase.powerlimitation.LimitPowerConsumptionUseCase;
import io.openems.edge.bridge.eebus.usecase.powerlimitation.LimitPowerProductionUseCase;
import io.openems.edge.bridge.eebus.usecase.powerlimitation.api.ILimitPowerConsumptionHandler;
import io.openems.edge.bridge.eebus.usecase.powerlimitation.api.ILimitPowerProductionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EebusUseCaseManagerImpl implements EebusUseCaseManager {
	private final Logger log = LoggerFactory.getLogger(EebusUseCaseManagerImpl.class);

	private final BridgeEebus bridge;
	protected Map<EebusUseCaseType, EebusUseCase> useCases = new EnumMap<>(EebusUseCaseType.class);
	protected boolean dirty = true;

	public EebusUseCaseManagerImpl(BridgeEebus bridge) {
		this.bridge = bridge;
	}

	public void addLimitPowerProductionHandler(ILimitPowerProductionHandler handler) {
		this.<LimitPowerProductionUseCase>addOrUpdate(EebusUseCaseType.LIMIT_POWER_PRODUCTION,
				x -> x.addHandler(handler));
	}

	public void removeLimitPowerProductionHandler(ILimitPowerProductionHandler handler) {
		this.<LimitPowerProductionUseCase>removeOrUpdate(EebusUseCaseType.LIMIT_POWER_PRODUCTION,
				x -> x.removeHandler(handler));
	}

	public void addLimitPowerConsumptionHandler(ILimitPowerConsumptionHandler handler) {
		this.<LimitPowerConsumptionUseCase>addOrUpdate(EebusUseCaseType.LIMIT_POWER_CONSUMPTION,
				x -> x.addHandler(handler));
	}

	public void removeLimitPowerConsumptionHandler(ILimitPowerConsumptionHandler handler) {
		this.<LimitPowerConsumptionUseCase>removeOrUpdate(EebusUseCaseType.LIMIT_POWER_CONSUMPTION,
				x -> x.removeHandler(handler));
	}

	public <T extends EebusUseCase> void injectCommand(EebusUseCaseType useCaseType, Consumer<T> command) {
		var useCase = this.<T>getUseCaseOrNull(useCaseType);
		if (useCase != null) {
			command.accept(useCase);
		}
	}

	protected <T extends EebusUseCase> void addOrUpdate(EebusUseCaseType useCaseType, Consumer<T> updateMethod) {
		var useCase = this.<T>getUseCaseOrNull(useCaseType);
		if (useCase == null) {
			useCase = (T) useCaseType.createInstance(this.bridge);
			updateMethod.accept(useCase);
			this.useCases.put(useCaseType, useCase);
			this.dirty = true;
			this.logDebug("Added use-case " + useCaseType);
		} else {
			updateMethod.accept(useCase);
			this.logDebug("Updated use-case " + useCaseType);
		}
	}

	protected <T extends EebusUseCase> void removeOrUpdate(EebusUseCaseType useCaseType, Consumer<T> updateMethod) {
		var useCase = this.<T>getUseCaseOrNull(useCaseType);
		if (useCase == null) {
			return;
		}

		updateMethod.accept(useCase);
		if (!useCase.isInUse()) {
			this.useCases.remove(useCaseType);
			this.dirty = true;
		}
	}

	private <T extends EebusUseCase> T getUseCaseOrNull(EebusUseCaseType useCaseType) {
		return (T) this.useCases.get(useCaseType);
	}

	public UseCase[] createUseCases() {
		this.dirty = false;
		return this.useCases.values().stream().map(EebusUseCase::createUseCase).toArray(UseCase[]::new);
	}

	public String debugLog() {
		var shortNames = this.useCases.keySet().stream().map(EebusUseCaseType::getShortName).collect(Collectors.joining(","));
		if (shortNames.isEmpty()) {
			return "";
		}

		return "UseCases:" + shortNames;
	}

	public boolean isReInitRequired() {
		if (this.dirty) {
			this.logDebug("A re-initialization is required because a use-case was added or removed");
			return true;
		}

		for (var useCase : this.useCases.values()) {
			if (useCase.requiresReInit()) {
				this.logDebug("A re-initialization is required because use-case " + useCase + " requires a re-init.");
				return true;
			}
		}

		return false;
	}

	private void logDebug(String msg) {
		if (this.bridge.getLogLevel().isDebug()) {
			this.log.info(msg);
		}
	}
}
