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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    name = "Metadata.Ldap",
    description = "Configures the Metadata Ldap provider"
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
        required = false
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
        name = "Group ou for guests",
        description = "The LDAP group ou for guests.",
        defaultValue = "cn=openems_owner,ou=groups,dc=test",
        required = true
    )
    String ldapGroupOuOwner();

    @AttributeDefinition(
        name = "Group ou for guests",
        description = "The LDAP group ou for guests.",
        defaultValue = "cn=openems_installer,ou=groups,dc=test",
        required = true
    )
    String ldapGroupOuInstaller();

    @AttributeDefinition(
        name = "Group ou for guests",
        description = "The LDAP group ou for guests.",
        defaultValue = "cn=openems_admin,ou=groups,dc=test",
        required = true
    )
    String ldapGroupOuAdmin();

    @AttributeDefinition(
        name = "Edges ou",
        description = "The LDAP edges ou.",
        defaultValue = "ou=edges,ou=ems,dc=test",
        required = true
    )
    String ldapEdgesOu();

    @AttributeDefinition(
        name = "Edges ou filter",
        description = "The LDAP edges ou filter.",
        defaultValue = "objectclass=device",
        required = true
    )
    String ldapEdgesOuFilter();

    @AttributeDefinition(
        name = "Edges group ou",
        description = "The LDAP edges group ou.",
        defaultValue = "ou=groups,ou=ems,dc=test",
        required = true
    )
    String ldapEdgesGroupOu();

    @AttributeDefinition(
        name = "Edges group ou filter",
        description = "The LDAP edges group ou filter.",
        defaultValue = "(&(objectclass=groupOfNames)(member=uid=%%userid%%,ou=users,dc=test))",
        required = true
    )
    String ldapEdgesGroupOuFilter();

    @AttributeDefinition(
        name = "Edges cache timeout",
        description = "The LDAP edges caching time in ms.",
        defaultValue = "120000",
        required = true,
        type = AttributeType.LONG
    )
    long ldapEdgeCacheTimeout();

    String webconsole_configurationFactory_nameHint() default "Metadata.Ldap";

}
