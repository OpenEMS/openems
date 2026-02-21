package io.openems.edge.ess.core.power.v2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyMetaEss;

/**
 * Shared test utilities for PowerDistributionHandlerV2 tests.
 */
final class V2TestUtils {

	private V2TestUtils() {
	}

	/**
	 * Result of creating a cluster with members.
	 */
	record ClusterSetup(//
			List<ManagedSymmetricEss> allEsss, //
			DummyMetaEss cluster, //
			List<DummyManagedSymmetricEss> members, //
			Map<String, AtomicInteger> appliedPowers) {

		int totalPower() {
			return this.appliedPowers.values().stream().mapToInt(AtomicInteger::get).sum();
		}

		int powerOf(String essId) {
			return this.appliedPowers.get(essId).get();
		}
	}

	/**
	 * Creates a cluster with members configured by [soc, allowedCharge,
	 * allowedDischarge].
	 *
	 * @param clusterId    the cluster component ID
	 * @param memberConfig array of [soc, allowedCharge, allowedDischarge] per
	 *                     member
	 * @return a {@link ClusterSetup} with all ESS instances and power tracking
	 */
	static ClusterSetup createCluster(String clusterId, int[][] memberConfig) {
		var members = new ArrayList<DummyManagedSymmetricEss>();
		var appliedPowers = new HashMap<String, AtomicInteger>();

		for (int i = 0; i < memberConfig.length; i++) {
			var cfg = memberConfig[i];
			var id = "ess" + (i + 1);
			var power = new AtomicInteger(0);
			appliedPowers.put(id, power);

			var ess = new DummyManagedSymmetricEss(id) //
					.withSoc(cfg[0]) //
					.withAllowedChargePower(cfg[1]) //
					.withAllowedDischargePower(cfg[2]) //
					.withMaxApparentPower(Math.max(Math.abs(cfg[1]), cfg[2]));
			ess.withSymmetricApplyPowerCallback(record -> power.set(record.activePower()));
			members.add(ess);
		}

		var cluster = new DummyMetaEss(clusterId, members.toArray(new DummyManagedSymmetricEss[0]));

		var allEsss = new ArrayList<ManagedSymmetricEss>();
		allEsss.add(cluster);
		allEsss.addAll(members);

		return new ClusterSetup(allEsss, cluster, members, appliedPowers);
	}

	/**
	 * Creates a handler for the given cluster setup.
	 * 
	 * @param s the {@link ClusterSetup}
	 * @return handler the {@link PowerDistributionHandlerV2}
	 */
	static PowerDistributionHandlerV2 createHandler(ClusterSetup s) {
		return new PowerDistributionHandlerV2(() -> s.allEsss);
	}
}
