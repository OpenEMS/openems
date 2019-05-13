package io.openems.common.access_control;

abstract class AccessControlDataSource {
    protected final AccessControl accessControl = AccessControl.getInstance();

    abstract void initializeAccessControl(String path);

}
