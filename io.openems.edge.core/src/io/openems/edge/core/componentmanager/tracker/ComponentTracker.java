package io.openems.edge.core.componentmanager.tracker;

import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.Map;
import java.util.Optional;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;

/**
 * ComponentTracker tracks OpenemsComponents in the OSGi environment.
 */
@Component(immediate = true)
public final class ComponentTracker {

	private static Long getServiceId(Map<String, Object> ref) {
		if (ref == null) {
			return null;
		}
		Optional<Long> sid = Optional.ofNullable(ref.get(Constants.SERVICE_ID))
				.map(v -> TypeUtils.getAsType(OpenemsType.LONG, v));
		return sid.orElse(null);
	}

	private final TrackedComponentsRegistry components;

	/**
	 * Binds an OpenemsComponent to the tracker.
	 * 
	 * @param component the OpenemsComponent to bind
	 * @param ref       the reference properties of the component
	 */
	@Reference(//
			cardinality = MULTIPLE, policy = DYNAMIC, policyOption = GREEDY, //
			bind = "bindComponent", updated = "updateComponent", unbind = "unbindComponent" //
	)
	public void bindComponent(OpenemsComponent component, Map<String, Object> ref) {
		final var sid = getServiceId(ref);
		this.components.activatedComponent(sid, component);
	}

	/**
	 * Updates an OpenemsComponent from the tracker.
	 *
	 * @param component the OpenemsComponent to bind
	 * @param ref       the reference properties of the component
	 */
	public void updateComponent(OpenemsComponent component, Map<String, Object> ref) {
		final var sid = getServiceId(ref);
		this.components.modifiedComponent(sid, component);
	}

	/**
	 * Unbinds an OpenemsComponent from the tracker.
	 *
	 * @param component the OpenemsComponent to bind
	 * @param ref       the reference properties of the component
	 */
	public void unbindComponent(OpenemsComponent component, Map<String, Object> ref) {
		final var sid = getServiceId(ref);
		this.components.deactivatedComponent(sid, component);
	}

	@Activate
	public ComponentTracker(//
			@Reference TrackedComponentsRegistry components //
	) {
		this.components = components;
	}

}
