package io.openems.edge.core.appmanager.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.component.ComponentContext;

import io.openems.common.session.Language;
import io.openems.edge.energy.api.EnergyScheduler;
import io.openems.edge.energy.api.Version;

@ExtendWith(MockitoExtension.class)
class CheckEnergySchedulerV2Test {

	@Mock
	private ComponentContext componentContext;

	@Mock
	private EnergyScheduler energyScheduler;

	@Test
	void testCheck_shouldPass_whenVersionV2() {
		when(this.energyScheduler.getImplementationVersion()).thenReturn(Version.V2_ENERGY_SCHEDULABLE);
		final var sut = new CheckEnergySchedulerV2(this.componentContext, this.energyScheduler);

		assertTrue(sut.check());
	}

	@Test
	void testCheck_shouldFail_whenVersionV1() {
		when(this.energyScheduler.getImplementationVersion()).thenReturn(Version.V1_ESS_ONLY);
		final var sut = new CheckEnergySchedulerV2(this.componentContext, this.energyScheduler);

		assertFalse(sut.check());
	}

	@Test
	void testGetErrorMessage_shouldBeAvailable() {
		final var sut = new CheckEnergySchedulerV2(this.componentContext, this.energyScheduler);

		final var errorMessage = sut.getErrorMessage(Language.EN);

		assertNotNull(errorMessage);
		assertFalse(errorMessage.isBlank());
	}

	@Test
	void testGetInvertedErrorMessage_shouldBeAvailable() {
		final var sut = new CheckEnergySchedulerV2(this.componentContext, this.energyScheduler);

		final var errorMessage = sut.getInvertedErrorMessage(Language.EN);

		assertNotNull(errorMessage);
		assertFalse(errorMessage.isBlank());
	}
}