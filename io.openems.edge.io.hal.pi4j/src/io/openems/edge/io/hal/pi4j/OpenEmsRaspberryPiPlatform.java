package io.openems.edge.io.hal.pi4j;

import com.pi4j.plugin.raspberrypi.platform.RaspberryPiPlatform;

public class OpenEmsRaspberryPiPlatform extends RaspberryPiPlatform {
    @Override
    protected String[] getProviders() {
        return new String[]{};
    }
}