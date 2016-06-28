package de.fenecon.femscore.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import de.fenecon.femscore.controller.BalancingWithAcGenerator;
import de.fenecon.femscore.controller.BalancingWithAcGeneratorInvertedCounter;
import de.fenecon.femscore.controller.BalancingWithoutAcGenerator;
import de.fenecon.femscore.controller.Controller;
import de.fenecon.femscore.controller.ControllerWorker;
import de.fenecon.femscore.modbus.ModbusConnection;
import de.fenecon.femscore.modbus.ModbusRtuConnection;
import de.fenecon.femscore.modbus.ModbusTcpConnection;
import de.fenecon.femscore.modbus.ModbusWorker;
import de.fenecon.femscore.modbus.device.ModbusDevice;
import de.fenecon.femscore.modbus.device.counter.Counter;
import de.fenecon.femscore.modbus.device.counter.Socomec;
import de.fenecon.femscore.modbus.device.ess.Cess;
import de.fenecon.femscore.modbus.device.ess.Ess;
import de.fenecon.femscore.monitoring.MonitoringWorker;
import de.fenecon.femscore.monitoring.fenecon.FeneconMonitoringWorker;

/**
 * Create a fems-core {@link Config} from json
 * 
 * @author stefan.feilmeier
 */
public class JsonConfigFactory {
	private static final Logger log = Logger.getLogger(JsonConfigFactory.class.getName());

	private final static File fileLin = new File("/etc/fems-core");
	private final static File fileWin = new File("D:/fems/fems-core/fems-core");

	public static Config readConfigFromJsonFile() throws Exception {
		JsonObject jsonConfig = readJsonFile();
		String devicekey = getDevicekey(jsonConfig.get("devicekey"));
		HashMap<String, ModbusWorker> modbuss = getModbusWorkers(jsonConfig.get("modbus"));
		HashMap<String, Ess> esss = getEsss(jsonConfig.get("ess"), modbuss);
		HashMap<String, Counter> counters = getCounters(jsonConfig.get("counter"), modbuss);
		HashMap<String, ControllerWorker> controllers = getControllerWorkers(jsonConfig.get("controller"), modbuss,
				esss, counters);

		Set<ModbusDevice> devices = new HashSet<>();
		devices.addAll(esss.values());
		devices.addAll(counters.values());
		HashMap<String, MonitoringWorker> monitorings = getMonitoringWorkers(jsonConfig.get("monitoring"), devicekey,
				devices);

		return new Config(devicekey, modbuss, esss, counters, controllers, monitorings);
	}

	private static JsonObject readJsonFile() throws JsonIOException, JsonSyntaxException, FileNotFoundException {
		File file = getConfigFile();
		log.log(Level.FINE, "Read configuration from " + file.getAbsolutePath());
		JsonParser parser = new JsonParser();
		JsonElement jsonElement = parser.parse(new FileReader(file));
		return jsonElement.getAsJsonObject();
	}

	private static File getConfigFile() {
		if (fileLin.exists()) {
			return fileLin;
		} else {
			return fileWin;
		}
	}

	/**
	 * Get unique devicekey from json config:
	 * 
	 * <pre>
	 * "devicekey": "Hhs49ZDzKuQK4ZxibFic"
	 * </pre>
	 * 
	 * @param jsonElement
	 * @return
	 * @throws Exception
	 */
	private static String getDevicekey(JsonElement jsonElement) throws Exception {
		String devicekey = null;
		if (jsonElement != null && jsonElement.isJsonPrimitive()) {
			devicekey = jsonElement.getAsString();
		}
		// TODO: if devicekey is still none: read hostname from device
		if (devicekey == null) {
			throw new Exception("Devicekey is mandatory!");
		}
		return devicekey;
	}

