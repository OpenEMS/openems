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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import org.junit.Test;

import io.openems.common.session.Language;
import io.openems.common.session.Role;

public class LdapUtilsTest {

    private static final Map<String, Role> roleMap;
    
    static {
        roleMap = new HashMap<>();
        roleMap.put("ldapGroupOuGuest", Role.GUEST);
        roleMap.put("ldapGroupOuOwner", Role.OWNER);
        roleMap.put("ldapGroupOuInstaller", Role.INSTALLER);
        roleMap.put("ldapGroupOuAdmin", Role.ADMIN);
    }

    //////////////// extractGlobalRole //////////////// 
    @Test
    public void extractGlobalRoleMissingMapTest() throws Exception {
        Attributes attributes = new BasicAttributes("memberOf", "ldapGroupOuGuest");
        Role globalRole = LdapUtils.extractGlobalRole(attributes, "memberOf", "username", null);
        assertNull(globalRole);
        
        globalRole = LdapUtils.extractGlobalRole(attributes, "memberOf", "username", new HashMap<>());
        assertNull(globalRole);
    }

    @Test
    public void extractGlobalRoleEmptyTest() throws Exception {
        Attributes attributes = new BasicAttributes();        
        Role globalRole = LdapUtils.extractGlobalRole(attributes, "memberOf", "username", roleMap);
        assertNull(globalRole);
    }

    @Test
    public void extractGlobalRoleUnknownTest() throws Exception {
        Attributes attributes = new BasicAttributes("memberOf", "XXX");
        Role globalRole = LdapUtils.extractGlobalRole(attributes, "memberOf", "username", roleMap);
        assertNull(globalRole);        
    }

    @Test
    public void extractGlobalRoleGuestTest() throws Exception {
        Attributes attributes = new BasicAttributes("memberOf", "ldapGroupOuGuest");
        Role globalRole = LdapUtils.extractGlobalRole(attributes, "memberOf", "username", roleMap);
        assertEquals(Role.GUEST, globalRole);        
    }    
    
    @Test
    public void extractGlobalRoleOwnerTest() throws Exception {
        Attributes attributes = new BasicAttributes("memberOf", "ldapGroupOuOwner");
        Role globalRole = LdapUtils.extractGlobalRole(attributes, "memberOf", "username", roleMap);
        assertEquals(Role.OWNER, globalRole);        
    }
    
    @Test
    public void extractGlobalRoleInstallerTest() throws Exception {
        Attributes attributes = new BasicAttributes("memberOf", "ldapGroupOuInstaller");
        Role globalRole = LdapUtils.extractGlobalRole(attributes, "memberOf", "username", roleMap);
        assertEquals(Role.INSTALLER, globalRole);        
    }    

    @Test
    public void extractGlobalRoleAdminTest() throws Exception {
        Attributes attributes = new BasicAttributes("memberOf", "ldapGroupOuAdmin");
        Role globalRole = LdapUtils.extractGlobalRole(attributes, "memberOf", "username", roleMap);
        assertEquals(Role.ADMIN, globalRole);        
    }    

    @Test
    public void extractGlobalRoleMultipleTest() throws Exception {
        Attributes attributes = new BasicAttributes();
        attributes.put("memberOf", "ldapGroupOuGuest");
        attributes.put("memberOf", "ldapGroupOuOwner");
        Role globalRole = LdapUtils.extractGlobalRole(attributes, "memberOf", "username", roleMap);
        assertEquals(Role.OWNER, globalRole);        

        attributes.put("memberOf", "ldapGroupOuInstaller");
        globalRole = LdapUtils.extractGlobalRole(attributes, "memberOf", "username", roleMap);
        assertEquals(Role.INSTALLER, globalRole);        

        attributes.put("memberOf", "ldapGroupOuAdmin");
        globalRole = LdapUtils.extractGlobalRole(attributes, "memberOf", "username", roleMap);
        assertEquals(Role.ADMIN, globalRole);        
    }    

    //////////////// extractLanguage ////////////////
    @Test
    public void extractLanguageTest() throws Exception {
        Attributes attributes = new BasicAttributes("preferredLanguage", "de");
        Language language = LdapUtils.extractLanguage(attributes, "preferredLanguage", "username");
        assertEquals(Language.DE, language);        
    }    
    
    @Test
    public void extractLanguageInvalidTest() throws Exception {
        Attributes attributes = new BasicAttributes("preferredLanguage", "XXXXXX");
        Language language = LdapUtils.extractLanguage(attributes, "preferredLanguage", "username");
        assertEquals(Language.DEFAULT, language);        
    }
    
    @Test
    public void extractLanguageEmptyTest() throws Exception {
        Attributes attributes = new BasicAttributes();
        Language language = LdapUtils.extractLanguage(attributes, "preferredLanguage", "username");
        assertEquals(Language.DEFAULT, language);        
    }
    
    @Test
    public void extractLanguageMultipeTest() throws Exception {
        
        BasicAttribute attribute = new BasicAttribute("preferredLanguage", true);
        attribute.add("fr");
        attribute.add("de");
        attribute.add("en");        
        
        Attributes attributes = new BasicAttributes();
        attributes.put(attribute);
        
        Language language = LdapUtils.extractLanguage(attributes, "preferredLanguage", "username");
        assertEquals(Language.FR, language);        
    }    
    
}