package io.openems.backend.common.edge.subscription;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionData(UUID subscriptionId, SubscriptionKey key, Instant subscribeDate) {
}