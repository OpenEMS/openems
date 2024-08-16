/*
 *   OpenEMS Metadata LDAP bundle
 *
 *   Written by Christian Poulter.
 *   Copyright (C) 2024 Christian Poulter <devel(at)poulter.de>
 *
 *   This program and the accompanying materials are made available under
 *   the terms of the Eclipse Public License v2.0 which accompanies this
 *   distribution, and is available at
 *
 *   https://www.eclipse.org/legal/epl-2.0
 *
 *   SPDX-License-Identifier: EPL-2.0
 *
 */

package io.openems.backend.metadata.ldap;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.response.GetEdgesResponse.EdgeMetadata;
import io.openems.common.session.Role;

public class EdgeManager {

    private static final Logger log = LoggerFactory.getLogger(EdgeManager.class);

    private final Map<String, LdapEdge> edgesById = new HashMap<>();
    private final Map<String, LdapEdge> edgesBySerialNumber = new HashMap<>();
    private final Map<String, Set<String>> edgesByEdgeGroup = new HashMap<>();

    private long nextEdgeRefresh;
    private long ldapEdgeCacheTimeout;

    private LdapEdgeReader ldapEdgeReader;

    public EdgeManager(Config config, LdapEdgeReader ldapEdgeReader) {
        log.info("Creating new LDAP edge manager.");

        this.ldapEdgeReader = ldapEdgeReader;

        nextEdgeRefresh = 0;
        ldapEdgeCacheTimeout = config.ldapEdgeCacheTimeout();
    }

    public Collection<LdapEdge> getEdges() {
        refresh();

        return edgesById.values();
    }

    public LdapEdge getById(String edgeId) {
        refresh();

        return edgesById.get(edgeId);
    }

    public LdapEdge getBySerialNumber(String serialNumber) {
        refresh();

        return edgesBySerialNumber.get(serialNumber);
    }

    public int size() {
        refresh();

        return edgesById.size();
    }

    public void clear() {
        log.info("Clearing edge cache.");

        edgesById.clear();
        edgesBySerialNumber.clear();
        edgesByEdgeGroup.clear();

        nextEdgeRefresh = 0;
    }

    public synchronized void refresh() {

        // no refresh of edges from LDAP required
        if (nextEdgeRefresh > System.currentTimeMillis()) {
            return;
        }

        log.info("Refreshing edge managers LDAP cache.");

        Map<String, LdapEdge> currentEdgesByCn = new HashMap<>();
        Map<String, LdapEdge> currentEdgesBySerialNumber = new HashMap<>();

        for (LdapEdge edge : edgesById.values()) {
            currentEdgesByCn.put(edge.getCn(), edge);
            currentEdgesBySerialNumber.put(edge.getSerialNumber(), edge);
        }

        clear();

        try {
            Map<LdapEdge, Set<String>> results = ldapEdgeReader.load(currentEdgesByCn, currentEdgesBySerialNumber);
            for (Entry<LdapEdge, Set<String>> result : results.entrySet()) {
                put(result.getKey(), result.getValue());
            }

        } catch (OpenemsNamedException ex) {
            log.error("Could not refresh edge list from LDAP.", ex);
            clear();
        }

        nextEdgeRefresh = System.currentTimeMillis() + ldapEdgeCacheTimeout;
    }

    private void put(LdapEdge edge, Set<String> edgeGroups) {
        if ((edge == null)
         || (edge.getId() == null)
         || (edge.getSerialNumber() == null)
         || (edgeGroups == null)
        ) {
            return;
        }

        log.info("Adding edge " + edge.getId() + " with serialnumber " + edge.getSerialNumber() + " for edge groups " + String.join(";", edgeGroups) + " to cache.");

        edgesById.put(edge.getId(), edge);
        edgesBySerialNumber.put(edge.getSerialNumber(), edge);

        for (String edgeGroup : edgeGroups) {
            Set<String> edges = edgesByEdgeGroup.computeIfAbsent(edgeGroup, key -> new HashSet<>());
            edges.add(edge.getId());
        }
    }

    public NavigableMap<String, Role> getEdgesWithRoles(String userId, Role globalRole) throws OpenemsNamedException {
        log.info("computing edges with roles for user " + userId + " and role " + globalRole + ".");

        NavigableMap<String, Role> roles = new TreeMap<>();

        refresh();

        // admin is allow to see all edges
        if (globalRole.isAtLeast(Role.ADMIN)) {
            for (LdapEdge edge : edgesById.values()) {
                roles.put(edge.getId(), Role.ADMIN);
            }

            // non admin users need assignment through groups in LDAP
        } else {
            Map<String, Role> edgeGroupsForUser = ldapEdgeReader.readEdgeGroupsForUser(userId);

            for (Entry<String, Role> edgeGroup : edgeGroupsForUser.entrySet()) {
                String edgeGroupName = edgeGroup.getKey();
                Role role = edgeGroup.getValue();
                Set<String> edges = edgesByEdgeGroup.get(edgeGroupName);

                if (edges != null) {
                    for (String edge : edges) {
                        roles.put(edge, role);
                    }
                }
            }
        }

        log.info("Found edges with roles for user " + userId + ": " + roles);

        return roles;
    }

    public EdgeMetadata getEdgeMetadataForUser(User user, String edgeId) throws OpenemsNamedException {
        if (user == null) {
            return null;
        }

        log.info("computing edge metadata for user " + user.getId() + " and edge + " + edgeId + ".");

        LdapEdge edge = edgesById.get(edgeId);
        if (edge == null) {
            return null;
        }

        NavigableMap<String, Role> userRoles = user.getEdgeRoles();
        Role userRoleForEdge = userRoles.get(edge.getId());

        if (userRoleForEdge == null) {
            return null;
        }

        return new EdgeMetadata(
            edge.getId(),
            edge.getComment(),
            edge.getProducttype(),
            edge.getVersion(),
            userRoleForEdge,
            edge.isOnline(),
            edge.getLastmessage(),
            null,
            edge.getSumState()
        );
    }

}
