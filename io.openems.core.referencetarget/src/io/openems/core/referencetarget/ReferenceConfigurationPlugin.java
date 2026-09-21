package io.openems.core.referencetarget;

import static io.openems.common.utils.FunctionUtils.lazySingleton;
import static io.openems.core.referencetarget.PropertyFilter.fromGenerateTargetsFromReferences;
import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationPlugin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.OpenemsConstants;
import io.openems.common.utils.ServiceUtils;

@Component(immediate = true)
public class ReferenceConfigurationPlugin implements ConfigurationPlugin {

	private final Logger log = LoggerFactory.getLogger(ReferenceConfigurationPlugin.class);

	@Reference
	private ServiceComponentRuntime scr;
	@Reference
	private MetaTypeService metaTypeService;

	@Activate
	public ReferenceConfigurationPlugin() {
		this.log.info("ReferenceConfigurationPlugin activated");
	}

	@Override
	public void modifyConfiguration(ServiceReference<?> reference, Dictionary<String, Object> properties) {
		try {
			final var filters = this.getPropertyFilters(reference, properties);

			if (filters.isEmpty()) {
				return;
			}

			final var valueProvider = Map.of(//
					"config",
					new ValueProviderFromConfig(properties, lazySingleton(() -> this.getOcd(reference, properties))) //
			);

			final var identifierProvider = lazySingleton(() -> getIdentifier(reference, properties));

			for (var filter : filters) {
				final var valueMap = this.getValuesByParameter(identifierProvider, filter, valueProvider);
				if (valueMap.size() != filter.targetTemplate().parameter().size()) {
					continue;
				}

				final var target = filter.targetTemplate().withParameters(valueMap);
				properties.put(filter.property() + ".target", target);
				if (this.log.isInfoEnabled()) {
					this.log.info("Set targetTemplate filter for component='{}', property='{}' to '{}'",
							identifierProvider.get(), filter.property(), target);
				}
			}
		} catch (Exception e) {
			this.log.error("Error during updating target", e);
		}
	}

	private List<PropertyFilter> getPropertyFilters(//
			ServiceReference<?> reference, //
			Dictionary<String, Object> properties //
	) {

		final var dto = this.getComponentDescription(reference, properties);
		if (dto == null) {
			return Collections.emptyList();
		}

		return getPropertyFilters(dto);
	}

	private static List<PropertyFilter> getPropertyFilters(ComponentDescriptionDTO dto) {
		final var propertyTargetsFromReferences = (String[]) dto.properties.get("generate.targets.from.references");
		if (propertyTargetsFromReferences == null) {
			return Collections.emptyList();
		}

		return fromGenerateTargetsFromReferences(dto, propertyTargetsFromReferences);
	}

	private Map<StringWithParams.Parameter, Object> getValuesByParameter(//
			Supplier<String> identifier, //
			PropertyFilter filter, //
			Map<String, ValueProviderFromConfig> valueProvider //
	) {
		return filter.targetTemplate().parameter().stream().map(parameter -> {
			final var v = valueProvider.get(parameter.topic());
			if (v == null) {
				return null;
			}
			final var value = v.getValue(parameter.variable());
			if (value == null) {
				if (this.log.isInfoEnabled()) {
					this.log.info(
							"Value for targetTemplate filter component='{}', property='{}', parameter='{}.{}' not found",
							identifier.get(), filter.property(), parameter.topic(), parameter.variable());
				}
				return null;
			}

			return Map.entry(parameter, value);
		}).filter(Objects::nonNull) //
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private ComponentDescriptionDTO getComponentDescription(//
			ServiceReference<?> reference, //
			Dictionary<String, Object> properties //
	) {
		final var pid = (String) properties.get(Constants.SERVICE_PID);
		final var factoryPid = (String) properties.get(OpenemsConstants.PROPERTY_FACTORY_PID);
		return this.scr.getComponentDescriptionDTO(reference.getBundle(), factoryPid == null ? pid : factoryPid);
	}

	private ObjectClassDefinition getOcd(ServiceReference<?> reference, Dictionary<String, Object> props) {
		final var ocd = ServiceUtils.getOcd(this.metaTypeService, reference.getBundle(), props);

		if (ocd == null) {
			this.log.warn("Unable to find ObjectClassDefinition."//
					+ " Default values may be missing and could"//
					+ " lead to incorrectly generated target filters");
		}

		return ocd;
	}

	private static String getIdentifier(ServiceReference<?> reference, Dictionary<String, Object> properties) {
		final var componentId = (String) properties.get(OpenemsConstants.PROPERTY_COMPONENT_ID);
		if (componentId != null) {
			return componentId;
		}

		final var pid = (String) properties.get(OpenemsConstants.PROPERTY_PID);
		if (pid != null) {
			return pid;
		}

		final var factoryPid = (String) properties.get(OpenemsConstants.PROPERTY_FACTORY_PID);
		if (factoryPid != null) {
			return factoryPid;
		}
		return reference.toString();
	}

}
