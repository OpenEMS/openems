package io.openems.edge.core.appmanager;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.ComponentUtil.PreferredRelay;
import io.openems.edge.core.appmanager.ComponentUtil.RelayContactInfo;
import io.openems.edge.core.appmanager.ComponentUtil.RelayInfo;
import io.openems.edge.core.host.Config;
import io.openems.edge.core.host.HostImpl;
import io.openems.edge.core.host.MyConfig;
import io.openems.edge.core.host.NetworkInterface;
import io.openems.edge.core.host.Routes;
import io.openems.edge.core.host.jsonrpc.SetNetworkConfig;
import io.openems.edge.io.test.DummyInputOutput;

public class ComponentUtilImplTest {

	private DummyConfigurationAdmin cm;
	private DummyPseudoComponentManager componentManager;
	private ComponentUtil componentUtil;

	@BeforeEach
	public void before() throws Exception {
		this.componentManager = new DummyPseudoComponentManager();
		this.cm = new DummyConfigurationAdmin();
		this.componentUtil = new ComponentUtilImpl(this.componentManager);
		final var finalComponentManager = this.componentManager;

		final var hostComponent = new HostImpl() {

			@Activate
			protected void activate(ComponentContext componentContext, BundleContext bundleContext,
					io.openems.edge.core.host.Config config) throws OpenemsException {
				super.activate(componentContext, bundleContext, config);
			}

			@Modified
			protected void modified(ComponentContext componentContext, BundleContext bundleContext, Config config) {
				super.modified(componentContext, bundleContext, config);
			}

			@Override
			@Deactivate
			protected void deactivate() {
				super.deactivate();
			}

			@Override
			public void handleSetNetworkConfigRequest(User user, SetNetworkConfig.Request request)
					throws OpenemsError.OpenemsNamedException {
				var newInterfaces = request.networkInterfaces();

				var interfacesJson = new JsonObject();
				for (NetworkInterface<?> ni : newInterfaces) {
					interfacesJson.add(ni.getName(), ni.toJson());
				}

				var newConfigJson = JsonUtils.buildJsonObject().add("interfaces", interfacesJson).build();

				finalComponentManager.updateHostConfiguration(newConfigJson.toString());
			}
		};
		new ComponentTest(hostComponent) //
				.addReference("oem", new DummyOpenemsEdgeOem()) //
				.addReference("cm", this.cm) //
				.activate(MyConfig.create() //
						.setNetworkConfiguration("""
								{
								    "interfaces": {
								        "eth0": {
								            "dhcp": false,
								            "linkLocalAddressing": false,
								            "gateway": "172.23.20.254",
								            "addresses": [
								                {
								                    "label": "",
								                    "address": "172.23.20.1",
								                    "subnetmask": "255.255.255.0"
								                },
								                {
								                    "label": "",
								                    "address": "10.12.119.157",
								                    "subnetmask": "255.255.255.0"
								                },
								                {
								                    "label": "",
								                    "address": "192.168.100.200",
								                    "subnetmask": "255.255.255.0"
								                },
								                {
								                    "label": "",
								                    "address": "192.168.100.100",
								                    "subnetmask": "255.255.255.0"
								                }
								            ],
								            "gatewayOnLink": true
								        },
								        "eth1": {
								            "dhcp": false,
								            "addresses": [
								                {
								                    "label": "",
								                    "address": "10.4.0.1",
								                    "subnetmask": "255.255.0.0"
								                },
								                {
								                    "label": "",
								                    "address": "192.168.1.9",
								                    "subnetmask": "255.255.255.248"
								                },
								                {
								                    "label": "",
								                    "address": "10.5.0.1",
								                    "subnetmask": "255.255.0.0"
								                },
								                {
								                    "label": "",
								                    "address": "172.23.21.1",
								                    "subnetmask": "255.255.255.0"
								                },
								                {
								                    "label": "",
								                    "address": "192.168.100.120",
								                    "subnetmask": "255.255.255.0"
								                },
								                {
								                    "label": "",
								                    "address": "192.168.0.2",
								                    "subnetmask": "255.255.255.224"
								                }
								            ],
								            "routes": [
								                {
								                    "routeGateway": "172.23.21.254",
								                    "routeDestination": "172.23.22.0/24",
								                    "routeGatewayOnLink": true
								                },
								                {
								                    "routeGateway": "172.23.21.254",
								                    "routeDestination": "172.23.23.0/24",
								                    "routeGatewayOnLink": true
								                },
								                {
								                    "routeGateway": "172.23.21.254",
								                    "routeDestination": "172.23.24.0/24",
								                    "routeGatewayOnLink": true
								                }
								            ]
								        }
								    }
								}
								""") //
						.setUsbConfiguration("") //
						.build());
		this.componentManager.addComponent(hostComponent);

		this.createTestRelay("io0");
	}

	@Test
	public void testGetAllRelayInfos() {
		assertEquals(1, this.componentUtil.getAllRelayInfos().size());
		assertEquals(10, this.componentUtil.getAllRelayInfos().get(0).channels().size());
	}

