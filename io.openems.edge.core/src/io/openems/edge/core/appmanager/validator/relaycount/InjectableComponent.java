package io.openems.edge.core.appmanager.validator.relaycount;

import java.util.Map;

import org.osgi.framework.BundleContext;

import io.openems.common.OpenemsConstants;
import io.openems.common.utils.ServiceUtils;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentConfiguration;

public interface InjectableComponent<R> {

	/**
	 * Tries to get the value from an {@link InjectableComponent} and returns it.
	 * 
	 * @param <R>             the type of the result
	 * @param <I>             the type of the {@link InjectableComponent}
	 * @param context         the context to get the service with
	 * @param clazz           the {@link Class} of the {@link InjectableComponent}
	 * @param componentConfig the {@link ComponentConfiguration}
	 * @return the result or null if the service was not found
	 * @throws Exception in service injection error
	 */
	public static <R, I extends InjectableComponent<R>> R inject(//
			BundleContext context, //
			Class<I> clazz, //
			InjectableComponentConfig componentConfig //
	) throws Exception {
		try (final var service = ServiceUtils.useService(context, clazz,
				"(" + OpenemsConstants.PROPERTY_OSGI_COMPONENT_NAME + "=" + componentConfig.name() + ")")) {
			final var injectableComponent = service.getService();
			if (injectableComponent == null) {
				return null;
			}
			injectableComponent.setProperties(componentConfig.parameters());
			return injectableComponent.apply();
		}
	}

	/**
	 * Sets the properties of the component.
	 * 
	 * @param properties the properties map
	 */
	public default void setProperties(Map<String, ?> properties) {

	}

	/**
	 * Creates the result based on the set configuration.
	 * 
	 * @return the result
	 */
	public R apply();

}