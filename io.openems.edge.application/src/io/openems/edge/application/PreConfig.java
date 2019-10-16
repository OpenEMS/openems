package io.openems.edge.application;

import java.io.IOException;
import java.util.Hashtable;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class PreConfig {

	protected static void initConfig(ConfigurationAdmin cm) {

		/*
		char[] password = { 'g', 'u', 'e', 's', 't' };

		try {
			SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
			byte[] salt = Base64.getDecoder().decode("dXNlcg==");

			PBEKeySpec spec = new PBEKeySpec(password, salt, 10, 256);
			SecretKey key = skf.generateSecret(spec);
			byte[] res = key.getEncoded();

			System.out.println("PASSWORT: " + Base64.getEncoder().encodeToString(res));

		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
		*/
		Configuration factory;

		Configuration[] configs;
		

		try {
			configs = cm.listConfigurations("(id=influx0)");

			if (configs == null || configs.length == 0) {
				factory = cm.createFactoryConfiguration("Timedata.InfluxDB", null);

				Hashtable<String, Object> influx = new Hashtable<>();
				influx.put("enabled", true);
				influx.put("database", "db");
				influx.put("id", "influx0");
				influx.put("alias", "");
				influx.put("ip", "localhost");
				influx.put("isReadOnly", false);
				influx.put("port", 8086);
				influx.put("retentionPolicy", "autogen");
				influx.put("username", "root");
				factory.update(influx);
			} else {
				System.out.println("Influx already active");
			}
		} catch (IOException | InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			configs = cm.listConfigurations("(service.pid=Core.User)");

			if (configs == null || configs.length == 0) {
				factory = cm.getConfiguration("Core.User", null);
				Hashtable<String, Object> coreuser = new Hashtable<>();
				factory.update(coreuser);
			}

		} catch (IOException | InvalidSyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		
		try {
			configs = cm.listConfigurations("(id=scheduler0)");

			if (configs == null || configs.length == 0) {
				factory = cm.createFactoryConfiguration("Scheduler.AllAlphabetically", null);

				Hashtable<String, Object> scheduler = new Hashtable<>();
				scheduler.put("enabled", true);
				scheduler.put("cycleTime", 1000);
				scheduler.put("id", "scheduler0");
				scheduler.put("alias", "");
				String[] ids = { "" };
				scheduler.put("controllers.ids", ids);
				factory.update(scheduler);
			} else {
				System.out.println("Scheduler already active");
			}
		} catch (IOException | InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			configs = cm.listConfigurations("(id=ctrlApiWebsocket0)");

			if (configs == null || configs.length == 0) {
				factory = cm.createFactoryConfiguration("Controller.Api.Websocket", null);

				Hashtable<String, Object> websocket = new Hashtable<>();
				websocket.put("enabled", true);
				websocket.put("apiTimeout", 60);
				websocket.put("id", "ctrlApiWebsocket0");
				websocket.put("alias", "");
				websocket.put("port", 8085);
				factory.update(websocket);
			} else {
				System.out.println("Websocket already active");
			}
		} catch (IOException | InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			configs = cm.listConfigurations("(id=ctrlDebugLog0)");

			if (configs == null || configs.length == 0) {
				factory = cm.createFactoryConfiguration("Controller.Debug.Log", null);
				Hashtable<String, Object> debug = new Hashtable<>();
				debug.put("enabled", true);
				debug.put("alias", "");
				debug.put("id", "ctrlDebugLog0");
				factory.update(debug);
			}
		} catch (IOException | InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		String userkey = "";

		try {
			configs = cm.listConfigurations("(&(id=kacoCore0)(service.factoryPid=Kaco.BlueplanetHybrid10.Core))");

			if (configs == null || configs.length == 0) {
				
				// Energy Depot Start

				Configuration[] oldconfigs = cm.listConfigurations("(service.factoryPid=EnergyDepot.EdCom)");

				if (oldconfigs != null) {

					Configuration edcom = oldconfigs[0];
					userkey = (String) edcom.getProperties().get("userkey");
					edcom.delete();
					
				}
				
				
				
				oldconfigs = cm.listConfigurations("(service.factoryPid=EnergyDepot.CenturioEss)");

				if (oldconfigs != null) {

					Configuration ess = oldconfigs[0];
					ess.delete();
					
				}

				oldconfigs = cm.listConfigurations("(service.factoryPid=EnergyDepot.CenturioPVMeter)");

				if (oldconfigs != null) {

					Configuration pvmeter = oldconfigs[0];
					pvmeter.delete();
					
				}
				
				oldconfigs = cm.listConfigurations("(service.factoryPid=EnergyDepot.Vectis)");

				if (oldconfigs != null) {

					Configuration vectis = oldconfigs[0];
					vectis.delete();
					
				}
				
				oldconfigs = cm.listConfigurations("(service.factoryPid=EnergyDepot.CenturioMeter)");

				if (oldconfigs != null) {

					Configuration gridmeter = oldconfigs[0];
					gridmeter.delete();
					
				}
				
				//Energy Depot End
				
				// Old Kaco Start
				oldconfigs = cm.listConfigurations("(service.factoryPid=KACO.bpCom)");

				if (oldconfigs != null) {

					Configuration bpCom = oldconfigs[0];
					userkey = (String) bpCom.getProperties().get("userkey");
					bpCom.delete();
					
				}
				
				oldconfigs = cm.listConfigurations("(service.factoryPid=KACO.bpEss)");

				if (oldconfigs != null) {

					Configuration bpEss = oldconfigs[0];
					bpEss.delete();
					
				}
				
				oldconfigs = cm.listConfigurations("(service.factoryPid=KACO.bpPVMeter)");

				if (oldconfigs != null) {

					Configuration bpPV = oldconfigs[0];
					bpPV.delete();
				}
				
				oldconfigs = cm.listConfigurations("(service.factoryPid=KACO.hy-switch)");

				if (oldconfigs != null) {

					Configuration hyswitch = oldconfigs[0];
					hyswitch.delete();
					
				}
				oldconfigs = cm.listConfigurations("(service.factoryPid=KACO.bpGridMeter)");

				if (oldconfigs != null) {

					Configuration bpgridMeter = oldconfigs[0];
					bpgridMeter.delete();
				}
				// old Kaco End
				

				// Create Kaco Core
				factory = cm.createFactoryConfiguration("Kaco.BlueplanetHybrid10.Core", null);

				Hashtable<String, Object> core = new Hashtable<>();
				core.put("enabled", true);
				core.put("serialnumber", "");
				core.put("id", "kacoCore0");
				core.put("alias", "");
				if(userkey == "") {
					userkey = "user";
				}
				core.put("userkey", userkey);
				core.put("master", true);
				factory.update(core);

				// Create ESS
				factory = cm.createFactoryConfiguration("Kaco.BlueplanetHybrid10.Ess", null);

				Hashtable<String, Object> ess = new Hashtable<>();
				ess.put("activateSurplusFeedIn", true);
				ess.put("alias", "Storage");
				ess.put("core.id", "kacoCore0");
				ess.put("enabled", true);
				ess.put("id", "ess0");
				ess.put("maxP", 3000);
				ess.put("readOnly", true);
				ess.put("selfRegulationDeactivated", false);

				factory.update(ess);

				// Create GridMeter
				factory = cm.createFactoryConfiguration("Kaco.BlueplanetHybrid10.GridMeter", null);

				Hashtable<String, Object> grid = new Hashtable<>();
				grid.put("enabled", true);
				grid.put("core.id", "kacoCore0");
				grid.put("id", "meter0");
				grid.put("alias", "Vectis");
				grid.put("external", false);
				factory.update(grid);
				
				
				// Create PVMeter
				factory = cm.createFactoryConfiguration("Kaco.BlueplanetHybrid10.PVMeter", null);

				Hashtable<String, Object> pv = new Hashtable<>();
				pv.put("enabled", true);
				pv.put("core.id", "kacoCore0");
				pv.put("id", "pv0");
				pv.put("alias", "PV");
				factory.update(pv);
				
				/*
				// Create Charger
				factory = cm.createFactoryConfiguration("Kaco.BlueplanetHybrid10.Charger", null);

				Hashtable<String, Object> charger = new Hashtable<>();
				charger.put("enabled", true);
				charger.put("core.id", "kacoCore0");
				charger.put("id", "charger0");
				charger.put("alias", "PV");
				factory.update(charger);
				 */
			} else {
				System.out.println("Kaco already active");
			}

		} catch (IOException | InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			configs = cm.listConfigurations("(id=ctrlDebugLog0)");

			if (configs == null || configs.length == 0) {
				factory = cm.createFactoryConfiguration("Controller.Debug.Log", null);
				Hashtable<String, Object> debug = new Hashtable<>();
				debug.put("enabled", true);
				debug.put("alias", "");
				debug.put("id", "ctrlDebugLog0");
				factory.update(debug);
			}
		} catch (IOException | InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}



}
