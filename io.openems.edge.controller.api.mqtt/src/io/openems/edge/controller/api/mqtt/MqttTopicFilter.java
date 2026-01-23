package io.openems.edge.controller.api.mqtt;

import java.util.regex.Pattern;

public class MqttTopicFilter {
	
	private static final Pattern pattern = Pattern.compile("^(?:([^#\\\\+/]+|\\+)(\\/(?:[^#\\\\+/]+|\\+))*)?(?:\\/#)?\\/?$");
	
	/**
	 * Check if the given filter is a valid MQTT topic filter.
	 * 
	 * @param filter to check
	 * @return true if valid
	 */
	public static boolean validFilter(String filter) {
		if (filter == null || filter.isBlank()) {
			return false;
		}
		return pattern.matcher(filter).matches();
	}
	
	/**
	 * Create a new MqttTopicFilter from the given filter string.
	 * 
	 * @param filter to use
	 * @return new MqttTopicFilter
	 * @throws IllegalArgumentException if the filter is invalid
	 */
	public static MqttTopicFilter of(String filter) throws IllegalArgumentException {
		if (!validFilter(filter)) {
			throw new IllegalArgumentException("Invalid MQTT topic filter: " + filter);
		}
		return new MqttTopicFilter(filter);
	}
	
	private final String[] filterLevels;
	private final boolean multiLevelWildcard;
	
	private MqttTopicFilter(String filter) {
		this.filterLevels = filter.split("/");
		this.multiLevelWildcard = filter.endsWith("/#") || filter.equals("#");
	}
	
	/**
	 * Check it the given topic matches this filter.
	 * 
	 * @param topic to check against
	 * @return true if this filter applies
	 */
	public boolean matches(String topic) {
		String[] topicLevels = topic.split("/");
		
		if (!this.multiLevelWildcard && topicLevels.length != this.filterLevels.length) {
			return false;
		}

        int i = 0;
        for (; i < this.filterLevels.length; i++) {
            final String f = this.filterLevels[i];
            if (f.equals("#")) {
                return true;
            }
            if (i >= topicLevels.length) {
                return false;
            }
            String t = topicLevels[i];
            if (!f.equals("+") && !f.equals(t)) {
                return false;
            }
        }
        return true;
	}

}
