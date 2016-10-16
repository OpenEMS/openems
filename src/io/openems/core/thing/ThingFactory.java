package io.openems.core.thing;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.api.device.Device;
import io.openems.api.exception.ConfigException;
import io.openems.api.thing.IsConfigParameter;
import io.openems.api.thing.Thing;
import io.openems.core.bridge.Bridge;
import io.openems.core.utilities.JsonUtilities;

public class ThingFactory {

	private static Logger log = LoggerFactory.getLogger(ThingFactory.class);

	/**
	 * Parses a {@link JsonObject} to create and configure all {@link Thing}s.
	 *
	 * JsonObject may look like this:
	 *
	 * <pre>
		{
		  "things": [
		    {
		      "class": "io.openems.impl.bridge.modbusrtu.ModbusRtu",
		      "serialinterface": "/dev/ttyUSB0",
		      "baudrate": 38400,
		      "devices": [
		        {
		          "class": "io.openems.impl.device.pro.FeneconPro",
		          "modbusUnitId": 100,
		          "ess": {
		            "thingId": "ess0",
		            "minSoc": 15
		          },
		          "meter": {
		            "thingId": "meter0"
		          }
		        }
		      ]
		    }
		  ],
		  "scheduler": {
		    "class": "io.openems.impl.scheduler.SimpleScheduler",
		    "controllers": [
		      {
		        "priority": 100,
		        "class": "io.openems.impl.controller.avoidtotaldischarge.AvoidTotalDischargeController"
		      },
		      {
		        "priority": 50,
		        "class": "io.openems.impl.controller.balancing.BalancingController",
		        "chargeFromAc": true,
		        "gridMeter": "meter0"
		      }
		    ]
		  }
		}
	 * </pre>
	 *
	 * @param jConfig
	 * @return
	 * @throws ConfigException
	 */
	public static Map<String, Thing> getFromConfig(JsonObject jConfig) throws ConfigException {
		Map<String, Thing> things = new HashMap<>();

		/*
		 * read each Bridge in "things" array
		 */
		JsonArray jThings = JsonUtilities.getAsJsonArray(jConfig, "things");
		for (JsonElement jBridgeElement : jThings) {
			JsonObject jBridge = JsonUtilities.getAsJsonObject(jBridgeElement);
			String bridgeClass = JsonUtilities.getAsString(jBridge, "class");
			Bridge bridge = (Bridge) getThingInstance(bridgeClass);
			injectConfigParameters(bridge, jBridge, things);
			things.put(bridge.getThingId(), bridge);
			/*
			 * read each Device in "things" array
			 */
			List<Device> devices = new ArrayList<>();
			JsonArray jDevices = JsonUtilities.getAsJsonArray(jBridge, "devices");
			for (JsonElement jDeviceElement : jDevices) {
				JsonObject jDevice = JsonUtilities.getAsJsonObject(jDeviceElement);
				String deviceClass = JsonUtilities.getAsString(jDevice, "class");
				Device device = (Device) getThingInstance(deviceClass);
				injectConfigParameters(device, jDevice, things);
				devices.add(device);
				things.put(device.getThingId(), device);
			}
			bridge.setDevices(devices.stream().toArray(Device[]::new));
		}
		return things;
	}

	/**
	 * Nicely prints all {@link Thing}s to system output
	 *
	 * @param things
	 */
	public static void printThings(Map<String, Thing> things) {
		log.info("");
		log.info("Things:");
		log.info("--------");
		for (Entry<String, Thing> entry : things.entrySet()) {
			log.info("thingId: " + entry.getKey());
			log.info("thing:   " + entry.getValue());
		}
	}

	/**
	 * Creates an instance of the given {@link Class}. {@link Object} arguments are optional.
	 *
	 * Restriction: this implementation tries only the first constructor of the Class.
	 *
	 * @param clazz
	 * @param args
	 * @return
	 * @throws ConfigException
	 */
	private static Thing getThingInstance(Class<?> clazz, Object... args) throws ConfigException {
		try {
			if (args.length == 0) {
				return (Thing) clazz.newInstance();
			} else {
				Constructor<?> constructor = clazz.getConstructors()[0];
				return (Thing) constructor.newInstance(args);
			}
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
			throw new ConfigException("Unable to instantiate class [" + clazz.getName() + "]: " + e.getMessage());
		} catch (ClassCastException e) {
			e.printStackTrace();
			throw new ConfigException("Class [" + clazz.getName() + "] is not a Thing");
		}
	}

	/**
	 * Creates an instance of the given {@link Class}name. Uses {@link getThingInstance()} internally.
	 *
	 * @param className
	 * @return
	 * @throws ConfigException
	 */
	@SuppressWarnings("unchecked")
	private static Thing getThingInstance(String className) throws ConfigException {
		Class<? extends Thing> clazz;
		try {
			clazz = (Class<? extends Thing>) Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new ConfigException("Class not found: [" + className + "]");
		}
		return getThingInstance(clazz);
	}

	/**
	 * Searches the given {@link Thing} for methods annotated with {@link IsConfigParameter}. Finds matching entries in
	 * the given jConfig {@link JsonObject}. Calls the annotated method with the matching entry. See
	 * {@link getFromConfig()) for more information on the config format.
	 *
	 * If the method finds, that the method requires a {@link Thing} it will take it from the given {@link Thing}-map.
	 * If no Thing with matching thingId is found, a new Thing is created and added to the map.
	 *
	 * @param thing
	 * @param jConfig
	 * @param things
	 * @throws ConfigException
	 */
	private static void injectConfigParameters(Thing thing, JsonObject jConfig, Map<String, Thing> things)
			throws ConfigException {
		for (Method method : thing.getClass().getMethods()) {
			IsConfigParameter annotation = method.getAnnotation(IsConfigParameter.class);
			if (annotation != null) {
				// found valid annotation
				String configParameterName = annotation.value();
				// prepare method parameter type
				Class<?> paramType;
				{
					Parameter[] parameters = method.getParameters();
					if (parameters.length != 1) {
						throw new ConfigException("Invalid 'IsConfigParameter' method [" + method.getName()
								+ "] in class [" + thing.getClass() + "]");
					}
					paramType = parameters[0].getType();
				}
				Object parameter = null;
				if (jConfig.has(configParameterName) && jConfig.get(configParameterName).isJsonPrimitive()) {
					/**
					 * Parameter is a JsonPrimitive
					 */
					JsonPrimitive jConfigParameter = JsonUtilities.getAsPrimitive(jConfig, configParameterName);
					parameter = JsonUtilities.getJsonPrimitiveAsClass(jConfigParameter, paramType);

				} else {
					/**
					 * Parameter is NOT a JsonPrimitive -> create a matching Thing
					 */
					JsonObject jConfigParameter = JsonUtilities
							.getAsJsonObject(JsonUtilities.getSubElement(jConfig, configParameterName));
					String thingId = JsonUtilities.getAsString(jConfigParameter, "thingId");
					Thing newThing;
					if (things.containsKey(thingId)) {
						newThing = things.get(thingId);
					} else {
						newThing = getThingInstance(paramType, thingId);
					}
					things.put(thingId, newThing);
					// Recursive call to inject config parameters for the newly created Thing
					injectConfigParameters(newThing, jConfigParameter, things);
					parameter = newThing;
				}
				try {
					method.invoke(thing, parameter);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new ConfigException("Unable to call method [" + method.getName() + "] with parameter ["
							+ parameter + "]: " + e.getMessage());
				}
			}
		}
	}
}
