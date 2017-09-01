package io.openems.impl.controller.asymmetric.avoidtotalcharge;

import java.util.Calendar;

/**
 * Created by maxo2 on 29.08.2017.
 */
public interface ChargingGraph {

    Double getAccordingVal(Calendar c);
    Double getCurrentVal();
}
