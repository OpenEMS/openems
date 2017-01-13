package io.openems.core;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Table;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.thing.Thing;

/**
 * Retreives and caches information about classes via reflection
 *
 * @author stefan.feilmeier
 */
public class ClassRepository {
	private final static Logger log = LoggerFactory.getLogger(ClassRepository.class);
	private static ClassRepository instance;

	public static synchronized ClassRepository getInstance() {
		if (ClassRepository.instance == null) {
			ClassRepository.instance = new ClassRepository();
		}
		return ClassRepository.instance;
	}

	private HashMultimap<Class<? extends Thing>, Member> thingChannels = HashMultimap.create();
	private Table<Class<? extends Thing>, Member, ConfigInfo> thingConfigChannels = HashBasedTable.create();

	public ClassRepository() {}

	/**
	 * Get all declared Channels of thing class.
	 *
	 * @param clazz
	 * @return
	 */
	public Set<Member> getThingChannels(Class<? extends Thing> clazz) {
		if (!thingChannels.containsKey(clazz)) {
			parseClass(clazz);
		}
		return Collections.unmodifiableSet(thingChannels.get(clazz));
	}

	/**
	 * Get all declared ConfigChannels of thing class.
	 *
	 * @param clazz
	 * @return
	 */
	public Map<Member, ConfigInfo> getThingConfigChannels(Class<? extends Thing> clazz) {
		if (!thingConfigChannels.containsRow(clazz)) {
			parseClass(clazz);
		}
		return Collections.unmodifiableMap(thingConfigChannels.row(clazz));
	}

	private void parseClass(Class<? extends Thing> clazz) {
		if (Thing.class.isAssignableFrom(Thing.class)) {
			for (Method method : clazz.getMethods()) {
				Class<?> type = null;
				if (method.getReturnType().isArray()) {
					Class<?> rtype = method.getReturnType();
					type = rtype.getComponentType();
				} else {
					type = method.getReturnType();
				}
				if (Channel.class.isAssignableFrom(type)) {
					thingChannels.put(clazz, method);
				}
				if (ConfigChannel.class.isAssignableFrom(type)) {
					ConfigInfo configAnnotation = method.getAnnotation(ConfigInfo.class);
					if (configAnnotation == null) {
						log.error("Config-Annotation is missing for method [" + method.getName() + "] in class ["
								+ clazz.getName() + "]");
					} else {
						thingConfigChannels.put(clazz, method, configAnnotation);
					}
				}
			}
			for (Field field : clazz.getFields()) {
				Class<?> type = field.getType();
				if (Channel.class.isAssignableFrom(type)) {
					thingChannels.put(clazz, field);
				}
				if (ConfigChannel.class.isAssignableFrom(type)) {
					ConfigInfo configAnnotation = field.getAnnotation(ConfigInfo.class);
					if (configAnnotation == null) {
						log.error("Config-Annotation is missing for field [" + field.getName() + "] in class ["
								+ clazz.getName() + "]");
					} else {
						thingConfigChannels.put(clazz, field, configAnnotation);
					}
				}
			}
		}
	}
}
