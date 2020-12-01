package io.openems.edge.ess.test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;

/**
 * Provides a generic test framework for OpenEMS {@link ManagedSymmetricEss}s.
 */
public class ManagedSymmetricEssTest extends AbstractComponentTest<ManagedSymmetricEssTest, ManagedSymmetricEss> {

	public ManagedSymmetricEssTest(ManagedSymmetricEss sut) {
		super(sut);
	}

	@Override
	protected void onBeforeWrite() throws OpenemsNamedException {
		ManagedSymmetricEss ess = this.getSut();
		int activePower = ess.getSetActivePowerEqualsChannel().getNextWriteValueAndReset().orElse(0);
		int reactivePower = ess.getSetReactivePowerEqualsChannel().getNextWriteValueAndReset().orElse(0);

		int allowedChargePower = ess.getAllowedChargePower().orElse(0);
		if (activePower < allowedChargePower) {
			activePower = allowedChargePower;
		}

		int allowedDischargePower = ess.getAllowedDischargePower().orElse(0);
		if (activePower > allowedDischargePower) {
			activePower = allowedDischargePower;
		}

		this.getSut().applyPower(activePower, reactivePower);
	}

	@Override
	protected ManagedSymmetricEssTest self() {
		return this;
	}

}