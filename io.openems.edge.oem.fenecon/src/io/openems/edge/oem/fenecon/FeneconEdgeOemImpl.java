package io.openems.edge.oem.fenecon;

import static io.openems.common.OpenemsConstants.VERSION_DEV_BRANCH;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.utils.StringUtils;

@Component
public class FeneconEdgeOemImpl implements OpenemsEdgeOem {

	private static final Logger LOG = LoggerFactory.getLogger(FeneconEdgeOemImpl.class);

	private final String manufacturerEmsSerialNumber;

	@Activate
	public FeneconEdgeOemImpl() {
		this.manufacturerEmsSerialNumber = readManufacturerEmsSerialNumber();
	}

	@Override
	public String getManufacturer() {
		return "FENECON GmbH";
	}

	@Override
	public String getManufacturerModel() {
		return "FEMS";
	}

	@Override
	public String getManufacturerOptions() {
		return "";
	}

	@Override
	public String getManufacturerVersion() {
		return "";
	}

	@Override
	public String getManufacturerSerialNumber() {
		return "";
	}

	@Override
	public String getManufacturerEmsSerialNumber() {
		return this.manufacturerEmsSerialNumber;
	}

	@Override
	public String getInfluxdbTag() {
		return "fems";
	}

	@Override
	public String getBackendApiUrl() {
		return "wss://www1.fenecon.de:443/openems-backend2";
	}

	@Override
	public SystemUpdateParams getSystemUpdateParams() {
		return getSystemUpdateParams(VERSION_DEV_BRANCH);
	}

	protected static SystemUpdateParams getSystemUpdateParams(String devBranch) {
		final String latestVersionUrl;
		final String updateScriptParams;
		if (devBranch == null || devBranch.isBlank()) {
			latestVersionUrl = "https://fenecon.de/fems-download/fems-latest.version";
			updateScriptParams = "";
		} else {
			latestVersionUrl = "https://dev.intranet.fenecon.de/" + devBranch + "/fems.version";
			updateScriptParams = "-fb \"" + devBranch + "\"";
		}

		return new SystemUpdateParams(//
				"fems", //
				latestVersionUrl, //
				"https://fenecon.de/fems-download/update-fems.sh", //
				updateScriptParams);
	}

	protected static String readManufacturerEmsSerialNumber() {
		String mesn = "";
		try (Scanner s = new Scanner(Runtime.getRuntime().exec("hostname").getInputStream())) {
			mesn = s.hasNext() ? s.next() : "";
		} catch (IOException ioe) {
			LOG.warn("Unable get hostname via OS-Command: " + ioe.getMessage());

			try {
				mesn = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException uhe) {
				LOG.error("Unable get hostname via DNS-Lookup: " + uhe.getMessage());
			}
		}
		return StringUtils.toShortString(mesn, 32);
	}

}
