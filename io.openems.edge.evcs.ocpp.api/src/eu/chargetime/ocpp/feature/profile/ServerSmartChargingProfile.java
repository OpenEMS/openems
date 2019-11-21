package eu.chargetime.ocpp.feature.profile;

import eu.chargetime.ocpp.feature.ClearChargingProfileFeature;
import eu.chargetime.ocpp.feature.Feature;
import eu.chargetime.ocpp.feature.ProfileFeature;
import eu.chargetime.ocpp.feature.SetChargingProfileFeature;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.core.ChargingProfile;
import eu.chargetime.ocpp.model.smartcharging.ClearChargingProfileRequest;
import eu.chargetime.ocpp.model.smartcharging.SetChargingProfileRequest;
import java.util.HashSet;
import java.util.UUID;

/*
 * ChargeTime.eu - Java-OCA-OCPP
 *
 * MIT License
 *
 * Copyright (C) 2017 Emil Christopher Solli Melar <emil@iconsultable.no>
 * Copyright (C) 2018 Fabian Röhr <fabian.roehr@netlight.com>
 * Copyright (C) 2018 Robin Roscher <r.roscher@ee-mobility.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

public class ServerSmartChargingProfile implements Profile {

	private HashSet<Feature> features;

	public ServerSmartChargingProfile() {
		features = new HashSet<>();
		features.add(new SetChargingProfileFeature(this));
		features.add(new ClearChargingProfileFeature(this));
	}

	@Override
	public ProfileFeature[] getFeatureList() {
		return features.toArray(new ProfileFeature[0]);
	}

	@Override
	public Confirmation handleRequest(UUID sessionIndex, Request request) {
		return null;
	}

	public SetChargingProfileRequest createSetChargingProfileRequest(int connectorId, ChargingProfile profile) {
		SetChargingProfileRequest request = new SetChargingProfileRequest();
		request.setConnectorId(connectorId);
		request.setCsChargingProfiles(profile);
		return request;
	}

	public ClearChargingProfileRequest createClearChargingProfileRequest() {
		return new ClearChargingProfileRequest();
	}
}
