package io.openems.backend.metadata.user_based;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
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
import io.openems.common.utils.JsonUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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

    public UserBased() {
        super("Metadata.UserBased");
    }

    @Activate
    void activate(Config config) {
        log.info("Activate [path=" + config.path() + "]");
        this.path = config.path();

        // Read the data async
        CompletableFuture.runAsync(() -> {
            this.refreshData();
        });
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

    @Override
    public BackendUser authenticate(String username, String password) throws OpenemsNamedException {
        Optional<Entry<BackendUser, String>> entryOpt = this.userPasswordMapping.entrySet().stream().filter(entry -> entry.getKey().getId().equals(username)).findFirst();
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
    public synchronized Optional<String> getEdgeIdForApikey(String apikey) {
        this.refreshData();
        for (Entry<String, Edge> entry : this.edges.entrySet()) {
            Edge edge = entry.getValue();
            if (edge.getApikey().equals(apikey)) {
                return Optional.of(edge.getId());
            }
        }
        return Optional.empty();
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
        this.refreshData();
        return Collections.unmodifiableCollection(this.edges.values());
    }

    @Override
    public TreeSet<ChannelAddress> checkChannels(String userId, String edgeId, TreeSet<ChannelAddress> requestedChannels) throws OpenemsException {
        if (!this.getUser(userId).isPresent()) {
            throw new OpenemsException("There is no user with id (" + userId + ")");
        }
        // there is a user configrued for the given userId
        BackendUser user = this.getUser(userId).get();
        if (user.getEdgeRoles().get(edgeId) != null && !user.getEdgeRoles().get(edgeId).isAtLeast(Role.GUEST)) {
            throw new OpenemsException("User (" + userId + ") does not have the permission to access the edge (" + edgeId + ")");
        }

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


    private synchronized void refreshData() {
        if (!this.edges.isEmpty()) {
            return;
        }
        // read file
        StringBuilder sb = new StringBuilder();
        String line = null;
        try (BufferedReader br = new BufferedReader(new FileReader(this.path))) {
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            this.logWarn(this.log, "Unable to read file [" + this.path + "]: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        List<Edge> edges = new ArrayList<>();

        // parse to JSON
        try {
            JsonElement config = JsonUtils.parse(sb.toString());
            JsonArray jUsers = JsonUtils.getAsJsonArray(config, "users");
            for (JsonElement jUser : jUsers) {
                // handle the user
                String userId = JsonUtils.getAsString(jUser, "userId");
                String name = JsonUtils.getAsString(jUser, "name");
                BackendUser user = new BackendUser(userId, name);
                this.userMap.put(userId, user);
                // handle the connected edges
                for (JsonElement jEdge : JsonUtils.getAsJsonArray(jUser, "edges")) {
                    String edgeId = JsonUtils.getAsString(jEdge, "edgeId");
                    JsonArray permittedChannels = JsonUtils.getAsJsonArray(jEdge, "permittedChannels");
                    user.addEdgeRole(edgeId, Role.ADMIN);
                    edges.add(new Edge(//
                            edgeId,
                            JsonUtils.getAsString(jEdge, "apiKey"), //
                            JsonUtils.getAsString(jEdge, "comment"), //
                            State.ACTIVE, // State
                            "", // Version
                            "", // Product-Type
                            new EdgeConfig(), // Config
                            null, // State of Charge
                            null // IPv4
                    ));
                    List<ChannelAddress> addresses = new ArrayList<>();
                    for (JsonElement channel : permittedChannels) {
                        addresses.add(new ChannelAddress(
                                JsonUtils.getAsString(channel, "componentId"),
                                JsonUtils.getAsString(channel, "channelId")));
                    }

                    user.addEdgeChannel(edgeId, addresses);
                }

            }
        } catch (OpenemsNamedException e) {
            this.logWarn(this.log, "Unable to JSON-parse file [" + this.path + "]: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Add Edges and configure User permissions
        for (Edge edge : edges) {
            this.edges.put(edge.getId(), edge);
        }
    }
}
