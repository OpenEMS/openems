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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Role;

public class LdapEdgeReader {

    private static final Logger log = LoggerFactory.getLogger(LdapEdgeReader.class);

    private String ldapEdgesOu;
    private String ldapEdgesOuFilter;
    private String ldapEdgesGroupOu;
    private String ldapEdgesGroupOuFilter;

    private LdapContextManager ldapContextManager;
    private MetadataLdap metadataLdap;

    public LdapEdgeReader(LdapContextManager ldapContextManager, MetadataLdap metadataLdap, Config config) {
        log.info("Creating new LDAP edge reader.");

        this.ldapContextManager = ldapContextManager;
        this.metadataLdap = metadataLdap;

        ldapEdgesOu = config.ldapEdgesOu();
        ldapEdgesOuFilter = config.ldapEdgesOuFilter();
        ldapEdgesGroupOu = config.ldapEdgesGroupOu();
        ldapEdgesGroupOuFilter = config.ldapEdgesGroupOuFilter();
    }

    public Map<LdapEdge, Set<String>> load(
        Map<String, LdapEdge> edgesByCn,
        Map<String, LdapEdge> edgesBySerialNumber
    ) throws OpenemsNamedException {
        log.info("Reading edge list from LDAP.");

        Map<LdapEdge, Set<String>> result = new HashMap<>();

        try {
            NamingEnumeration<SearchResult> renum = ldapContextManager.getContext().search(
                ldapEdgesOu,
                ldapEdgesOuFilter,
                LdapUtils.createSearchControls("cn", "description", "owner", "serialNumber")
            );

            while (renum.hasMore()) {
                SearchResult result3 = renum.next();

                String distinguishedName = result3.getNameInNamespace();
                Attributes resultAttributes = result3.getAttributes();

                // cn
                String cn = LdapUtils.extractValueFromAttributes(resultAttributes, "cn", distinguishedName);
                if (cn == null) {
                    continue;
                }

                // serialnumber
                String serialNumber = LdapUtils.extractValueFromAttributes(resultAttributes, "serialNumber", distinguishedName);
                if (serialNumber == null) {
                    continue;
                }

                // description
                String description = LdapUtils.extractValueFromAttributes(
                    resultAttributes,
                    "description",
                    "Edge " + cn,
                    distinguishedName,
                    false
                );

                // edge groups
                List<String> edgeGroups = LdapUtils.extractValuesFromAttributes(resultAttributes, "owner", distinguishedName);

                LdapEdge edge = edgesByCn.get(cn);

                if (edge == null) {
                    log.info("Adding edge " + cn + " with serial number " + serialNumber + ".");

                    LdapEdge edgeBySerialNumber = edgesBySerialNumber.get(serialNumber);
                    if (edgeBySerialNumber != null) {
                        log.error("Edge with serial number " + serialNumber + " already exists: " + edgeBySerialNumber.getCn() + ".");
                        continue;
                    }

                    edge = new LdapEdge(metadataLdap, cn, serialNumber, description);

                } else {
                    log.info("Updating edge " + cn + " with serialNumber " + serialNumber + ".");

                    edge.setComment(description);
                    edge.setSerialNumber(serialNumber);

                    edgesByCn.remove(cn);
                }

                result.put(edge, new HashSet<>(edgeGroups));
            }

        } catch (Exception ex) {
            log.error("Could not access LDAP.", ex);
            throw OpenemsError.GENERIC.exception("Could not access LDAP server: " + ex.getMessage());
        }

        log.info("Found " + result.size() + " edges in LDAP.");
        return result;
    }

    public Map<String, Role> readEdgeGroupsForUser(String userId) throws OpenemsNamedException {
        log.info("Reading edge groups from LDAP for user " + userId + ".");

        Map<String, Role> edgeGroups = new HashMap<>();

        try {
            NamingEnumeration<SearchResult> renum = ldapContextManager.getContext().search(
                ldapEdgesGroupOu,
                ldapEdgesGroupOuFilter.replace("%%userid%%", userId),
                LdapUtils.createSearchControls("businessCategory")
            );

            while (renum.hasMoreElements()) {
                SearchResult result4 = renum.next();

                String distinguishedName = result4.getNameInNamespace();
                Attributes resultAttributes4 = result4.getAttributes();

                // businessCategory
                String businessCategory = LdapUtils.extractValueFromAttributes(resultAttributes4, "businessCategory", distinguishedName);
                if (businessCategory == null) {
                    continue;
                }

                Role role = Role.getRole(businessCategory);
                edgeGroups.put(distinguishedName, role);
            }

            log.info("edge groups: " + edgeGroups);

            return edgeGroups;

        } catch (NamingException ex) {
            log.error("Could not access LDAP.", ex);
            throw OpenemsError.GENERIC.exception("Could not access LDAP server: " + ex.getMessage());
        }

    }
}
