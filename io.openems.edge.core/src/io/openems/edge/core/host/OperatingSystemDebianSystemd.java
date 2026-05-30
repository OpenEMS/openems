package io.openems.edge.core.host;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.common.utils.InetAddressUtils.parseOrNull;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
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
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingConsumer;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.types.ConfigurationProperty;
import io.openems.common.utils.InetAddressUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.common.update.Updateable;
import io.openems.edge.common.user.User;
import io.openems.edge.core.host.Bash.Command;
import io.openems.edge.core.host.NetworkInterface.IpMasqueradeSetting;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandRequest;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandRequest.SystemCommand;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandResponse;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandResponse.SystemCommandResponse;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemRestartRequest;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemRestartResponse;
import io.openems.edge.core.host.jsonrpc.GetNetworkInfo;
import io.openems.edge.core.host.jsonrpc.GetNetworkInfo.NetworkInfoAddress;
import io.openems.edge.core.host.jsonrpc.GetNetworkInfo.NetworkInfoWrapper;
import io.openems.edge.core.host.jsonrpc.GetNetworkInfo.Route;
import io.openems.edge.core.host.jsonrpc.SetNetworkConfig;

/**
 * OperatingSystem implementation for Debian with systemd.
 */
public class OperatingSystemDebianSystemd implements OperatingSystem {

	private static final String NETWORK_BASE_PATH = "/etc/systemd/network";
	private static final Path UDEV_PATH = Paths.get("/etc/udev/rules.d/99-usb-serial.rules");
	private static final int DEFAULT_DHCP_ROUTE_METRIC = 1024;
	private static final String MATCH_SECTION = "[Match]";
	private static final String NETWORK_SECTION = "[Network]";
	private static final String ROUTE_SECTION = "[Route]";
	private static final String DHCP_SECTION = "[DHCP]";
	private static final String ADDRESS_SECTION = "[Address]";
	private static final String EMPTY_SECTION = "";

	private static final Logger log = LoggerFactory.getLogger(OperatingSystemDebianSystemd.class);

	private static enum Block {
		UNDEFINED, MATCH, NETWORK, ADDRESS, ROUTE, DHCP
	}

