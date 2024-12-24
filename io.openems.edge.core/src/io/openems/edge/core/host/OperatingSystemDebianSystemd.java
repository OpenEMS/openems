package io.openems.edge.core.host;

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingConsumer;
import io.openems.common.types.ConfigurationProperty;
import io.openems.common.utils.InetAddressUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.StringUtils;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.common.user.User;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandRequest;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandRequest.SystemCommand;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandResponse;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandResponse.SystemCommandResponse;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemRestartRequest;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemRestartResponse;
import io.openems.edge.core.host.jsonrpc.SetNetworkConfigRequest;

/**
 * OperatingSystem implementation for Debian with systemd.
 */
public class OperatingSystemDebianSystemd implements OperatingSystem {

	private static final String NETWORK_BASE_PATH = "/etc/systemd/network";
	private static final Path UDEV_PATH = Paths.get("/etc/udev/rules.d/99-usb-serial.rules");
	private static final int DEFAULT_METRIC = 1024;
	private static final String MATCH_SECTION = "[Match]";
	private static final String NETWORK_SECTION = "[Network]";
	private static final String ROUTE_SECTION = "[Route]";
	private static final String DHCP_SECTION = "[DHCP]";
	private static final String ADDRESS_SECTION = "[Address]";
	private static final String EMPTY_SECTION = "";

	private static enum Block {
		UNDEFINED, MATCH, NETWORK, ADDRESS, ROUTE, DHCP
	}

	private final HostImpl parent;

	protected OperatingSystemDebianSystemd(HostImpl parent) {
		this.parent = parent;
	}

