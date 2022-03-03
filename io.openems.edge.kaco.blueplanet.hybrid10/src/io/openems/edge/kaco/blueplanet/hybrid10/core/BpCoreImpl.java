package io.openems.edge.kaco.blueplanet.hybrid10.core;

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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.kaco.blueplanet.hybrid10.edcom.BatteryData;
import io.openems.edge.kaco.blueplanet.hybrid10.edcom.Client;
import io.openems.edge.kaco.blueplanet.hybrid10.edcom.ClientListener;
import io.openems.edge.kaco.blueplanet.hybrid10.edcom.Discovery;
import io.openems.edge.kaco.blueplanet.hybrid10.edcom.EnergyMeter;
import io.openems.edge.kaco.blueplanet.hybrid10.edcom.InverterData;
import io.openems.edge.kaco.blueplanet.hybrid10.edcom.Settings;
import io.openems.edge.kaco.blueplanet.hybrid10.edcom.Status;
import io.openems.edge.kaco.blueplanet.hybrid10.edcom.SystemInfo;
import io.openems.edge.kaco.blueplanet.hybrid10.edcom.Util;
import io.openems.edge.kaco.blueplanet.hybrid10.edcom.VectisData;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Kaco.BlueplanetHybrid10.Core", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		})
public class BpCoreImpl extends AbstractOpenemsComponent implements BpCore, OpenemsComponent, EventHandler {

	private static final byte[] IDENT_KEY = { (byte) 0xbd, (byte) 0xdb, (byte) 0x2f, (byte) 0x76, (byte) 0xe4,
			(byte) 0x7c, (byte) 0xf6, (byte) 0xe7 };

	private final Logger log = LoggerFactory.getLogger(BpCoreImpl.class);
	private final ScheduledExecutorService configExecutor = Executors.newSingleThreadScheduledExecutor();

	private Config config = null;
	private ScheduledFuture<?> configFuture = null;
	private Client client = null;
	private BatteryData battery = null;
	private InverterData inverter = null;
	private Status status = null;
	private Settings settings = null;
	private VectisData vectis = null;
	private EnergyMeter energy = null;
	private SystemInfo systemInfo = null;

	private StableVersion stableVersion;

	public BpCoreImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BpCore.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws UnknownHostException, SocketException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		// System.setProperty("java.net.preferIPv4Stack" , "true");

		/*
		 * Async initialize library and connection
		 */
		this.config = config;

