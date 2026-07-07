package io.openems.edge.core.appmanager.dependency.aggregatetask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.InterfaceConfiguration;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.AppManagerAppHelperImpl;
import io.openems.edge.core.host.Routes;

@Component(//
		service = { //
				AggregateTask.class, //
				StaticIpAggregateTask.class, //
				StaticIpAggregateTaskImpl.class //
		}, //
		scope = ServiceScope.SINGLETON //
)
public class StaticIpAggregateTaskImpl implements StaticIpAggregateTask {

	private record StaticIpExecutionConfiguration(//
			List<InterfaceConfiguration> ips //
	) implements AggregateTask.AggregateTaskExecutionConfiguration {

		private StaticIpExecutionConfiguration {
			Objects.requireNonNull(ips);
		}

		@Override
		public String identifier() {
			return "StaticIp";
		}

		@Override
		public JsonElement toJson() {
			if (this.ips.isEmpty()) {
				return JsonNull.INSTANCE;
			}
			return JsonUtils.buildJsonObject() //
					.add("interfaces", this.ips.stream() //
							.map(t -> JsonUtils.buildJsonObject() //
									.addProperty("interface", t.interfaceName) //
									.add("addresses", t.getIps().stream() //
											.map(ip -> JsonUtils.buildJsonObject() //
													.addProperty("address", ip.getInet4Address().getHostAddress()) //
													.build()) //
											.collect(JsonUtils.toJsonArray())) //
									.onlyIf(t.getGateway() != null, //
											b -> b.addProperty("gateway", t.getGateway())) //
									.onlyIf(t.getGatewayOnLink() != null, //
											b -> b.addProperty("gatewayOnLink", t.getGatewayOnLink())) //
									.add("routes", t.getRoutes().stream() //
											.map(route -> JsonUtils.buildJsonObject() //
													.addProperty("gateway", route.getRouteGateway()) //
													.addProperty("destination", route.getRouteDestination()) //
													.addProperty("gatewayOnLink", route.isRouteGatewayOnLink()) //
													.onlyIf(route.geRouteMetric() != null, //
															b -> b.addProperty("metric", route.geRouteMetric())) //
													.build()) //
											.collect(JsonUtils.toJsonArray())) //
									.build())
							.collect(JsonUtils.toJsonArray()))
					.build();
		}

	}

	private final boolean isWindows = System.getProperty("os.name").startsWith("Windows");

	private final ComponentUtil componentUtil;

	private List<InterfaceConfiguration> ips;
	private List<InterfaceConfiguration> ips2Delete;

	@Activate
	public StaticIpAggregateTaskImpl(@Reference ComponentUtil componentUtil) {
		this.componentUtil = componentUtil;
	}

	@Override
	public void reset() {
		this.ips = new LinkedList<>();
		this.ips2Delete = new LinkedList<>();
	}

	@Override
	public void aggregate(StaticIpConfiguration instance, StaticIpConfiguration oldConfig) {
		// Skip static IP configuration for Windows systems, as it is not supported
		if (this.isWindows) {
			return;
		}

		// Add new interface configurations
		if (instance != null) {
			this.ips.addAll(instance.interfaceConfiguration());
		}

		// Add interfaces to delete from the old configuration
		if (oldConfig != null) {
			this.ips2Delete.addAll(oldConfig.interfaceConfiguration());
		}
	}

	@Override
	public void create(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		this.execute(user, otherAppConfigurations, this.ips, this.ips2Delete);
	}

	@Override
	public void delete(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		this.execute(user, otherAppConfigurations, null, this.ips2Delete);
	}

	@Override
	public AggregateTaskExecutionConfiguration getExecutionConfiguration() {
		return new StaticIpExecutionConfiguration(this.ips);
	}

	private void execute(//
			final User user, //
			final List<AppConfiguration> otherAppConfigurations, //
			final List<InterfaceConfiguration> ips, //
			final List<InterfaceConfiguration> ipsToDelete //
	) throws OpenemsNamedException {
		if (!this.anyChanges()) {
			return;
		}

		// Remove duplicated IPs that are already configured in other applications
		InterfaceConfiguration.removeDuplicatedIps(ipsToDelete, //
				AppConfiguration
						.flatMap(otherAppConfigurations, StaticIpAggregateTask.class, c -> c.interfaceConfiguration())
						.toList());

		// Apply IP and route changes to the system
		this.componentUtil.updateHosts(//
				user, //
				ips == null ? null : InterfaceConfiguration.summarize(ips), //
				InterfaceConfiguration.summarize(ipsToDelete) //
		);
	}

	@Override
	public String getGeneralFailMessage(Language l) {
		final var bundle = AppManagerAppHelperImpl.getTranslationBundle(l);
		return TranslationUtil.getTranslation(bundle, "canNotUpdateStaticIps");
	}

