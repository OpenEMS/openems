package io.openems.edge.core.utility;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

/**
 * This Interface provides static Method to update the ComponentConfig you desire.
 * The Methods provided by this Interface will throw Exceptions when the ServicePID cannot be found.
 * To use the Methods, just provide a ConfigurationAdmin, the service PID and the Key,Value pairs to update the config with,
 * of your OpenEMS component.
 */
public interface ConfigurationUpdate {
    /**
     * Updates the Component Config corresponding to the servicePID with the keyValuePairs.
     * Used for Updating multiple Values of the config.
     * @param ca the ConfigurationAdmin usually provided by the OpenEMS Component
     * @param servicePid the servicePID of the Component you want to update
     * @param propertyMap the propertyMap containing the "key" (property key of the config entry) and the Values (config entry value)
     * @throws IOException throws an exception when the service PID / Configuration cannot be found.
     */
    static void updateConfig(ConfigurationAdmin ca, String servicePid, Map<String, Object> propertyMap) throws IOException {
        Configuration c;
        c = ca.getConfiguration(servicePid, "?");
        Dictionary<String, Object> properties = c.getProperties();
        propertyMap.forEach(properties::put);
        c.update(properties);

    }

    /**
     * Updates the Component Config corresponding to the servicePID with the given Key and Value.
     * Used for Updating One Value of the config.
     * @param ca the ConfigurationAdmin usually provided by the OpenEMS Component
     * @param servicePid the servicePID of the Component you want to update
     * @param propertyKey Property key of the config entry
     * @param propertyValue config entry value to apply
     * @throws IOException throws an exception when the service PID / Configuration cannot be found.
     */
    static void updateConfig(ConfigurationAdmin ca, String servicePid, String propertyKey, Object propertyValue) throws IOException {
        Map<String, Object> keyValueMap = new HashMap<>();
        keyValueMap.put(propertyKey, propertyValue);
        updateConfig(ca, servicePid, keyValueMap);
    }

}
