package io.openems.backend.common.metadata.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.AbstractMetadata;
import io.openems.backend.common.metadata.AuthenticationMetadata;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.EdgeMetadata;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Metadata.Backend", //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class BackendMetadataImpl extends AbstractMetadata implements Metadata {

	private final Logger log = LoggerFactory.getLogger(BackendMetadataImpl.class);

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private AuthenticationMetadata authenticationMetadata;
	
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private EdgeMetadata edgeMetadata;
	
	public BackendMetadataImpl() {
		super("Metadata.Backend");
	}

	@Activate
	private void activate(Config config) {
		this.logInfo(this.log, "Activate");
	}

	@Deactivate
	private void deactivate() {
		this.logInfo(this.log, "Deactivate");
	}

	@Override
	public User authenticate(String username, String password) throws OpenemsNamedException {
		return authenticationMetadata.authenticate(username, password);
	}

	@Override
	public User authenticate(String token) throws OpenemsNamedException {
		return authenticationMetadata.authenticate(token);
	}

	@Override
	public void logout(User user) {
		authenticationMetadata.logout(user);
	}

	@Override
	public synchronized Optional<String> getEdgeIdForApikey(String apikey) {
		return edgeMetadata.getEdgeIdForApikey(apikey);
	}

	@Override
	public synchronized Optional<Edge> getEdgeBySetupPassword(String setupPassword) {
		return edgeMetadata.getEdgeBySetupPassword(setupPassword);
	}

	@Override
	public synchronized Optional<Edge> getEdge(String edgeId) {
		return edgeMetadata.getEdge(edgeId);
	}

	@Override
	public Optional<User> getUser(String userId) {
		return authenticationMetadata.getUser(userId);
	}

	@Override
	public synchronized Collection<Edge> getAllEdges() {
		return edgeMetadata.getAllEdges();
	}

	@Override
	public void addEdgeToUser(User user, Edge edge) throws OpenemsNamedException {
		authenticationMetadata.addEdgeToUser(user, edge);
	}

	@Override
	public Map<String, Object> getUserInformation(User user) throws OpenemsNamedException {
		return authenticationMetadata.getUserInformation(user);
	}

	@Override
	public void setUserInformation(User user, JsonObject jsonObject) throws OpenemsNamedException {
		authenticationMetadata.setUserInformation(user, jsonObject);
	}

	@Override
	public byte[] getSetupProtocol(User user, int setupProtocolId) throws OpenemsNamedException {
		throw new IllegalArgumentException("BackendMetadataImpl.getSetupProtocol() is not implemented");
	}

	@Override
	public int submitSetupProtocol(User user, JsonObject jsonObject) {
		throw new IllegalArgumentException("BackendMetadataImpl.submitSetupProtocol() is not implemented");
	}

	@Override
	public void registerUser(JsonObject jsonObject) throws OpenemsNamedException {
		authenticationMetadata.registerUser(jsonObject);
	}

	@Override
	public void updateUserLanguage(User user, Language locale) throws OpenemsNamedException {
		authenticationMetadata.updateUserLanguage(user, locale);
	}

}
