package io.openems.edge.core.appmanager;

import java.util.List;
import java.util.Optional;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.ComponentUtilImpl.Relay;
import io.openems.edge.core.host.NetworkInterface;

public interface ComponentUtil {

	/**
	 * Gets the interfaces of the currently active network settings.
	 *
	 * @return all interfaces in the host configuration
	 * @throws OpenemsNamedException on error
	 */
	public List<NetworkInterface<?>> getInterfaces() throws OpenemsNamedException;

	/**
	 * Checks if any component has the given String in its configuration.
	 *
	 * @param value     that no component should have
	 * @param ignoreIds the id s of components which configuration should be ignored
	 * @return true if a component has the given String in its configuration
	 */
	public boolean anyComponentUses(String value, List<String> ignoreIds);

	/**
	 * Gets a list of current Relays. e. g. 'io0/Relay1'
	 *
	 * @return a list of Relays
	 * @throws OpenemsNamedException on error
	 */
	public List<Relay> getAllRelays();

	/**
	 * Gets a list of currently available Relays of IOs which are not used by any
	 * component. e. g. 'io0/Relay1'
	 *
	 * @return a list of available Relays
	 * @throws OpenemsNamedException on error
	 */
	public List<Relay> getAvailableRelays();

	/**
	 * Gets a list of currently available Relays of IOs which are not used by any
	 * component. like 'io0/Relay1'
	 *
	 * @param ignoreIds the Component-IDs that should be ignored
	 * @return a list of available Relays
	 * @throws OpenemsNamedException on error
	 */
	public List<Relay> getAvailableRelays(List<String> ignoreIds);

	/**
	 * Gets a list of currently available Relays of given IO which are not used by
	 * any component. like 'io0/Relay1'
	 *
	 * @param ioId the Component-ID of the DigitalOutput
	 * @return a list of available Relays
	 * @throws OpenemsNamedException if the io was not found
	 */
	public List<String> getAvailableRelays(String ioId) throws OpenemsNamedException;

	/**
	 * Gets a list of currently available Relays of given IO which are not used by
	 * any component. e. g. 'io0/Relay1'
	 *
	 * @param ioId      the Component-ID of the DigitalOutput
	 * @param ignoreIds the Component-IDs that should be ignored
	 * @return a list of available Relays
	 * @throws OpenemsNamedException if the io was not found
	 */
	public List<String> getAvailableRelays(String ioId, List<String> ignoreIds) throws OpenemsNamedException;

	/**
	 * Searches a component with the given component configuration.
	 *
	 * @param component with the configuration to be searched for
	 * @return the found component or null if not found
	 */
	public Component getComponentByConfig(Component component);

	/**
	 * Gets the enabled Components with the starting id.
	 *
	 * @param <T> the type to which the components should be assignable to
	 * @param id  the starting id of the components
	 * @return a list of found components
	 */
	public <T extends OpenemsComponent> List<T> getEnabledComponentsOfStartingId(String id);

	/**
	 * Gets the enabled Components of a Type.
	 *
	 * <p>
	 * the method 'this.componentManager.getEnabledComponentsOfType(clazz)' does not
	 * return the component if the given class is an interface and the component has
	 * the interface implemented
	 *
	 * @param <T>   the type to which the components should be assignable to
	 * @param clazz to which the component should be assignable to
	 * @return a list of found components
	 */
	public <T extends OpenemsComponent> List<T> getEnabledComponentsOfType(Class<T> clazz);

	/**
	 * Gets the next available id with the baseName.
	 *
	 * @param baseName   like ess, meter without a number
	 * @param components the used components from the other apps, because if the
	 *                   user updates multiple instances very quickly and components
	 *                   of the same type are created they are not instantly added
	 *                   to the componentManager
	 * @return the id
	 */
	public default String getNextAvailableId(String baseName, List<Component> components) {
		return this.getNextAvailableId(baseName, 0, components);
	}

