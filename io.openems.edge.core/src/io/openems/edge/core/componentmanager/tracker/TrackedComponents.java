package io.openems.edge.core.componentmanager.tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.component.OpenemsComponent;

@Component(scope = ServiceScope.SINGLETON)
public final class TrackedComponents implements TrackedComponentsView, TrackedComponentsRegistry {

	private static final Logger log = LoggerFactory.getLogger(TrackedComponents.class);

	private static boolean isComponentValid(OpenemsComponent component, Long servicePid) {
		if (component == null || component.id() == null) {
			log.warn("Received reference for component with null ID. Ignoring.");
			return false;
		}
		if (servicePid == null || servicePid < 0) {
			final var componentId = component.id();
			log.error("Received reference for component {} without valid service ID. Ignoring.", componentId);
			return false;
		}
		return true;
	}

	private final Map<Long, String> componentsByServiceId = new ConcurrentHashMap<>(24);
	private final Map<String, OpenemsComponent> trackedComponents = new ConcurrentHashMap<>(24);
	private final Map<String, List<OpenemsComponent>> duplicates = new ConcurrentHashMap<>(6);

	private final Object lock = new Object();

	@Activate
	public TrackedComponents() {
	}

	@Override
	public void activatedComponent(Long servicePid, OpenemsComponent component) {
		if (!isComponentValid(component, servicePid)) {
			return;
		}
		synchronized (this.lock) {
			final var componentId = component.id();
			log.debug("Adding component {} with service ID {} to tracking.", componentId, servicePid);
			this.componentsByServiceId.put(servicePid, componentId);
			if (this.trackedComponents.containsKey(componentId)) {
				this.duplicates.compute(componentId, (k, v) -> {
					final var list = v != null ? v : new ArrayList<OpenemsComponent>();
					list.add(component);
					return list;
				});
				return;
			}
			this.trackedComponents.put(componentId, component);
		}
	}

	@Override
	public void modifiedComponent(Long servicePid, OpenemsComponent component) {
		if (!isComponentValid(component, servicePid)) {
			return;
		}
		this.deactivatedComponent(servicePid, component);
		this.activatedComponent(servicePid, component);
	}

	@Override
	public void deactivatedComponent(Long servicePid, OpenemsComponent component) {
		if (!isComponentValid(component, servicePid)) {
			return;
		}
		synchronized (this.lock) {
			final var componentId = this.componentsByServiceId.remove(servicePid);
			if (componentId == null) {
				final var altComponentId = component.id();
				log.warn(
						"Tried to remove component {} with service ID {}, but no matching component found in tracking.",
						altComponentId, servicePid);
				return;
			}

			log.debug("Removing component {} with service ID {} from tracking.", componentId, servicePid);
			final var dups = this.duplicates.get(componentId);
			final var trackedComponent = this.trackedComponents.get(componentId);

			if (trackedComponent == component) {
				this.trackedComponents.remove(componentId);

				if (dups != null) {
					this.trackedComponents.put(componentId, dups.removeFirst());
				}
			} else {
				if (dups != null) {
					dups.remove(component);
				}
			}

			if (dups != null && dups.isEmpty()) {
				this.duplicates.remove(componentId);
			}
		}
	}

	@Override
	public List<OpenemsComponent> getAllComponents() {
		return this.trackedComponents.values().stream().toList();
	}

	@Override
	public List<OpenemsComponent> getEnabledComponents() {
		return this.trackedComponents.values().stream() //
				.filter(OpenemsComponent::isEnabled) //
				.toList();
	}

	@Override
	public Optional<OpenemsComponent> getComponentById(String componentId) {
		return Optional.ofNullable(this.trackedComponents.get(componentId));
	}

	@Override
	public List<String> getAllComponentIds() {
		return this.trackedComponents.keySet().stream().toList();
	}

	@Override
	public List<String> getDuplicateIds() {
		return this.duplicates.keySet().stream().toList();
	}

	@Override
	public boolean hasDuplicates() {
		return !this.duplicates.isEmpty();
	}

}
