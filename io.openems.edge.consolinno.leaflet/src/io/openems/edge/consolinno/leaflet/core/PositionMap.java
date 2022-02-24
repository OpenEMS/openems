package io.openems.edge.consolinno.leaflet.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PositionMap {

    private final Map<Integer, List<Integer>> positionMap;


    /**
     * This Map Saves the Positions in the Module Type (TMP,RELAY,PWM) that are Occupied by a configured Modbus Device.
     *
     * @param moduleNumber Number of the Module specified on the Device
     * @param position     Pin position of the device on the module
     */
    public PositionMap(int moduleNumber, int position) {
        HashMap<Integer, List<Integer>> mapToPut = new HashMap<>();
        List<Integer> initList = new ArrayList<>(position);
        mapToPut.put(moduleNumber, initList);
        this.positionMap = mapToPut;
    }

    public Map<Integer, List<Integer>> getPositionMap() {
        return positionMap;
    }
}
