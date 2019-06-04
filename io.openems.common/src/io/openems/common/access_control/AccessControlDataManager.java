package io.openems.common.access_control;

import java.util.Collection;
import java.util.Set;

/**
 * This interface is used for adding and getting the data which is needed for the RBAC (Role based access control)
 *
 * @author Sebastian.Walbrun
 */
interface AccessControlDataManager {

    /**
     * This method adds the handed over roles to the existing ones.
     * In case merge==true all channels and method permissions will be added otherwise just non
     * existing roles will be added
     * @param roles roles to add
     * @param merge if true channels and methods will be merged otherwise just new roles will be added
     */
    void addRoles(Set<Role> roles, boolean merge);

    /**
     * This method adds a new user to the existing ones. In case the same is already part of the cache,
     * the new one will NOT be added
     * @param user the user to add
     * @return true if user did not exist before
     */
    boolean addUser(User user);

    /**
     * @return all created users via the providers
     */
    Collection<User> getUsers();

    /**
     * @return all created roles via the providers
     */
    Collection<Role> getRoles();
}