	@Override
	public void validate(//
			List<String> errors, //
			AppConfiguration appConfiguration, //
			StaticIpConfiguration config, //
			Map<OpenemsAppInstance, AppConfiguration> allConfigurations//
	) {
		// setting ip configuration is not implemented for windows
		if (this.isWindows) {
			return;
		}

		if (config.interfaceConfiguration().isEmpty()) {
			return;
		}
		try {
			var interfaces = this.componentUtil.getInterfaces();
			config.interfaceConfiguration().stream() //
					.forEach(i -> {
						if (i.getCreateIfNotExist() != null && i.getCreateIfNotExist()) {
							return;
						}

						var existingInterface = interfaces.stream() //
								.filter(t -> t.getName().equals(i.interfaceName)) //
								.findFirst().orElse(null);

						if (existingInterface == null) {
							errors.add("Interface '" + i.interfaceName + "' not found.");
							return;
						}

						// Validate IP masquerading settings
						if (i.getIpMasquerade() != null
								&& !i.getIpMasquerade().equals(existingInterface.getIpMasquerade().getValue())) {
							errors.add("Property 'IPMasquerade' on interface '" + i.interfaceName + "' should be '"
									+ i.getIpMasquerade() + "'");
						}

						// Validate gateway settings
						if (i.getGateway() != null) {
							var existingGateway = existingInterface.getGateway();
							if (!existingGateway.isSet()
									|| !Objects.equals(existingGateway.getValue().getHostAddress(), i.getGateway())) {
								errors.add("Gateway '" + i.getGateway() + "' is not configured on interface '"
										+ i.interfaceName + "'");
							}
						}

						// Validate GatewayOnLink settings
						if (i.getGatewayOnLink() != null) {
							var existingGatewayOnLink = existingInterface.getGatewayOnLink();
							if (!existingGatewayOnLink.isSet()
									|| !Objects.equals(existingGatewayOnLink.getValue(), i.getGatewayOnLink())) {
								errors.add("GatewayOnLink '" + i.getGatewayOnLink()
										+ "' is not configured on interface '" + i.interfaceName + "'");
							}
						}

						// Validate routes configuration
						if (i.getRoutes() != null && !i.getRoutes().isEmpty()) {
							var existingRoutes = existingInterface.getRoutes().isSet()
									? existingInterface.getRoutes().getValue()
									: Collections.<Routes>emptySet();

							var missingRoutes = i.getRoutes().stream() //
									.filter(requiredRoute -> {
										// Skip route validation if no properties are defined
										if (requiredRoute.getRouteGateway() == null
												&& requiredRoute.getRouteDestination() == null
												&& requiredRoute.geRouteMetric() == null) {
											return false;
										}

										// Compare route parameters to detect missing ones
										return existingRoutes.stream() //
												.noneMatch(existingRoute -> {
													var matches = true;

													// Compare only defined properties
													if (requiredRoute.getRouteGateway() != null) {
														var gatewayMatch = Objects.equals(
																existingRoute.getRouteGateway(),
																requiredRoute.getRouteGateway());
														matches = matches && gatewayMatch;
													}

													if (requiredRoute.getRouteDestination() != null) {
														var destinationMatch = Objects.equals(
																existingRoute.getRouteDestination(),
																requiredRoute.getRouteDestination());
														matches = matches && destinationMatch;
													}

													if (requiredRoute.geRouteMetric() != null) {
														var metricMatch = Objects.equals(existingRoute.geRouteMetric(),
																requiredRoute.geRouteMetric());
														matches = matches && metricMatch;
													}

													// Always compare GatewayOnLink (defaults to false)
													var gatewayOnLinkMatch = existingRoute
															.isRouteGatewayOnLink() == requiredRoute
																	.isRouteGatewayOnLink();
													matches = matches && gatewayOnLinkMatch;

													return matches;
												});
									}).toList();

							if (!missingRoutes.isEmpty()) {
								errors.add("Routes [" + missingRoutes.stream().map(route -> {
									List<String> props = new ArrayList<>();
									if (route.getRouteGateway() != null) {
										props.add("Gateway=" + route.getRouteGateway());
									}
									if (route.getRouteDestination() != null) {
										props.add("Destination=" + route.getRouteDestination());
									}
									props.add("GatewayOnLink=" + route.isRouteGatewayOnLink());
									if (route.geRouteMetric() != null) {
										props.add("Metric=" + route.geRouteMetric());
									}
									return String.join(", ", props);
								}).collect(Collectors.joining("; ")) + "] are not configured on interface '"
										+ i.interfaceName + "'");
							}
						}

					});
		} catch (OpenemsNamedException ex) {
			errors.add("Error validating static IP configuration: " + ex.getMessage());
		}
	}

	private final boolean anyChanges() {
		return !this.isWindows && (!this.ips.isEmpty() || !this.ips2Delete.isEmpty());
	}

}
