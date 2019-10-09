package eu.chargetime.ocpp;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import eu.chargetime.ocpp.feature.Feature;
import eu.chargetime.ocpp.feature.profile.Profile;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.utilities.MoreObjects;

public class FeatureRepository implements IFeatureRepository {

  private final Map<String, Feature> actionMap = new HashMap<>();
  private final Map<Class<?>, Feature> classMap = new HashMap<>();

  /**
   * Add {@link Profile} to support a group of features.
   *
   * @param profile supported feature {@link Profile}
   * @see Profile
   */
  public void addFeatureProfile(Profile profile) {
    for (Feature feature : profile.getFeatureList()) {
      addFeature(feature);
    }
  }

  /**
   * Add {@link Feature} to support.
   *
   * @param feature supported {@link Feature}.
   */
  public void addFeature(Feature feature) {
    actionMap.put(feature.getAction(), feature);
    classMap.put(feature.getRequestType(), feature);
    classMap.put(feature.getConfirmationType(), feature);
  }

  /**
   * Search for supported features added with the addProfile. If no supported feature is found,
   * {@link Optional#empty()} is returned
   *
   * <p>Can take multiple inputs: {@link String}, search for the action name of the feature. {@link
   * Request}/{@link Confirmation}, search for a feature that matches. Anything else will return
   * {@link Optional#empty()}.
   *
   * @param needle Object supports {@link String}, {@link Request} or {@link Confirmation}
   * @return Optional of instance of the supported Feature
   */
  @Override
  public Optional<Feature> findFeature(Object needle) {
    if (needle instanceof String) {
      return Optional.ofNullable(actionMap.get(needle));
    }

    if ((needle instanceof Request) || (needle instanceof Confirmation)) {
      return Optional.ofNullable(classMap.get((needle.getClass())));
    }

    return Optional.empty();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper("FeatureRepository")
        .add("actionMap", actionMap)
        .add("classMap", classMap)
        .toString();
  }
}
