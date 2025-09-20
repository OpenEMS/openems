package io.openems.edge.ess.test;

import java.util.function.Consumer;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

/**
 * Provides a simple, simulated {@link ManagedAsymmetricEss} component that can
 * be used together with the OpenEMS Component test framework.
 */
public class DummyManagedAsymmetricEss extends AbstractDummyManagedSymmetricEss<DummyManagedAsymmetricEss> implements
		ManagedAsymmetricEss, ManagedSymmetricEss, AsymmetricEss, SymmetricEss, StartStoppable, OpenemsComponent {

	private Consumer<AsymmetricApplyPowerRecord> asymmetricApplyPowerCallback = null;

	public DummyManagedAsymmetricEss(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				ManagedAsymmetricEss.ChannelId.values(), //
				StartStoppable.ChannelId.values());
	}

	@Override
	protected final DummyManagedAsymmetricEss self() {
		return this;
	}

	@Override
	public final void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) throws OpenemsNamedException {
		if (this.asymmetricApplyPowerCallback != null) {
			this.asymmetricApplyPowerCallback.accept(new AsymmetricApplyPowerRecord(activePowerL1, reactivePowerL1,
					activePowerL2, reactivePowerL2, activePowerL3, reactivePowerL3));
		}
	}

	/**
	 * Sets a callback for {@link #applyPower(int, int, int, int, int, int)}.
	 * 
	 * @param callback the callback
	 */
	public final void withAsymmetricApplyPowerCallback(Consumer<AsymmetricApplyPowerRecord> callback) {
		this.asymmetricApplyPowerCallback = callback;
	}

	public record AsymmetricApplyPowerRecord(int activePowerL1, int reactivePowerL1, int activePowerL2,
			int reactivePowerL2, int activePowerL3, int reactivePowerL3) {
	}

}
