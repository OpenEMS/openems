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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    name = "Core User LDAP",
    description = "This component handles User authentication through LDAP."
)
@interface Config {

    @AttributeDefinition(
        name = "Initial Context Factory",
        description = "The Initial Context Factory.",
        defaultValue = "com.sun.jndi.ldap.LdapCtxFactory",
        required = true
    )
    String ldapInitialContextFactory();

    @AttributeDefinition(
        name = "Provider URL",
        description = "The LDAP provider URL.",
        defaultValue = "ldaps://ldap.hostname:636",
        required = true
    )
    String ldapProviderUrl();

    @AttributeDefinition(
        name = "Security principal",
        description = "The LDAP security principal.",
        required = false
    )
    String ldapSecurityPrincipal();

    @AttributeDefinition(
        name = "Security credentials",
        description = "The LDAP security credentials.",
        required = false,
        type = AttributeType.PASSWORD
    )
    String ldapSecurityCredentials();

    @AttributeDefinition(
        name = "Principal template",
        description = "The LDAP principal template.",
        defaultValue = "uid=%%username%%,ou=users,dc=test",
        required = true
    )
    String ldapSecurityPrincipalTemplate();

    @AttributeDefinition(
        name = "Users ou",
        description = "The LDAP users ou.",
        defaultValue = "ou=users,dc=test",
        required = true
    )
    String ldapUsersOu();

    @AttributeDefinition(
        name = "Users ou filter",
        description = "The LDAP users ou filter.",
        defaultValue = "(&(uid=%%username%%)(objectClass=inetOrgPerson))",
        required = true
    )
    String ldapUsersOuFilter();

    @AttributeDefinition(
        name = "Group ou for guests",
        description = "The LDAP group ou for guests.",
        defaultValue = "cn=openems_guest,ou=groups,dc=test",
        required = true
    )
    String ldapGroupOuGuest();

    @AttributeDefinition(
        name = "Group ou for owners",
        description = "The LDAP group ou for owners.",
        defaultValue = "cn=openems_owner,ou=groups,dc=test",
        required = true
    )
    String ldapGroupOuOwner();

    @AttributeDefinition(
        name = "Group ou for installers",
        description = "The LDAP group ou for installers.",
        defaultValue = "cn=openems_installer,ou=groups,dc=test",
        required = true
    )
    String ldapGroupOuInstaller();

    @AttributeDefinition(
        name = "Group ou for admins",
        description = "The LDAP group ou for admins.",
        defaultValue = "cn=openems_admin,ou=groups,dc=test",
        required = true
    )
    String ldapGroupOuAdmin();
    
	String webconsole_configurationFactory_nameHint() default "Core User LDAP";
}