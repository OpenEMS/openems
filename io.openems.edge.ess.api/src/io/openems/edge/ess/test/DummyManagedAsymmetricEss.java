package io.openems.edge.ess.test;

import java.util.function.Consumer;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

/**
 * Provides a simple, simulated ManagedAsymmetricEss component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummyManagedAsymmetricEss extends DummyManagedSymmetricEss
		implements ManagedAsymmetricEss, ManagedSymmetricEss, AsymmetricEss, SymmetricEss, OpenemsComponent {

	private Consumer<AsymmetricApplyPowerRecord> asymmetricApplyPowerCallback = null;

	public DummyManagedAsymmetricEss(String id, Power power) {
		super(id, power, //
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				ManagedAsymmetricEss.ChannelId.values() //
		);
	}

	public DummyManagedAsymmetricEss(String id) {
		this(id, new DummyPower(MAX_APPARENT_POWER));
	}

	@Override
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
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
	public void withAsymmetricApplyPowerCallback(Consumer<AsymmetricApplyPowerRecord> callback) {
		this.asymmetricApplyPowerCallback = callback;
	}

	public static class AsymmetricApplyPowerRecord {
		public final int activePowerL1;
		public final int reactivePowerL1;
		public final int activePowerL2;
		public final int reactivePowerL2;
		public final int activePowerL3;
		public final int reactivePowerL3;

		public AsymmetricApplyPowerRecord(int activePowerL1, int reactivePowerL1, int activePowerL2,
				int reactivePowerL2, int activePowerL3, int reactivePowerL3) {
			this.activePowerL1 = activePowerL1;
			this.reactivePowerL1 = reactivePowerL1;
			this.activePowerL2 = activePowerL2;
			this.reactivePowerL2 = reactivePowerL2;
			this.activePowerL3 = activePowerL3;
			this.reactivePowerL3 = reactivePowerL3;
		}
	}
}
