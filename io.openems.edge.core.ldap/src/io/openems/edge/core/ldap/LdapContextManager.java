/*
 *   OpenEMS Edge Core LDAP bundle
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

package io.openems.edge.core.ldap;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdapContextManager {

    private static final Logger log = LoggerFactory.getLogger(LdapContextManager.class);

    private String ldapInitialContextFactory;
    private String ldapProviderUrl;
    private String ldapSecurityPrincipal;
    private String ldapSecurityCredentials;

    private InitialDirContext context;

    private boolean active;

    private final Object lock = new Object();

    public LdapContextManager(Config config) {
        log.info("Creating new LDAP context manager.");

        ldapInitialContextFactory = config.ldapInitialContextFactory();
        ldapProviderUrl = config.ldapProviderUrl();
        ldapSecurityPrincipal = config.ldapSecurityPrincipal();
        ldapSecurityCredentials = config.ldapSecurityCredentials();

        active = true;
    }

    private void createContext() throws NamingException {
        log.info("Creating new LDAP context.");

        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, ldapInitialContextFactory);
        props.put(Context.PROVIDER_URL, ldapProviderUrl);
    
        if ((ldapSecurityPrincipal != null) && (ldapSecurityCredentials != null)) {
            props.put(Context.SECURITY_PRINCIPAL, ldapSecurityPrincipal);
            props.put(Context.SECURITY_CREDENTIALS, ldapSecurityCredentials);
        }
    
        context = new InitialDirContext(props);

        log.info("New LDAP context created.");
    }

    public DirContext getContext() throws NamingException {
        if (!active) {
            throw new NamingException("The LDAP context manager is not active.");
        }

        synchronized (lock) {
            if (context == null) {
                createContext();
            }
        }

        return context;
    }

    public void reconnect() {
        log.info("Reconnecting LDAP context.");

        close();
    }

    public void shutdown() {
        log.info("Shutting down LDAP context manager.");

        close();

        active = false;
    }

    private void close() {

        synchronized (lock) {
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException ex) {
                    log.error("Could not shutdown LDAP context manager.", ex);
                }
            }

            context = null;
        }
    }
}