	/**
	 * Create {@link Ess}s from json config:
	 * 
	 * <pre>
	 * "ess": {
	 *   "cess0": {
	 *     "essType": "CESS",
	 *     "modbus": "192.168.1.88",
	 *     "unitid": 100
	 *   }
	 * },
	 * </pre>
	 * 
	 * @param jsonElement
	 * @param modbusWorkers
	 * @return
	 */
	private static HashMap<String, Ess> getEsss(JsonElement jsonElement, HashMap<String, ModbusWorker> modbusWorkers) {
		HashMap<String, Ess> esss = new HashMap<String, Ess>();
		if (jsonElement != null && jsonElement.isJsonObject()) {
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
				JsonObject obj = entry.getValue().getAsJsonObject();
				Ess ess = null;
				switch (obj.get("essType").getAsString()) {
				case "CESS":
					ess = new Cess(entry.getKey(), obj.get("modbus").getAsString(), obj.get("unitid").getAsInt());
					break;
				default:
					throw new UnsupportedOperationException(
							"EssType " + obj.get("essType").getAsString() + " is not implemented!");
				}
				esss.put(entry.getKey(), ess);
				// register to ModbusWorker
				ModbusWorker worker = modbusWorkers.get(ess.getModbusid());
				worker.registerDevice(ess);
			}
		}
		return esss;
	}

	/**
	 * Create {@link Counter}s from json config:
	 * 
	 * <pre>
	 * "counter": {
	 *   "grid": {
	 *     "counterType": "Socomec",
	 *     "modbus": "/dev/ttyUSB0",
	 *     "unitid": 5
	 *   }
	 * },
	 * </pre>
	 * 
	 * @param jsonElement
	 *            to be interpreted
	 * @param modbusWorkers
	 *            to be used by {@link Counter}s
	 * @return
	 */
	private static HashMap<String, Counter> getCounters(JsonElement jsonElement,
			HashMap<String, ModbusWorker> modbusWorkers) {
		HashMap<String, Counter> counters = new HashMap<String, Counter>();
		if (jsonElement != null && jsonElement.isJsonObject()) {
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
				JsonObject obj = entry.getValue().getAsJsonObject();
				Counter counter = null;
				switch (obj.get("counterType").getAsString()) {
				case "Socomec":
					counter = new Socomec(entry.getKey(), obj.get("modbus").getAsString(),
							obj.get("unitid").getAsInt());
					break;
				default:
					throw new UnsupportedOperationException(
							"CounterType " + obj.get("counterType").getAsString() + " is not implemented!");
				}
				counters.put(entry.getKey(), counter);
				// register to ModbusWorker
				ModbusWorker worker = modbusWorkers.get(counter.getModbusid());
				worker.registerDevice(counter);
			}
		}
		return counters;
	}

	/**
	 * Create {@link ModbusWorker}s from json config:
	 * 
	 * <pre>
	 * "modbus": {
	 *   "/dev/ttyUSB0": {
	 *     "modbusType": "RTU",
	 *     "baudrate": "38400",
	 *     "serialinterface": "/dev/ttyUSB0",
	 *     "databits": 8,
	 *     "parity": "even",
	 *     "stopbits": 1,
	 *     "cycle": 1000
	 *   },
	 *   "192.168.1.88": {
	 *     "modbusType": "TCP",
	 *     "inetAddress": "192.168.1.88",
	 *     "cycle": 1000
	 *   }
	 * }
	 * </pre>
	 * 
	 * @param jsonElement
	 * @return
	 * @throws UnknownHostException
	 */
	private static HashMap<String, ModbusWorker> getModbusWorkers(JsonElement jsonElement) throws UnknownHostException {
		HashMap<String, ModbusWorker> modbusWorkers = new HashMap<String, ModbusWorker>();
		if (jsonElement != null && jsonElement.isJsonObject()) {
			JsonObject jsonObject = jsonElement.getAsJsonObject();

			for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
				JsonObject obj = entry.getValue().getAsJsonObject();
				ModbusConnection modbusConnection = null;
				switch (obj.get("modbusType").getAsString()) {
				case "RTU":
					modbusConnection = new ModbusRtuConnection(obj.get("serialinterface").getAsString(),
							obj.get("baudrate").getAsString(), obj.get("databits").getAsInt(),
							obj.get("parity").getAsString(), obj.get("stopbits").getAsInt(),
							obj.get("cycle").getAsInt());
					break;

				case "TCP":
					modbusConnection = new ModbusTcpConnection(
							InetAddress.getByName(obj.get("inetAddress").getAsString()), obj.get("cycle").getAsInt());
					break;

				default:
					throw new UnsupportedOperationException(
							"ModbusType " + obj.get("modbusType").getAsString() + " is not implemented!");
				}

				modbusWorkers.put(entry.getKey(), new ModbusWorker(entry.getKey(), modbusConnection));
			}
		}
		return modbusWorkers;
	}

	/**
	 * Create a {@link Controller} from json config:
	 * 
	 * <pre>
	 * "controller": {
	 *   "controller0": {
	 *     "ess": [ 
	 *       "cess0"
	 *     ], 
	 *     "counter": [
	 *       "grid"
	 *     ], 
	 *     "strategy": [{
	 *       "implementation": "BalancingWithoutAcGenerator",
	 *       "minSoc": 10 
	 *     }] 
	 *   } 
	 * }
	 * </pre>
	 * 
	 * @param jsonElement
	 * @param esss
	 * @param counters
	 * @return
	 */
	private static HashMap<String, ControllerWorker> getControllerWorkers(JsonElement jsonElement,
			HashMap<String, ModbusWorker> modbusWorkers, HashMap<String, Ess> esss, HashMap<String, Counter> counters) {
		HashMap<String, ControllerWorker> controllerWorkers = new HashMap<String, ControllerWorker>();
		if (jsonElement != null && jsonElement.isJsonObject()) {
			JsonObject json = jsonElement.getAsJsonObject();
			for (Entry<String, JsonElement> jsonControllerElement : json.entrySet()) {
				JsonObject jsonController = jsonControllerElement.getValue().getAsJsonObject();
				// get esss
				HashMap<String, Ess> controllerEsss = new HashMap<String, Ess>();
				for (JsonElement ess : jsonController.get("ess").getAsJsonArray()) {
					controllerEsss.put(ess.getAsString(), esss.get(ess.getAsString()));
				}
				// get counters
				HashMap<String, Counter> controllerCounters = new HashMap<String, Counter>();
				for (JsonElement counter : jsonController.get("counter").getAsJsonArray()) {
					controllerCounters.put(counter.getAsString(), counters.get(counter.getAsString()));
				}
				// get controller
				JsonArray jsonControllerStrategyArray = jsonController.get("strategy").getAsJsonArray();
				for (int i = 0; i < jsonControllerStrategyArray.size(); i++) {
					// get controller name
					String name = jsonControllerElement.getKey();
					if (i > 0) {
						name += "-" + i;
					}
					JsonObject jsonControllerStrategy = jsonControllerStrategyArray.get(i).getAsJsonObject();

					Controller controller;
					switch (jsonControllerStrategy.get("implementation").getAsString()) {

					case "BalancingWithoutAcGenerator": {
						BalancingWithoutAcGenerator c = new BalancingWithoutAcGenerator(jsonControllerElement.getKey(),
								controllerEsss, controllerCounters);
						if (jsonControllerStrategy.has("minSoc")) {
							c.setMinSoc(jsonControllerStrategy.get("minSoc").getAsInt());
						}
						controller = c;
						break;
					}

					case "BalancingWithAcGenerator": {
						BalancingWithAcGenerator c = new BalancingWithAcGenerator(jsonControllerElement.getKey(),
								controllerEsss, controllerCounters);
						if (jsonControllerStrategy.has("minSoc")) {
							c.setMinSoc(jsonControllerStrategy.get("minSoc").getAsInt());
						}
						controller = c;
						break;
					}

					case "BalancingWithAcGeneratorInvertedCounter": {
						BalancingWithAcGeneratorInvertedCounter c = new BalancingWithAcGeneratorInvertedCounter(
								jsonControllerElement.getKey(), controllerEsss, controllerCounters);
						if (jsonControllerStrategy.has("minSoc")) {
							c.setMinSoc(jsonControllerStrategy.get("minSoc").getAsInt());
						}
						controller = c;
						break;
					}

					default:
						throw new UnsupportedOperationException("Controller strategy "
								+ jsonControllerStrategy.get("implementation").getAsString() + " is not implemented!");
					}

					controllerWorkers.put(name, new ControllerWorker(name, modbusWorkers.values(), controller));
				}
			}
		}
		return controllerWorkers;
	}

	/**
	 * Create {@link MonitoringWorker}s from json config:
	 * 
	 * <pre>
	 * "monitoring": {
	 *   "fenecon": {
	 *     "url": "...",
	 *     "enabled": false
	 *   }
	 * },
	 * </pre>
	 * 
	 * @param jsonElement
	 * @return
	 */
	private static HashMap<String, MonitoringWorker> getMonitoringWorkers(JsonElement jsonElement, String devicekey,
			Set<ModbusDevice> devices) {
		HashMap<String, MonitoringWorker> monitoringWorkers = new HashMap<String, MonitoringWorker>();
		// default monitoring
		FeneconMonitoringWorker feneconMonitoring = new FeneconMonitoringWorker(devicekey);
		for (ModbusDevice device : devices) { // add listener for all elements
			for (String elementName : device.getElements()) {
				device.getElement(elementName).addListener(feneconMonitoring);
			}
		}
		monitoringWorkers.put("fenecon", feneconMonitoring);

		if (jsonElement != null && jsonElement.isJsonObject()) {
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
				JsonObject obj = entry.getValue().getAsJsonObject();
				if (obj.has("enabled") && !obj.get("enabled").getAsBoolean()) {
					// remove if monitoring is not enabled but already existing
					// in the map per default
					if (monitoringWorkers.containsKey(entry.getKey())) {
						monitoringWorkers.remove(entry.getKey());
					}
				} else {
					// TODO implement other monitorings or changes from default
					// for fenecon
				}
			}
		}

		return monitoringWorkers;
	}
}