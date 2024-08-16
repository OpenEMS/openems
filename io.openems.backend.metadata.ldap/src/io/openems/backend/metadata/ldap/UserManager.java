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
