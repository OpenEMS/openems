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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.User;

public class UserManager {

    private static final Logger log = LoggerFactory.getLogger(UserManager.class);

    private final ConcurrentHashMap<String, User> usersById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, User> usersByToken = new ConcurrentHashMap<>();

    public UserManager() {
        log.info("Creating new LDAP user manager.");
    }

    public void put(User user) {
        if ((user == null) || (user.getId() == null) || (user.getToken() == null)) {
            return;
        }

        log.info("Adding " + user.getId() + " with " + user.getToken() + " to cache.");

        usersById.put(user.getId(), user);
        usersByToken.put(user.getToken(), user);
    }

    public User getByUserId(String userId) {
        return usersById.get(userId);
    }

    public User getByToken(String token) {
        return usersByToken.get(token);
    }

    public void remove(User user) {
        if ((user == null) || (user.getId() == null) || (user.getToken() == null)) {
            return;
        }
        log.info("Removing " + user.getId() + " with " + user.getToken() + " from cache.");

        usersById.remove(user.getId());
        usersByToken.remove(user.getToken());
    }

    public Map<String, Object> getUserInformation(User user) {
        log.info("Computing user information for user " + user.getId() + ".");

        JsonObject settings = user.getSettings();

        Map<String, Object> userInformation = new HashMap<>();
        addStringToMapFromJsonObject(settings, userInformation, "firstname");
        addStringToMapFromJsonObject(settings, userInformation, "lastname");
        addStringToMapFromJsonObject(settings, userInformation, "phone");
        addStringToMapFromJsonObject(settings, userInformation, "email");

        JsonObject address = settings.getAsJsonObject("address");
        if (address != null) {
            addStringToMapFromJsonObject(address, userInformation, "street");
            addStringToMapFromJsonObject(address, userInformation, "zip");
            addStringToMapFromJsonObject(address, userInformation, "city");
            addStringToMapFromJsonObject(address, userInformation, "country");
        }

        log.info("Computed user information: " + userInformation);

        return userInformation;
    }

    private void addStringToMapFromJsonObject(JsonObject data, Map<String, Object> result, String key) {
        JsonElement value = data.get(key);
        if (value == null) {
            return;
        }

        String valueAsString = value.getAsString();
        if (valueAsString == null) {
            return;
        }

        result.put(key, valueAsString);
    }
}