	@Test
	public void testGetAllRelayInfosWithExistingComponent() {
		this.createTestComponent("io0/InputOutput1");
		assertEquals(1, this.componentUtil.getAllRelayInfos().size());
		assertEquals(10, this.componentUtil.getAllRelayInfos().get(0).channels().size());
	}

	@Test
	public void testGetPreferredRelays() {
		final var result = this.componentUtil.getPreferredRelays(this.componentUtil.getAllRelayInfos(), 2,
				List.of(PreferredRelay.of(10, new int[] { 2, 3 })));
		assertNotNull(result);
		assertEquals(2, result.length);
		assertEquals("io0/InputOutput1", result[0]);
		assertEquals("io0/InputOutput2", result[1]);
	}

	@Test
	public void testGetPreferredRelaysWithExistingComponent() {
		this.createTestRelay("io1");
		this.createTestComponent("io0/InputOutput2", "io0/InputOutput3");
		final var result = this.componentUtil.getPreferredRelays(this.componentUtil.getAllRelayInfos(), 2,
				List.of(PreferredRelay.of(10, new int[] { 2, 3 })));
		assertNotNull(result);
		assertEquals(2, result.length);
		assertEquals("io1/InputOutput1", result[0]);
		assertEquals("io1/InputOutput2", result[1]);
	}

	@Test
	public void testGetPreferredRelaysWithSpecialConstraints() {
		final var constraints = new PreferredRelay(t -> t.id().equals("io0"), new int[] { 1, 2 });
		final var result = this.componentUtil.getPreferredRelays(this.componentUtil.getAllRelayInfos(), 2,
				List.of(constraints, PreferredRelay.of(10, new int[] { 2, 3 })));
		assertNotNull(result);
		assertEquals(2, result.length);
		assertEquals("io0/InputOutput0", result[0]);
		assertEquals("io0/InputOutput1", result[1]);
	}

	@Test
	public void testGetPreferredRelaysWithMissingRelayContacts() {
		final var relayInfo = new RelayInfo("io0", "alias", 10, List.of(//
				new RelayContactInfo("io0/InputOutput0", null, 0, emptyList(), emptyList()), //
				new RelayContactInfo("io0/InputOutput1", null, 1, emptyList(), emptyList()), //
				new RelayContactInfo("io0/InputOutput2", null, 2, emptyList(), emptyList()), //
				new RelayContactInfo("io0/InputOutput3", null, 3, emptyList(), emptyList()), //
				// skip 4
				new RelayContactInfo("io0/InputOutput5", null, 5, emptyList(), emptyList()), //
				new RelayContactInfo("io0/InputOutput6", null, 6, emptyList(), emptyList()), //
				// skip 7-8
				new RelayContactInfo("io0/InputOutput9", null, 9, emptyList(), emptyList()) //
		));
		final var result = this.componentUtil.getPreferredRelays(List.of(relayInfo), 2,
				List.of(PreferredRelay.of(10, new int[] { 6, 7 })));
		assertNotNull(result);
		assertEquals(2, result.length);
		assertEquals("io0/InputOutput5", result[0]);
		assertEquals("io0/InputOutput6", result[1]);
	}

	private void createTestComponent(String... blockingRelayContacts) {
		this.componentManager.addComponent(new EdgeConfig.Component("dummyId0", "dummyAlias", "dummy.factory.id", //
				JsonUtils.buildJsonObject() //
						.onlyIf(blockingRelayContacts.length != 0, b -> {
							var cnt = 0;
							for (final var relayContact : blockingRelayContacts) {
								b.addProperty("someRelayConfig" + (cnt++), relayContact);
							}
						}).build()));
	}

	private void createTestRelay(String ioName) {
		final var dummyRelay = new DummyInputOutput(ioName);
		this.cm.getOrCreateEmptyConfiguration(ioName);
		this.componentManager.addComponent(dummyRelay);
	}

	@Test
	public void updateHosts() throws Exception {
		this.componentUtil.updateHosts(DUMMY_ADMIN, List.of(//
				new InterfaceConfiguration("eth0") //
		), List.of(//
				new InterfaceConfiguration("eth0") //
						.setDhcp(false), //
				new InterfaceConfiguration("eth1") //
						.addRoute(Routes.builder() //
								.setRouteGateway("172.23.21.254") //
								.setRouteDestination("172.23.22.0/24") //
								.setRouteGatewayOnLink(true) //
								.build())));

		final var newInterfaces = this.componentUtil.getInterfaces();

		final var eth1 = newInterfaces.stream().filter(t -> t.getName().equals("eth1")).findAny().orElse(null);
		final var hasRoute = eth1.getRoutes().getValue().stream().anyMatch(route -> {
			return route.getRouteGateway().equals("172.23.21.254")//
					&& route.getRouteDestination().equals("172.23.22.0/24")//
					&& route.isRouteGatewayOnLink();
		});

		assertFalse(hasRoute);
	}
}
