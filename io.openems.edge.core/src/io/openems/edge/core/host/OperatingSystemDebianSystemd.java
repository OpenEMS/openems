package io.openems.edge.core.host;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.openems.common.exceptions.CheckedConsumer;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ConfigurationProperty;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.core.host.NetworkInterface.Inet4AddressWithNetmask;

/**
 * OperatingSystem implementation for Debian with systemd.
 */
public class OperatingSystemDebianSystemd implements OperatingSystem {

	private static final String NETWORK_BASE_PATH = "/etc/systemd/network";

	private static enum Block {
		UNDEFINED, MATCH, NETWORK
	}

	private static final Pattern MATCH_NAME = Pattern.compile("^Name=(\\w+)$");
	private static final Pattern NETWORK_ADDRESS = Pattern
			.compile("^Address=(" + NetworkConfiguration.PATTERN_INET4ADDRESS + "/\\d+)$");
	private static final Pattern NETWORK_DHCP = Pattern.compile("^DHCP=(\\w+)$");
	private static final Pattern NETWORK_LINK_LOCAL_ADDRESSING = Pattern.compile("^LinkLocalAddressing=(\\w+)$");
	private static final Pattern NETWORK_GATEWAY = Pattern
			.compile("^Gateway=(" + NetworkConfiguration.PATTERN_INET4ADDRESS + ")$");
	private static final Pattern NETWORK_DNS = Pattern
			.compile("^DNS=(" + NetworkConfiguration.PATTERN_INET4ADDRESS + ")$");

