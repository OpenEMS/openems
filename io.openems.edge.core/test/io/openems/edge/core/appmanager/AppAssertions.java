package io.openems.edge.core.appmanager;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;

import io.openems.common.session.Language;

/**
 * Assertions for an {@link AbstractOpenemsAppWithProps}.
 *
 * @param <APP>       the app type
 * @param <PROPERTY>  the property type
 * @param <PARAMETER> the parameter type
 */
public final class AppAssertions<//
		APP extends AbstractOpenemsAppWithProps<APP, PROPERTY, PARAMETER>, //
		PROPERTY extends Type<PROPERTY, APP, PARAMETER>, //
		PARAMETER> {

	private final APP app;

	AppAssertions(APP app) {
		this.app = app;
	}

	/**
	 * Creates assertions for the given property.
	 *
	 * @param property the property under test
	 * @return the property assertions
	 */
	public PropertyAssertions<APP, PROPERTY, PARAMETER> assertThatProperty(PROPERTY property) {
		return new PropertyAssertions<>(this.app, property, this.getParameter(property));
	}

	/**
	 * Verifies that the app exposes exactly the given properties, independent of
	 * their order.
	 *
	 * @param expectedProperties the expected properties
	 * @return this
	 */
	@SafeVarargs
	public final AppAssertions<APP, PROPERTY, PARAMETER> hasOnlyProperties(PROPERTY... expectedProperties) {
		final var expectedNames = new LinkedHashSet<String>();
		final var duplicateNames = new LinkedHashSet<String>();
		for (var property : expectedProperties) {
			if (!expectedNames.add(property.name())) {
				duplicateNames.add(property.name());
			}
		}

		assertTrue(duplicateNames.isEmpty(), //
				() -> "Duplicate expected properties for app [" + this.app.getAppId() + "]: " + duplicateNames);

		final var actualNames = new LinkedHashSet<String>();
		for (var property : this.app.getProperties()) {
			actualNames.add(property.name);
		}

		final var missingNames = new LinkedHashSet<>(expectedNames);
		missingNames.removeAll(actualNames);
		final var unexpectedNames = new LinkedHashSet<>(actualNames);
		unexpectedNames.removeAll(expectedNames);

		assertTrue(missingNames.isEmpty() && unexpectedNames.isEmpty(), //
				() -> "Unexpected properties for app [" + this.app.getAppId() + "] - missing: " + missingNames
						+ ", unexpected: " + unexpectedNames);
		return this;
	}

	private PARAMETER getParameter(PROPERTY property) {
		return property.getParamter().apply(new Type.GetParameterValues<>(this.app, Language.DEFAULT));
	}
}
