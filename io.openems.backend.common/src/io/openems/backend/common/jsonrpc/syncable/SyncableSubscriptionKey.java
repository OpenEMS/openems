package io.openems.backend.common.jsonrpc.syncable;

import io.openems.backend.common.edge.subscription.SubscriptionKey;
import io.openems.common.jsonrpc.syncable.SyncableKey;

public record SyncableSubscriptionKey(SyncableKey<?> syncableKey) implements SubscriptionKey {
}