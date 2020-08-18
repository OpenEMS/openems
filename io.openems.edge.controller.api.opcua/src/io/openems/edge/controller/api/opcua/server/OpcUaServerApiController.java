package io.openems.edge.controller.api.opcua.server;

import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.OPC-UA.Server", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class OpcUaServerApiController extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private Config config = null;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public OpcUaServerApiController() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		OpcUaServerConfig serverConfig = OpcUaServerConfig.builder() //
//				.setApplicationUri(applicationUri) //
//				.setApplicationName(LocalizedText.english(" OPC UA Server")) //
//				.setBindPort(impl.getPort()) //
//				.setBindAddresses(bindAddresses) //
//				.setEndpointAddresses(endpointAddresses) //
//				.setBuildInfo(new BuildInfo("urn:smg:actuatorclient:opcua:server", "fortiss", " OPC UA Server 1.0",
//						OpcUaServer.SDK_VERSION, "", DateTime.now())) //
//				.setCertificateManager(certificateManager) //
//				.setCertificateValidator(certificateValidator) //
//				.setIdentityValidator(identityValidator) //
//				.setProductUri("urn:smg:actuatorclient:opcua:server") //
//				.setServerName(impl.getEndpointUrl()) //
//				.setSecurityPolicies(EnumSet.of(SecurityPolicy.None, SecurityPolicy.Basic128Rsa15
//				/*
//				 * , SecurityPolicy.Basic256, SecurityPolicy.Basic256Sha256
//				 */
//				)) //
//				.setUserTokenPolicies(ImmutableList.of(USER_TOKEN_POLICY_ANONYMOUS, USER_TOKEN_POLICY_USERNAME)) //
				.build();
		OpcUaServer server = new OpcUaServer(serverConfig);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
	}
}
