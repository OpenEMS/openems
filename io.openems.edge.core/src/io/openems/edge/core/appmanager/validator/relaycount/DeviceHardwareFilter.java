package io.openems.edge.core.appmanager.validator.relaycount;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Objects;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.edge.app.common.props.RelayProps;
import io.openems.edge.app.common.props.RelayProps.RelayContactFilter;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsAppCategory;

@Component(//
		name = DeviceHardwareFilter.COMPONENT_NAME, //
		scope = ServiceScope.PROTOTYPE //
)
public class DeviceHardwareFilter implements CheckRelayCountFilter {

	public static final String COMPONENT_NAME = "CheckRelayCount.Filter.DeviceHardware";

	private final AppManagerUtil appManagerUtil;

	@Activate
	public DeviceHardwareFilter(//
			@Reference AppManagerUtil appManagerUtil //
	) {
		super();
		this.appManagerUtil = appManagerUtil;
	}

	@Override
	public RelayContactFilter apply() {
		final var deviceHardwareInstances = this.appManagerUtil
				.getInstantiatedAppsByCategories(OpenemsAppCategory.OPENEMS_DEVICE_HARDWARE);

		if (deviceHardwareInstances.isEmpty()) {
			return RelayProps.emptyFilter();
		}

		final var dependencyInstances = deviceHardwareInstances.stream() //
				.flatMap(t -> t.dependencies.stream()) //
				.map(t -> this.appManagerUtil.findInstanceById(t.instanceId).orElse(null)) //
				.filter(Objects::nonNull) //
				.toList();

		final var blacklistedComponents = dependencyInstances.stream() //
				.flatMap(instance -> {
					try {
						final var configuration = this.appManagerUtil.getAppConfiguration(ConfigurationTarget.UPDATE,
								instance, Language.DEFAULT);

						return configuration.getComponents().stream().map(t -> t.id());
					} catch (OpenemsNamedException e) {
						return Stream.empty();
					}
				}) //
				.collect(toUnmodifiableSet());

		return RelayContactFilter.create() //
				.withComponentFilter(t -> !blacklistedComponents.contains(t.id()));
	}

}