	@Override
	public NetworkConfiguration getNetworkConfiguration() throws OpenemsNamedException {
		var path = Paths.get(NETWORK_BASE_PATH).toFile();
		if (!path.exists()) {
			throw new OpenemsException("Base-Path [" + path + "] does not exist.");
		}

		var interfaces = new TreeMap<String, NetworkInterface<?>>();

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
				var lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
				NetworkInterface<File> networkInterface = parseSystemdNetworkdConfigurationFile(lines, file);

				// check for null value
				TypeUtils.assertNull("Network interface Name", networkInterface.getName());

				// add to result
				interfaces.put(networkInterface.getName(), networkInterface);

			} catch (IllegalArgumentException | IOException e) {
				throw new OpenemsException("Unable to read file [" + file + "]: " + e.getMessage());
			}
		}

		return new NetworkConfiguration(interfaces);
	}

	@Override
	public void handleSetNetworkConfigRequest(User user, NetworkConfiguration oldNetworkConfiguration,
			SetNetworkConfigRequest request) throws OpenemsNamedException {
		var isChanged = false;
		var networkInterfaces = request.getNetworkInterface();
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
			if (!networkInterfaces.stream().anyMatch(i -> i.getName().equals(entry.getKey()))) {
				continue;
			}
			NetworkInterface<?> iface = entry.getValue();
			var file = (File) iface.getAttachment();
			var lines = this.toFileFormat(user, iface);
			try {
				Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
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
		this.handleExecuteSystemCommandRequest(ExecuteSystemCommandRequest
				.runInBackgroundWithoutAuthentication("systemctl restart systemd-networkd --no-block"));
	}

	/**
	 * Helper function to match a String in the configuration file.
	 *
	 * @param pattern  the regular expression pattern
	 * @param line     the line of the file
	 * @param callback the callback that should get called
	 * @throws OpenemsNamedException on error
	 */
	private static void onMatchString(Pattern pattern, String line,
			ThrowingConsumer<String, OpenemsNamedException> callback) throws OpenemsNamedException {
		var matcher = pattern.matcher(line);
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
	private static void onMatchInet4Address(Pattern pattern, String line,
			ThrowingConsumer<Inet4Address, OpenemsNamedException> callback) throws OpenemsNamedException {
		onMatchString(pattern, line, property -> {
			callback.accept(InetAddressUtils.parseOrError(property));
		});
	}

	/**
	 * Converts the NetworkInterface object to systemd-networkd file format.
	 *
	 * @param user  the User
	 * @param iface the input network interface configuration
	 * @return a list of strings for writing it to a file
	 * @throws OpenemsNamedException on error
	 */
	private List<String> toFileFormat(User user, NetworkInterface<?> iface) throws OpenemsNamedException {
		List<String> result = new ArrayList<>();
		result.add("# changedBy: " //
				+ user.getName());
		result.add("# changedAt: " //
				+ LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).toString());

		// Match Section
		result.add(MATCH_SECTION);
		result.add("Name=" + iface.getName());
		result.add(EMPTY_SECTION);

		// Network Section
		result.add(NETWORK_SECTION);
		if (iface.getDhcp().isSetAndNotNull()) {
			result.add("DHCP=" + (iface.getDhcp().getValue() ? "yes" : "no"));
		}
		if (iface.getDns().isSetAndNotNull()) {
			result.add("DNS=" + iface.getDns().getValue().getHostAddress());
		}
		if (iface.getLinkLocalAddressing().isSetAndNotNull()) {
			result.add("LinkLocalAddressing=" + (iface.getLinkLocalAddressing().getValue() ? "yes" : "no"));
		}

		var metric = DEFAULT_METRIC;
		if (iface.getMetric().isSetAndNotNull()) {
			metric = iface.getMetric().getValue().intValue();
		}

		if (iface.getDhcp().isSetAndNotNull()) {
			var dhcp = iface.getDhcp().getValue();
			result.add(EMPTY_SECTION);
			if (dhcp) { // dhcp == yes
				result.add(DHCP_SECTION);
				result.add("RouteMetric=" + metric);
			} else {
				result.add(ROUTE_SECTION);
				if (iface.getGateway().isSetAndNotNull()) {
					result.add("Gateway=" + iface.getGateway().getValue().getHostAddress());
				}
				result.add("Metric=" + metric);
			}
		}
		if (iface.getAddresses().isSetAndNotNull()) {
			for (var address : iface.getAddresses().getValue()) {
				final var label = address.getLabel();
				result.add(EMPTY_SECTION);
				result.add(ADDRESS_SECTION);
				result.add("Address=" + address.toString());
				if (!label.isBlank()) {
					result.add("Label=" + label);
				}
			}
		}
		return result;
	}

	@Override
	public CompletableFuture<ExecuteSystemCommandResponse> handleExecuteSystemCommandRequest(
			ExecuteSystemCommandRequest request) {
		var result = new CompletableFuture<ExecuteSystemCommandResponse>();
		this.execute(request.systemCommand, //
				scr -> result.complete(new ExecuteSystemCommandResponse(request.id, scr)),
				e -> result.completeExceptionally(e));
		return result;
	}

	@Override
	public CompletableFuture<ExecuteSystemRestartResponse> handleExecuteSystemRestartRequest(
			ExecuteSystemRestartRequest request) {
		final var result = new CompletableFuture<ExecuteSystemRestartResponse>();
		var sc = new SystemCommand(//
				switch (request.type) { // actual command string
				case HARD -> "/usr/bin/systemctl reboot -i"; // "-i" is for "ignore inhibitors and users"
				case SOFT -> "/usr/bin/systemctl restart openems";
				}, //
				false, // runInBackground
				5, // timeoutSeconds
				Optional.empty(), // username
				Optional.empty()); // password
		this.execute(sc, //
				scr -> result.complete(new ExecuteSystemRestartResponse(request.id, scr)),
				e -> result.completeExceptionally(e));
		return result;
	}

	private void execute(SystemCommand sc, Consumer<SystemCommandResponse> scr, Consumer<Throwable> error) {
		try {
			final Process proc;
			if (sc.username().isPresent() && sc.password().isPresent()) {
				// Authenticate with user and password
				proc = getRuntime().exec(new String[] { //
						"/bin/bash", "-c", "--", //
						"echo " + sc.password().get() + " | " //
								+ " /usr/bin/sudo -Sk -p '' -u \"" + sc.username().get() + "\" -- " //
								+ sc.command() });
			} else if (sc.password().isPresent()) {
				// Authenticate with password (user must have 'sudo' permissions)
				proc = getRuntime().exec(new String[] { //
						"/bin/bash", "-c", "--", //
						"echo " + sc.password().get() + " | " //
								+ " /usr/bin/sudo -Sk -p '' -- " //
								+ sc.command() });
			} else {
				// No authentication: run as current user
				proc = getRuntime().exec(new String[] { //
						"/bin/bash", "-c", "--", sc.command() });
			}

			// get stdout and stderr
			var stdoutFuture = supplyAsync(new InputStreamToString(this.parent, sc.command(), proc.getInputStream()));
			var stderrFuture = supplyAsync(new InputStreamToString(this.parent, sc.command(), proc.getErrorStream()));

			if (sc.runInBackground()) {
				/*
				 * run in background
				 */
				var stdout = new String[] { //
						"Command [" + sc.command() + "] executed in background...", //
						"Check system logs for more information." };
				scr.accept(new SystemCommandResponse(stdout, new String[0], 0));

			} else {
				/*
				 * run in foreground with timeout
				 */
				runAsync(() -> {
					var stderr = new ArrayList<>();
					try {
						// apply command timeout
						if (!proc.waitFor(sc.timeoutSeconds(), TimeUnit.SECONDS)) {
							stderr.add("Command [" + sc.command() + "] timed out.");
							proc.destroy();
						}

						var stdout = stdoutFuture.get(1, TimeUnit.SECONDS);
						stderr.addAll(stderrFuture.get(1, TimeUnit.SECONDS));
						scr.accept(new SystemCommandResponse(//
								stdout.toArray(new String[stdout.size()]), //
								stderr.toArray(new String[stderr.size()]), //
								proc.exitValue() //
						));

					} catch (Throwable e) {
						error.accept(e);
					}
				});
			}
		} catch (IOException e) {
			error.accept(e);
		}
	}

	/**
	 * Asynchronously converts a InputStream to a String.
	 */
	private static class InputStreamToString implements Supplier<List<String>> {
		private final Logger log = LoggerFactory.getLogger(InputStreamToString.class);

		private final HostImpl parent;
		private final String command;
		private final InputStream stream;

		public InputStreamToString(HostImpl parent, String command, InputStream stream) {
			this.parent = parent;
			this.command = StringUtils.toShortString(command, 20);
			this.stream = stream;
		}

		@Override
		public List<String> get() {
			List<String> result = new ArrayList<>();
			BufferedReader reader = null;
			String line = null;
			try {
				reader = new BufferedReader(new InputStreamReader(this.stream));
				while ((line = reader.readLine()) != null) {
					result.add(line);
					this.parent.logInfo(this.log, "[" + this.command + "] " + line);
				}
			} catch (Throwable e) {
				result.add(e.getClass().getSimpleName() + ": " + line);
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						/* ignore */
					}
				}
			}
			return result;
		}
	}

	@Override
	public String getUsbConfiguration() throws OpenemsNamedException {
		try {
			if (!Files.exists(UDEV_PATH)) {
				return "";
			}
			var lines = Files.readAllLines(UDEV_PATH, StandardCharsets.UTF_8);
			return String.join("\n", lines);
		} catch (IOException e) {
			throw new OpenemsException("Unable to read file [" + UDEV_PATH + "]: " + e.getMessage());
		}
	}

	private static final Pattern MATCH_NAME = Pattern //
			.compile("^Name=([a-zA-Z0-9*]+)$");
	private static final Pattern ADDRESS_LABEL = Pattern //
			.compile("^Label=([a-zA-Z*]+)$");
	private static final Pattern NETWORK_ADDRESS = Pattern //
			.compile("^Address=(" + NetworkConfiguration.PATTERN_INET4ADDRESS + "/\\d+)$");
	private static final Pattern NETWORK_DHCP = Pattern //
			.compile("^DHCP=(\\w+)$");
	private static final Pattern NETWORK_LINK_LOCAL_ADDRESSING = Pattern //
			.compile("^LinkLocalAddressing=(\\w+)$");
	private static final Pattern NETWORK_GATEWAY = Pattern //
			.compile("^Gateway=(" + NetworkConfiguration.PATTERN_INET4ADDRESS + ")$");
	private static final Pattern NETWORK_DNS = Pattern //
			.compile("^DNS=(" + NetworkConfiguration.PATTERN_INET4ADDRESS + ")$");
	private static final Pattern GATEWAY_METRIC = Pattern //
			.compile("^Metric=([0-9]+)$");
	private static final Pattern ROUTE_METRIC = Pattern //
			.compile("^RouteMetric=([0-9]+)$");

	/**
	 * Parses a Systemd-Networkd configuration file.
	 *
	 * <p>
	 * See <a href=
	 * "https://man7.org/linux/man-pages/man5/systemd.network.5.html">systemd.network.5</a>
	 * man page
	 *
	 * @param <A>        the type of the attachment
	 * @param lines      the lines to parse
	 * @param attachment to be added as an attachment to the
	 *                   {@link NetworkInterface}
	 * @return a {@link NetworkInterface}
	 * @throws OpenemsNamedException on error
	 */
	protected static <A> NetworkInterface<A> parseSystemdNetworkdConfigurationFile(List<String> lines, A attachment)
			throws OpenemsNamedException {
		var currentBlock = Block.UNDEFINED;
		final var name = new AtomicReference<String>();
		final var dhcp = new AtomicReference<ConfigurationProperty<Boolean>>(//
				ConfigurationProperty.asNotSet());
		final var linkLocalAddressing = new AtomicReference<ConfigurationProperty<Boolean>>(//
				ConfigurationProperty.asNotSet());
		final var gateway = new AtomicReference<ConfigurationProperty<Inet4Address>>(//
				ConfigurationProperty.asNotSet());
		final var metric = new AtomicReference<ConfigurationProperty<Integer>>(//
				ConfigurationProperty.asNotSet());
		final var dns = new AtomicReference<ConfigurationProperty<Inet4Address>>(//
				ConfigurationProperty.asNotSet());
		final var addresses = new AtomicReference<ConfigurationProperty<Set<Inet4AddressWithSubnetmask>>>(//
				ConfigurationProperty.asNotSet());

		// holds the latest found address
		final var tmpAddress = new AtomicReference<Inet4AddressWithSubnetmask>();

		for (String line : lines) {
			line = line.trim();
			if (line.isBlank()) {
				continue;
			}

			/*
			 * Find current configuration block
			 */
			if (line.startsWith("[")) {
				switch (line) {
				case MATCH_SECTION:
					currentBlock = Block.MATCH;
					break;
				case NETWORK_SECTION:
					currentBlock = Block.NETWORK;
					break;
				case ADDRESS_SECTION:
					tmpAddress.set(null);
					currentBlock = Block.ADDRESS;
					break;
				case ROUTE_SECTION:
					currentBlock = Block.ROUTE;
					break;
				case DHCP_SECTION:
					currentBlock = Block.DHCP;
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
				onMatchString(NETWORK_ADDRESS, line, property -> {
					var addressDetails = addresses.get().getValue();
					if (addressDetails == null) {
						addressDetails = new HashSet<>();
					}
					addressDetails.add(Inet4AddressWithSubnetmask.fromString("" /* empty default label */, property));
					addresses.set(ConfigurationProperty.of(addressDetails));
				});
				break;
			case ADDRESS:
				onMatchString(NETWORK_ADDRESS, line, property -> {
					// Storing here temporarily so that we can use it if when we find label.
					var address = Inet4AddressWithSubnetmask.fromString("" /* empty default label */, property);
					tmpAddress.set(address);

					var addressDetails = addresses.get().getValue();
					if (addressDetails == null) {
						addressDetails = new HashSet<>();
					}
					// Add it with empty label now, later replace with label if we find one.
					addressDetails.add(address);
					addresses.set(ConfigurationProperty.of(addressDetails));
				});
				onMatchString(ADDRESS_LABEL, line, property -> {
					// IP address contains Only static labels or with no labels.
					var addressDetails = addresses.get().getValue();
					var address = tmpAddress.get();
					if (addressDetails == null || address == null) {
						// ignore label
						return;
					}

					// Replace the value with static or any other label in future.
					addressDetails.remove(address);
					address = new Inet4AddressWithSubnetmask(property, address.getInet4Address(),
							address.getSubnetmaskAsCidr());
					addressDetails.add(address);
				});
				break;
			case ROUTE:
				onMatchInet4Address(NETWORK_GATEWAY, line, property -> {
					gateway.set(ConfigurationProperty.of(property));
				});
				onMatchString(GATEWAY_METRIC, line, property -> {
					metric.set(ConfigurationProperty.of(Integer.parseInt(property)));
				});
				break;
			case DHCP:
				onMatchString(ROUTE_METRIC, line, property -> {
					metric.set(ConfigurationProperty.of(Integer.parseInt(property)));
				});
				break;
			case UNDEFINED:
				break;
			default:
				break;
			}
		}
		return new NetworkInterface<>(name.get(), //
				dhcp.get(), linkLocalAddressing.get(), gateway.get(), dns.get(), addresses.get(), metric.get(),
				attachment);
	}

	@Override
	public List<Inet4Address> getSystemIPs() throws OpenemsNamedException {
		var req = ExecuteSystemCommandRequest.withoutAuthentication("ip -j -4 address show", false, 5);
		try {
			var result = this.handleExecuteSystemCommandRequest(req).get().getResult().toString();
			return parseIpJson(result);
		} catch (InterruptedException | ExecutionException e) {
			return Collections.emptyList();
		}

	}

	/**
	 * Parses the json returned by ip address get command.
	 * 
	 * @param result the json to be parsed
	 * @return a list of parsed ips
	 * @throws OpenemsNamedException on error
	 */
	protected static List<Inet4Address> parseIpJson(String result) throws OpenemsNamedException {
		final var stdout = JsonUtils.getAsJsonArray(JsonUtils.getAsJsonObject(JsonUtils.parse(result)), "stdout");
		final var networkData = stdout.get(0).getAsString();
		final var networkDataJson = JsonUtils.parseOptional(networkData);
		if (networkDataJson.isPresent() && networkDataJson.get().isJsonArray()) {
			final var networkInterfaces = JsonUtils.getAsJsonArray(JsonUtils.parse(networkData));

			if (networkData.startsWith("[")) {
				return networkInterfaces.asList().stream().map(JsonElement::getAsJsonObject)
						.map(interfaceObject -> interfaceObject.getAsJsonArray("addr_info"))
						.flatMap(addrInfoArray -> StreamSupport.stream(addrInfoArray.spliterator(), false))
						.map(JsonElement::getAsJsonObject)
						.filter(addrInfoObject -> "inet".equals(addrInfoObject.get("family").getAsString()))
						.map(addrInfoObject -> addrInfoObject.get("local").getAsString()) //
						.<Inet4Address>mapMulti((t, u) -> {
							try {
								u.accept((Inet4Address) Inet4Address.getByName(t));
							} catch (UnknownHostException e) {
								// do nothing
							}
						}) //
						.toList();//
			}
		}
		return Collections.emptyList();
	}

	@Override
	public CompletableFuture<String> getOperatingSystemVersion() {
		final var sc = new SystemCommand(//
				"cat /etc/os-release", //
				false, // runInBackground
				5, // timeoutSeconds
				Optional.empty(), // username
				Optional.empty()); // password

		final var versionFuture = new CompletableFuture<String>();
		this.execute(sc, success -> {
			final var osVersionName = Stream.of(success.stdout()) //
					.map(t -> t.split("=", 2)) //
					.filter(t -> t.length == 2) //
					.filter(t -> t[0].equals("PRETTY_NAME")) //
					.map(t -> t[1]) //
					.map(t -> {
						if (t.startsWith("\"") && t.endsWith("\"")) {
							return t.substring(1, t.length() - 1);
						}
						return t;
					}) //
					.findAny();

			osVersionName.ifPresentOrElse(versionFuture::complete, () -> versionFuture
					.completeExceptionally(new OpenemsException("OS-Version name not found in /etc/os-release")));
		}, versionFuture::completeExceptionally);
		return versionFuture;
	}

}
