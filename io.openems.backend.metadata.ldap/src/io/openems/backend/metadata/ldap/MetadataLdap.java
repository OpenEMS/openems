/*
 *   OpenEMS Metadata LDAP bundle
 *   
 *   Written by Christian Poulter.   
 *   Copyright (C) 2024 Christian Poulter <devel(at)poulter.de>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package io.openems.backend.metadata.ldap;

import static java.util.stream.Collectors.joining;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.backend.common.alerting.OfflineEdgeAlertingSetting;
import io.openems.backend.common.alerting.SumStateAlertingSetting;
import io.openems.backend.common.alerting.UserAlertingSettings;
import io.openems.backend.common.metadata.AbstractMetadata;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.EdgeHandler;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.SimpleEdgeHandler;
import io.openems.backend.common.metadata.User;
import io.openems.common.channel.Level;
import io.openems.common.event.EventReader;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.GetEdgesRequest.PaginationOptions;
import io.openems.common.jsonrpc.request.GetEdgesRequest.PaginationOptions.SearchParams;
import io.openems.common.jsonrpc.response.GetEdgesResponse.EdgeMetadata;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.StringUtils;

@Designate(
    ocd = Config.class,
    factory = false
)
@Component(
    name = "Metadata.Ldap",
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    immediate = true
)
@EventTopics(
    {
        Edge.Events.ON_SET_CONFIG,
        Edge.Events.ON_SET_SUM_STATE,
        Edge.Events.ON_SET_PRODUCTTYPE,
        Edge.Events.ON_SET_VERSION,
        Metadata.Events.AFTER_IS_INITIALIZED
    }
)
public class MetadataLdap extends AbstractMetadata implements Metadata, EventHandler {

    private final Logger log = LoggerFactory.getLogger(MetadataLdap.class);

    @Reference
    private EventAdmin eventAdmin;

    private final SimpleEdgeHandler edgeHandler = new SimpleEdgeHandler();

    private UserManager userManager;
    private EdgeManager edgeManager;

    private LdapContextManager ldapContextManager;
    private LdapUserReader ldapReader;
    private LdapEdgeReader ldapEdgeReader;

    public MetadataLdap() {
        super("Metadata.Ldap");

        log.info("Creating new MetadataLdap.");
    }

    @Activate
    private void activate(Config config) {
        log.info("Activating Metadata LDAP");

        ldapContextManager = new LdapContextManager(config);
        ldapEdgeReader = new LdapEdgeReader(ldapContextManager, this, config);

        userManager = new UserManager();
        edgeManager = new EdgeManager(config, ldapEdgeReader);
        ldapReader = new LdapUserReader(ldapContextManager, edgeManager, config);

        setInitialized();

        CompletableFuture.runAsync(() -> edgeManager.refresh());
    }

    @Deactivate
    private void deactivate() throws OpenemsNamedException {
        log.info("Deactivating Metadata LDAP");

        CompletableFuture.runAsync(() -> ldapContextManager.shutdown());

        ldapReader = null;
        edgeManager = null;
        userManager = null;
        ldapEdgeReader = null;
        ldapContextManager = null;
    }

    // -------------------------------------------
    // users
    // -------------------------------------------

    @Override
    public User authenticate(String username, String password) throws OpenemsNamedException {
        log.info("Authenticating username " + username + ".");

        // authenticate user against LDAP
        ldapReader.authenticate(username, password);
        User user = ldapReader.readUser(username);
        userManager.put(user);

        log.info("Authentication for user " + user.getId() + " with token " + user.getToken() + " successfully. Global role is " + user.getGlobalRole() + ".");

        return user;
    }

    @Override
    public User authenticate(String token) throws OpenemsNamedException {
        log.info("Authenticating token " + token + ".");

        User user = userManager.getByToken(token);
        if (user == null) {
            throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
        }

        return user;
    }

    @Override
    public void logout(User user) {
        log.info("Logging out user " + user.getId() + ".");

        userManager.remove(user);
    }

    @Override
    public Optional<User> getUser(String userId) {
        return Optional.ofNullable(userManager.getByUserId(userId));
    }

    @Override
    public Map<String, Object> getUserInformation(User user) throws OpenemsNamedException {
        return userManager.getUserInformation(user);
    }

    // -------------------------------------------
    // edges
    // -------------------------------------------

    @Override
    public synchronized Optional<Edge> getEdge(String edgeId) {
        LdapEdge edge = edgeManager.getById(edgeId);
        return Optional.ofNullable(edge);
    }

    @Override
    public synchronized Optional<Edge> getEdgeBySetupPassword(String setupPassword) {
        log.warn("Get edge by setup password is not supported.");

        return Optional.empty();
    }

    @Override
    public synchronized Optional<String> getEdgeIdForApikey(String apiKey) {

        // Metadata LDAP uses the serialnumber as api key
        LdapEdge edge = edgeManager.getBySerialNumber(apiKey);

        if (edge != null) {
            return Optional.of(edge.getId());
        }

        return Optional.empty();
    }

    @Override
    public Optional<String> getSerialNumberForEdge(Edge edge) {
        LdapEdge myEdge = edgeManager.getById(edge.getId());
        if (myEdge == null) {
            return Optional.empty();
        }

        return Optional.of(myEdge.getSerialNumber());
    }

    @Override
    public Optional<Level> getSumState(String edgeId) {
        LdapEdge myEdge = edgeManager.getById(edgeId);
        if (myEdge == null) {
            return Optional.empty();
        }

        return Optional.of(myEdge.getSumState());
    }

    @Override
    public synchronized Collection<Edge> getAllOfflineEdges() {
        log.info("Getting all offline edges.");

        return edgeManager.getEdges()
            .stream()
            .filter(Edge::isOffline)
            .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<EdgeMetadata> getPageDevice(User user, PaginationOptions paginationOptions) throws OpenemsNamedException {
        log.info("Getting page device for user " + user.getId() + ".");

        NavigableMap<String, Role> userRoles = user.getEdgeRoles();
        Stream<LdapEdge> pagesStream = edgeManager.getEdges().stream();

        if (user.getGlobalRole().isLessThan(Role.ADMIN)) {
            pagesStream = pagesStream.filter(edge -> userRoles.containsKey(edge.getId()));
        }

        String query = paginationOptions.getQuery();
        if (query != null) {
            pagesStream = pagesStream.filter(
                edge -> StringUtils.containsWithNullCheck(edge.getId(), query)
                    || StringUtils.containsWithNullCheck(edge.getComment(), query)
                    || StringUtils.containsWithNullCheck(edge.getProducttype(), query)
            );
        }

        SearchParams searchParams = paginationOptions.getSearchParams();
        if (searchParams != null) {
            if (searchParams.searchIsOnline()) {
                pagesStream = pagesStream.filter(edge -> edge.isOnline() == searchParams.isOnline());
            }

            List<String> productTypes = searchParams.productTypes();
            if ((productTypes != null) && (productTypes.size() > 0)) {
                pagesStream = pagesStream.filter(edge -> productTypes.contains(edge.getProducttype()));
            }

            List<Level> sumStates = searchParams.sumStates();
            if ((sumStates != null) && (sumStates.size() > 0)) {
                pagesStream = pagesStream.filter(edge -> sumStates.contains(edge.getSumState()));
            }
        }

        return pagesStream
            .sorted((s1, s2) -> s1.getId().compareTo(s2.getId()))
            .skip(paginationOptions.getPage() * paginationOptions.getLimit())
            .limit(paginationOptions.getLimit())
            .map(edge -> new EdgeMetadata(
                edge.getId(),
                edge.getComment(),
                edge.getProducttype(),
                edge.getVersion(),
                userRoles.get(edge.getId()),
                edge.isOnline(),
                edge.getLastmessage(),
                null,
                Level.OK
            )
        ).toList();
    }

    @Override
    public EdgeMetadata getEdgeMetadataForUser(User user, String edgeId) throws OpenemsNamedException {
        return edgeManager.getEdgeMetadataForUser(user, edgeId);
    }

    // -------------------------------------------
    // miscellaneous functions
    // -------------------------------------------

    @Override
    public void logGenericSystemLog(GenericSystemLog systemLog) {
        log.info(
            "%s on %s executed %s [%s]",
            systemLog.user().getId(),
            systemLog.edgeId(),
            systemLog.teaser(),
            systemLog.getValues().entrySet().stream().map(t -> t.getKey() + "=" + t.getValue()).collect(joining(", "))
        );
    }

    @Override
    public EventAdmin getEventAdmin() {
        return this.eventAdmin;
    }

    @Override
    public EdgeHandler edge() {
        return this.edgeHandler;
    }

    @Override
    public void handleEvent(Event event) {
        log.info("Handling event with topic " + event.getTopic() + ".");
        EventReader reader = new EventReader(event);

        switch (event.getTopic()) {
        case Edge.Events.ON_SET_CONFIG:
            this.edgeHandler.setEdgeConfigFromEvent(reader);
            break;

        case Edge.Events.ON_SET_SUM_STATE: {
            LdapEdge edge = (LdapEdge) reader.getProperty(Edge.Events.OnSetLastmessage.EDGE);
            Level sumState = (Level) reader.getProperty(Edge.Events.OnSetSumState.SUM_STATE);

            edge.setSumState(sumState);
        }
            break;

        case Edge.Events.ON_SET_PRODUCTTYPE: {
            LdapEdge edge = (LdapEdge) reader.getProperty(Edge.Events.OnSetProducttype.EDGE);
            String producttype = reader.getString(Edge.Events.OnSetProducttype.PRODUCTTYPE);

            edge.setProducttype(producttype);
        }
            break;

        case Edge.Events.ON_SET_VERSION: {
            LdapEdge edge = (LdapEdge) reader.getProperty(Edge.Events.OnSetVersion.EDGE);
            SemanticVersion version = (SemanticVersion) reader.getProperty(Edge.Events.OnSetVersion.VERSION);

            edge.setVersion(version);
        }
            break;

        default:
            log.warn("Unhandeled event " + event.getTopic() + ".");
        }
    }

    // -------------------------------------------
    // unsupported by LDAP component
    // -------------------------------------------

    @Override
    public void setUserInformation(User user, JsonObject jsonObject) throws OpenemsNamedException {
        log.info("MetadataLdap.setUserInformation() [user=" + user.getId() + ", jsonObject=" + jsonObject + "]");

        throw new UnsupportedOperationException("setUserInformation() is not implemented");
    }

    @Override
    public void addEdgeToUser(User user, Edge edge) throws OpenemsNamedException {
        log.info("MetadataLdap.addEdgeToUser()");

        throw new UnsupportedOperationException("addEdgeToUser() is not implemented");
    }

    @Override
    public byte[] getSetupProtocol(User user, int setupProtocolId) throws OpenemsNamedException {
        log.info("MetadataLdap.getSetupProtocol()");

        throw new UnsupportedOperationException("getSetupProtocol() is not implemented");
    }

    @Override
    public JsonObject getSetupProtocolData(User user, String edgeId) throws OpenemsNamedException {
        log.info("MetadataLdap.getSetupProtocolData()");

        throw new UnsupportedOperationException("getSetupProtocolData() is not implemented");
    }

    @Override
    public int submitSetupProtocol(User user, JsonObject jsonObject) {
        log.info("MetadataLdap.submitSetupProtocol()");

        throw new UnsupportedOperationException("submitSetupProtocol() is not implemented");
    }

    @Override
    public void registerUser(JsonObject jsonObject, String oem) throws OpenemsNamedException {
        log.info("MetadataLdap.registerUser()");

        throw new UnsupportedOperationException("registerUser() is not implemented");
    }

    @Override
    public void updateUserLanguage(User user, Language locale) throws OpenemsNamedException {
        log.info("MetadataLdap.updateUserLanguage()");

        throw new UnsupportedOperationException("updateUserLanguage() is not implemented");
    }

    @Override
    public void updateUserSettings(User user, JsonObject settings) {
        log.info("MetadataLdap.updateUserSettings()");

        throw new UnsupportedOperationException("updateUserSettings() is not implemented");
    }

    @Override
    public UserAlertingSettings getUserAlertingSettings(String edgeId, String userId) throws OpenemsException {
        log.info("MetadataLdap.getUserAlertingSettings()");

        throw new UnsupportedOperationException("getUserAlertingSettings() is not implemented");
    }

    @Override
    public List<UserAlertingSettings> getUserAlertingSettings(String edgeId) {
        log.info("MetadataLdap.getUserAlertingSettings()");

        throw new UnsupportedOperationException("getUserAlertingSettings() is not implemented");
    }

    @Override
    public List<OfflineEdgeAlertingSetting> getEdgeOfflineAlertingSettings(String edgeId) throws OpenemsException {
        log.info("MetadataLdap.getEdgeOfflineAlertingSettings()");

        throw new UnsupportedOperationException("getEdgeOfflineAlertingSettings() is not implemented");
    }

    @Override
    public List<SumStateAlertingSetting> getSumStateAlertingSettings(String edgeId) throws OpenemsException {
        log.info("MetadataLdap.getSumStateAlertingSettings()");

        throw new UnsupportedOperationException("getSumStateAlertingSettings() is not implemented");
    }

    @Override
    public void setUserAlertingSettings(User user, String edgeId, List<UserAlertingSettings> users) {
        log.info("MetadataLdap.setUserAlertingSettings()");

        throw new UnsupportedOperationException("setUserAlertingSettings() is not implemented");
    }
}
