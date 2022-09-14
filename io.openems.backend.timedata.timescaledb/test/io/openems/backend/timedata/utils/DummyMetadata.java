package io.openems.backend.timedata.utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.event.EventAdmin;

import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.EdgeHandler;
import io.openems.backend.common.metadata.EdgeUser;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.User;
import io.openems.common.OpenemsOEM;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;

public class DummyMetadata implements Metadata {

	@Override
	public boolean isInitialized() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public User authenticate(String username, String password) throws OpenemsNamedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public User authenticate(String token) throws OpenemsNamedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void logout(User user) {
		// TODO Auto-generated method stub

	}

	@Override
	public Optional<String> getEdgeIdForApikey(String apikey) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public Optional<List<EdgeUser>> getUserToEdge(String edgeId) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public Optional<Edge> getEdge(String edgeId) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public Optional<Edge> getEdgeBySetupPassword(String setupPassword) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public Optional<User> getUser(String userId) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public Collection<Edge> getAllEdges() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addEdgeToUser(User user, Edge edge) throws OpenemsNamedException {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<String, Object> getUserInformation(User user) throws OpenemsNamedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setUserInformation(User user, JsonObject jsonObject) throws OpenemsNamedException {
		// TODO Auto-generated method stub

	}

	@Override
	public byte[] getSetupProtocol(User user, int setupProtocolId) throws OpenemsNamedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JsonObject getSetupProtocolData(User user, String edgeId) throws OpenemsNamedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int submitSetupProtocol(User user, JsonObject jsonObject) throws OpenemsNamedException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void registerUser(JsonObject user, OpenemsOEM.Manufacturer oem) throws OpenemsNamedException {
		// TODO Auto-generated method stub
	}

	@Override
	public void updateUserLanguage(User user, Language language) throws OpenemsNamedException {
		// TODO Auto-generated method stub

	}

	@Override
	public Optional<EdgeUser> getEdgeUserTo(String edgeId, String userId) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public EventAdmin getEventAdmin() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EdgeHandler edge() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<String> getSerialNumberForEdge(Edge edge) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

}