package io.openems.edge.controller.evse.cluster.powerdistribute;

import io.openems.edge.controller.evse.single.Params;

public record PowerDistributionResult(Params params, int setPointInWatt, int idealSetPointInWatt) {
}
