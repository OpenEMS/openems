package io.openems.edge.core.host;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.CheckedConsumer;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.core.host.NetworkInterface.Inet4AddressWithNetmask;

public class NetworkConfiguration {

	private final static String BASE_PATH = "/etc/systemd/network";

	/**
	 * This default address is always added to eth0.
	 */
	private final static String DEFAULT_ETH0_ADDRESS = "192.168.100.100/24";

	private static enum Block {
		UNDEFINED, MATCH, NETWORK
	}

	private final static String PATTERN_INET4ADDRESS = "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

	private final static Pattern MATCH_NAME = Pattern.compile("^Name=(\\w+)$");
	private final static Pattern NETWORK_ADDRESS = Pattern.compile("^Address=(" + PATTERN_INET4ADDRESS + "/\\d+)$");
	private final static Pattern NETWORK_DHCP = Pattern.compile("^DHCP=(\\w+)$");
	private final static Pattern NETWORK_LINK_LOCAL_ADDRESSING = Pattern.compile("^LinkLocalAddressing=(\\w+)$");
	private final static Pattern NETWORK_GATEWAY = Pattern.compile("^Gateway=(" + PATTERN_INET4ADDRESS + ")$");
	private final static Pattern NETWORK_DNS = Pattern.compile("^DNS=(" + PATTERN_INET4ADDRESS + ")$");

	private static void onMatchString(Pattern pattern, String line, CheckedConsumer<String> callback)
			throws OpenemsNamedException {
		Matcher matcher = pattern.matcher(line);
		if (matcher.find() && matcher.groupCount() > 0) {
			callback.accept(matcher.group(1));
		}
	}

	private static void onMatchInet4Address(Pattern pattern, String line, CheckedConsumer<Inet4Address> callback)
			throws OpenemsNamedException {
		onMatchString(pattern, line, property -> {
			try {
				callback.accept((Inet4Address) Inet4Address.getByName(property));
			} catch (UnknownHostException e) {
				throw new OpenemsException("Unable to parse IPv4 address [" + property + "]: " + e.getMessage());
			}
		});
	}

	public static NetworkConfiguration getNetworkConfiguration() throws OpenemsNamedException {
		File path = Paths.get(BASE_PATH).toFile();
		if (!path.exists()) {
			throw new OpenemsException("Base-Path [" + path + "] does not exist.");
		}

		List<NetworkInterface> interfaces = new ArrayList<>();

		for (final File file : path.listFiles()) {
			/*
			 * Read all systemd network configuration files
			 */
			if (file.isDirectory()) {
				continue;
			}
			try {
				/*
				 * Parse the content of the network configuration file
				 */
				Block currentBlock = Block.UNDEFINED;
				AtomicReference<String> name = new AtomicReference<>();
				AtomicReference<Boolean> dhcp = new AtomicReference<>();
				AtomicReference<Boolean> linkLocalAddressing = new AtomicReference<>();
				AtomicReference<Inet4Address> gateway = new AtomicReference<>();
				AtomicReference<Inet4Address> dns = new AtomicReference<>();
				Set<Inet4AddressWithNetmask> addresses = new HashSet<Inet4AddressWithNetmask>();

				List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
				for (String line : lines) {
					/*
					 * Find current configuration block
					 */
					if (line.startsWith("[")) {
						switch (line) {
						case "[Match]":
							currentBlock = Block.MATCH;
							break;
						case "[Network]":
							currentBlock = Block.NETWORK;
							break;
						default:
							currentBlock = Block.UNDEFINED;
							break;
						}
						continue;
					}

					/*
					 * Parse Block
					 */
					switch (currentBlock) {
					case MATCH:
						onMatchString(MATCH_NAME, line, property -> {
							name.set(property);
						});
						break;
					case NETWORK:
						onMatchString(NETWORK_ADDRESS, line, property -> {
							addresses.add(Inet4AddressWithNetmask.fromString(property));
						});
						onMatchString(NETWORK_DHCP, line, property -> {
							dhcp.set(property.toLowerCase().equals("yes"));
						});
						onMatchString(NETWORK_LINK_LOCAL_ADDRESSING, line, property -> {
							linkLocalAddressing.set(property.toLowerCase().equals("yes"));
						});
						onMatchInet4Address(NETWORK_GATEWAY, line, property -> {
							gateway.set(property);
						});
						onMatchInet4Address(NETWORK_DNS, line, property -> {
							dns.set(property);
						});
						break;
					case UNDEFINED:
						break;
					}
				}

				// check for null value
				TypeUtils.assertNull("Network interface Name", name);

				// add default eth0 network address
				if (name.get().equals("eth0")) {
					addresses.add(Inet4AddressWithNetmask.fromString(DEFAULT_ETH0_ADDRESS));
				}

				// add to result
				interfaces.add(//
						new NetworkInterface(name.get(), dhcp.get(), linkLocalAddressing.get(), gateway.get(),
								dns.get(), addresses));

			} catch (IOException e) {
				throw new OpenemsException("Unable to read file [" + file + "]");
			}
		}

		return new NetworkConfiguration(interfaces);
	}

	private final List<NetworkInterface> interfaces;

	private NetworkConfiguration(List<NetworkInterface> interfaces) {
		this.interfaces = interfaces;
	}

	/**
	 * Return this NetworkConfiguration as a JSON object.
	 * 
	 * <p>
	 * 
	 * <pre>
	 * {
	 *   "interfaces": {
	 *     [name: string]: {
	 *       "dhcp": boolean,
	 *       "linkLocalAddressing": boolean,
	 *       "gateway": string,
	 *       "dns": string,
	 *       "addresses": string[]
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	public JsonObject toJson() {
		JsonObject interfaces = new JsonObject();
		for (NetworkInterface iface : this.interfaces) {
			interfaces.add(iface.getName(), iface.toJson());
		}
		return JsonUtils.buildJsonObject() //
				.add("interfaces", interfaces) //
				.build();
	}

}
