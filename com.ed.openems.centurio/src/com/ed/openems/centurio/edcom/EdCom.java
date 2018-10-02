package com.ed.openems.centurio.edcom;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.jmdns.ServiceInfo;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ed.data.BatteryData;
import com.ed.data.EnergyMeter;
import com.ed.data.InverterData;
import com.ed.data.Settings;
import com.ed.data.Status;
import com.ed.data.VectisData;
import com.ed.edcom.Client;
import com.ed.edcom.Discovery;
import com.ed.edcom.Util;
import com.ed.openems.centurio.datasource.api.EdComData;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "EnergyDepot.EdCom", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "="
				+ EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE)
public class EdCom extends AbstractOpenemsComponent implements EdComData {

	private final Logger log = LoggerFactory.getLogger(EdCom.class);

	private final ScheduledExecutorService configExecutor = Executors.newSingleThreadScheduledExecutor();
	private ScheduledFuture<?> configFuture = null;

	private Client client = null;
	private BatteryData battery = null;
	private InverterData inverter = null;
	private Status status = null;
	private Settings settings = null;
	private VectisData vectis = null;
	private EnergyMeter energy = null;

	@Activate
	void activate(ComponentContext context, Config config) throws UnknownHostException, SocketException {
		super.activate(context, config.service_pid(), config.id(), config.enabled());

		/*
		 * Async initialize library and connection
		 */
		this.configFuture = configExecutor.schedule(() -> {
			// TODO Static instance? What happens if we have more than one instance of this
			// component? Is it really necessary as we set the userkey with setUserKey()
			// later?
			Util.getInstance().setUserName( //
					"K+JxgBxJPPzGuCZjznH35ggVlzY8NVV8Y9vZ8nU9k3RTiQBJxBcY8F0Umv3H2tCfCTpQTcZBDIZFd52Y54WvBojYm"
							+ "BxD84MoHXexNpr074zyhahFwppN+fZPXMIGaYTng0Mvv1XdYKdCMhh6xElc7eM3Q9e9JOWAbpD3eTX8L/yOVT8sVv"
							+ "n0q6oL4m2+pASNLHBFAVfRFjtNYVCIsjpnEEbsNN7OwO6IdokBV1qbbXbaWWljco/Sz3zD/l35atntDHwkyTG2Tpv"
							+ "Z1HWGBZVt39z17LxK8baCVIRw02/P6QjCStbnCPaVEEZquW/YpGrHRg5v8E3wlNx8U+Oy/TyIsA==");

			InetAddress inverterAddress = null;

			if (config.ip() != null) {
				/*
				 * IP address was set. No need for discovery.
				 */
				try {
					inverterAddress = InetAddress.getByName(config.ip());
				} catch (UnknownHostException e) {
					log.error(e.getMessage());
					e.printStackTrace();
					return;
				}

			} else {
				/*
				 * No IP address was set. Use discovery.
				 */
				Enumeration<NetworkInterface> ifaces;
				try {
					ifaces = NetworkInterface.getNetworkInterfaces();
				} catch (SocketException e) {
					log.error(e.getMessage());
					e.printStackTrace();
					return;
				}
				for (NetworkInterface iface : Collections.list(ifaces)) {
					// Initialize discovery
					InetAddress localAddress = iface.getInetAddresses().nextElement();
					log.info("Edcom start discovery on [" + iface.getDisplayName() + ", "
							+ localAddress.getHostAddress() + "]");
					Discovery discovery;
					try {
						discovery = Discovery.getInstance(localAddress);
					} catch (IOException e) {
						log.error(e.getMessage());
						e.printStackTrace();
						return;
					}

					// Start discovery
					ServiceInfo inverter = null;
					if (config.serialnumber() != null) {
						// Search by serialnumber if it was configured
						inverter = discovery.getBySerialNumber(config.serialnumber());
					} else {
						// Otherwise discover all and take first discovered inverter
						ServiceInfo[] inverterList = discovery.refreshInverterList();
						if (inverterList.length > 0) {
							inverter = inverterList[0];
						}
					}

					// Finalize discovery
					try {
						discovery.close();
					} catch (IOException e) {
						log.error(e.getMessage());
					}

					// Get inverterAddress
					InetAddress[] addresses = inverter.getInetAddresses();
					if (addresses.length > 0) {
						inverterAddress = addresses[0]; // use the first address
						break; // quit searching
					}
				}
			}

			// Initialize the Client
			if (inverterAddress != null) {
				InetAddress localAddress = EdCom.getMatchingLocalInetAddress(inverterAddress);
				try {
					this.client = new Client(inverterAddress, localAddress, 1);
				} catch (Exception e) {
					log.error(e.getMessage());
					e.printStackTrace();
					return;
				}
			}

			// Initialize all Data classes
			this.client.setUserKey(config.userkey());

			try {
				this.battery = new BatteryData();
				this.battery.registerData(client);
			} catch (Exception e) {
				log.error("Unable to initialize 'Battery': " + e.getMessage());
			}
			try {
				this.inverter = new InverterData();
				this.inverter.registerData(client);
			} catch (Exception e) {
				log.error("Unable to initialize 'Inverter': " + e.getMessage());
			}
			try {
				this.status = new Status();
				this.status.registerData(client);
			} catch (Exception e) {
				log.error("Unable to initialize 'Status': " + e.getMessage());
			}
			try {
				this.settings = new Settings();
				this.settings.registerData(client);
			} catch (Exception e) {
				log.error("Unable to initialize 'Settings': " + e.getMessage());
			}
			try {
				this.energy = new EnergyMeter();
				this.energy.registerData(client);
			} catch (Exception e) {
				log.error("Unable to initialize 'EnergyMeter': " + e.getMessage());
			}
			try {
				this.vectis = new VectisData();
				this.vectis.registerData(client);
			} catch (Exception e) {
				log.error("Unable to initialize 'Vectis': " + e.getMessage());
			}

			this.client.start();

		}, 0, TimeUnit.SECONDS);
	}

