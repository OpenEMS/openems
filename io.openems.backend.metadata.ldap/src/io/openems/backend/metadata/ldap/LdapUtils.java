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

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdapUtils {

    private static final Logger log = LoggerFactory.getLogger(LdapUtils.class);

    public static SearchControls createSearchControls(String... attributeNames) {
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setReturningAttributes(attributeNames);

        return searchControls;
    }

    public static String extractValueFromAttributes(
        Attributes attributes,
        String attributeId,
        String defaultValue,
        String distinguishedName
    ) throws NamingException {
        return extractValueFromAttributes(attributes, attributeId, defaultValue, distinguishedName, true);
    }

    public static String extractValueFromAttributes(
        Attributes attributes,
        String attributeId,
        String defaultValue,
        String distinguishedName,
        boolean required
    ) throws NamingException {
        String value = extractValueFromAttributes(attributes, attributeId, distinguishedName, required);

        if (value == null) {
            if (required) {
                log.info(attributeId + " attribute not found for dn " + distinguishedName + ", using default value " + defaultValue + ".");
            }

            value = defaultValue;
        }

        return value;
    }

    public static String extractValueFromAttributes(
        Attributes attributes,
        String attributeId,
        String distinguishedName
    ) throws NamingException {
        return extractValueFromAttributes(attributes, attributeId, distinguishedName, true);
    }

    public static String extractValueFromAttributes(
        Attributes attributes,
        String attributeId,
        String distinguishedName,
        boolean required
    ) throws NamingException {
        Attribute attribute = attributes.get(attributeId);

        if (attribute == null) {
            if (required) {
                log.info(attributeId + " attribute not found for dn " + distinguishedName + ".");
            }
            return null;
        }
        return (String) attribute.get();
    }

    public static List<String> extractValuesFromAttributes(
        Attributes attributes,
        String attributeId,
        String distinguishedName
    ) throws NamingException {
        return extractValuesFromAttributes(attributes, attributeId, distinguishedName, true);
    }

    public static List<String> extractValuesFromAttributes(
        Attributes attributes,
        String attributeId,
        String distinguishedName,
        boolean required
    ) throws NamingException {
        Attribute attribute = attributes.get(attributeId);

        if (attribute == null) {
            if (required) {
                log.info(attributeId + " attribute not found for dn " + distinguishedName + ".");
            }

            return null;
        }

        List<String> values = new ArrayList<>();
        NamingEnumeration<?> namingEnumeration = attribute.getAll();

        while (namingEnumeration.hasMore()) {
            String value = (String) namingEnumeration.next();
            if (value != null) {
                values.add(value);
            }
        }

        return values;
    }
}
