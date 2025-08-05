package io.openems.edge.goodwe.batteryinverter;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceBinder<T, S> {

	private record BindService<S>(S service, ServiceRegistration<? super S> serviceRegistration) {

		public void deactivate() {
			if (this.serviceRegistration() != null) {
				this.serviceRegistration().unregister();
			}
		}

	}

	private final Logger log = LoggerFactory.getLogger(ServiceBinder.class);

	private final Class<? super S> registerClass;
	private final Function<T, S> serviceFactory;
	private final Consumer<S> deactivateService;
	private final Map<T, BindService<S>> bindServices = new HashMap<>();
	private BundleContext bundleContext;

	public ServiceBinder(Class<? super S> registerClass, Function<T, S> serviceFactory, Consumer<S> deactivateService) {
		this.registerClass = registerClass;
		this.serviceFactory = serviceFactory;
		this.deactivateService = deactivateService;
	}

	/**
	 * Binds a service.
	 * 
	 * @param service the service to bind
	 */
	public synchronized void bindService(T service) {
		final var mappedService = this.serviceFactory.apply(service);
		this.bindServices.put(service, new BindService<>(mappedService, this.createServiceReference(mappedService)));
	}

	/**
	 * Unbinds a service and cleans up resources.
	 * 
	 * @param service the service to unbind
	 */
	public synchronized void unbindService(T service) {
		final var prev = this.bindServices.remove(service);
		if (prev == null) {
			return;
		}
		this.deactivateBindService(prev);
	}

	/**
	 * Updates the bundle context tor register or unregister services.
	 * 
	 * @param bundleContext the {@link BundleContext} to use to register the
	 *                      services
	 */
	public synchronized void updateBundleContext(BundleContext bundleContext) {
		if (Objects.equals(this.bundleContext, bundleContext)) {
			return;
		}
		this.bundleContext = bundleContext;

		for (var entry : this.bindServices.entrySet()) {
			if (entry.getValue().service() == null) {
				continue;
			}
			if (entry.getValue().serviceRegistration() != null) {
				entry.getValue().deactivate();
			}
			entry.setValue(new BindService<>(entry.getValue().service(),
					this.createServiceReference(entry.getValue().service())));
		}
	}

	/**
	 * Triggers a configuration update. Should be called when the service factory
	 * method should be called again, because input parameters changed.
	 */
	public synchronized void updateConfiguration() {
		for (var entry : this.bindServices.entrySet()) {
			this.deactivateBindService(entry.getValue());

			final var mappedService = this.serviceFactory.apply(entry.getKey());
			entry.setValue(new BindService<>(mappedService, this.createServiceReference(mappedService)));
		}
	}

	/**
	 * Deactivates this component and cleans up all bind services.
	 */
	public void deactivate() {
		for (var bindService : this.bindServices.values()) {
			this.deactivateBindService(bindService);
		}
	}

	private void deactivateBindService(BindService<S> bindService) {
		bindService.deactivate();

		if (bindService.service() == null) {
			return;
		}
		try {
			this.deactivateService.accept(bindService.service());
		} catch (RuntimeException e) {
			this.log.warn("Unable to deactivate service of type {}", bindService.service().getClass(), e);
		}
	}

	private ServiceRegistration<? super S> createServiceReference(S service) {
		if (this.bundleContext == null) {
			return null;
		}

		return this.bundleContext.registerService(this.registerClass, service, new Hashtable<>());
	}

}
