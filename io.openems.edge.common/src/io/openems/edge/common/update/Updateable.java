package io.openems.edge.common.update;

import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.edge.common.update.jsonrpc.GetUpdateState;

/**
 * Interface which defines that something can be updated.
 * 
 * <p>
 * In order to register a Updateable the following two ways are recommended:
 * 
 * <p>
 * 1. One instance per component (should be mostly used to have better control
 * of the Updateable):
 * 
 * <pre>
    private ServiceRegistration&lt;Updateable&gt serviceRegistration;

    <code>@</code>Activate
    public void activate(ComponentContext context) {
        this.serviceRegistration = context.getBundleContext().registerService(Updateable.class, 
                new YourUpdateable(), new Hashtable<>());
    }

    <code>@</code>Deactivate
    public void deactivate() {
        this.serviceRegistration.unregister();
    }
 * </pre>
 * 
 * <p>
 * 2. Use OSGi components for Singletons (do not use for factory components).
 * e.g. for core components which only exist once
 * 
 * <pre>
    <code>@</code>Component(scope = ServiceScope.SINGLETON)
    public class YourUpdateable implements Updateable {
    ...
    }
 * </pre>
 */
public interface Updateable {

	public record UpdateableMetaInfo(//
			String name, //
			String description, //
			Role requiredMinRole //
	) {

	}

	/**
	 * Gets the meta information of an {@link Updateable}. This method should not
	 * block and therefore not try to get information about the current version.
	 * 
	 * @param language the {@link Language} of the information
	 * @return the {@link UpdateableMetaInfo}
	 */
	UpdateableMetaInfo getMetaInfo(Language language);

	/**
	 * Triggers to start the update process. This method should not block.
	 */
	void executeUpdate();

	/**
	 * Gets the current state of the {@link Updateable}. This method should only
	 * block if necessary.
	 * 
	 * @return the current {@link GetUpdateState.UpdateState}
	 */
	GetUpdateState.UpdateState getUpdateState();

}
