package io.openems.edge.core.appmanager;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.osgi.service.component.annotations.Component;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.validator.Checkable;
import io.openems.edge.core.appmanager.validator.ValidatorConfig.CheckableConfig;

public final class DummyValidator {

	/**
	 * Creates a {@link CheckableConfig} for a test check.
	 * 
	 * @param check                the check implementation
	 * @param errorMessage         the error message method
	 * @param invertedErrorMessage the inverted error message method
	 * @return the {@link CheckableConfig}
	 */
	public static CheckableConfig testCheckable(//
			final Supplier<Boolean> check, //
			final Function<Language, String> errorMessage, //
			final Function<Language, String> invertedErrorMessage//
	) {
		final var props = new HashMap<String, Object>();
		props.put("check", check);
		if (errorMessage != null) {
			props.put("errorMessage", errorMessage);
		}
		if (invertedErrorMessage != null) {
			props.put("invertedErrorMessage", invertedErrorMessage);
		}

		return new CheckableConfig(TestCheckable.COMPONENT_NAME, false, props);
	}

	/**
	 * Creates a {@link CheckableConfig} for a test check.
	 * 
	 * @param check                the check implementation
	 * @param errorMessage         the error message
	 * @param invertedErrorMessage the inverted error message
	 * @return the {@link CheckableConfig}
	 */
	public static CheckableConfig testCheckable(//
			final Supplier<Boolean> check, //
			final String errorMessage, //
			final String invertedErrorMessage//
	) {
		return testCheckable(check, //
				errorMessage == null ? null : l -> errorMessage, //
				invertedErrorMessage == null ? null : l -> invertedErrorMessage //
		);
	}

	/**
	 * Creates a {@link CheckableConfig} for a test check.
	 * 
	 * @param check the check implementation
	 * @return the {@link CheckableConfig}
	 */
	public static CheckableConfig testCheckable(//
			final Supplier<Boolean> check //
	) {
		return testCheckable(check, (String) null, (String) null);
	}

	@Component(name = TestCheckable.COMPONENT_NAME)
	public static class TestCheckable implements Checkable {

		public static final String COMPONENT_NAME = "Test.Validator.Checkable.TestCheckable";

		private Supplier<Boolean> check;
		private Function<Language, String> errorMessage;
		private Function<Language, String> invertedErrorMessage;

		@Override
		public String getComponentName() {
			return COMPONENT_NAME;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void setProperties(Map<String, ?> properties) {
			this.check = (Supplier<Boolean>) properties.get("check");
			this.errorMessage = (Function<Language, String>) properties.get("errorMessage");
			this.invertedErrorMessage = (Function<Language, String>) properties.get("invertedErrorMessage");
		}

		@Override
		public boolean check() {
			if (this.check == null) {
				return false;
			}
			return this.check.get();
		}

		@Override
		public String getErrorMessage(Language language) {
			if (this.errorMessage == null) {
				return "No error message provided!";
			}
			return this.errorMessage.apply(language);
		}

		@Override
		public String getInvertedErrorMessage(Language language) {
			if (this.invertedErrorMessage == null) {
				return "No inverted error message provided!";
			}
			return this.invertedErrorMessage.apply(language);
		}

	}

	private DummyValidator() {
	}

}