	protected OperatingSystemDebianSystemd() {
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
			SetNetworkConfig.Request request) throws OpenemsNamedException {
		var isChanged = false;
		var newInterfacesToCreate = new ArrayList<NetworkInterface<?>>();

		for (NetworkInterface<?> networkInterface : request.networkInterfaces()) {
			NetworkInterface<?> iface = oldNetworkConfiguration.getInterfaces().get(networkInterface.getName());

			// If interface doesn't exist, mark it as a new interface to create
			if (iface == null) {
				newInterfacesToCreate.add(networkInterface);
				isChanged = true;
				continue;
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

		// First create .network files for new interfaces
		for (NetworkInterface<?> newInterface : newInterfacesToCreate) {
			var fileName = newInterface.getName() + ".network";
			var file = new File(NETWORK_BASE_PATH, fileName);
			var lines = this.toFileFormat(user, newInterface);
			try {
				// Ensure the directory exists
				Files.createDirectories(Paths.get(NETWORK_BASE_PATH));
				Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

				// Add the new interface to oldNetworkConfiguration
				oldNetworkConfiguration.getInterfaces().put(//
						newInterface.getName(), //
						new NetworkInterface<>(//
								newInterface.getName(), //
								newInterface.getDhcp(), //
								newInterface.getLinkLocalAddressing(), //
								newInterface.getGateway(), //
								newInterface.getDns(), //
								newInterface.getAddresses(), //
								newInterface.getDhcpRouteMetric(), //
								newInterface.getIpv4Forwarding(), //
								newInterface.getIpMasquerade(), //
								newInterface.getDestination(), //
								newInterface.getGatewayOnLink(), //
								newInterface.getRoutes(), //
								file));

				log.info("Created new network interface configuration file: " + fileName);
			} catch (IOException e) {
				log.error("Failed to create network interface configuration file: " + fileName, e);
				writeException = e;
			}
		}

		// Update configuration files for existing interfaces
		for (Entry<String, NetworkInterface<?>> entry : oldNetworkConfiguration.getInterfaces().entrySet()) {
			if (request.networkInterfaces().stream().noneMatch(i -> i.getName().equals(entry.getKey()))) {
				continue;
			}

			// Skip newly created interfaces
			if (newInterfacesToCreate.stream().anyMatch(ni -> ni.getName().equals(entry.getKey()))) {
				continue;
			}

			NetworkInterface<?> iface = entry.getValue();
			var file = (File) iface.getAttachment();

			if (file == null) {
				// Create file if it doesn't exist (fallback for old interfaces)
				var fileName = iface.getName() + ".network";
				file = new File(NETWORK_BASE_PATH, fileName);
				log.warn("Network interface file not found for " + iface.getName() + ", creating new file");
			}

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
		onMatchString(pattern, line, property -> callback.accept(InetAddressUtils.parseOrError(property)));
	}

	/**
	 * Converts the NetworkInterface object to systemd-networkd file format.
	 *
	 * @param user  the User
	 * @param iface the input network interface configuration
	 * @return a list of strings for writing it to a file
	 * @throws OpenemsNamedException on error
	 */
	private List<String> toFileFormat(User user, NetworkInterface<?> iface) {
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
		if (iface.getGateway().isSetAndNotNull()) {
			result.add("Gateway=" + iface.getGateway().getValue().getHostAddress());
		}
		if (iface.getGatewayOnLink().isSetAndNotNull()) {
			result.add("GatewayOnLink=" + (iface.getGatewayOnLink().getValue() ? "yes" : "no"));
		}
		if (iface.getLinkLocalAddressing().isSetAndNotNull()) {
			result.add("LinkLocalAddressing=" + (iface.getLinkLocalAddressing().getValue() ? "yes" : "no"));
		}
		if (iface.getIpv4Forwarding().isSetAndNotNull()) {
			result.add("IPv4Forwarding=" + (iface.getIpv4Forwarding().getValue() ? "yes" : "no"));
		}
		if (iface.getIpMasquerade().isSetAndNotNull()) {
			result.add("IPMasquerade=" + iface.getIpMasquerade().getValue().settingValue);
		}

		var dhcpRouteMetric = DEFAULT_DHCP_ROUTE_METRIC;
		if (iface.getDhcpRouteMetric().isSetAndNotNull()) {
			dhcpRouteMetric = iface.getDhcpRouteMetric().getValue();
		}

		if (iface.getDhcp().isSetAndNotNull()) {
			final boolean dhcp = iface.getDhcp().getValue();
			result.add(EMPTY_SECTION);
			if (dhcp) { // dhcp == yes
				result.add(DHCP_SECTION);
				result.add("RouteMetric=" + dhcpRouteMetric);
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
		if (iface.getRoutes().isSet() && !iface.getRoutes().getValue().isEmpty()) {
			for (var route : iface.getRoutes().getValue()) {
				result.add(EMPTY_SECTION);
				result.add(ROUTE_SECTION);
				if (route.getRouteGateway() != null) {
					result.add("Gateway=" + route.getRouteGateway());
				}
				if (route.getRouteDestination() != null) {
					result.add("Destination=" + route.getRouteDestination());
				}
				if (route.isRouteGatewayOnLink()) {
					result.add("GatewayOnLink=yes");
				}
				if (route.geRouteMetric() != null) {
					result.add("Metric=" + route.geRouteMetric());
				}
			}
		}

		return result;
	}

	@Override
	public CompletableFuture<ExecuteSystemCommandResponse> handleExecuteSystemCommandRequest(
			ExecuteSystemCommandRequest request) {
		return execute(request.systemCommand).thenApply(cmd -> { //
			final var scr = new SystemCommandResponse(cmd.stdout(), cmd.stderr(), cmd.exitCode());
			return new ExecuteSystemCommandResponse(request.id, scr);
		});
	}

	@Override
	public CompletableFuture<ExecuteSystemRestartResponse> handleExecuteSystemRestartRequest(
			ExecuteSystemRestartRequest request) {
		var sc = new SystemCommand(//
				switch (request.type) { // actual command string
				case HARD -> "/usr/bin/systemctl reboot -i"; // "-i" is for "ignore inhibitors and users"
				case SOFT -> "/usr/bin/systemctl restart openems";
				}, //
				false, // runInBackground
				5, // timeoutSeconds
				Optional.empty(), // username
				Optional.empty()); // password
		return execute(sc).thenApply(cmd -> { //
			final var scr = new SystemCommandResponse(cmd.stdout(), cmd.stderr(), cmd.exitCode());
			return new ExecuteSystemRestartResponse(request.id, scr);
		});
	}

	private static CompletableFuture<Command> execute(SystemCommand sc) {
		return new Bash(sc.command()) //
				.withTimeout(sc.timeoutSeconds()) //
				.withSudo(sc.username().orElse(null), sc.password().orElse(null)) //
				.runInBackground(sc.runInBackground()) //
				.execute();
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
	private static final Pattern NETWORK_IPV4_FORWARDING = Pattern //
			.compile("^IPv4Forwarding=(\\w+)$");
	private static final Pattern NETWORK_IP_MASQUERADE = Pattern //
			.compile("^IPMasquerade=(\\w+)$");
	private static final Pattern ROUTE_METRIC = Pattern //
			.compile("^Metric=([0-9]+)$");
	private static final Pattern DHCP_METRIC = Pattern //
			.compile("^RouteMetric=([0-9]+)$");
	private static final Pattern NETWORK_DESTINATION = Pattern //
			.compile("^Destination=(" + NetworkConfiguration.PATTERN_INET4ADDRESS + "/\\d+)$");
	private static final Pattern NETWORK_GATEWAY_ON_LINK = Pattern.compile("^GatewayOnLink=(yes|no)$");
	private static final Pattern ROUTE_GATEWAY = Pattern //
			.compile("^Gateway=(" + NetworkConfiguration.PATTERN_INET4ADDRESS + ")$");
	private static final Pattern ROUTE_DESTINATION = Pattern //
			.compile("^Destination=(" + NetworkConfiguration.PATTERN_INET4ADDRESS + "/\\d+)$");
	private static final Pattern ROUTE_GATEWAY_ON_LINK = Pattern.compile("^GatewayOnLink=(yes|no)$");

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
		final var dhcpRouteMetric = new AtomicReference<ConfigurationProperty<Integer>>(//
				ConfigurationProperty.asNotSet());
		final var dns = new AtomicReference<ConfigurationProperty<Inet4Address>>(//
				ConfigurationProperty.asNotSet());
		final var addresses = new AtomicReference<ConfigurationProperty<Set<Inet4AddressWithSubnetmask>>>(//
				ConfigurationProperty.asNotSet());
		final var ipv4Forwarding = new AtomicReference<ConfigurationProperty<Boolean>>(//
				ConfigurationProperty.asNotSet());
		final var ipMasquerade = new AtomicReference<ConfigurationProperty<IpMasqueradeSetting>>(//
				ConfigurationProperty.asNotSet());
		final var destination = new AtomicReference<ConfigurationProperty<Set<Inet4AddressWithSubnetmask>>>(//
				ConfigurationProperty.asNotSet());
		final var gatewayOnLink = new AtomicReference<ConfigurationProperty<Boolean>>(//
				ConfigurationProperty.asNotSet());
		final var routes = new AtomicReference<ConfigurationProperty<Set<Routes>>>(//
				ConfigurationProperty.asNotSet());
		final var routeGateway = new AtomicReference<ConfigurationProperty<Inet4Address>>(//
				ConfigurationProperty.asNotSet());
		final var routeDestination = new AtomicReference<ConfigurationProperty<Set<Inet4AddressWithSubnetmask>>>(//
				ConfigurationProperty.asNotSet());
		final var routeGatewayOnLink = new AtomicReference<ConfigurationProperty<Boolean>>(//
				ConfigurationProperty.asNotSet());
		final var routeMetric = new AtomicReference<ConfigurationProperty<Integer>>(//
				ConfigurationProperty.asNotSet());

		final var allRoutes = new ArrayList<Routes>();

		// holds the latest found address
		final var tmpAddress = new AtomicReference<Inet4AddressWithSubnetmask>();

		for (var line : lines) {
			line = line.trim();
			if (line.isBlank()) {
				continue;
			}
			/*
			 * Find current configuration block
			 */
			if (line.startsWith("[")) {
				var previousBlock = currentBlock;
				if (previousBlock == Block.ROUTE) {
					addCurrentRouteToList(routeGateway, routeDestination, routeGatewayOnLink, routeMetric, allRoutes);
				}
				currentBlock = switch (line) {
				case MATCH_SECTION //
					-> Block.MATCH;
				case NETWORK_SECTION //
					-> Block.NETWORK;
				case ADDRESS_SECTION -> {
					tmpAddress.set(null);
					yield Block.ADDRESS;
				}
				case ROUTE_SECTION -> {
					routeGateway.set(ConfigurationProperty.asNotSet());
					routeDestination.set(ConfigurationProperty.asNotSet());
					routeGatewayOnLink.set(ConfigurationProperty.asNotSet());
					routeMetric.set(ConfigurationProperty.asNotSet());
					yield Block.ROUTE;
				}
				case DHCP_SECTION //
					-> Block.DHCP;
				default //
					-> Block.UNDEFINED;
				};
				continue;
			}

			/*
			 * Parse Block
			 */
			switch (currentBlock) {
			case MATCH -> onMatchString(MATCH_NAME, line, name::set);
			case NETWORK -> {
				onMatchString(NETWORK_DHCP, line, property -> {
					dhcp.set(ConfigurationProperty.of(property.equalsIgnoreCase("yes")));
				});
				onMatchString(NETWORK_LINK_LOCAL_ADDRESSING, line, property -> {
					linkLocalAddressing.set(ConfigurationProperty.of(property.equalsIgnoreCase("yes")));
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
				onMatchString(NETWORK_IPV4_FORWARDING, line, property -> {
					ipv4Forwarding.set(ConfigurationProperty.of(property.equalsIgnoreCase("yes")));
				});
				onMatchString(NETWORK_IP_MASQUERADE, line, property -> {
					ipMasquerade.set(ConfigurationProperty.of(IpMasqueradeSetting.findBySettingValue(property)));
				});
				onMatchString(NETWORK_DESTINATION, line, property -> {
					var destinations = destination.get().getValue();
					if (destinations == null) {
						destinations = new HashSet<>();
					}
					destinations.add(Inet4AddressWithSubnetmask.fromString("", property));
					destination.set(ConfigurationProperty.of(destinations));
				});
				onMatchString(NETWORK_GATEWAY_ON_LINK, line, property -> {
					gatewayOnLink.set(ConfigurationProperty.of(property.equalsIgnoreCase("yes")));
				});

			}
			case ADDRESS -> {
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
			}
			case ROUTE -> {
				onMatchInet4Address(ROUTE_GATEWAY, line, property -> {
					routeGateway.set(ConfigurationProperty.of(property));
				});
				onMatchString(ROUTE_DESTINATION, line, property -> {
					routeDestination
							.set(ConfigurationProperty.of(Set.of(Inet4AddressWithSubnetmask.fromString("", property))));
				});
				onMatchString(ROUTE_GATEWAY_ON_LINK, line, property -> {
					routeGatewayOnLink.set(ConfigurationProperty.of(property.equalsIgnoreCase("yes")));
				});
				onMatchString(ROUTE_METRIC, line, property -> {
					routeMetric.set(ConfigurationProperty.of(Integer.parseInt(property)));
				});
			}
			case DHCP -> {
				onMatchString(DHCP_METRIC, line, property -> {
					dhcpRouteMetric.set(ConfigurationProperty.of(Integer.parseInt(property)));
				});
			}
			case UNDEFINED -> doNothing();
			}
		}
		if (currentBlock == Block.ROUTE) {
			addCurrentRouteToList(routeGateway, routeDestination, routeGatewayOnLink, routeMetric, allRoutes);
		}
		if (!allRoutes.isEmpty()) {
			routes.set(ConfigurationProperty.of(new HashSet<>(allRoutes)));
		} else {
			routes.set(ConfigurationProperty.asNotSet());
		}

		return new NetworkInterface<>(name.get(), //
				dhcp.get(), //
				linkLocalAddressing.get(), //
				gateway.get(), //
				dns.get(), //
				addresses.get(), //
				dhcpRouteMetric.get(), //
				ipv4Forwarding.get(), //
				ipMasquerade.get(), //
				destination.get(), //
				gatewayOnLink.get(), //
				routes.get(), //
				attachment);

	}

	private static void addCurrentRouteToList(//
			AtomicReference<ConfigurationProperty<Inet4Address>> routeGateway, //
			AtomicReference<ConfigurationProperty<Set<Inet4AddressWithSubnetmask>>> routeDestination, //
			AtomicReference<ConfigurationProperty<Boolean>> routeGatewayOnLink, //
			AtomicReference<ConfigurationProperty<Integer>> routeMetric, //
			List<Routes> allRoutes//
	) {
		var gatewayString = routeGateway.get().isSetAndNotNull() ? routeGateway.get().getValue().getHostAddress()
				: null;
		var destinationString = routeDestination.get().isSetAndNotNull()
				? routeDestination.get().getValue().iterator().next().toString()
				: null;
		var gatewayOnLinkValue = routeGatewayOnLink.get().isSetAndNotNull() ? routeGatewayOnLink.get().getValue()
				: false;
		var metricValue = routeMetric.get().isSetAndNotNull() ? routeMetric.get().getValue() : null;
		var hasRouteData = gatewayString != null || destinationString != null || gatewayOnLinkValue
				|| metricValue != null;
		if (hasRouteData) {
			var routeBuilder = Routes.builder();

			if (gatewayString != null) {
				routeBuilder.setRouteGateway(gatewayString);
			}

			if (destinationString != null) {
				routeBuilder.setRouteDestination(destinationString);
			}

			routeBuilder.setRouteGatewayOnLink(gatewayOnLinkValue);

			if (metricValue != null) {
				routeBuilder.setRouteMetric(metricValue);
			}

			try {
				var builtRoute = routeBuilder.build();
				allRoutes.add(builtRoute);
			} catch (Exception e) {
				log.warn("Skipping invalid route due to: " + e);
			}
		}
	}

	@Override
	public List<Inet4Address> getSystemIPs() throws OpenemsNamedException {
		var reqIpShow = ExecuteSystemCommandRequest.withoutAuthentication("ip -j -4 address show", false, 5);
		try {
			var resultIpShow = this.handleExecuteSystemCommandRequest(reqIpShow).get().getResult().toString();
			return parseShowJson(resultIpShow).stream() //
					.flatMap(t -> t.ips().stream() //
							.map(d -> d.ip().getInet4Address())) //
					.toList();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Collections.emptyList();
		} catch (ExecutionException e) {
			return Collections.emptyList();
		}

	}

	@Override
	public GetNetworkInfo.Response getNetworkInfo() throws OpenemsNamedException {
		var reqIpShow = ExecuteSystemCommandRequest.withoutAuthentication("ip -j -4 address show", false, 5);
		var reqIpRoute = ExecuteSystemCommandRequest.withoutAuthentication("ip -j route", false, 5);
		try {
			var resultIpShow = this.handleExecuteSystemCommandRequest(reqIpShow).get().getResult().toString();
			var resultIpRoute = this.handleExecuteSystemCommandRequest(reqIpRoute).get().getResult().toString();
			return new GetNetworkInfo.Response(parseShowJson(resultIpShow), parseRouteJson(resultIpRoute));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return new GetNetworkInfo.Response(Collections.emptyList(), Collections.emptyList());
		} catch (ExecutionException e) {
			return new GetNetworkInfo.Response(Collections.emptyList(), Collections.emptyList());
		}

	}

	protected static List<JsonObject> parseIpJson(String json) throws OpenemsNamedException {
		final var stdout = JsonUtils.getAsJsonArray(JsonUtils.getAsJsonObject(JsonUtils.parse(json)), "stdout");
		if (stdout.isEmpty()) {
			return Collections.emptyList();
		}
		final var networkData = JsonUtils.getAsString(stdout.get(0));
		final var networkDataJson = JsonUtils.parseOptional(networkData);
		if (networkDataJson.isPresent() && networkDataJson.get().isJsonArray()) {
			final var networkInterfaces = JsonUtils.getAsJsonArray(JsonUtils.parse(networkData));
			if (networkData.startsWith("[")) {
				return JsonUtils.stream(networkInterfaces)//
						.map(JsonElement::getAsJsonObject)//
						.toList();
			}
		}

		return Collections.emptyList();
	}

	protected static List<Route> parseRouteJson(String routeJson) throws OpenemsNamedException {
		final var networkData = parseIpJson(routeJson);
		if (networkData == null) {
			return Collections.emptyList();
		}
		return networkData.stream() //
				.map(t -> Route.serializer().deserialize(t)) //
				.toList();
	}

	/**
	 * Parses the json returned by ip address get command.
	 * 
	 * @param resultIpShow the json to be parsed
	 * @return a list of parsed ips
	 * @throws OpenemsNamedException on error
	 */
	protected static List<NetworkInfoWrapper> parseShowJson(String resultIpShow) throws OpenemsNamedException {
		final var networkInterfaces = parseIpJson(resultIpShow);
		if (networkInterfaces == null) {
			return Collections.emptyList();
		}

		return networkInterfaces.stream() //
				.map(SystemdInterface.serializer()::deserialize) //
				.map(t -> new NetworkInfoWrapper(t.ifname(), t.addressInfos.stream() //
						.map(address -> new NetworkInfoAddress(//
								new Inet4AddressWithSubnetmask(address.label(), parseOrNull(address.local()),
										address.prefixlen),
								address.dynamic)) //
						.toList())) //
				.toList();
	}

	private record SystemdInterface(String ifname, List<SystemdAddressInfo> addressInfos) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link SystemdInterface}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<SystemdInterface> serializer() {
			return jsonObjectSerializer(SystemdInterface.class, //
					json -> new SystemdInterface(//
							json.getString("ifname"), //
							json.getList("addr_info", SystemdAddressInfo.serializer())),
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("ifname", obj.ifname()) //
							.add("addr_info", SystemdAddressInfo.serializer().toListSerializer() //
									.serialize(obj.addressInfos())) //
							.build() //
			);
		}

	}

	private record SystemdAddressInfo(String family, String local, int prefixlen, String label, boolean dynamic) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link SystemdAddressInfo}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<SystemdAddressInfo> serializer() {
			return jsonObjectSerializer(SystemdAddressInfo.class, //
					json -> new SystemdAddressInfo(//
							json.getString("family"), //
							json.getString("local"), //
							json.getInt("prefixlen"), //
							json.getStringOrNull("label"), //
							json.getOptionalBoolean("dynamic").orElse(false)), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("family", obj.family()) //
							.addProperty("local", obj.local()) //
							.addProperty("prefixlen", obj.prefixlen()) //
							.addProperty("label", obj.label()) //
							.onlyIf(obj.dynamic(), b -> b//
									.addProperty("dynamic", obj.dynamic()))
							.build());
		}

	}

	@Override
	public CompletableFuture<String> getOperatingSystemVersion() {
		final var sc = new SystemCommand(//
				"cat /etc/os-release", //
				false, // runInBackground
				5, // timeoutSeconds
				Optional.empty(), // username
				Optional.empty()); // password

		return execute(sc).thenApply(success -> {
			final var osVersionName = success.stdout().stream() //
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

			return osVersionName.orElseThrow(() -> new CompletionException(
					new OpenemsException("OS-Version name not found in /etc/os-release")));
		});
	}

	@Override
	public Updateable getSystemUpdateable() {
		return null;
	}

	@Override
	public void deleteNetworkInterfaces(User user, List<String> interfaceNames) throws OpenemsNamedException {
		var errors = new ArrayList<String>();

		for (var interfaceName : interfaceNames) {
			var fileName = interfaceName + ".network";
			var file = new File(NETWORK_BASE_PATH, fileName);

			if (file.exists()) {
				try {
					Files.delete(file.toPath());
					log.info("Deleted network interface configuration file: " + fileName);
				} catch (IOException e) {
					log.error("Failed to delete network interface configuration file: " + fileName, e);
					errors.add("Failed to delete interface " + interfaceName + ": " + e.getMessage());
				}
			} else {
				log.warn("Network interface configuration file not found: " + fileName);
			}
		}

		if (!errors.isEmpty()) {
			throw new OpenemsException("Errors while deleting interfaces: " + String.join(", ", errors));
		}

		// Restart systemd-networkd to apply changes
		this.handleExecuteSystemCommandRequest(ExecuteSystemCommandRequest
				.runInBackgroundWithoutAuthentication("systemctl restart systemd-networkd --no-block"));
	}

}
