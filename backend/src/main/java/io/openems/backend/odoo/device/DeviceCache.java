package io.openems.backend.odoo.device;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abercap.odoo.OdooApiException;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

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
	public BiMap<String, Device> devices = Maps.synchronizedBiMap(HashBiMap.create());

	private Runnable refreshDeviceInfo = () -> {
		log.info("Refresh device information from Odoo...");
		try {
			for (Device device : Odoo.instance().getDeviceModel().readAllObjects()) {
				this.devices.put(device.getName(), device);
				// TODO replace only if updated + send event
			}
		} catch (XmlRpcException | OdooApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	};

	public DeviceCache() {
		scheduler.scheduleWithFixedDelay(refreshDeviceInfo, 0, 5, TimeUnit.MINUTES); // TODO: minutes
	}

	/**
	 * Returns the device for this ID from cache
	 *
	 * @param id
	 * @return
	 */
	public Optional<Device> getDeviceForName(String deviceName) {
		Device device = this.devices.get(deviceName);
		if (device == null) {
			return Optional.empty();
		}
		return Optional.of(device);
	}
}