	/**
	 * Gets the current network configuration for systemd-networkd.
	 * 
	 * @return the current network configuration
	 * @throws OpenemsException on error
	 */
	@Override
	public NetworkConfiguration getNetworkConfiguration() throws OpenemsNamedException {
		File path = Paths.get(NETWORK_BASE_PATH).toFile();
		if (!path.exists()) {
			throw new OpenemsException("Base-Path [" + path + "] does not exist.");
		}

		Map<String, NetworkInterface<?>> interfaces = new HashMap<>();

		for (final File file : path.listFiles()) {
			/*
			 * Read all systemd network configuration files
			 */
			if (file.isDirectory() || !file.getName().endsWith(".network")) {
				continue;
			}
			try {
				/*
				 * Parse the content of the network configuration file
				 */
				Block currentBlock = Block.UNDEFINED;
				final AtomicReference<String> name = new AtomicReference<>();
				final AtomicReference<ConfigurationProperty<Boolean>> dhcp = new AtomicReference<>(
						ConfigurationProperty.asNotSet());
				final AtomicReference<ConfigurationProperty<Boolean>> linkLocalAddressing = new AtomicReference<>(
						ConfigurationProperty.asNotSet());
				final AtomicReference<ConfigurationProperty<Inet4Address>> gateway = new AtomicReference<>(
						ConfigurationProperty.asNotSet());
				final AtomicReference<ConfigurationProperty<Inet4Address>> dns = new AtomicReference<>(
						ConfigurationProperty.asNotSet());
				final AtomicReference<ConfigurationProperty<Set<Inet4AddressWithNetmask>>> addresses = new AtomicReference<>(
						ConfigurationProperty.asNotSet());

				List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.US_ASCII);
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
							Set<Inet4AddressWithNetmask> content = addresses.get().getValue();
							if (content == null) {
								content = new HashSet<>();
							}
							content.add(Inet4AddressWithNetmask.fromString(property));
							addresses.set(ConfigurationProperty.of(content));
						});
						onMatchString(NETWORK_DHCP, line, property -> {
							dhcp.set(ConfigurationProperty.of(property.toLowerCase().equals("yes")));
						});
						onMatchString(NETWORK_LINK_LOCAL_ADDRESSING, line, property -> {
							linkLocalAddressing.set(ConfigurationProperty.of(property.toLowerCase().equals("yes")));
						});
						onMatchInet4Address(NETWORK_GATEWAY, line, property -> {
							gateway.set(ConfigurationProperty.of(property));
						});
						onMatchInet4Address(NETWORK_DNS, line, property -> {
							dns.set(ConfigurationProperty.of(property));
						});
						break;
					case UNDEFINED:
						break;
					}
				}

				// check for null value
				TypeUtils.assertNull("Network interface Name", name.get());

				// add to result
				interfaces.put(name.get(), new NetworkInterface<File>(name.get(), //
						dhcp.get(), linkLocalAddressing.get(), gateway.get(), dns.get(), addresses.get(), file));

			} catch (IOException e) {
				throw new OpenemsException("Unable to read file [" + file + "]");
			}
		}

		return new NetworkConfiguration(interfaces);
	}

	/**
	 * Handles a SetNetworkConfigRequest for systemd-networkd.
	 * 
	 * @param oldNetworkConfiguration the current/old network configuration
	 * @param request                 the JSON-RPC request
	 * @throws OpenemsException on error
	 */
	@Override
	public void handleSetNetworkConfigRequest(NetworkConfiguration oldNetworkConfiguration,
			SetNetworkConfigRequest request) throws OpenemsNamedException {
		boolean isChanged = false;
		List<NetworkInterface<?>> networkInterfaces = request.getNetworkInterface();
		for (NetworkInterface<?> networkInterface : networkInterfaces) {
			NetworkInterface<?> iface = oldNetworkConfiguration.getInterfaces().get(networkInterface.getName());
			if (iface == null) {
				throw new OpenemsException("No network interface with name [" + networkInterface.getName() + "]");
			}
			if (iface.updateFrom(networkInterface)) {
				isChanged = true;
			}
		}

		// stop early if there are no changes
		if (!isChanged) {
			throw new OpenemsException("Received no changes to network configuration");
		}

		// write configuration files
		IOException writeException = null;
		for (Entry<String, NetworkInterface<?>> entry : oldNetworkConfiguration.getInterfaces().entrySet()) {
			NetworkInterface<?> iface = entry.getValue();
			File file = (File) iface.getAttachment();
			List<String> lines = this.toFileFormat(iface);
			try {
				Files.write(file.toPath(), lines, StandardCharsets.US_ASCII);
			} catch (IOException e) {
				writeException = e;
			}
		}

		// did an exception happen while writing?
		if (writeException != null) {
			throw new OpenemsException("Unable to write file. " + writeException.getClass().getSimpleName() + ": "
					+ writeException.getMessage() + ". Network configuration might be inconsistent!");
		}

		// apply the configuration by restarting the systemd-networkd service
		this.executeCommand("", "systemctl restart systemd-networkd  --no-block", false, 10);
	}

	/**
	 * Helper function to match a String in the configuration file.
	 * 
	 * @param pattern  the regular expression pattern
	 * @param line     the line of the file
	 * @param callback the callback that should get called
	 * @throws OpenemsNamedException on error
	 */
	private static void onMatchString(Pattern pattern, String line, CheckedConsumer<String> callback)
			throws OpenemsNamedException {
		Matcher matcher = pattern.matcher(line);
		if (matcher.find() && matcher.groupCount() > 0) {
			callback.accept(matcher.group(1));
		}
	}

	/**
	 * Helper function to match an Inet4Address in the configuration file.
	 * 
	 * @param pattern  the regular expression pattern
	 * @param line     the line of the file
	 * @param callback the callback that should get called
	 * @throws OpenemsNamedException on error
	 */
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

	/**
	 * Converts the NetworkInterface object to systemd-networkd file format.
	 * 
	 * @param iface the input network interface configuration
	 * @return a list of strings for writing it to a file
	 */
	private List<String> toFileFormat(NetworkInterface<?> iface) {
		List<String> result = new ArrayList<>();
		result.add("[Match]");
		result.add("Name=" + iface.getName());
		result.add("");

		result.add("[Network]");
		if (iface.getDhcp().isSetAndNotNull()) {
			result.add("DHCP=" + (iface.getDhcp().getValue() ? "yes" : "no"));
		}
		if (iface.getLinkLocalAddressing().isSetAndNotNull()) {
			result.add("LinkLocalAddressing=" + (iface.getLinkLocalAddressing().getValue() ? "yes" : "no"));
		}
		if (iface.getGateway().isSetAndNotNull()) {
			result.add("Gateway=" + iface.getGateway().getValue().getHostAddress());
		}
		if (iface.getDns().isSetAndNotNull()) {
			result.add("DNS=" + iface.getDns().getValue().getHostAddress());
		}
		if (iface.getAddresses().isSetAndNotNull()) {
			for (Inet4AddressWithNetmask address : iface.getAddresses().getValue()) {
				result.add("Address=" + address.toString());
			}
		}
		return result;
	}

	/**
	 * Executes a command.
	 * 
	 * @param password       the system user password
	 * @param command        the command
	 * @param background     run the command in background (true) or in foreground
	 *                       (false)
	 * @param timeoutSeconds interrupt the command after ... seconds
	 * @return the output of the command
	 * @throws OpenemsException on error
	 */
	public String executeCommand(String password, String command, boolean background, int timeoutSeconds)
			throws OpenemsException {
		try {
			StringBuilder builder = new StringBuilder();
			Process proc = Runtime.getRuntime().exec(new String[] { "/bin/bash", "-c",
					"echo " + password + " | /usr/bin/sudo -Sk -p '' -- /bin/bash -c -- '" + command + "' 2>&1" });
			// TODO improve enforcement of password when already running as root
			// this complex argument tries to enforce the sudo password and to avoid
			// security vulnerabilites.

			// get stdout and stderr
			ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(2);
			Future<String> stdout = newFixedThreadPool.submit(new InputStreamToString(proc.getInputStream()));
			Future<String> stderr = newFixedThreadPool.submit(new InputStreamToString(proc.getErrorStream()));
			newFixedThreadPool.shutdown();

			if (background) {
				builder.append("Command [" + command + "] executed in background...\n");
				builder.append("Check system logs for more information.\n");

			} else {
				// apply command timeout
				if (!proc.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
					String error = "Command [" + command + "] timed out.";
					builder.append(error);
					builder.append("\n");
					builder.append("\n");
					proc.destroy();
				}

				builder.append("output:\n");
				builder.append("-------\n");
				builder.append(stdout.get(timeoutSeconds, TimeUnit.SECONDS));
				builder.append("\n");
				builder.append("error:\n");
				builder.append("------\n");
				builder.append(stderr.get(timeoutSeconds, TimeUnit.SECONDS));
			}
			return builder.toString();
		} catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
			throw new OpenemsException(
					"Execution of command failed. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	private static class InputStreamToString implements Callable<String> {
		private final InputStream stream;

		public InputStreamToString(InputStream stream) {
			this.stream = stream;
		}

		@Override
		public String call() {
			StringBuilder builder = new StringBuilder();
			BufferedReader reader = null;
			String line = null;
			try {
				reader = new BufferedReader(new InputStreamReader(stream));
				while ((line = reader.readLine()) != null) {
					builder.append(line);
					builder.append("\n");
				}
			} catch (IOException e) {
				builder.append(e.getMessage());
				builder.append("\n");
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						/* ignore */
					}
				}
			}
			return builder.toString();
		}
	}
}