	@Deactivate
	protected void deactivate() {
		if (this.client != null) {
			try {
				this.client.close();
			} catch (IOException e) {
				log.error(e.getMessage());
				e.printStackTrace();
			}
		}
		super.deactivate();

		// Shutdown executor
		if (this.configExecutor != null) {
			this.configFuture.cancel(true);
		}
		try {
			configExecutor.shutdown();
			configExecutor.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			this.logWarn(this.log, "tasks interrupted");
		} finally {
			if (!configExecutor.isTerminated()) {
				this.logWarn(this.log, "cancel non-finished tasks");
			}
			configExecutor.shutdownNow();
		}
	}

	public BatteryData getBatteryData() {

		if (this.battery.dataReady()) {
			BatteryData data = this.battery;
			this.battery.refresh();
			return data;
		} else {
			return this.battery;
		}

	}

	@Override
	public InverterData getInverterData() {
		if (this.inverter.dataReady()) {
			InverterData data = this.inverter;
			this.inverter.refresh();
			return data;
		} else {
			return this.inverter;
		}
	}

	@Override
	public Status getStatusData() {
		if (this.status.dataReady()) {
			Status data = this.status;
			this.status.refresh();
			return data;
		} else {
			return this.status;
		}
	}

	@Override
	public boolean isConnected() {
		return this.client != null && this.client.isConnected();
	}

	@Override
	public Settings getSettings() {
		if (this.settings.dataReady()) {
			Settings data = this.settings;
			this.settings.refresh();
			return data;
		} else {
			return this.settings;
		}
	}

	@Override
	public VectisData getVectis() {
		if (this.vectis.dataReady()) {
			VectisData data = this.vectis;
			this.vectis.refresh();
			return data;
		} else {
			return this.vectis;
		}

	}

	@Override
	public EnergyMeter getEnergyMeter() {
		if (this.energy.dataReady()) {
			EnergyMeter data = this.energy;
			this.energy.refresh();
			return data;
		} else {
			return this.energy;
		}
	}

	/**
	 * Gets a local IP address which is able to access the given remote IP.
	 * 
	 * @param inetAddress
	 * @return a local IP address or null if no match was found
	 */
	public static InetAddress getMatchingLocalInetAddress(InetAddress inetAddress) {
		try (DatagramSocket socket = new DatagramSocket()) {
			socket.connect(inetAddress, 0);
			InetAddress localAddress = socket.getLocalAddress();
			if (localAddress.isAnyLocalAddress()) {
				return null;
			} else {
				return localAddress;
			}
		} catch (SocketException e) {
			return null;
		}
	}

}
