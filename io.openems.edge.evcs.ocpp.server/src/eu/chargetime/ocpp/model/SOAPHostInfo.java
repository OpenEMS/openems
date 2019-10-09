package eu.chargetime.ocpp.model; /*
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

import java.util.Objects;

import eu.chargetime.ocpp.utilities.MoreObjects;

public class SOAPHostInfo {
  public static final String NAMESPACE_CHARGEBOX = "urn://Ocpp/Cp/2015/10/";
  public static final String NAMESPACE_CENTRALSYSTEM = "urn://Ocpp/Cs/2015/10/";

  private String chargeBoxIdentity;
  private String fromUrl;
  private String toUrl;
  private String namespace;
  private boolean isClient;

  public String getChargeBoxIdentity() {
    return chargeBoxIdentity;
  }

  public String getFromUrl() {
    return fromUrl;
  }

  public String getToUrl() {
    return toUrl;
  }

  public void setToUrl(String toUrl) {
    this.toUrl = toUrl;
  }

  public String getNamespace() {
    return namespace;
  }

  public boolean isClient() {
    return isClient;
  }

  public static class Builder {
    private String chargeBoxIdentity;
    private String fromUrl;
    private String toUrl;
    private String namespace;
    private boolean isClient;

    public Builder chargeBoxIdentity(String chargeBoxIdentity) {
      if (chargeBoxIdentity == null) {
        throw new IllegalArgumentException("The object 'chargeBoxIdentity' cannot be null");
      }
      this.chargeBoxIdentity = chargeBoxIdentity;
      return this;
    }

    public Builder fromUrl(String fromUrl) {
      if (fromUrl == null) {
        throw new IllegalArgumentException("The object 'fromUrl' cannot be null");
      }
      this.fromUrl = fromUrl;
      return this;
    }

    public Builder toUrl(String toUrl) {
      if (toUrl == null) {
        throw new IllegalArgumentException("The object 'toUrl' cannot be null");
      }
      this.toUrl = toUrl;
      return this;
    }

    public Builder namespace(String namespace) {
      if (namespace == null) {
        throw new IllegalArgumentException("The object 'namespace' cannot be null");
      }
      this.namespace = namespace;
      return this;
    }

    public Builder isClient(boolean value) {
      this.isClient = value;
      return this;
    }

    public SOAPHostInfo build() {
      SOAPHostInfo res = new SOAPHostInfo();
      res.fromUrl = this.fromUrl;
      res.chargeBoxIdentity = this.chargeBoxIdentity;
      res.namespace = this.namespace;
      res.toUrl = this.toUrl;
      res.isClient = this.isClient;
      validate(res);
      return res;
    }

    private void validate(SOAPHostInfo hostInfo) {
      if (hostInfo.fromUrl == null
          || hostInfo.chargeBoxIdentity == null
          || hostInfo.namespace == null) {
        throw new IllegalStateException("Some required fields where not set.");
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SOAPHostInfo that = (SOAPHostInfo) o;
    return isClient == that.isClient
        && Objects.equals(chargeBoxIdentity, that.chargeBoxIdentity)
        && Objects.equals(fromUrl, that.fromUrl)
        && Objects.equals(toUrl, that.toUrl)
        && Objects.equals(namespace, that.namespace);
  }

  @Override
  public int hashCode() {
    return Objects.hash(chargeBoxIdentity, fromUrl, toUrl, namespace, isClient);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("chargeBoxIdentity", chargeBoxIdentity)
        .add("fromUrl", fromUrl)
        .add("toUrl", toUrl)
        .add("namespace", namespace)
        .add("isClient", isClient)
        .toString();
  }
}
