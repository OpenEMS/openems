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
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ed.edcom.Client;
import com.ed.edcom.ClientListener;
import com.ed.edcom.Discovery;
import com.ed.edcom.Util;

import io.openems.common.types.OpenemsType;
import io.openems.common.utils.InetAddressUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Kaco.BlueplanetHybrid10.Core", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class BpCoreImpl extends AbstractOpenemsComponent implements BpCore, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(BpCoreImpl.class);
	private final ScheduledExecutorService configExecutor = Executors.newSingleThreadScheduledExecutor();

	private Config config = null;
	private ScheduledFuture<?> configFuture = null;
	private Client client = null;
	private BpData _bpData = null;
	private StableVersion stableVersion;

	public BpCoreImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BpCore.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws UnknownHostException, SocketException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		/*
		 * Async initialize library and connection
		 */
		this.config = config;

		final InetAddress inverterAddress = InetAddressUtils.parseOrNull(config.ip().trim());
		Runnable initializeLibrary = () -> {
			while (true) {
				try {
					this.initialize(config, inverterAddress);
					break; // stop forever loop
				} catch (Exception e) {
					this.logError(this.log, e.getMessage());
					e.printStackTrace();
				}
				try {
					Thread.sleep(2000); // wait for next try
				} catch (InterruptedException e) {
					this.logError(this.log, e.getMessage());
				}
			}
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
			public byte[] updateIdentKey(byte[] randomKey) {
				var identKeyString = config.identkey();
				if (identKeyString.startsWith("0x")) {
					identKeyString = identKeyString.substring(2);
				}
				byte[] identKey = new byte[identKeyString.length() / 2];
				for (int i = 0; i < identKey.length; i++) {
					int index = i * 2;
					int j = Integer.parseInt(identKeyString.substring(index, index + 2), 16);
					identKey[i] = (byte) j;
				}

				final var len = 8;
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
	public BpData getBpData() {
		var client = this.client;
		if (client == null) {
			return null;
		}
		if (!client.isConnected()) {
			return null;
		}
		return this._bpData;
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

		// Initialize all DataSets
		this._bpData = BpData.from(this.client);

		this.client.start();

		// Available initialization response
		boolean availInitResponse;

		do {
			availInitResponse = true;

			// Get current software version
			Float comVersion = TypeUtils.getAsType(OpenemsType.FLOAT, this._bpData.systemInfo.getComVersion());
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

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.refreshBpData();
			this.updateChannels();
			break;
		}
	}

	private void refreshBpData() {
		var bpData = this.getBpData();
		if (bpData == null) {
			return;
		}
		bpData.refreshAll();
	}

	private void updateChannels() {
		String serialNumber = null;
		Float versionCom = null;

		var bpData = this.getBpData();
		if (bpData != null) {
			if (bpData.systemInfo != null) {
				serialNumber = bpData.systemInfo.getSerialNumber();
				if (serialNumber != null) {
					serialNumber = serialNumber.strip();
				}
				try {
					versionCom = Float.parseFloat(bpData.systemInfo.getComVersion());
				} catch (NumberFormatException e) {
					this.logWarn(this.log,
							"Unable to parse Com-Version from [" + bpData.systemInfo.getComVersion() + "]");
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
