package io.openems.edge.bridge.lucidcontrol.api;

/**
 * The LucidControlBridge Interface provides methods, so LucidControl modules and devices can be added and tasks
 * can be handled.
 */
public interface LucidControlBridge {

    /**
     * Adds the Path of the LucidControl Module.
     *
     * @param id   id of the module, usually from config.id()
     * @param path path of the Module, usually from config.path()
     */

    void addPath(String id, String path);

    /**
     * Adds the max Voltage the LucidControl Module provides.
     *
     * @param id      id of the module, usually from config.id()
     * @param voltage max voltage of module, usually from config.voltage()
     */

    void addVoltage(String id, String voltage);

    /**
     * Removes the LucidControl Module from the Bridge.
     *
     * @param id the id of the module, usually from super.id()
     */

    void removeModule(String id);

    /**
     * Removes the LucidControlTask identified by the id.
     *
     * @param id id of the Task, usually from LucidControlDevice super.id()
     */

    void removeTask(String id);

    /**
     * Adds a LucidControlTask.
     *
     * @param id    id of the LucidControl Device
     * @param lucid the Task, usually created by LucidControl Device
     */

    void addLucidControlTask(String id, LucidControlBridgeTask lucid);

    /**
     * Gets the Path of the LucidControl Module.
     *
     * @param moduleId usually from LucidControl Device config.moduleId()
     * @return the Path of the Module
     */
    String getPath(String moduleId);

    /**
     * Gets the maxVoltage as a String identified via given key.
     *
     * @param moduleId is the key to get the max Voltage, usually from Device config.moduleId()
     * @return the maximum Voltage configured.
     */
    String getVoltage(String moduleId);


}
