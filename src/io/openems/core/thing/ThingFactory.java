package io.openems.core.thing;

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

import io.openems.api.controller.Controller;
import io.openems.api.device.Device;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InjectionException;
import io.openems.api.thing.IsConfig;
import io.openems.api.thing.Thing;
import io.openems.core.bridge.Bridge;
import io.openems.core.databus.Databus;
import io.openems.core.scheduler.Scheduler;
import io.openems.core.utilities.InjectionUtils;
import io.openems.core.utilities.JsonUtils;

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
	 * @throws InjectionException
	 */
	public static Databus getFromConfig(JsonObject jConfig) throws ConfigException, InjectionException {
		Databus databus = new Databus();
		/*
		 * read each Bridge in "things" array
		 */
		JsonArray jThings = JsonUtils.getAsJsonArray(jConfig, "things");
		for (JsonElement jBridgeElement : jThings) {
			JsonObject jBridge = JsonUtils.getAsJsonObject(jBridgeElement);
			String bridgeClass = JsonUtils.getAsString(jBridge, "class");
			Bridge bridge = (Bridge) getThingInstance(bridgeClass);
			injectConfigParameters(bridge, jBridge, databus);
			databus.addThing(bridge.getThingId(), bridge);
			/*
			 * read each Device in "things" array
			 */
			List<Device> devices = new ArrayList<>();
			JsonArray jDevices = JsonUtils.getAsJsonArray(jBridge, "devices");
			for (JsonElement jDeviceElement : jDevices) {
				JsonObject jDevice = JsonUtils.getAsJsonObject(jDeviceElement);
				String deviceClass = JsonUtils.getAsString(jDevice, "class");
				Device device = (Device) getThingInstance(deviceClass);
				injectConfigParameters(device, jDevice, databus);
				devices.add(device);
				databus.addThing(device.getThingId(), device);
			}
			bridge.setDevices(devices.stream().toArray(Device[]::new));
		}

		/*
		 * read Scheduler
		 */
		JsonObject jScheduler = JsonUtils.getAsJsonObject(jConfig, "scheduler");
		String schedulerClass = JsonUtils.getAsString(jScheduler, "class");
		Scheduler scheduler = (Scheduler) getThingInstance(schedulerClass, databus);
		injectConfigParameters(scheduler, jScheduler, databus);
		databus.addThing(scheduler.getThingId(), scheduler);
		/*
		 * read each Controller in "controllers" array
		 */
		JsonArray jControllers = JsonUtils.getAsJsonArray(jScheduler, "controllers");
		for (JsonElement jControllerElement : jControllers) {
			JsonObject jController = JsonUtils.getAsJsonObject(jControllerElement);
			String controllerClass = JsonUtils.getAsString(jController, "class");
			Controller controller = (Controller) getThingInstance(controllerClass);
			injectConfigParameters(controller, jController, databus);
			databus.addThing(controller.getThingId(), controller);
			scheduler.addController(controller);
		}
		return databus;
	}

	/**
	 * Creates a Thing instance of the given {@link Class}. {@link Object} arguments are optional.
	 *
	 * @param clazz
	 * @param args
	 * @return
	 * @throws ConfigException
	 */
	public static Thing getThingInstance(Class<?> clazz, Object... args) throws ConfigException {
		try {
			return (Thing) InjectionUtils.getInstance(clazz, args);
		} catch (ClassCastException e) {
			e.printStackTrace();
			throw new ConfigException("Class [" + clazz.getName() + "] is not a Thing");
		}
	}

	/**
	 * Finds in a collection of Things all {@link Thing}s with matching type
	 *
	 * @param type
	 * @return
	 */
	public static Map<String, Thing> getThingsByClass(Databus databus, Class<? extends Thing> type) {
		Map<String, Thing> result = new HashMap<>();
		for (String thingId : databus.getThingIds()) {
			Thing thing = databus.getThing(thingId);
			if (type.isAssignableFrom(thing.getClass())) {
				result.put(thingId, thing);
			}
		}
		return result;
	}

	/**
	 * Nicely prints all {@link Thing}s to system output
	 *
	 * @param things
	 */
	public static void printThings(Map<String, Thing> things) {
		log.info("Things:");
		log.info("--------");
		for (Entry<String, Thing> entry : things.entrySet()) {
			log.info("Thing [" + entry.getKey() + "]: " + entry.getValue());
		}
	}

	/**
	 * Creates an instance of the given {@link Class}name. Uses {@link getThingInstance()} internally. {@link Object}
	 * arguments are optional.
	 *
	 * @param className
	 * @return
	 * @throws ConfigException
	 */
	@SuppressWarnings("unchecked")
	private static Thing getThingInstance(String className, Object... args) throws ConfigException {
		Class<? extends Thing> clazz;
		try {
			clazz = (Class<? extends Thing>) Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new ConfigException("Class not found: [" + className + "]");
		}
		return getThingInstance(clazz, args);
	}

	/**
	 * Searches the given {@link Thing} for methods annotated with {@link IsConfig}. Finds matching entries in
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
	private static void injectConfigParameters(Thing thing, JsonObject jConfig, Databus databus)
			throws InjectionException, ConfigException {
		for (Method method : thing.getClass().getMethods()) {
			if (method.isAnnotationPresent(IsConfig.class)) {
				@SuppressWarnings("null")
				IsConfig annotation = method.getAnnotation(IsConfig.class);
				// found valid annotation
				String configParameterName = annotation.value();
				// prepare method parameter type
				Class<?> paramType;
				{
					Parameter[] parameters = method.getParameters();
					if (parameters.length != 1) {
						throw new InjectionException("Invalid 'IsConfigParameter' method [" + method.getName()
								+ "] in class [" + thing.getClass() + "]");
					}
					paramType = parameters[0].getType();
				}
				Object parameter = null;
				if (jConfig.has(configParameterName) && jConfig.get(configParameterName).isJsonPrimitive()) {
					/**
					 * Parameter is a JsonPrimitive
					 */
					JsonPrimitive jConfigParameter = JsonUtils.getAsPrimitive(jConfig, configParameterName);
					parameter = JsonUtils.getJsonPrimitiveAsClass(jConfigParameter, paramType);

				} else {
					/**
					 * Parameter is NOT a JsonPrimitive -> create a matching Thing
					 */
					JsonObject jConfigParameter = JsonUtils
							.getAsJsonObject(JsonUtils.getSubElement(jConfig, configParameterName));
					String thingId = JsonUtils.getAsString(jConfigParameter, "thingId");
					Thing newThing = databus.getThing(thingId);
					if (newThing == null) {
						newThing = getThingInstance(paramType, thingId);
					}
					databus.addThing(thingId, newThing);
					// Recursive call to inject config parameters for the newly created Thing
					injectConfigParameters(newThing, jConfigParameter, databus);
					parameter = newThing;
				}
				try {
					method.invoke(thing, parameter);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new InjectionException("Unable to call method [" + method.getName() + "] with parameter ["
							+ parameter + "]: " + e.getMessage());
				}
			}
		}
	}
}
