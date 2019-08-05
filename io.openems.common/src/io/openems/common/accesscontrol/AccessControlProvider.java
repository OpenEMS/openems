package io.openems.common.accesscontrol;

/**
 * This interface can be used for initializing the {@link AccessControlDataManager}. The implementations of this class
 * provide a set of {@link User users} and {@link Role roles} which are stored in any way. The information will then
 * be stored in the data manager. The {@link AccessControl} accesses the data manager for handling its own requests
 *
 * @author Sebastian.Walbrun
 */
interface AccessControlProvider extends Comparable<AccessControlProvider> {

    /**
     * This method gets called while the service is starting up. With the call of this method the provider can fetch its
     * information and set it into the handed over data manager
     * @param accessControlDataManager the data manager which needs to be initialized
     */
    void initializeAccessControl(AccessControlDataManager accessControlDataManager);

    /**
     * This priority gets used by the {@link AccessControl} for sorting all configured providers. The providers which
     * will be handled first can create new {@link Role roles} and {@link User users} whereas later providers can only
     * append but not overwrite information!
     * @return
     */
    int priority();

    @Override
    default int compareTo(AccessControlProvider o) {
        return this.priority() - o.priority();
    }
}
