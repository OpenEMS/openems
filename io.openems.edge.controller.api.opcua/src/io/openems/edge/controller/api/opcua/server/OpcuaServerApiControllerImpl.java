package io.openems.edge.controller.api.opcua.server;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.identity.AnonymousIdentityValidator;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.server.EndpointConfiguration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.OPC-UA.Server", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class OpcuaServerApiControllerImpl extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(OpcuaServerApiControllerImpl.class);

	private OpcUaServer server = null;
	private MyNamespace namespace = null;

	public OpcuaServerApiControllerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				OpcuaServerApiController.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws Exception {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.server = new OpcUaServer(OpcUaServerConfig.builder() //
				.setApplicationUri("urn:openems:edge") //
				.setApplicationName(LocalizedText
						.english(OpenemsConstants.MANUFACTURER_MODEL + " OPC UA Server [" + config.id() + "]")) //
				.setBuildInfo(BuildInfo.builder() //
						.productUri("urn:openems:edge") //
						.manufacturerName(OpenemsConstants.MANUFACTURER) //
						.productName(OpenemsConstants.MANUFACTURER_MODEL) //
						.softwareVersion(OpenemsConstants.VERSION_STRING) //
						.buildNumber("") //
						.buildDate(DateTime.now()) //
						.build()) //
				.setEndpoints(Collections.singleton(EndpointConfiguration.newBuilder() //
						.setBindAddress("0.0.0.0") //
						.setBindPort(config.port()) //
						.addTokenPolicies(//
								OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS //
						) //
						.build())) //
				.setIdentityValidator(new AnonymousIdentityValidator())//
				.setProductUri("urn:eclipse:milo:example-server") //
				.build());

		this.namespace = new MyNamespace(this.server);
		this.namespace.startup();

		this.server.startup().thenAccept((s) -> {
			this.logInfo(this.log, "Successfully started OPC UA Server");
		});
	}

	@Deactivate
	protected void deactivate() {
		if (this.server != null) {
			try {
				this.server.shutdown().get(5, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				this.logWarn(this.log, "Unable to stop OPC-UA Server: " + e.getMessage());
				e.printStackTrace();
			}
			if (this.namespace != null) {
				this.namespace.shutdown();
			}
		}
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		// TODO
		this.logInfo(this.log, "Controller run()");
	}

}
