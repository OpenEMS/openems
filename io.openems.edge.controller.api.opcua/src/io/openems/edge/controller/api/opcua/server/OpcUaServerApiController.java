package io.openems.edge.controller.api.opcua.server;

import java.io.File;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.identity.CompositeValidator;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.X509IdentityValidator;
import org.eclipse.milo.opcua.sdk.server.util.HostnameUtil;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaRuntimeException;
import org.eclipse.milo.opcua.stack.core.security.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.security.DefaultTrustListManager;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.transport.TransportProfile;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.core.util.CertificateUtil;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateGenerator;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedHttpsCertificateBuilder;
import org.eclipse.milo.opcua.stack.server.EndpointConfiguration;
import org.eclipse.milo.opcua.stack.server.security.DefaultServerCertificateValidator;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import io.openems.common.OpenemsConstants;
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

	private final Logger log = LoggerFactory.getLogger(OpcUaServerApiController.class);

	private static final int TCP_BIND_PORT = 12686;
	private static final int HTTPS_BIND_PORT = 8443;

	private Config config = null;
	private OpcUaServer server = null;
	private ExampleNamespace namespace = null;

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
	void activate(ComponentContext context, Config config) throws Exception {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		// TODO OpenEMS directory
		File securityTempDir = new File(System.getProperty("java.io.tmpdir"), "security");
		if (!securityTempDir.exists() && !securityTempDir.mkdirs()) {
			throw new Exception("unable to create security temp dir: " + securityTempDir);
		}
		LoggerFactory.getLogger(getClass()).info("security temp dir: {}", securityTempDir.getAbsolutePath());

		KeyStoreLoader loader = new KeyStoreLoader().load(securityTempDir);

		DefaultCertificateManager certificateManager = new DefaultCertificateManager(loader.getServerKeyPair(),
				loader.getServerCertificateChain());

		File pkiDir = securityTempDir.toPath().resolve("pki").toFile();
		DefaultTrustListManager trustListManager = new DefaultTrustListManager(pkiDir);
		LoggerFactory.getLogger(getClass()).info("pki dir: {}", pkiDir.getAbsolutePath());

		DefaultServerCertificateValidator certificateValidator = new DefaultServerCertificateValidator(
				trustListManager);

		KeyPair httpsKeyPair = SelfSignedCertificateGenerator.generateRsaKeyPair(2048);

		SelfSignedHttpsCertificateBuilder httpsCertificateBuilder = new SelfSignedHttpsCertificateBuilder(httpsKeyPair);
		httpsCertificateBuilder.setCommonName(HostnameUtil.getHostname());
		HostnameUtil.getHostnames("0.0.0.0").forEach(httpsCertificateBuilder::addDnsName);
		X509Certificate httpsCertificate = httpsCertificateBuilder.build();

		UsernameIdentityValidator identityValidator = new UsernameIdentityValidator(true, authChallenge -> {
			String username = authChallenge.getUsername();
			String password = authChallenge.getPassword();

			boolean userOk = "user".equals(username) && "password1".equals(password);
			boolean adminOk = "admin".equals(username) && "password2".equals(password);

			return userOk || adminOk;
		});

		X509IdentityValidator x509IdentityValidator = new X509IdentityValidator(c -> true);

		// If you need to use multiple certificates you'll have to be smarter than this.
		X509Certificate certificate = certificateManager.getCertificates().stream().findFirst()
				.orElseThrow(() -> new UaRuntimeException(StatusCodes.Bad_ConfigurationError, "no certificate found"));

		// The configured application URI must match the one in the certificate(s)
		String applicationUri = CertificateUtil.getSanUri(certificate)
				.orElseThrow(() -> new UaRuntimeException(StatusCodes.Bad_ConfigurationError,
						"certificate is missing the application URI"));

		Set<EndpointConfiguration> endpointConfigurations = createEndpointConfigurations(certificate);

		String productUri = "urn:openems:edge:opcua:server:" + config.id();

		OpcUaServerConfig serverConfig = OpcUaServerConfig.builder() //
				.setApplicationUri(applicationUri) //
				.setApplicationName(LocalizedText.english("OpenEMS Edge OPC UA Server [" + config.id() + "]")) //
				.setEndpoints(endpointConfigurations) //
				.setBuildInfo(new BuildInfo(//
						productUri, //
						OpenemsConstants.MANUFACTURER, //
						OpenemsConstants.MANUFACTURER_MODEL, //
						OpenemsConstants.VERSION_STRING, //
						OpenemsConstants.MANUFACTURER_VERSION, //
						DateTime.now())) //
				.setCertificateManager(certificateManager) //
				.setTrustListManager(trustListManager) //
				.setCertificateValidator(certificateValidator) //
				.setHttpsKeyPair(httpsKeyPair) //
				.setHttpsCertificate(httpsCertificate) //
				.setIdentityValidator(new CompositeValidator(identityValidator, x509IdentityValidator)) //
				.setProductUri(productUri) //
				.build();

		this.server = new OpcUaServer(serverConfig);
		this.server.startup().thenAccept((server) -> {
			this.logInfo(this.log, "Server started");
		});

		this.namespace = new ExampleNamespace(server);
		this.namespace.startup();
	}

	@Deactivate
	protected void deactivate() {
		if (this.server != null) {
			this.server.shutdown().thenAccept((server) -> {
				this.logInfo(this.log, "Server stopped");
			});
		}
		if (this.namespace != null) {
			this.namespace.shutdown();
		}
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		// TODO
		this.logInfo(this.log, "Controller run()");
	}

	private Set<EndpointConfiguration> createEndpointConfigurations(X509Certificate certificate) {
		Set<EndpointConfiguration> endpointConfigurations = new LinkedHashSet<>();

		List<String> bindAddresses = Lists.newArrayList();
		bindAddresses.add("0.0.0.0");

		Set<String> hostnames = new LinkedHashSet<>();
		hostnames.add(HostnameUtil.getHostname());
		hostnames.addAll(HostnameUtil.getHostnames("0.0.0.0"));

		for (String bindAddress : bindAddresses) {
			for (String hostname : hostnames) {
				EndpointConfiguration.Builder builder = EndpointConfiguration.newBuilder().setBindAddress(bindAddress)
						.setHostname(hostname).setPath("/milo").setCertificate(certificate)
						.addTokenPolicies(OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS,
								OpcUaServerConfig.USER_TOKEN_POLICY_USERNAME, OpcUaServerConfig.USER_TOKEN_POLICY_X509);

				EndpointConfiguration.Builder noSecurityBuilder = builder.copy().setSecurityPolicy(SecurityPolicy.None)
						.setSecurityMode(MessageSecurityMode.None);

				endpointConfigurations.add(buildTcpEndpoint(noSecurityBuilder));
				endpointConfigurations.add(buildHttpsEndpoint(noSecurityBuilder));

				// TCP Basic256Sha256 / SignAndEncrypt
				endpointConfigurations
						.add(buildTcpEndpoint(builder.copy().setSecurityPolicy(SecurityPolicy.Basic256Sha256)
								.setSecurityMode(MessageSecurityMode.SignAndEncrypt)));

				// HTTPS Basic256Sha256 / Sign (SignAndEncrypt not allowed for HTTPS)
				endpointConfigurations.add(buildHttpsEndpoint(builder.copy()
						.setSecurityPolicy(SecurityPolicy.Basic256Sha256).setSecurityMode(MessageSecurityMode.Sign)));

				/*
				 * It's good practice to provide a discovery-specific endpoint with no security.
				 * It's required practice if all regular endpoints have security configured.
				 *
				 * Usage of the "/discovery" suffix is defined by OPC UA Part 6:
				 *
				 * Each OPC UA Server Application implements the Discovery Service Set. If the
				 * OPC UA Server requires a different address for this Endpoint it shall create
				 * the address by appending the path "/discovery" to its base address.
				 */

				EndpointConfiguration.Builder discoveryBuilder = builder.copy().setPath("/milo/discovery")
						.setSecurityPolicy(SecurityPolicy.None).setSecurityMode(MessageSecurityMode.None);

				endpointConfigurations.add(buildTcpEndpoint(discoveryBuilder));
				endpointConfigurations.add(buildHttpsEndpoint(discoveryBuilder));
			}
		}

		return endpointConfigurations;
	}

	private static EndpointConfiguration buildTcpEndpoint(EndpointConfiguration.Builder base) {
		return base.copy().setTransportProfile(TransportProfile.TCP_UASC_UABINARY).setBindPort(TCP_BIND_PORT).build();
	}

	private static EndpointConfiguration buildHttpsEndpoint(EndpointConfiguration.Builder base) {
		return base.copy().setTransportProfile(TransportProfile.HTTPS_UABINARY).setBindPort(HTTPS_BIND_PORT).build();
	}

}
