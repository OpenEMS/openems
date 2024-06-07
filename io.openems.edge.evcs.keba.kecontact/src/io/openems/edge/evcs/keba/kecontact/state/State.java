package io.openems.edge.evcs.keba.kecontact.state;

import io.openems.edge.evcs.keba.kecontact.EvcsKebaKeContactImpl;

public interface State {
    void handlePower(int power, EvcsKebaKeContactImpl context);
    void switchPhase(EvcsKebaKeContactImpl context);
}
