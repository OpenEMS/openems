package io.openems.edge.controller.heatnetwork.communication.api;

/**
 * ManageTypes, handled by each manager on their own.
 */
public enum ManageType {
    FIFO, LIFO, DYNAMIC;

    public static boolean contains(String manage) {
        for (ManageType manageType : ManageType.values()) {
            if (manageType.name().equals(manage)) {
                return true;
            }
        }
        return false;
    }
}
