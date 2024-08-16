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

import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.common.session.Role;

public class LdapUserReader {

    private static final Logger log = LoggerFactory.getLogger(LdapUserReader.class);

    private String ldapInitialContextFactory;
    private String ldapProviderUrl;
    private String ldapSecurityPrincipalTemplate;
    private String ldapUsersOu;
    private String ldapUsersOuFilter;
    private String ldapGroupOuGuest;
    private String ldapGroupOuOwner;
    private String ldapGroupOuInstaller;
    private String ldapGroupOuAdmin;

    private LdapContextManager ldapContextManager;
    private EdgeManager edgeManager;

    public LdapUserReader(LdapContextManager ldapContextManager, EdgeManager edgeManager, Config config) {
        log.info("Creating new LDAP user reader.");

        this.ldapContextManager = ldapContextManager;
        this.edgeManager = edgeManager;

        ldapInitialContextFactory = config.ldapInitialContextFactory();
        ldapProviderUrl = config.ldapProviderUrl();
        ldapSecurityPrincipalTemplate = config.ldapSecurityPrincipalTemplate();
        ldapUsersOu = config.ldapUsersOu();
        ldapUsersOuFilter = config.ldapUsersOuFilter();
        ldapGroupOuGuest = config.ldapGroupOuGuest();
        ldapGroupOuOwner = config.ldapGroupOuOwner();
        ldapGroupOuInstaller = config.ldapGroupOuInstaller();
        ldapGroupOuAdmin = config.ldapGroupOuAdmin();
    }

    public boolean authenticate(String username, String credentials) throws OpenemsNamedException {

        // authenticate user against LDAP
        try {
            if (!username.matches("[\\w\\s]*") || !credentials.matches("[\\w]*")) {
                log.warn("User " + username + " or password contains illegal characters.");

                throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
            }

            String principal = ldapSecurityPrincipalTemplate.replace("%%username%%", username);

            Properties props = new Properties();
            props.put(Context.INITIAL_CONTEXT_FACTORY, ldapInitialContextFactory);
            props.put(Context.PROVIDER_URL, ldapProviderUrl);
            props.put(Context.SECURITY_PRINCIPAL, principal);
            props.put(Context.SECURITY_CREDENTIALS, credentials);

            InitialDirContext authContext = new InitialDirContext(props);
            authContext.close();

            return true;

        } catch (AuthenticationException ex) {
            log.warn("User " + username + " not found or wrong password.");
            throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();

        } catch (NamingException ex) {
            log.error("Could not access LDAP.", ex);
            throw OpenemsError.GENERIC.exception("Could not access LDAP server: " + ex.getMessage());
        }
    }

    public User readUser(String username) throws OpenemsNamedException {
        try {
            DirContext context = ldapContextManager.getContext();

            NamingEnumeration<SearchResult> searchResults = context.search(
                ldapUsersOu,
                ldapUsersOuFilter.replace("%%username%%", username),
                LdapUtils.createSearchControls(
                    "uid", "memberOf",
                    "displayName", "givenName", "sn",
                    "street", "postalCode", "l", "st",
                    "telephoneNumber", "mail",
                    "preferredLanguage"
                )
            );

            if (!searchResults.hasMore()) {
                log.warn("User " + username + " not found.");
                throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
            }

            SearchResult searchResult = searchResults.next();
            Attributes resultAttributes = searchResult.getAttributes();

            // id
            String id = LdapUtils.extractValueFromAttributes(resultAttributes, "uid", username);
            if (id == null) {
                throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
            }

            // name
            final String name = LdapUtils.extractValueFromAttributes(resultAttributes, "displayName", id, username);

            // firstname
            final String firstname = LdapUtils.extractValueFromAttributes(resultAttributes, "givenName", "", username, false);

            // lastname
            final String lastname = LdapUtils.extractValueFromAttributes(resultAttributes, "sn", "", username, false);

            // phone
            final String phone = LdapUtils.extractValueFromAttributes(resultAttributes, "telephoneNumber", username, false);

            // email
            final String email = LdapUtils.extractValueFromAttributes(resultAttributes, "mail", username, false);

            // street
            final String street = LdapUtils.extractValueFromAttributes(resultAttributes, "street", username, false);

            // zip
            final String zip = LdapUtils.extractValueFromAttributes(resultAttributes, "postalCode", username, false);

            // city
            final String city = LdapUtils.extractValueFromAttributes(resultAttributes, "l", username, false);

            // country
            final String country = LdapUtils.extractValueFromAttributes(resultAttributes, "st", username, false);

            // token
            final String token = UUID.randomUUID().toString();

            // language
            final String preferredLanguage = LdapUtils.extractValueFromAttributes(resultAttributes, "preferredLanguage", username, false);
            Language language = Language.from(Optional.ofNullable(preferredLanguage));
            if (language == null) {
                language = Language.DEFAULT;
                log.info("language attribute not found for user " + username + ", using " + language + ".");
            }

            // globalRole
            final List<String> memberOf = LdapUtils.extractValuesFromAttributes(resultAttributes, "memberOf", username);

            Role globalRole = null;
            if (memberOf.contains(ldapGroupOuGuest)) {
                globalRole = Role.GUEST;
            }
            if (memberOf.contains(ldapGroupOuOwner)) {
                globalRole = Role.OWNER;
            }
            if (memberOf.contains(ldapGroupOuInstaller)) {
                globalRole = Role.INSTALLER;
            }
            if (memberOf.contains(ldapGroupOuAdmin)) {
                globalRole = Role.ADMIN;
            }

            if (globalRole == null) {
                log.error("GlobalRole could not be determined for user " + username + ".");
                throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
            }

            // roles
            NavigableMap<String, Role> roles = edgeManager.getEdgesWithRoles(id, globalRole);

            // hasMultipleEdges
            final boolean hasMultipleEdges = (roles.size() > 1);

            // settings
            JsonObject address = new JsonObject();
            if (street != null) {
                address.addProperty("street", street);
            }

            if (zip != null) {
                address.addProperty("zip", zip);
            }

            if (city != null) {
                address.addProperty("city", city);
            }

            if (country != null) {
                address.addProperty("country", country);
            }

            JsonObject settings = new JsonObject();
            settings.add("address", address);

            if (firstname != null) {
                settings.addProperty("firstname", firstname);
            }

            if (lastname != null) {
                settings.addProperty("lastname", lastname);
            }

            if (email != null) {
                settings.addProperty("email", email);
            }

            if (phone != null) {
                settings.addProperty("phone", phone);
            }

            User user = new User(id, name, token, language, globalRole, roles, hasMultipleEdges, settings);

            return user;

        } catch (NamingException ex) {
            log.error("Could not access LDAP.", ex);
            throw OpenemsError.GENERIC.exception("Could not access LDAP server: " + ex.getMessage());
        }
    }

}
