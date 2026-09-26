package io.openems.common.bridge.eebus.api;

import org.openmuc.jeebus.ship.api.ConnectionHandler;
import org.openmuc.jeebus.ship.api.DisconnectReason;
import org.openmuc.jeebus.ship.api.ShipConnectionInterface;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import java.net.Inet4Address;
import java.util.Arrays;
import java.util.Objects;

@Component(scope = ServiceScope.SINGLETON, service = EebusDeviceDiscovery.class)
public class EebusDeviceDiscovery {
	/*private ServiceRegistry registry;

	@Activate
	public void activate() {
		this.registry = new ServiceRegistry(Inet4Address.getLoopbackAddress(), "me", "_ship._tcp.local.", new ConnectionHandler() {
			@Override
			public void onMessageReceived(byte[] fullMsg, byte[] payload, ShipConnectionInterface shipConn) {
			}

			@Override
			public void onDisconnect(DisconnectReason reason, ShipConnectionInterface shipConn) {
			}

			@Override
			public void serviceAdded(String socketAddress, String ski) {
				System.out.println("Service resolved: " + socketAddress + " - " + ski);
				// TODO@fenecon: this is called whenever a SHIP mDNS Service is
				//  resolved.
				//  If the socket and SKI are not enough, you may use
				//  registry.listServices(); and filter that for the full
				//  service info. It is an ugly work-around though.
				System.out.println(Arrays.stream(EebusDeviceDiscovery.this.registry.listServices())
						.filter(s -> Objects.equals(
								s.getAddress().getHostAddress() + ":" + s.getPort(),
								socketAddress
						)).findAny().get());
			}

			@Override
			public void serviceRemoved(String ipAddr) {
			}

			@Override
			public void connectionDataExchangeEnabled(String ipAddr) {
			}
		});
	}*/

}
