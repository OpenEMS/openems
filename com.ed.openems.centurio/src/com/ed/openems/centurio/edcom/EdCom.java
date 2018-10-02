package com.ed.openems.centurio.edcom;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
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

	private InetAddress lHost = null;
	private Client cl = null;
	private BatteryData battery = null;
	private InverterData inverter = null;
	private Status status = null;
	private Settings settings = null;
	private ServiceInfo si = null;
	private Discovery nd = null;
	private VectisData vectis = null;
	private EnergyMeter energy = null;

	@Activate
	void activate(ComponentContext context, Config config) throws UnknownHostException, SocketException {
		super.activate(context, config.service_pid(), config.id(), config.enabled());

		this.lHost = InetAddress.getLocalHost();
		Enumeration<NetworkInterface> eni = NetworkInterface.getNetworkInterfaces();

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

			/* Init library */
			Util.getInstance().setUserName(
					"K+JxgBxJPPzGuCZjznH35ggVlzY8NVV8Y9vZ8nU9k3RTiQBJxBcY8F0Umv3H2tCfCTpQTcZBDIZFd52Y54WvBojYmBxD84MoHXexNpr074zyhahFwppN+fZPXMIGaYTng0Mvv1XdYKdCMhh6xElc7eM3Q9e9JOWAbpD3eTX8L/yOVT8sVvn0q6oL4m2+pASNLHBFAVfRFjtNYVCIsjpnEEbsNN7OwO6IdokBV1qbbXbaWWljco/Sz3zD/l35atntDHwkyTG2TpvZ1HWGBZVt39z17LxK8baCVIRw02/P6QjCStbnCPaVEEZquW/YpGrHRg5v8E3wlNx8U+Oy/TyIsA==");

			InetAddress inverterAddress = null;
			boolean found = false;

			while (found == false && eni.hasMoreElements()) {
				try {

					NetworkInterface ni = eni.nextElement();

					this.lHost = ni.getInetAddresses().nextElement();

					System.out.println("Edcom Interface: " + ni.getDisplayName() + ", " + this.lHost.getHostAddress());

					this.nd = Discovery.getInstance(this.lHost);
					if (!config.sn().trim().isEmpty() && config.sn() != null) {
						this.si = nd.getBySerialNumber(config.sn());
					}

					this.nd.close();
					if (this.si == null) {
						inverterAddress = InetAddress.getByName(config.ip());
					} else {
						inverterAddress = InetAddress.getByName(this.si.getHostAddress());
					}
					if (inverterAddress != null) {
						found = true;
					}

					this.cl = new Client(inverterAddress, this.lHost, 1);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			try {
				this.cl.setUserKey(config.uk());
				this.battery = new BatteryData();

				this.inverter = new InverterData();

				this.status = new Status();

				this.settings = new Settings();

				this.energy = new EnergyMeter();

				this.vectis = new VectisData();

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.battery.registerData(cl);
			this.inverter.registerData(cl);
			this.status.registerData(cl);
			this.settings.registerData(cl);
			this.vectis.registerData(cl);
			this.energy.registerData(cl);
			this.cl.start();
			if (this.cl.isConnected()) {

			} else {

			}

		}, 0, TimeUnit.SECONDS);
	}

	@Deactivate
	protected void deactivate() {
		try {
			this.cl.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		return this.cl != null && this.cl.isConnected();
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

}
