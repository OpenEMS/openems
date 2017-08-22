package io.openems.backend.odoo.device;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abercap.odoo.OdooApiException;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;

import io.openems.backend.odoo.Odoo;

/**
 * Caches information about all available devices.
 *
 * @author stefan.feilmeier
 *
 */
public class DeviceCache {

	private final Logger log = LoggerFactory.getLogger(DeviceCache.class);
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	// 1st: database id; 2nd: name; 3rd: Device
	public Table<Integer, String, Device> devices = Tables.synchronizedTable(HashBasedTable.create());

	private Runnable refreshDeviceInfo = () -> {
		log.info("Refresh device information from Odoo...");
		try {
			for (Device device : Odoo.getInstance().getDeviceModel().readAllObjects()) {
				this.devices.put(device.getId(), device.getName(), device);
				// TODO replace only if updated + send event
			}
		} catch (XmlRpcException | OdooApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("Finished refresh device information from Odoo.");
	};

	public DeviceCache() {
		scheduler.scheduleWithFixedDelay(refreshDeviceInfo, 0, 5, TimeUnit.SECONDS); // TODO: minutes
	}
}
