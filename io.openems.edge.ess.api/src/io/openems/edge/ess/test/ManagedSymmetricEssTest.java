package io.openems.edge.ess.test;

import java.util.Optional;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;

/**
 * Provides a generic test framework for OpenEMS {@link ManagedSymmetricEss}s.
 */
public class ManagedSymmetricEssTest extends AbstractComponentTest<ManagedSymmetricEssTest, ManagedSymmetricEss> {

	public ManagedSymmetricEssTest(ManagedSymmetricEss sut) throws OpenemsException {
		super(sut);
	}

	@Override
	protected void onBeforeWrite() throws OpenemsNamedException {
		ManagedSymmetricEss ess = this.getSut();
		Optional<Integer> activePower = ess.getSetActivePowerEqualsChannel().getNextWriteValueAndReset();
		Optional<Integer> reactivePower = ess.getSetReactivePowerEqualsChannel().getNextWriteValueAndReset();

		this.getSut().applyPower(activePower.orElse(0), reactivePower.orElse(0));
	}

	@Override
	protected ManagedSymmetricEssTest self() {
		return this;
	}

}