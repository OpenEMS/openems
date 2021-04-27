package io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass;

import io.openems.common.exceptions.OpenemsError;
import org.joda.time.DateTime;

public interface LineHeater {
    boolean startHeating() throws OpenemsError.OpenemsNamedException;
    boolean stopHeating(DateTime lifecycle) throws OpenemsError.OpenemsNamedException;

    DateTime getLifeCycle();

    void setLifeCycle(DateTime lifeCycle);
}