	/**
	 * Gets the next available id with the baseName starting with the given
	 * startingNumber.
	 *
	 * @param baseName       like ess, meter without a number
	 * @param startingNumber the number at the end of the id to start from
	 * @param components     the used components from the other apps, because if the
	 *                       user updates multiple instances very quickly and
	 *                       components of the same type are created they are not
	 *                       instantly added to the componentManager
	 * @return the id
	 */
	public String getNextAvailableId(String baseName, int startingNumber, List<Component> components);

	/**
	 * Gets the preferred relays. If the default ports are are already taken the
	 * next available in a row are taken. If not enough in a row are available the
	 * first available relays of any relayboard are returned.
	 *
	 * @param ignoreIds      the ids of the components that should be ignored
	 * @param relays4Channel the default ports on a 4-Channel Relay
	 * @param relays8Channel the default ports on a 8-Channel Relay
	 * @return the relays
	 */
	public String[] getPreferredRelays(List<String> ignoreIds, int[] relays4Channel, int[] relays8Channel);

	/**
	 * updates the interfaces in the Host configuration.
	 *
	 * @param user       the executing user
	 * @param interfaces the new interfaces
	 * @throws OpenemsNamedException on error
	 */
	public void updateInterfaces(User user, List<NetworkInterface<?>> interfaces) throws OpenemsNamedException;

	/**
	 * updates the execution order of the scheduler only adds or changes order of
	 * the given id s.
	 *
	 * @param user                    the executing user
	 * @param schedulerExecutionOrder the execution order
	 * @param components              the components which are currently created
	 * @throws OpenemsNamedException when the scheduler can not be updated
	 */
	public void updateScheduler(User user, List<String> schedulerExecutionOrder, List<EdgeConfig.Component> components)
			throws OpenemsNamedException;

	/**
	 * removes the given id s from the scheduler if they exist in the scheduler.
	 *
	 * @param user       the executing user
	 * @param removedIds the ip s that should be removed
	 * @throws OpenemsNamedException on error
	 */
	public void removeIdsInSchedulerIfExisting(User user, List<String> removedIds) throws OpenemsNamedException;

	/**
	 * Gets the current id's in the scheduler.
	 *
	 * @return the id's
	 * @throws OpenemsNamedException on error
	 */
	public List<String> getSchedulerIds() throws OpenemsNamedException;

	/**
	 * Gets the scheduler Component.
	 *
	 * @return the scheduler component
	 * @throws OpenemsNamedException if more or no scheduler is available
	 */
	public Component getScheduler() throws OpenemsNamedException;

	/**
	 * Creates a new List with only components which exist in the current
	 * configuration or in the passed components list.
	 *
	 * @param ids        the initial list
	 * @param components the current creating components
	 * @return a new list only with id's of components that exist
	 */
	public List<String> removeIdsWhichNotExist(List<String> ids, List<EdgeConfig.Component> components);

	/**
	 * Inserts the insertOrder into the actual Order.
	 *
	 * @param actualOrder the current scheduler order
	 * @param insertOrder the order which should be inserted
	 * @return the complete order
	 */
	public List<String> insertSchedulerOrder(List<String> actualOrder, List<String> insertOrder);

	/**
	 * updates the host configuration deletes ip s that are in {@link oldIps} but
	 * not in {@link ips} and adds ip s that are in {@link ips} but not in
	 * {@link oldIps}.
	 *
	 * @param user   the executing user
	 * @param ips    the ip s that should be in the configuration
	 * @param oldIps the old ip s that were in the old configuration
	 * @throws OpenemsNamedException on error
	 */
	public void updateHosts(User user, List<InterfaceConfiguration> ips, List<InterfaceConfiguration> oldIps)
			throws OpenemsNamedException;

	/**
	 * Gets an {@link Optional} of an {@link EdgeConfig.Component}.
	 *
	 * @param id        the id of the component
	 * @param factoryId the factoryId of the component
	 * @return the optional component
	 */
	public Optional<EdgeConfig.Component> getComponent(String id, String factoryId);
}
