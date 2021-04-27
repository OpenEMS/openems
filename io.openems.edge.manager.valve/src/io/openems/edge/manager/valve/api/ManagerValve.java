package io.openems.edge.manager.valve.api;

import io.openems.edge.heatsystem.components.Valve;

public interface ManagerValve {

    void addValve(String id, Valve valve);

    void removeValve(String id);

}
