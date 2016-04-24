package de.fenecon.femscore.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import de.fenecon.femscore.modbus.ModbusConnection;
import de.fenecon.femscore.modbus.ModbusRtuConnection;
import de.fenecon.femscore.modbus.ModbusTcpConnection;
import de.fenecon.femscore.modbus.ModbusWorker;
import de.fenecon.femscore.modbus.device.counter.Counter;
import de.fenecon.femscore.modbus.device.counter.Socomec;
import de.fenecon.femscore.modbus.device.ess.Cess;
import de.fenecon.femscore.modbus.device.ess.Ess;

/**
 * ControllerFactory creates a {@link Controller} from a json configuration.
 * 
 * Example:
 * {
 *   "modbus": {
 *     "/dev/ttyUSB0": {
 *       "modbusType": "RTU",
 *       "baudrate": "38400",
 *       "serialinterface": "/dev/ttyUSB0",
 *       "databits": 8,
 *       "parity": "even",
 *       "stopbits": 1	
 *     },
 *     "192.168.1.88": {
 *       "modbusType": "TCP",
 *       "inetAddress": "192.168.1.88"
 *     }
 *   },
 *   "ess": {
 *     "cess0": {
 *       "essType": "CESS",
 *       "modbus": "192.168.1.88",
 *       "unitid": 100
 *     }
 *   },
 *   "counter": {
 *     "grid": {
 *       "modbus": "/dev/ttyUSB0",
 *       "unitid": 5
 *     }
 *   }
 * }
 * 
 * @author stefan.feilmeier
 *
 */
public class ControllerFactory {
	private static final Logger log = Logger.getLogger(ControllerFactory.class.getName());

	private final static File fileLin = new File("/etc/fems-core");
	private final static File fileWin = new File("D:/fems/fems-core/fems-core");
	
	public static Controller createControllerFromJson(JsonObject json) throws UnknownHostException {
		HashMap<String, ModbusWorker> modbusWorkers = getModbusConnections(json.get("modbus").getAsJsonObject());
		
		// Connect ModbusWorkers and EssDevices
		List<Ess> essDevices = getEssDevices(json.get("ess").getAsJsonObject());
		for(Ess ess : essDevices) {
			ModbusWorker worker = modbusWorkers.get(ess.getModbusid());
			worker.registerDevice(ess);
		}
		
		// Connect ModbusWorkers and CounterDevices
		List<Counter> counterDevices = getCounterDevices(json.get("counter").getAsJsonObject());
		for(Counter counter : counterDevices) {
			ModbusWorker worker = modbusWorkers.get(counter.getModbusid());
			worker.registerDevice(counter);
		}
		
		Controller controller = new Controller(modbusWorkers.values());
		return controller;
	}
	
	public static Controller createControllerFromConfigFile() throws JsonIOException, JsonSyntaxException, FileNotFoundException, UnknownHostException {
		JsonObject json = readConfigFile();
		return createControllerFromJson(json);
	}
	
	private static HashMap<String, ModbusWorker> getModbusConnections(JsonObject json) throws UnknownHostException {
		HashMap<String, ModbusWorker> modbusWorkers = new HashMap<String, ModbusWorker>();
		for(Entry<String, JsonElement> entry : json.entrySet()) {
			JsonObject obj = entry.getValue().getAsJsonObject();
			ModbusConnection modbusConnection = null;
			switch(obj.get("modbusType").getAsString()) {
			case "RTU":
				modbusConnection = new ModbusRtuConnection(
						obj.get("serialinterface").getAsString(),
						obj.get("baudrate").getAsString(),
						obj.get("databits").getAsInt(),
						obj.get("parity").getAsString(),
						obj.get("stopbits").getAsInt());
				break;
				
			case "TCP":
				modbusConnection = new ModbusTcpConnection(
						InetAddress.getByName(obj.get("inetAddress").getAsString()));
				break;
			
			default:
				throw new UnsupportedOperationException("ModbusType " + obj.get("modbusType").getAsString() + " is not implemented!");
			}
			
			modbusWorkers.put(entry.getKey(), new ModbusWorker(modbusConnection));
		}
		return modbusWorkers;
	}

	private static List<Ess> getEssDevices(JsonObject json) {
		List<Ess> essDevices = new ArrayList<Ess>();
		for(Entry<String, JsonElement> entry : json.entrySet()) {
			JsonObject obj = entry.getValue().getAsJsonObject();
			Ess ess = null;
			switch(obj.get("essType").getAsString()) {
			case "CESS":
				ess = new Cess(
						entry.getKey(),
						obj.get("modbus").getAsString(),
						obj.get("unitid").getAsInt());
				break;
			default:
				throw new UnsupportedOperationException("EssType " + obj.get("essType").getAsString() + " is not implemented!");
			}
			essDevices.add(ess);
		}
		return essDevices;
	}

	private static List<Counter> getCounterDevices(JsonObject json) {
		List<Counter> counterDevices = new ArrayList<Counter>();
		for(Entry<String, JsonElement> entry : json.entrySet()) {
			JsonObject obj = entry.getValue().getAsJsonObject();
			Counter counter = null;
			switch(obj.get("counterType").getAsString()) {
			case "Socomec":
				counter = new Socomec(
						entry.getKey(),
						obj.get("modbus").getAsString(),
						obj.get("unitid").getAsInt());
				break;
			default:
				throw new UnsupportedOperationException("CounterType " + obj.get("counterType").getAsString() + " is not implemented!");
			}
			counterDevices.add(counter);
		}
		return counterDevices;
	}

	
	private static JsonObject readConfigFile() throws JsonIOException, JsonSyntaxException, FileNotFoundException {
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
}
