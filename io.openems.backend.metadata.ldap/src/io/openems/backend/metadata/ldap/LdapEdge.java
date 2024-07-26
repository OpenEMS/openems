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
