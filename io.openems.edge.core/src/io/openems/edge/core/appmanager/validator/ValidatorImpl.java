package io.openems.edge.core.appmanager.validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import io.openems.common.OpenemsConstants;
import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.validator.ValidatorConfig.CheckableConfig;

@Component
public class ValidatorImpl implements Validator {

	private static final Logger LOG = LoggerFactory.getLogger(ValidatorImpl.class);

	@Activate
	public ValidatorImpl() {
	}

	@Override
	public List<String> getErrorMessages(List<CheckableConfig> checkableConfigs, Language language,
			boolean returnImmediate) {
		if (checkableConfigs.isEmpty()) {
			return new ArrayList<>();
		}
		var errorMessages = new ArrayList<String>(checkableConfigs.size());
		var bundleContext = FrameworkUtil.getBundle(Checkable.class).getBundleContext();
		// build filter
		var filterBuilder = new StringBuilder();
		if (checkableConfigs.size() > 1) {
			filterBuilder.append("(|");
		}
		checkableConfigs.forEach(t -> filterBuilder.append("(component.name=" + t.checkableComponentName + ")"));
		if (checkableConfigs.size() > 1) {
			filterBuilder.append(")");
		}
		try {
			// get all service references
			Collection<ServiceReference<Checkable>> serviceReferences = bundleContext
					.getServiceReferences(Checkable.class, filterBuilder.toString());
			var noneExistingCheckables = Lists.<CheckableConfig>newArrayList();
			checkableConfigs.forEach(c -> noneExistingCheckables.add(c));
			var isReturnedImmediate = false;
			var usedReferencens = new ArrayList<ServiceReference<Checkable>>(serviceReferences.size());
			for (var reference : serviceReferences) {
				var componentName = (String) reference.getProperty(OpenemsConstants.PROPERTY_OSGI_COMPONENT_NAME);
				var checkableConfig = checkableConfigs.stream()
						.filter(c -> c.checkableComponentName.equals(componentName)).findFirst().orElse(null);
				var checkable = bundleContext.getService(reference);
				usedReferencens.add(reference);
				if (checkableConfig.properties != null) {
					checkable.setProperties(checkableConfig.properties);
				}
				noneExistingCheckables.removeIf(c -> c.equals(checkableConfig));
				var result = checkable.check();
				if (result == checkableConfig.invertResult) {
					var errorMessage = checkable.getErrorMessage(language);
					if (checkableConfig.invertResult) {
						errorMessage = "Invert[" + errorMessage + "]";
					}
					errorMessages.add(errorMessage);
					if (returnImmediate) {
						isReturnedImmediate = true;
						break;
					}
				}
			}

			if (!noneExistingCheckables.isEmpty() && !isReturnedImmediate) {
				LOG.warn("Checkables[" + noneExistingCheckables.stream().map(c -> c.checkableComponentName)
						.collect(Collectors.joining(";")) + "] are not found!");
			}

			// free all service references
			for (var reference : usedReferencens) {
				bundleContext.ungetService(reference);
			}
		} catch (InvalidSyntaxException | IllegalStateException e) {
			// Can not get service references
			e.printStackTrace();
		}
		return errorMessages;
	}

}
