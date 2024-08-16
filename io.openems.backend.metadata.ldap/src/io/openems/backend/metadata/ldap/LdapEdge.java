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

import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Metadata;
import io.openems.common.channel.Level;

public class LdapEdge extends Edge {

    private final String cn;
    private String serialNumber;
    private Level sumState;

    public LdapEdge(Metadata parent, String cn, String serialNumber, String description) {
        super(parent, cn, description, null, null, null);

        this.cn = cn;
        this.serialNumber = serialNumber;
        this.sumState = Level.FAULT;
    }

    public String getCn() {
        return cn;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Level getSumState() {
        return sumState;
    }

    public void setSumState(Level sumState) {
        this.sumState = sumState;
    }

    @Override
    public String toString() {
        return "MyEdge ["
            + "cn=" + cn + ", "
            + "id=" + getId() + ", "
            + "serialNumber=" + serialNumber + ", "
            + "comment=" + getComment() + ", "
            + "version=" + getVersion() + ", "
            + "producttype=" + getProducttype() + ", "
            + "lastmessage=" + getLastmessage() + ", "
            + "isOnline=" + isOnline() + ", "
            + "sumState=" + sumState
            + "]";
    }
}
