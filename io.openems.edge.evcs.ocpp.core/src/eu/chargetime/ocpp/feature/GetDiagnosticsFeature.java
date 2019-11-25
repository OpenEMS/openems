package eu.chargetime.ocpp.feature;

/*
   ChargeTime.eu - Java-OCA-OCPP

   MIT License

   Copyright (C) 2016-2018 Thomas Volden <tv@chargetime.eu>

   Permission is hereby granted, free of charge, to any person obtaining a copy
   of this software and associated documentation files (the "Software"), to deal
   in the Software without restriction, including without limitation the rights
   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
   copies of the Software, and to permit persons to whom the Software is
   furnished to do so, subject to the following conditions:

   The above copyright notice and this permission notice shall be included in all
   copies or substantial portions of the Software.

   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
   SOFTWARE.
*/

import eu.chargetime.ocpp.feature.profile.Profile;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.firmware.GetDiagnosticsConfirmation;
import eu.chargetime.ocpp.model.firmware.GetDiagnosticsRequest;

public class GetDiagnosticsFeature extends ProfileFeature {

	/**
	 * Creates link back to the {@link Profile}.
	 *
	 * @param ownerProfile the {@link Profile} that owns the function.
	 */
	public GetDiagnosticsFeature(Profile ownerProfile) {
		super(ownerProfile);
	}

	@Override
	public Class<? extends Request> getRequestType() {
		return GetDiagnosticsRequest.class;
	}

	@Override
	public Class<? extends Confirmation> getConfirmationType() {
		return GetDiagnosticsConfirmation.class;
	}

	@Override
	public String getAction() {
		return "GetDiagnostics";
	}
}
