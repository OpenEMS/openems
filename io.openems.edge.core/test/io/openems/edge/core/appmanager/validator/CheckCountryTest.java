package io.openems.edge.core.appmanager.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.component.ComponentContext;

import io.openems.common.session.Language;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.meta.types.CountryCode;

@ExtendWith(MockitoExtension.class)
class CheckCountryTest {

	@Mock
	private ComponentContext componentContext;

	@Mock
	private Meta meta;

	@Test
	void testCheck_shouldPass_whenUndefinedCountry() {
		when(this.meta.getCountryCode()).thenReturn(CountryCode.UNDEFINED);
		final var sut = new CheckCountry(this.componentContext, this.meta);
		sut.setProperties(Map.of("allowedCountries", Set.of(CountryCode.DE)));

		assertTrue(sut.check());
	}

	@Test
	void testCheck_shouldPass_whenAllowedCountry() {
		when(this.meta.getCountryCode()).thenReturn(CountryCode.DE);
		final var sut = new CheckCountry(this.componentContext, this.meta);
		sut.setProperties(Map.of("allowedCountries", Set.of(CountryCode.DE, CountryCode.AT)));

		assertTrue(sut.check());
	}

	@Test
	void testCheck_shouldFail_whenNotAllowedCountry() {
		when(this.meta.getCountryCode()).thenReturn(CountryCode.AT);
		final var sut = new CheckCountry(this.componentContext, this.meta);
		sut.setProperties(Map.of("allowedCountries", Set.of(CountryCode.DE)));

		assertFalse(sut.check());
	}

	@Test
	void testGetErrorMessage_shouldBeAvailable() {
		when(this.meta.getCountryCode()).thenReturn(CountryCode.AT);
		final var sut = new CheckCountry(this.componentContext, this.meta);

		final var errorMessage = sut.getErrorMessage(Language.EN);

		assertNotNull(errorMessage);
		assertFalse(errorMessage.isBlank());
	}

	@Test
	void testGetInvertedErrorMessage_shouldBeAvailable() {
		when(this.meta.getCountryCode()).thenReturn(CountryCode.AT);
		final var sut = new CheckCountry(this.componentContext, this.meta);

		final var errorMessage = sut.getInvertedErrorMessage(Language.EN);

		assertNotNull(errorMessage);
		assertFalse(errorMessage.isBlank());
	}
}