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

    private boolean running;

    private final Object lock = new Object();

    public LdapContextManager(Config config) {
        log.info("Creating new LDAP context manager.");

        ldapInitialContextFactory = config.ldapInitialContextFactory();
        ldapProviderUrl = config.ldapProviderUrl();
        ldapSecurityPrincipal = config.ldapSecurityPrincipal();
        ldapSecurityCredentials = config.ldapSecurityCredentials();

        running = true;
    }

    private void createContext() throws NamingException {
        log.info("Creating new LDAP context.");

        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, ldapInitialContextFactory);
        props.put(Context.PROVIDER_URL, ldapProviderUrl);

        if (ldapSecurityPrincipal != null) {
            props.put(Context.SECURITY_PRINCIPAL, ldapSecurityPrincipal);
            props.put(Context.SECURITY_CREDENTIALS, ldapSecurityCredentials);
        }

        context = new InitialDirContext(props);

        log.info("New LDAP context created.");
    }

    public DirContext getContext() throws NamingException {
        if (!running) {
            throw new NamingException("The LDAP context manager is shutdown.");
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

        running = false;
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
