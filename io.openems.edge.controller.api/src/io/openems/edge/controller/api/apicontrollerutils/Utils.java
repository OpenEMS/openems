package io.openems.edge.controller.api.apicontrollerutils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;

import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.edge.common.component.OpenemsComponent;

public class Utils {

	private final static Logger log = LoggerFactory.getLogger(Utils.class);

	/**
	 * Gets Meta information about active components.
	 * 
	 * @param components
	 * @return a JsonObject in the form
	 * 
	 *         <pre>
	 * {
	 *   "Controller.Symmetric.Balancing": {
	 *     "implements":[ "Controller" ]
	 *   },
	 *   "Simulator.EssSymmetric.Reacting": {
	 *     "implements":[ "Ess", "SymmetricEss" ]
	 *   }
	 * }
	 *         </pre>
	 */
	protected static JsonObject getComponentsMeta(List<OpenemsComponent> components) {
		JsonObject j = new JsonObject();
		components.stream() //
				// sort by Component ID
				.sorted((c1, c2) -> c1.id().compareTo(c2.id())) //
				.forEach(component -> {
					JsonObject jComponent = new JsonObject();

					JsonArray jImplements = new JsonArray();
					Arrays.stream(component.getClass().getInterfaces()) //
							// filter interesting Interfaces
							.filter(iface -> !(iface.equals(OpenemsComponent.class))
									&& OpenemsComponent.class.isAssignableFrom(iface))
							// sort by SimpleName
							.sorted((i1, i2) -> i1.getSimpleName().compareTo(i2.getSimpleName())) //
							.forEach(iface -> {
								jImplements.add(iface.getSimpleName());
							});
					jComponent.add("implements", jImplements);

					j.add(component.componentContext().getProperties().get("component.name").toString(), jComponent);
				});
		return j;
	}

	/**
	 * Get the configuration of active components.
	 * 
	 * @param configs
	 * @return a JsonObject in the form
	 * 
	 *         <pre>
	 * {
	 *   "ess0": {
	 *     "enabled": "true",
	 *     "modbus.id": "modbus0",
	 *     "service.factoryPid": "Ess.Fenecon.Commercial40",
	 *     "service.pid": "Ess.Fenecon.Commercial40.bcd5e8da-33c8-4258-ade5-480b5c0bbd2e"
	 * }
	 *         </pre>
	 */
	protected static JsonObject getComponents(Configuration[] configs) {
		JsonObject j = new JsonObject();
		for (Configuration config : configs) {
			Dictionary<String, Object> properties = config.getProperties();
			String id = (String) properties.get("id");
			if (id == null) {
				continue;
			}
			JsonObject jComponent = new JsonObject();
			Collections.list(properties.keys()).stream() //
					// filter interesting config properties
					.filter(key -> !key.equals("id") && !key.endsWith(".target")) //
					// sort by key (property name)
					.sorted() //
					.forEach(key -> {
						Object obj = properties.get(key);
						if (obj instanceof String[]) {
							jComponent.addProperty(key, String.join(",", (String[]) obj));
						} else {
							jComponent.add(key, toJson(obj));
						}
					});

			j.add(id, jComponent);
		}
		return j;
	}

	/**
	 * Converts an object to a JsonPrimitive
	 * 
	 * @param value
	 * @return
	 */
	private static JsonPrimitive toJson(Object value) {
		if (value instanceof Number) {
			return new JsonPrimitive((Number) value);
		} else if (value instanceof Boolean) {
			return new JsonPrimitive((Boolean) value);
		} else {
			return new JsonPrimitive(value.toString());
		}
	}

	protected static JsonObject toDeprecatedJsonConfig(Configuration[] configs) {
		JsonObject jResult = new JsonObject();
		for (Configuration config : configs) {
			String id = (String) config.getProperties().get("id");
			if (id == null) {
				continue;
			}
			JsonObject j = new JsonObject();
			j.addProperty("id", id);
			j.addProperty("alias", id);
			switch (config.getFactoryPid()) {
			case "Bridge.Modbus.Tcp":
				j.addProperty("class", "io.openems.impl.protocol.modbus.ModbusTcp");
				break;
			case "Bridge.Modbus.Serial":
				j.addProperty("class", "io.openems.impl.protocol.modbus.ModbusRtu");
				break;
			case "Ess.Fenecon.Commercial40":
				j.addProperty("class", "io.openems.impl.device.commercial.FeneconCommercialEss");
				j.addProperty("chargeSoc", 10);
				j.addProperty("minSoc", 15);
				break;
			case "EssDcCharger.Fenecon.Commercial40":
				j.addProperty("class", "io.openems.impl.device.commercial.FeneconCommercialCharger");
				j.addProperty("maxActualPower", 48000);
				break;
			case "Meter.SOCOMEC.DirisA14":
				j.addProperty("class", "io.openems.impl.device.socomec.SocomecMeter");
				j.addProperty("type", "grid"); // TODO set correct type
				j.addProperty("minActivePower", -40000); // set correct values
				j.addProperty("maxActivePower", 40000); // set correct values
				break;
			case "Evcs.Keba.KeContact":
				j.addProperty("class", "io.openems.impl.device.keba.KebaEvcs");
				break;
			case "Simulator.EssSymmetric.Reacting":
				j.addProperty("class", "io.openems.impl.device.simulator.SimulatorSymmetricEss");
				break;
			case "Simulator.GridMeter.Acting":
				j.addProperty("class", "io.openems.impl.device.simulator.SimulatorGridMeter");
				j.addProperty("type", "grid");
				break;
			case "Controller.Symmetric.Balancing":
				j.addProperty("class", "io.openems.impl.controller.symmetric.balancing.BalancingController");
				break;
			case "Controller.Debug.Log":
				j.addProperty("class", "io.openems.impl.controller.debuglog.DebugLogController");
				break;
			case "Controller.Api.Websocket":
				j.addProperty("class", "io.openems.impl.controller.api.websocket.WebsocketApiController");
				break;
			case "Simulator.Datasource.SLP":
			case "Scheduler.AllAlphabetically":
			case "Timedata.InfluxDB":
				// ignore
				continue;
			default:
				log.warn("FactoryPID [" + config.getFactoryPid() + "] has no Config converter");
				continue;
			}
			jResult.add(id, j);
		}
		return jResult;
	}
}
