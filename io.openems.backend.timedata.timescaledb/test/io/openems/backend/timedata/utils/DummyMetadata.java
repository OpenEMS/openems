package io.openems.backend.timedata.utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.event.EventAdmin;

import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.AlertingSetting;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.EdgeHandler;
import io.openems.backend.common.metadata.EdgeUser;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.User;
import io.openems.common.OpenemsOEM;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Language;

public class DummyMetadata implements Metadata {

	@Override
	public boolean isInitialized() {
		return false;
	}

	@Override
	public User authenticate(String username, String password) throws OpenemsNamedException {
		return null;
	}

	@Override
	public User authenticate(String token) throws OpenemsNamedException {
		return null;
	}

	@Override
	public void logout(User user) {
	}

	@Override
	public Optional<String> getEdgeIdForApikey(String apikey) {
		return Optional.empty();
	}

	@Override
	public Optional<List<EdgeUser>> getUserToEdge(String edgeId) {
		return Optional.empty();
	}

	@Override
	public Optional<Edge> getEdge(String edgeId) {
		return Optional.empty();
	}

	@Override
	public Optional<Edge> getEdgeBySetupPassword(String setupPassword) {
		return Optional.empty();
	}

	@Override
	public Optional<User> getUser(String userId) {
		return Optional.empty();
	}

	@Override
	public Collection<Edge> getAllEdges() {
		return null;
	}

	@Override
	public void addEdgeToUser(User user, Edge edge) throws OpenemsNamedException {
	}

	@Override
	public Map<String, Object> getUserInformation(User user) throws OpenemsNamedException {
		return null;
	}

	@Override
	public void setUserInformation(User user, JsonObject jsonObject) throws OpenemsNamedException {
	}

	@Override
	public byte[] getSetupProtocol(User user, int setupProtocolId) throws OpenemsNamedException {
		return null;
	}

	@Override
	public JsonObject getSetupProtocolData(User user, String edgeId) throws OpenemsNamedException {
		return null;
	}

	@Override
	public int submitSetupProtocol(User user, JsonObject jsonObject) throws OpenemsNamedException {
		return 0;
	}

	@Override
	public void registerUser(JsonObject user, OpenemsOEM.Manufacturer oem) throws OpenemsNamedException {
	}

	@Override
	public void updateUserLanguage(User user, Language language) throws OpenemsNamedException {
	}

	@Override
	public EventAdmin getEventAdmin() {
		return null;
	}

	@Override
	public EdgeHandler edge() {
		return null;
	}

	@Override
	public Optional<String> getSerialNumberForEdge(Edge edge) {
		return Optional.empty();
	}

	@Override
	public List<AlertingSetting> getUserAlertingSettings(String edgeId) {
		return null;
	}

	@Override
	public AlertingSetting getUserAlertingSettings(String edgeId, String userId) throws OpenemsException {
		return null;
	}

	@Override
	public void setUserAlertingSettings(User user, String edgeId, List<AlertingSetting> users) {

	}

}