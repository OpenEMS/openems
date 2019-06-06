package io.openems.common.config;

import java.util.Enumeration;
import java.util.Iterator;

class ConfigEnumeration implements Enumeration<Config> {

	// private final Logger log = LoggerFactory.getLogger(ConfigEnumeration.class);

	private final Iterator<Config> iterator;

	public ConfigEnumeration(Iterator<Config> iterator) {
		this.iterator = iterator;
	}

	public Config nextElement() {
		Config config = iterator.next();
		// log.debug("Reading Config for PID ["+config.getPid()+"]");
		return config;
	}

	public boolean hasMoreElements() {
		return iterator.hasNext();
	}

}