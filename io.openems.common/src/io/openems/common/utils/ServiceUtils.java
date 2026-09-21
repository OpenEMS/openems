package io.openems.common.utils;

import java.util.Dictionary;
import java.util.Objects;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

import io.openems.common.OpenemsConstants;

public class ServiceUtils {

	public static class CloseableService<T> implements AutoCloseable {

		private final BundleContext bundleContext;
		private final ServiceReference<T> serviceReference;
		private final T service;

		public CloseableService(BundleContext bundleContext, Class<T> clazz, String filter)
				throws InvalidSyntaxException {
			this.bundleContext = Objects.requireNonNull(bundleContext);
			final var foundServices = bundleContext.getServiceReferences(Objects.requireNonNull(clazz), filter);
			this.serviceReference = foundServices.size() != 0 ? foundServices.iterator().next() : null;
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
	 * try (var componentManagerService = ServiceUtils.useService(bundleContext, ComponentManager.class)) {
	 * 	var componentManager = componentManagerService.getService();
	 * 	// use componentManager here (may be null) ...
	 * } catch (Exception e) {
	 * }
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
		try {
			return useService(bundleContext, clazz, null);
		} catch (InvalidSyntaxException e) {
			// exception can only be thrown with filter
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a {@link CloseableService} with the given parameters.
	 * 
	 * <p>
	 * Usage: <br>
	 * 
	 * <pre>
	 * try (var componentManagerService = ServiceUtils.useService(bundleContext, ComponentManager.class)) {
	 * 	var componentManager = componentManagerService.getService();
	 * 	// use componentManager here (may be null) ...
	 * } catch (Exception e) {
	 * }
	 * </pre>
	 * 
	 * @param <T>           the type of the service
	 * @param bundleContext the {@link BundleContext} of the service
	 * @param clazz         the class type of the service
	 * @param filter        the filter expression or null for any service
	 * @return the {@link CloseableService}
	 * @throws InvalidSyntaxException If the specified filter contains an invalid
	 *                                filter expression that cannot be parsed.
	 */
	public static <T> CloseableService<T> useService(//
			final BundleContext bundleContext, //
			final Class<T> clazz, //
			final String filter // nullable
	) throws InvalidSyntaxException {
		return new CloseableService<>(bundleContext, clazz, filter);
	}

	/**
	 * Gets the {@link ObjectClassDefinition} for the given parameters.
	 *
	 * @param metaTypeService the {@link MetaTypeService} to use
	 * @param bundle          the bundle to search in
	 * @param props           the properties of the service. Should contain either
	 *                        {@link OpenemsConstants#PROPERTY_FACTORY_PID} or
	 *                        {@link OpenemsConstants#PROPERTY_PID} if it is a
	 *                        singleton.
	 * @return the found {@link ObjectClassDefinition} or null if not found
	 */
	public static ObjectClassDefinition getOcd(//
			MetaTypeService metaTypeService, //
			Bundle bundle, //
			Dictionary<String, Object> props //
	) {
		if (props == null) {
			return null;
		}
		final var mti = metaTypeService.getMetaTypeInformation(bundle);
		if (mti == null) {
			return null;
		}

		final var factoryPid = (String) props.get(OpenemsConstants.PROPERTY_FACTORY_PID);
		final var pid = (String) props.get(OpenemsConstants.PROPERTY_PID);

		if (ArrayUtils.containsIgnoreNull(mti.getFactoryPids(), factoryPid)) {
			return mti.getObjectClassDefinition(factoryPid, null);
		}
		if (ArrayUtils.containsIgnoreNull(mti.getPids(), pid)) {
			return mti.getObjectClassDefinition(pid, null);
		}
		return null;
	}

}
