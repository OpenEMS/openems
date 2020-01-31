package io.openems.edge.controller.kacoupdate;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.KacoUpdate", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class KacoUpdate extends AbstractOpenemsComponent implements Controller, OpenemsComponent {
	

	private final Logger log = LoggerFactory.getLogger(KacoUpdate.class);

	private Config config = null;
	private static final String URL = "https://www.energydepot.de/primus/update/";
	// private static final String LOCAL_FOLDER = "/usr/lib/hy-control/";
	private static final String LOCAL_FOLDER = "C:\\Users\\hummelsberger\\Documents\\";
	private static final String EDGE_FILE = "hy-control.jar";
	private static final String UI_FILE = "edge.zip";

	private int now = 0;
	private int lastchecked = 0;

	public KacoUpdate() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ThisChannelId.values()//
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());
		

	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		
		this.channel(ThisChannelId.HAS_EDGE_UPDATE).setNextValue(0);
		this.channel(ThisChannelId.HAS_UI_UPDATE).setNextValue(0);

		this.now = ZonedDateTime.now().getDayOfMonth();

		if (this.lastchecked != this.now) {

			this.logInfo(this.log, "Checking for Edge Update");
			try {
				if (checkUpdate(KacoUpdate.EDGE_FILE, this.getLastMod(KacoUpdate.EDGE_FILE))) {
					this.channel(ThisChannelId.HAS_EDGE_UPDATE).setNextValue(1);
					this.logInfo(this.log, "Edge Update Available!");
				} else {
					this.logInfo(this.log, "No Edge Update Available!");
				}
			} catch (IllegalArgumentException | IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();

			}

			this.logInfo(this.log, "Checking for UI Update");
			try {
				if (checkUpdate(KacoUpdate.UI_FILE, this.getLastMod(KacoUpdate.UI_FILE))) {
					this.channel(ThisChannelId.HAS_UI_UPDATE).setNextValue(1);
					this.logInfo(this.log, "UI Update Available!");
				} else {
					this.logInfo(this.log, "Edge Update Available!");
				}
			} catch (IllegalArgumentException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			this.lastchecked = this.now;
		}

	}

	private boolean checkUpdate(String target, Long lastmod) throws MalformedURLException, IOException {
		int responsecode;

		HttpURLConnection.setFollowRedirects(false);
		HttpURLConnection con = (HttpURLConnection) new URL(KacoUpdate.URL + target).openConnection();
		con.setRequestMethod("HEAD");

		con.setConnectTimeout(5000);
		con.setReadTimeout(5000);
		con.setIfModifiedSince(lastmod);
		responsecode = con.getResponseCode();
		con.disconnect();
		if (responsecode == 200) {
			return true;
		}

		return false;
	}

	private Long getLastMod(String filename) {

		File file = new File(KacoUpdate.LOCAL_FOLDER + filename);

		return file.lastModified();

	}

}
