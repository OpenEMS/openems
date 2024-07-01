package io.openems.edge.core.appmanager.validator;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ReferenceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.OpenemsConstants;
import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.validator.ValidatorConfig.CheckableConfig;

@Component
public class ValidatorImpl implements Validator {

	private static final Logger LOG = LoggerFactory.getLogger(ValidatorImpl.class);

	@Reference(//
			cardinality = ReferenceCardinality.MULTIPLE, //
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			// requires prototype for thread safety
			scope = ReferenceScope.PROTOTYPE_REQUIRED //
	)
	private volatile List<ComponentServiceObjects<Checkable>> checkableFactories;

	@Activate
	public ValidatorImpl() {
	}

	@Override
	public List<String> getErrorMessages(List<CheckableConfig> checkableConfigs, Language language,
			boolean returnImmediate) {
		if (checkableConfigs.isEmpty()) {
			return emptyList();
		}
		final var errorMessages = new ArrayList<String>(checkableConfigs.size());

		for (var config : checkableConfigs) {
			// find the componentServiceObjects base on the given configuration name
			final var cso = this.checkableFactories.stream()//
					.filter(csoCheckable -> {
						var sr = csoCheckable.getServiceReference();
						var srName = (String) sr.getProperty(OpenemsConstants.PROPERTY_OSGI_COMPONENT_NAME);
						return srName.equals(config.checkableComponentName());
					}).findAny().orElse(null);

			if (cso == null) {
				LOG.info("Unable to get Checkable '" + config.checkableComponentName() + "'!");
				continue;
			}

			// get the service from the cso
			final var checkable = cso.getService();

			if (checkable == null) {
				LOG.info("Unable to get Checkable '" + config.checkableComponentName() + "'!");
				continue;
			}

			try {
				// validate checkable
				checkable.setProperties(config.properties());
				var result = checkable.check();
				if (result == config.invertResult()) {
					String errorMessage;
					try {
						errorMessage = config.invertResult() ? checkable.getInvertedErrorMessage(language)
								: checkable.getErrorMessage(language);
					} catch (UnsupportedOperationException e) {
						LOG.error("Missing implementation for getting " + (config.invertResult() ? "inverted " : "")
								+ "error message for check \"" + config.checkableComponentName() + "\"!", e);
						errorMessage = "Check \"" + config.checkableComponentName() + "\" failed.";
					}
					errorMessages.add(errorMessage);
					if (returnImmediate) {
						return errorMessages;
					}
				}
			} finally {
				// free checkable from cso
				cso.ungetService(checkable);
			}
		}
		return errorMessages;
	}

}
