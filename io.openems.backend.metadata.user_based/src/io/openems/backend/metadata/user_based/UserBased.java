package io.openems.backend.metadata.user_based;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.common.access_control.AccessControl;
import io.openems.common.access_control.RoleId;
import io.openems.common.utils.FileUtils;
import io.openems.backend.metadata.api.BackendUser;
import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.Edge.State;
import io.openems.backend.metadata.api.IntersectChannels;
import io.openems.backend.metadata.api.Metadata;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonKeys;
import io.openems.common.utils.JsonUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO Fill the comment as soon as the logic works
 */
@Designate(ocd = Config.class, factory = false)
@Component(name = "Metadata.UserBased", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class UserBased extends AbstractOpenemsBackendComponent implements Metadata, IntersectChannels {

    private final Logger log = LoggerFactory.getLogger(UserBased.class);
    private final Map<String, BackendUser> sessionIdUserMap = new HashMap<>();
    private final Map<String, BackendUser> userMap = new HashMap<>();
    private final Map<BackendUser, String> userPasswordMapping = new HashMap<>();
    private final Map<String, Edge> edges = new HashMap<>();
    private String path = "";
    private AtomicInteger sessionId = new AtomicInteger(0);
    private final AccessControl accessControl = AccessControl.getInstance();

    public UserBased() {
        super("Metadata.UserBased");
    }

    @Activate
    void activate(Config config) {
        log.info("Activate [path=" + config.path() + "]");
        this.path = config.path();

        // Read the data async
        CompletableFuture.runAsync(this::refreshData);
    }

    @Deactivate
    void deactivate() {
        this.logInfo(this.log, "Deactivate");
    }

    @Override
    public BackendUser authenticate() throws OpenemsNamedException {
        // TODO remove this method
        return userMap.get("user0");
    }

    public BackendUser authenticate(String username, String password) throws OpenemsNamedException {
        Optional<Entry<BackendUser, String>> entryOpt = this.userPasswordMapping.entrySet().stream().filter(
                entry -> entry.getKey().getId().equals(username)).findFirst();
        final BackendUser[] user = {null};
        entryOpt.ifPresent(entry -> {
            if (entry.getValue().equals("Password")) {
                userPasswordMapping.put(entry.getKey(), String.valueOf(this.sessionId.incrementAndGet()));
                user[0] = entry.getKey();
            }
        });
        if (user[0] != null) {
            return user[0];
        } else {
            throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
        }
    }

    @Override
    public BackendUser authenticate(String sessionId) throws OpenemsNamedException {
        if (this.sessionIdUserMap.containsKey(sessionId)) {
            return this.sessionIdUserMap.get(sessionId);
        }
        throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
    }

    @Override
    public RoleId authenticate2(String userName, String password, String roleId) throws OpenemsException {
        return accessControl.login(userName, password, roleId);
    }

    @Override
    public synchronized Optional<String> getEdgeIdForApikey(String apikey) {
        Optional<Optional<Entry<String, Edge>>> edgeId = Optional.of(this.edges.entrySet().stream().filter(
                e -> e.getValue().getApikey().equals(apikey)).findFirst());
        if (edgeId.isPresent() && edgeId.get().isPresent()) {
            return Optional.of(edgeId.get().get().getValue().getId());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public synchronized Optional<Edge> getEdge(String edgeId) {
        this.refreshData();
        Edge edge = this.edges.get(edgeId);
        return Optional.ofNullable(edge);
    }

    @Override
    public Optional<BackendUser> getUser(String userId) {
        return Optional.of(this.userMap.get(userId));
    }

    @Override
    public synchronized Collection<Edge> getAllEdges() {
        return Collections.unmodifiableCollection(this.edges.values());
    }

    /**
     * This method checks whether the requested channels are available for the user with the given user id
     * @param userId the requesting user
     * @param edgeId the id of the edge of the channels
     * @param requestedChannels the requested channels
     * @return all permitted channels
     * @throws OpenemsException gets thrown in case the user is not permitted of accessing the edge
     */
    @Override
    public TreeSet<ChannelAddress> checkChannels(String userId, String edgeId, TreeSet<ChannelAddress> requestedChannels) throws OpenemsException {
        if (!this.getUser(userId).isPresent()) {
            throw new OpenemsException("There is no user with id (" + userId + ")");
        }
        BackendUser user = checkPermissionAndThrow(userId, edgeId);

        // the user also has the permission to access the edge
        List<ChannelAddress> permittedChannels = user.getChannels(edgeId);
        if (permittedChannels == null || permittedChannels.isEmpty()) {
            // Seems like there is an edge configured without any channels which may get accessed
            return new TreeSet<>();
        }

        TreeSet<ChannelAddress> retVal = new TreeSet<>(permittedChannels);
        retVal.retainAll(requestedChannels);
        return retVal;
    }

    /**
     * Checks whether the user is permitted to access the given edge
     * @param userId id of user
     * @param edgeId if of edge
     * @return the requested user
     * @throws OpenemsException gets thrown if the permission is missing
     */
    private BackendUser checkPermissionAndThrow(String userId, String edgeId) throws OpenemsException {
        // there is a user configured for the given userId
        BackendUser user = this.getUser(userId).get();
        if (user.getEdgeRoles().get(edgeId) != null && !user.getEdgeRoles().get(edgeId).isAtLeast(Role.GUEST)) {
            throw new OpenemsException("User (" + userId + ") does not have the permission to access the edge (" + edgeId + ")");
        }
        return user;
    }


    /**
     * In case there is a path for a configuration file configured, this method extracts the JSON-encoded information
     * and fills the fields of the class.<br>
     * See also: {@link UserBased#userMap}, {@link UserBased#edges}
     */
    private synchronized void refreshData() {
        if (!this.edges.isEmpty()) {
            return;
        }

        StringBuilder sb = FileUtils.checkAndGetFileContent(this.path);
        if (sb == null) {
            // exception occurred. File could not be read
            return;
        }

        List<Edge> edges = new ArrayList<>();

        // parse to JSON
        try {
            JsonElement config = JsonUtils.parse(sb.toString());
            JsonArray jUsers = JsonUtils.getAsJsonArray(config, JsonKeys.USERS.value());
            for (JsonElement jUser : jUsers) {
                // handle the user
                String userId = JsonUtils.getAsString(jUser, JsonKeys.USER_ID.value());
                String name = JsonUtils.getAsString(jUser, JsonKeys.NAME.value());
                BackendUser user = new BackendUser(userId, name);
                this.userMap.put(userId, user);
                // handle the connected edges
                for (JsonElement jEdge : JsonUtils.getAsJsonArray(jUser, JsonKeys.EDGES.value())) {
                    String edgeId = JsonUtils.getAsString(jEdge, JsonKeys.EDGE_ID.value());
                    JsonArray permittedChannels = JsonUtils.getAsJsonArray(jEdge, JsonKeys.PERMITTED_CHANNELS.value());
                    // TODO handle the permissions of the user
                    user.addEdgeRole(edgeId, Role.ADMIN);
                    edges.add(new Edge(//
                            edgeId,
                            JsonUtils.getAsString(jEdge, JsonKeys.API_KEY.value()), //
                            JsonUtils.getAsString(jEdge, JsonKeys.COMMENT.value()), //
                            State.ACTIVE, // State
                            "", // Version
                            "", // Product-Type
                            new EdgeConfig(), // Config
                            null, // State of Charge
                            null // IPv4
                    ));

                    // handle the permitted channels for the current user's edge
                    List<ChannelAddress> addresses = new ArrayList<>();
                    for (JsonElement channel : permittedChannels) {
                        addresses.add(new ChannelAddress(
                                JsonUtils.getAsString(channel, JsonKeys.COMPONENT_ID.value()),
                                JsonUtils.getAsString(channel, JsonKeys.CHANNEL_ID.value())));
                    }

                    user.addEdgeChannel(edgeId, addresses);
                }
            }
        } catch (OpenemsNamedException e) {
            this.logWarn(this.log, "Unable to parse JSON-file [" + this.path + "]: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Add Edges and configure User permissions
        for (Edge edge : edges) {
            this.edges.put(edge.getId(), edge);
        }
    }
}
