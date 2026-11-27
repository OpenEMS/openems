package io.openems.edge.core.appmanager.validator;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.validator.ValidatorConfig.CheckableConfig;

@Component
public class ValidatorImpl implements Validator {

	private final Logger log = LoggerFactory.getLogger(ValidatorImpl.class);

	private final CheckableFactory checkableFactory;

	@Activate
	public ValidatorImpl(@Reference CheckableFactory checkableFactory) {
		this.checkableFactory = checkableFactory;
	}

	@Override
	public List<OpenemsNamedException> getNamedExceptions(//
			final List<CheckableConfig> checkableConfigs, //
			final Language language, //
			final boolean returnImmediate //
	) {
		if (checkableConfigs.isEmpty()) {
			return emptyList();
		}
		final var checkableErrors = new ArrayList<OpenemsNamedException>(checkableConfigs.size());

		for (var config : checkableConfigs) {
			try (final var checkable = this.checkableFactory.useCheckable(config.checkableComponentName())) {
				if (checkable == null) {
					continue;
				}

				// validate checkable
				checkable.setProperties(config.properties());
				var result = checkable.check();
				if (result == config.invertResult()) {
					var checkableError = new ArrayList<OpenemsNamedException>();
					try {
						var error = config.invertResult() ? checkable.getInvertedValidationError(language)
								: checkable.getValidationError(language);
						checkableError.add(error);
					} catch (UnsupportedOperationException e) {
						this.log.error(
								"Missing implementation for getting " + (config.invertResult() ? "inverted " : "")
										+ "error message for check \"" + config.checkableComponentName() + "\"!",
								e);
						checkableError.add(OpenemsError.GENERIC
								.exception("Check \"" + config.checkableComponentName() + "\" failed."));
					}
					checkableErrors.addAll(checkableError);
					if (returnImmediate) {
						return checkableErrors;
					}
				}
			} catch (Exception e) {
				this.log.error("Error while using checkable " + config.checkableComponentName() + "!", e);
			}
		}
		return checkableErrors;
	}

}
