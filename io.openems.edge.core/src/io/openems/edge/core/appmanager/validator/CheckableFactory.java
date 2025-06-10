package io.openems.edge.core.appmanager.validator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ReferenceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.OpenemsConstants;
import io.openems.common.session.Language;

@Component(//
		service = CheckableFactory.class //
)
public class CheckableFactory {

	private final Logger log = LoggerFactory.getLogger(CheckableFactory.class);

	private final Map<String, ComponentServiceObjects<Checkable>> checkableFactories = new HashMap<>();

	/**
	 * Binds a {@link Checkable} {@link ComponentServiceObjects}.
	 * 
	 * @param cso the cso to bind
	 */
	@Reference(//
			cardinality = ReferenceCardinality.MULTIPLE, //
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			// requires prototype for thread safety
			scope = ReferenceScope.PROTOTYPE_REQUIRED //
	)
	public void bindCso(ComponentServiceObjects<Checkable> cso) {
		var sr = cso.getServiceReference();
		var srName = (String) sr.getProperty(OpenemsConstants.PROPERTY_OSGI_COMPONENT_NAME);
		this.checkableFactories.put(srName, cso);
	}

	/**
	 * Unbinds a {@link Checkable} {@link ComponentServiceObjects}.
	 * 
	 * @param cso the cso to unbind
	 */
	public void unbindCso(ComponentServiceObjects<Checkable> cso) {
		var sr = cso.getServiceReference();
		var srName = (String) sr.getProperty(OpenemsConstants.PROPERTY_OSGI_COMPONENT_NAME);
		this.checkableFactories.remove(srName);
	}

	@Activate
	public CheckableFactory() {
	}

	/**
	 * Gets a {@link Checkable} which component name matches the provided name.
	 * 
	 * @param checkableComponentName the component name to search for
	 * @return a found {@link Checkable} service or null if not found
	 */
	public ClosableCheckable useCheckable(String checkableComponentName) {
		final var cso = this.checkableFactories.get(checkableComponentName);
		if (cso == null) {
			this.log.warn("Unable to find checkable with name '" + checkableComponentName + "'.");
			return null;
		}

		final var service = cso.getService();
		if (service == null) {
			this.log.warn("Unable to create checkable with name '" + checkableComponentName + "'.");
			return null;
		}

		return new ClosableCheckable(service, t -> {
			cso.ungetService(service);
		});
	}

	public static class ClosableCheckable implements Checkable, AutoCloseable {

		private final Checkable checkable;
		private final Consumer<Checkable> onClose;

		private ClosableCheckable(Checkable checkable, Consumer<Checkable> onClose) {
			super();
			this.checkable = checkable;
			this.onClose = onClose;
		}

		@Override
		public String getComponentName() {
			return this.checkable.getComponentName();
		}

		@Override
		public void setProperties(Map<String, ?> properties) {
			this.checkable.setProperties(properties);
		}

		@Override
		public boolean check() {
			return this.checkable.check();
		}

		@Override
		public String getErrorMessage(Language language) {
			return this.checkable.getErrorMessage(language);
		}

		@Override
		public String getInvertedErrorMessage(Language language) {
			return this.checkable.getInvertedErrorMessage(language);
		}

		@Override
		public void close() throws Exception {
			this.onClose.accept(this.checkable);
		}

	}

}
