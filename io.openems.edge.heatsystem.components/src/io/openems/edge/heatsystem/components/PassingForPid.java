package io.openems.edge.heatsystem.components;

import io.openems.common.exceptions.OpenemsError;

public interface PassingForPid extends PassingChannel {

    boolean readyToChange() throws OpenemsError.OpenemsNamedException;

    boolean changeByPercentage(double percentage);

}
