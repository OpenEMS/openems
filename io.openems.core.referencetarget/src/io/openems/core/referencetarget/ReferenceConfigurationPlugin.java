package io.openems.core.referencetarget;

import static io.openems.core.referencetarget.PropertyFilter.fromGenerateTargets;
import static io.openems.core.referencetarget.PropertyFilter.fromGenerateTargetsFromReferences;
import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationPlugin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.OpenemsConstants;

@Component
public class ReferenceConfigurationPlugin implements ConfigurationPlugin {

	private final Logger log = LoggerFactory.getLogger(ReferenceConfigurationPlugin.class);

	@Reference
	private ServiceComponentRuntime scr;

	@Activate
	public ReferenceConfigurationPlugin() {
	}

	@Override
	public void modifyConfiguration(ServiceReference<?> reference, Dictionary<String, Object> properties) {
		try {
			final var filters = this.getPropertyFilters(reference, properties);

			if (filters.isEmpty()) {
				return;
			}

			final var valueProvider = Map.of(//
					"config", new ValueProviderFromConfig(properties) //
			);

			for (var filter : filters) {
				final var valueMap = this.getValuesByParameter(filter, valueProvider);
				if (valueMap.size() != filter.targetTemplate().parameter().size()) {
					continue;
				}

				final var target = filter.targetTemplate().withParameters(valueMap);
				properties.put(filter.property() + ".target", target);
				this.log.info("Set targetTemplate filter for {} to {}", filter.property(), target);
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
		final var propertyTargets = (String[]) dto.properties.get("generate.targets");
		final var propertyTargetsFromReferences = (String[]) dto.properties.get("generate.targets.from.references");
		if (propertyTargets == null && propertyTargetsFromReferences == null) {
			return Collections.emptyList();
		}

		return Stream.concat(//
				fromGenerateTargets(propertyTargets).stream(), //
				fromGenerateTargetsFromReferences(dto, propertyTargetsFromReferences).stream() //
		).toList();
	}

	private Map<StringWithParams.Parameter, Object> getValuesByParameter(//
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
				this.log.warn("Value for variable {}.{} not found", parameter.topic(), parameter.variable());
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

}
