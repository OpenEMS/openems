package io.openems.edge.application;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class EdgeApp {

	private final Logger log = LoggerFactory.getLogger(EdgeApp.class);

	@Reference
	ConfigurationAdmin cm;

	@Activate
	void activate() {
		log.debug("Activate EdgeApp");

		// Example: Create new Scheduler
		//		new Thread(() -> {
		//			try {
		//				Thread.sleep(10000);
		//				System.out.println("Create config");
		//				Configuration config = cm.createFactoryConfiguration("Scheduler.FixedOrder", "?");
		//				Hashtable<String, Object> map = new Hashtable<>();
		//				map.put("id", "scheduler23");
		//				map.put("name", "HALLO WELT");
		//				config.update(map);
		//				System.out.println(config);
		//			} catch (Exception e) {
		//				e.printStackTrace();
		//			}
		//		}).start();

		// Example: Delete Scheduler
		//		new Thread(() -> {
		//			try {
		//				Thread.sleep(20000);
		//				System.out.println("Delete Config");
		//				Configuration[] cs = cm.listConfigurations("(id=scheduler23)");
		//				for (Configuration c : cs) {
		//					c.delete();
		//				}
		//			} catch (Exception e) {
		//				e.printStackTrace();
		//			}
		//		}).start();
	}

	@Deactivate
	void deactivate() {
		log.debug("Deactivate EdgeApp");
	}

}
