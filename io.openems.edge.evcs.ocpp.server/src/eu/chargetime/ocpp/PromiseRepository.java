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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;

public class PromiseRepository implements IPromiseRepository {

  private Map<String, CompletableFuture<Confirmation>> promises;

  public PromiseRepository() {
    this.promises = new ConcurrentHashMap<>();
  }

  /**
   * Creates call back {@link CompletableFuture} for later use
   *
   * @param uniqueId identification for the {@link Request}
   * @return call back {@link CompletableFuture}
   */
  public CompletableFuture<Confirmation> createPromise(String uniqueId) {
    CompletableFuture<Confirmation> promise = new CompletableFuture<>();
    promises.put(uniqueId, promise);
    return promise;
  }

  /**
   * Get stored call back {@link CompletableFuture}.
   *
   * @param uniqueId identification for the {@link Request}
   * @return optional of call back {@link CompletableFuture}
   */
  public Optional<CompletableFuture<Confirmation>> getPromise(String uniqueId) {
    return Optional.ofNullable(promises.get(uniqueId));
  }

  /**
   * Remove stored call back {@link CompletableFuture}.
   *
   * @param uniqueId identification for the {@link Request}
   */
  public void removePromise(String uniqueId) {
    promises.remove(uniqueId);
  }
}
