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

import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.edge.common.user.User;

public class LdapUser extends User {

    // why the hell is the constructor protected??
    public LdapUser(String id, String name, Language language, Role role) {
        super(id, name, language, role);        
    }

}
