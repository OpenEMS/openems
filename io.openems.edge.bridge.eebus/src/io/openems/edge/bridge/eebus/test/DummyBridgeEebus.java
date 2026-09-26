package io.openems.edge.bridge.eebus.test;

import com.google.common.collect.ImmutableList;
import io.openems.edge.bridge.eebus.Config;
import io.openems.edge.bridge.eebus.api.BridgeEebus;
import io.openems.edge.bridge.eebus.api.EebusPeer;
import io.openems.edge.bridge.eebus.api.EebusUseCaseManager;
import io.openems.edge.bridge.eebus.usecase.EebusUseCaseManagerImpl;
import io.openems.edge.bridge.eebus.usecase.EebusUseCaseType;
import io.openems.edge.bridge.eebus.usecase.powerlimitation.LimitPowerConsumptionUseCase;
import io.openems.edge.bridge.eebus.usecase.powerlimitation.api.LimitPowerState;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;

public class DummyBridgeEebus extends AbstractDummyOpenemsComponent<DummyBridgeEebus> implements BridgeEebus {
	private final EebusUseCaseManagerImpl useCaseManager;

	private final EebusPeer dummyPeer;

	public DummyBridgeEebus(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				BridgeEebus.ChannelId.values() //
		);
		this.useCaseManager = new EebusUseCaseManagerImpl(this);
		this.dummyPeer = new DummyEebusPeer("eebuspeer0");

		super.activate(null, id, "", true);
	}

	@Override
	public ImmutableList<EebusPeer> getPeers() {
		return ImmutableList.<EebusPeer>builder().add(this.dummyPeer).build();
	}

	@Override
	public String[] getTrustedSkis() {
		return new String[] { this.dummyPeer.getSki() };
	}

	@Override
	public EebusUseCaseManager getUseCaseManager() {
		return this.useCaseManager;
	}

	@Override
	protected DummyBridgeEebus self() {
		return this;
	}

	public void sendPowerConsumptionLimit(LimitPowerState state, DummyLimitation limit) {
		this.useCaseManager.injectCommand(EebusUseCaseType.LIMIT_POWER_CONSUMPTION,
				(LimitPowerConsumptionUseCase useCase) -> {
					switch (limit) {
					case DummyLimitation.DummyNoLimitation() -> useCase.handleLimit(state, null);
					case DummyLimitation.DummyAbsoluteLimitation(var absoluteLimit) ->
						useCase.handleLimit(state, (double) absoluteLimit);
					case DummyLimitation.DummyPercentageLimitation(var percentage) ->
						useCase.handleLimit(state, useCase.getNominalMax() * percentage);
					}
				});
	}

}
