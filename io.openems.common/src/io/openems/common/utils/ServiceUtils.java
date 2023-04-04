package io.openems.common.utils;

import java.util.Objects;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ServiceUtils {

	public static class CloseableService<T> implements AutoCloseable {

		private final BundleContext bundleContext;
		private final ServiceReference<T> serviceReference;
		private final T service;

		public CloseableService(BundleContext bundleContext, Class<T> clazz) {
			this.bundleContext = Objects.requireNonNull(bundleContext);
			this.serviceReference = bundleContext.getServiceReference(Objects.requireNonNull(clazz));
			if (this.serviceReference == null) {
				this.service = null;
				return;
			}
			this.service = bundleContext.getService(this.serviceReference);
		}

		@Override
		public void close() throws Exception {
			if (this.serviceReference == null) {
				return;
			}
			this.bundleContext.ungetService(this.serviceReference);
		}

		public final T getService() {
			return this.service;
		}

	}

	private ServiceUtils() {
	}

	/**
	 * Creates a {@link CloseableService} with the given parameters.
	 * 
	 * <p>
	 * Usage: <br> 
	 * 
	 * <pre>
	 * try (var componentManagerService =
	 * 	    ServiceUtils.useService(bundleContext, ComponentManager.class)) {
	 *     var componentManager = componentManagerService.getService();
	 *     // use componentManager here (may be null) ... 
	 * } catch (Exception e) { }
	 * </pre>
	 * 
	 * @param <T>           the type of the service
	 * @param bundleContext the {@link BundleContext} of the service
	 * @param clazz         the class type of the service
	 * @return the {@link CloseableService}
	 */
	public static <T> CloseableService<T> useService(//
			final BundleContext bundleContext, //
			final Class<T> clazz //
	) {
		return new CloseableService<>(bundleContext, clazz);
	}

}
