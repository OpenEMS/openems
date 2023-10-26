package io.openems.backend.common.test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.event.EventAdmin;

import com.google.gson.JsonObject;

import io.openems.backend.common.alerting.OfflineEdgeAlertingSetting;
import io.openems.backend.common.alerting.SumStateAlertingSetting;
import io.openems.backend.common.alerting.UserAlertingSettings;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.EdgeHandler;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.User;
import io.openems.common.OpenemsOEM;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.GetEdgesRequest.PaginationOptions;
import io.openems.common.jsonrpc.response.GetEdgesResponse.EdgeMetadata;
import io.openems.common.session.Language;

public class DummyMetadata implements Metadata {
	@Override
	public boolean isInitialized() {
		return false;
	}

	@Override
	public Optional<Edge> getEdge(String edgeId) {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public Collection<Edge> getAllOfflineEdges() {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public UserAlertingSettings getUserAlertingSettings(String edgeId, String userId) throws OpenemsException {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}
	
	@Override
	public List<UserAlertingSettings> getUserAlertingSettings(String edgeId) {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public List<OfflineEdgeAlertingSetting> getEdgeOfflineAlertingSettings(String edgeId) throws OpenemsException {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public List<SumStateAlertingSetting> getSumStateAlertingSettings(String edgeId) throws OpenemsException {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public void setUserAlertingSettings(User user, String edgeId, List<UserAlertingSettings> settings) {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public User authenticate(String username, String password) throws OpenemsNamedException {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public User authenticate(String token) throws OpenemsNamedException {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public void logout(User user) {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public Optional<String> getEdgeIdForApikey(String apikey) {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public Optional<Edge> getEdgeBySetupPassword(String setupPassword) {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public Optional<User> getUser(String userId) {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public void addEdgeToUser(User user, Edge edge) throws OpenemsNamedException {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public Map<String, Object> getUserInformation(User user) throws OpenemsNamedException {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public void setUserInformation(User user, JsonObject jsonObject) throws OpenemsNamedException {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public byte[] getSetupProtocol(User user, int setupProtocolId) throws OpenemsNamedException {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public JsonObject getSetupProtocolData(User user, String edgeId) throws OpenemsNamedException {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public int submitSetupProtocol(User user, JsonObject jsonObject) throws OpenemsNamedException {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public void registerUser(JsonObject user, OpenemsOEM.Manufacturer oem) throws OpenemsNamedException {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public void updateUserLanguage(User user, Language language) throws OpenemsNamedException {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public EventAdmin getEventAdmin() {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public EdgeHandler edge() {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public Optional<String> getSerialNumberForEdge(Edge edge) {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public List<EdgeMetadata> getPageDevice(User user, PaginationOptions paginationOptions)
			throws OpenemsNamedException {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public EdgeMetadata getEdgeMetadataForUser(User user, String edgeId) throws OpenemsNamedException {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public Optional<Level> getSumState(String edgeId) {
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}
}