		final InetAddress inverterAddress;
		if (config.ip() == null) {
			inverterAddress = null;
		} else {
			inverterAddress = InetAddress.getByName(config.ip());
		}
		Runnable initializeLibrary = () -> {
			while (true) {
				try {
					this.initialize(config, inverterAddress);
					break; // stop forever loop
				} catch (Exception e) {
					this.logError(this.log, e.getMessage());
					this._setCommunicationFailed(true);
					e.printStackTrace();
				}
				try {
					Thread.sleep(2000); // wait for next try
				} catch (InterruptedException e) {
					this.logError(this.log, e.getMessage());
				}
			}
			this._setCommunicationFailed(false);
		};
		this.configFuture = this.configExecutor.schedule(initializeLibrary, 0, TimeUnit.SECONDS);
	}

	private void initialize(Config config, InetAddress inverterAddress) throws Exception {
		Util util = Util.getInstance();

		/*
		 * Init and listener must be set at the beginning for edcom library > 8. There
		 * is no possibility to separate between the kaco versions before
		 * this.client.isConnected() is called.
		 */
		util.init();

		util.setListener(new ClientListener() {
			public byte[] updateIdentKey(byte[] param1ArrayOfbyte) {
				return BpCoreImpl.encryptIdentKey(param1ArrayOfbyte, BpCoreImpl.IDENT_KEY, 8);
			}
		});

		if (inverterAddress != null) {
			/*
			 * IP address was set. No need for discovery.
			 */
			this.logInfo(this.log, "Kaco core was configured with static ip: " + inverterAddress.getHostAddress());
		} else {
			/*
			 * No IP address was set. Use discovery.
			 */
			Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
			for (NetworkInterface iface : Collections.list(ifaces)) {
				// Initialize discovery

				InetAddress localAddress = null;
				Enumeration<InetAddress> localAddresses = iface.getInetAddresses();
				while (localAddresses.hasMoreElements()) {
					localAddress = localAddresses.nextElement();
					this.logInfo(this.log, "Edcom start discovery on [" + iface.getDisplayName() + ", "
							+ localAddress.getHostAddress() + "]");
					Discovery discovery = Discovery.getInstance(localAddress);

					// Start discovery
					ServiceInfo inverter = null;
					if (this.config.serialnumber() != null) {
						// Search by serialnumber if it was configured
						inverter = discovery.getBySerialNumber(this.config.serialnumber());
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
						this.logWarn(this.log, e.getMessage());
					}

					// Get inverterAddress
					if (inverter != null) {
						InetAddress[] addresses = inverter.getInetAddresses();
						if (addresses.length > 0) {
							inverterAddress = addresses[0]; // use the first address
							this.logInfo(this.log, "Found inverter: " + inverterAddress.toString());
							break; // quit searching
						}
					}
				}

				if (inverterAddress != null) {
					break;
				}
			}
		}

		if (inverterAddress != null) {
			this.initClient(config, inverterAddress);
		}
	}

	@Deactivate
	protected void deactivate() {
		if (this.client != null) {
			try {
				this.client.close();
			} catch (IOException e) {
				this.logError(this.log, e.getMessage());
				e.printStackTrace();
			}
		}

		// Shutdown executor
		if (this.configExecutor != null) {
			this.configFuture.cancel(true);
		}
		try {
			this.configExecutor.shutdown();
			this.configExecutor.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			this.logWarn(this.log, "tasks interrupted");
		} finally {
			if (!this.configExecutor.isTerminated()) {
				this.logWarn(this.log, "cancel non-finished tasks");
			}
			this.configExecutor.shutdownNow();
		}
		super.deactivate();
	}

	@Override
	public BatteryData getBatteryData() {
		if (!this.isConnected()) {
			return null;
		}
		if (this.battery != null && this.battery.dataReady()) {
			this.battery.refresh();
		}
		return this.battery;
	}

	@Override
	public InverterData getInverterData() {
		if (!this.isConnected()) {
			return null;
		}
		if (this.inverter != null && this.inverter.dataReady()) {
			this.inverter.refresh();
		}
		return this.inverter;
	}

	@Override
	public Status getStatusData() {
		if (!this.isConnected()) {
			return null;
		}
		if (this.status != null && this.status.dataReady()) {
			this.status.refresh();
		}
		return this.status;
	}

	@Override
	public boolean isConnected() {
		boolean isConnected = this.client != null && this.client.isConnected();
		this._setCommunicationFailed(!isConnected);
		return isConnected;
	}

	@Override
	public Settings getSettings() {
		if (!this.isConnected()) {
			return null;
		}
		if (this.settings != null && this.settings.dataReady()) {
			this.settings.refresh();
		}
		return this.settings;
	}

	@Override
	public VectisData getVectis() {
		if (!this.isConnected()) {
			return null;
		}
		if (this.vectis != null && this.vectis.dataReady()) {
			this.vectis.refresh();
		}
		return this.vectis;
	}

	@Override
	public EnergyMeter getEnergyMeter() {
		if (!this.isConnected()) {
			return null;
		}
		if (this.energy != null && this.energy.dataReady()) {
			this.energy.refresh();
		}
		return this.energy;
	}

	@Override
	public SystemInfo getSystemInfo() {
		if (!this.isConnected()) {
			return null;
		}
		if (this.systemInfo != null && this.systemInfo.dataReady()) {
			this.systemInfo.refresh();
		}
		return this.systemInfo;
	}

	@Override
	public StableVersion getStableVersion() {
		return this.stableVersion;
	}

	@Override
	public boolean isDefaultUser() {
		if (this.config != null && this.config.userkey() != null && this.config.userkey().equals("user")) {
			return true;
		}
		return false;
	}

	/**
	 * Gets a local IP address which is able to access the given remote IP.
	 * 
	 * @param inetAddress the remote IP
	 * @return a local IP address or null if no match was found
	 */
	public static InetAddress getMatchingLocalInetAddress(InetAddress inetAddress) {
		try (DatagramSocket socket = new DatagramSocket()) {
			socket.connect(inetAddress, 9760);
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

	private void initClient(Config config, InetAddress inverterAddress) throws Exception {
		// Initialize the Client
		InetAddress localAddress = BpCoreImpl.getMatchingLocalInetAddress(inverterAddress);
		this.client = new Client(inverterAddress, localAddress, 1);

		// Set user password
		this.client.setUserPass(this.config.userkey());

		// Initialize all Data classes
		this.battery = new BatteryData();
		this.battery.registerData(this.client);
		this.inverter = new InverterData();
		this.inverter.registerData(this.client);
		this.status = new Status();
		this.status.registerData(this.client);
		this.settings = new Settings();
		this.settings.registerData(this.client);
		this.energy = new EnergyMeter();
		this.energy.registerData(this.client);
		this.vectis = new VectisData();
		this.vectis.registerData(this.client);
		this.systemInfo = new SystemInfo();
		this.systemInfo.registerData(this.client);

		this.client.start();

		// Available initialization response
		boolean availInitResponse;

		do {
			availInitResponse = true;

			// Get current software version
			Float comVersion = TypeUtils.getAsType(OpenemsType.FLOAT, this.systemInfo.getComVersion());
			this.stableVersion = StableVersion.getCurrentStableVersion(comVersion);

			switch (this.stableVersion) {
			case VERSION_7_OR_OLDER:
				availInitResponse = this.initVersion7();
				break;
			case UNDEFINED:
			case VERSION_8:
				availInitResponse = this.initVersion8();
				break;
			}

			if (!availInitResponse) {
				Thread.sleep(1000); // try again after 1 second
			}

		} while (!availInitResponse);
	}

	/**
	 * Initialization of an older kaco version.
	 * 
	 * <p>
	 * This is necessary because accessFeedb always returns 0 for older versions.
	 * 
	 * @return boolean if there was a response of the initialization.
	 */
	@SuppressWarnings("deprecation")
	private boolean initVersion7() {
		// Old access feedback
		int userStatus = this.client.getUserStatus();

		/*
		 * Deprecated authentication. If this is used, the password must be 'user'. If
		 * this is not used, EnergyDepot will be returned.
		 */
		// Util.getInstance().setUserName( //
		// "K+JxgBxJPPzGuCZjznH35ggVlzY8NVV8Y9vZ8nU9k3RTiQBJxBcY8F0Umv3H2tCfCTpQTcZBDIZFd52Y54WvBojYm"
		// +
		// "BxD84MoHXexNpr074zyhahFwppN+fZPXMIGaYTng0Mvv1XdYKdCMhh6xElc7eM3Q9e9JOWAbpD3eTX8L/yOVT8sVv"
		// +
		// "n0q6oL4m2+pASNLHBFAVfRFjtNYVCIsjpnEEbsNN7OwO6IdokBV1qbbXbaWWljco/Sz3zD/l35atntDHwkyTG2Tpv"
		// +
		// "Z1HWGBZVt39z17LxK8baCVIRw02/P6QjCStbnCPaVEEZquW/YpGrHRg5v8E3wlNx8U+Oy/TyIsA==");

		// Set USER_ACCESS_DENIED Fault-Channel
		this._setUserAccessDenied(userStatus == 0);

		switch (userStatus) {
		case -1: // not read
			return false;
		case 0: // access denied
			this._setUserAccessDenied(true);
			this.logWarn(this.log, "User Status: Access denied");
			break;
		case 1: // no password required
			this._setUserAccessDenied(false);
			this.logInfo(this.log, "User Status: No password required");
			break;
		case 2: // password accepted
			this._setUserAccessDenied(false);
			this.logInfo(this.log, "User Status: Password accepted");
			break;
		case 3: // energy depot
			this._setUserAccessDenied(false);
			this.logInfo(this.log, "User Status: EnergyDepot");
			break;
		}

		return true;
	}

	/**
	 * Initialization of a kaco version greater or equals 8.
	 * 
	 * @return boolean if there was a response of the initialization.
	 */
	private boolean initVersion8() {
		/**
		 * Get accessFeedb (from edcom package 8) Bit0 = Bootloader active; Bit1 =
		 * identKey accepted; Bit2 = userKey accepted
		 */
		byte accessFeedb = this.client.getAccessFeedb();

		// Bootloader
		if (this.accessBitTest(accessFeedb, 0) || accessFeedb == 0) {
			this.channel(BpCore.ChannelId.MULTIPLE_ACCESS).setNextValue(false);
			return false;
		} else if (accessFeedb > 7) {
			// Kaco is read by multiple device. Its response is invalid
			this.channel(BpCore.ChannelId.MULTIPLE_ACCESS).setNextValue(true);
			return false;
		} else {
			this.channel(BpCore.ChannelId.MULTIPLE_ACCESS).setNextValue(false);
			// Ident key accepted
			if (this.accessBitTest(accessFeedb, 1)) {
				this.logInfo(this.log, "Access Status: Ident Key Accepted");
			} else {
				this.logInfo(this.log, "Access Status: Ident Key Reject");
			}

			// User key accepted
			if (this.accessBitTest(accessFeedb, 2)) {
				this._setUserAccessDenied(false);
				this.logInfo(this.log, "Access Status: User Key Accepted");
			} else {
				this._setUserAccessDenied(true);
				this.logInfo(this.log, "Access Status: User Key Reject");
			}
		}
		return true;
	}

	private static byte[] encryptIdentKey(byte[] randomKey, byte[] identKey, int len) {
		byte[] tmp = new byte[len];
		System.arraycopy(identKey, 0, tmp, 0, len);
		for (int i = 0; i < tmp.length && i < randomKey.length; i++) {
			tmp[i] += randomKey[i];
		}
		for (int i = 0; i < 99; i++) {
			tmp[i % len] += 1;
			tmp[i % len] += tmp[(i + 10) % len];
			tmp[(i + 3) % len] *= tmp[(i + 11) % len];
			tmp[i % len] += tmp[(i + 7) % len];
		}
		return tmp;
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.updateChannels();
			break;
		}
	}

	private void updateChannels() {
		String serialNumber = null;
		Float versionCom = null;

		if (this.isConnected()) {
			SystemInfo systemInfo = this.getSystemInfo();
			if (systemInfo != null) {
				serialNumber = systemInfo.getSerialNumber();
				if (serialNumber != null) {
					serialNumber = serialNumber.strip();
				}
				try {
					versionCom = Float.parseFloat(systemInfo.getComVersion());
				} catch (NumberFormatException e) {
					this.logWarn(this.log, "Unable to parse Com-Version from [" + systemInfo.getComVersion() + "]");
				}
			}
		}

		this._setSerialnumber(serialNumber);
		this._setVersionCom(versionCom);
	}

	private boolean accessBitTest(byte accessFeedb, int pos) {
		return (accessFeedb & (1 << pos)) != 0;
	}

}
