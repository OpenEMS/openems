package io.openems.edge.controller.ess.powersolvercluster;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;

import java.util.List;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.api.SymmetricEss;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealVector;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.io.openems.edge.controller.ess.powersolvercluster", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class PowerSolverClusterImpl extends AbstractOpenemsComponent
		implements PowerSolverCluster, Controller, OpenemsComponent {

	private Config config = null;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	protected ComponentManager componentManager;

	OperationMode operationMode = OperationMode.THREE;
	TargetDirection targetDirection = TargetDirection.ZERO;

	int targetSoc = 50;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private MetaEss esss;

	public PowerSolverClusterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				PowerSolverCluster.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		if (this.applyConfig(context, config)) {
			return;
		}
	}

	private boolean applyConfig(ComponentContext context, Config config) {
		this.config = config;
		return OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id());
	}

	static enum TypeOfcluster {
		HOMOGENOUS, //
		HETEROGENOUS
	}

	static enum OperationMode {
		ONE, //
		TWO, //
		THREE
	}

	static enum TargetDirection {
		CHARGE, //
		DISCHARGE, //
		ZERO
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {

		ArrayList<ManagedSymmetricEss> esss = new ArrayList<ManagedSymmetricEss>();

		// Get the power set value for the cluster from the Config.
		int POWER_SET = this.config.power();
		System.out.println(this.esss.getEssIds());

		// Get all the enabled Ess in the cluster, except the cluster
		for (OpenemsComponent component : this.componentManager.getEnabledComponents()) {
			if (component instanceof SymmetricEss) {

				ManagedSymmetricEss managedSymmetricEss = (ManagedSymmetricEss) component;
				if (managedSymmetricEss instanceof MetaEss) {
					continue;
				}
				esss.add(managedSymmetricEss);
			}
		}

		// If not ess's in the cluster return. stop the logic
		if (esss.isEmpty()) {
			System.out.println("No ess's in the clulster");
			return;
		}

		// Sort the ESS based on the SOC
		Collections.sort(esss, (o1, o2) -> o1.getSoc().get().compareTo(o2.getSoc().get()));

		// determine the TypeofCluster
		TypeOfcluster typeOfcluster = Utils.getClusterType(esss);
		System.out.println("typeOfcluster : " + typeOfcluster);

		// Get the Target Direction
		if (POWER_SET < 0) {
			targetDirection = TargetDirection.CHARGE;
		} else if (POWER_SET > 0) {
			targetDirection = TargetDirection.DISCHARGE;
		} else {
			targetDirection = TargetDirection.ZERO;
		}
		System.out.println("targetDirection : " + targetDirection);

		// Get the Pmax of the cluster
		double PMAXCLUSTER = Utils.getPMaxChargeOfCluster(esss);

		switch (typeOfcluster) {
		case HETEROGENOUS:

			switch (targetDirection) {
			case CHARGE:
				System.out.println("operationMode : " + operationMode);
				operationMode = Utils.getOperationMode(Math.abs(PMAXCLUSTER), Math.abs(POWER_SET), esss.size());

				switch (operationMode) {
				case ONE:
					ManagedSymmetricEss lowestSocInverter = esss.get(0); // First inverter has the lowest soc, because
																			// it is sorted

					for (ManagedSymmetricEss m : esss) {
						if (m == lowestSocInverter) {
							m.setActivePowerEquals(this.config.power());
						} else {
							m.setActivePowerEquals(0);
						}
					}
					break;
				case TWO:

					ArrayList<ArrayList<ManagedSymmetricEss>> wantedUnwantedEss = Utils
							.getWantedUnwantedEssCharging(esss, targetSoc, POWER_SET);

					double[] socRatioList = null;

					if (Math.abs(POWER_SET) <= PMAXCLUSTER) {
						socRatioList = Utils.getSocRatioList(wantedUnwantedEss.get(0), POWER_SET);
					}

					for (int i = 0; i < wantedUnwantedEss.get(0).size(); i++) {
						wantedUnwantedEss.get(0).get(i).setActivePowerEquals((int) socRatioList[i]);
					}
					for (ManagedSymmetricEss m : wantedUnwantedEss.get(1)) {
						m.setActivePowerEquals(0);
					}

					break;
				case THREE:

					for (ManagedSymmetricEss m : esss) {
						m.setActivePowerEquals((int) (POWER_SET / esss.size()));
					}
					break;
				}

				break;
			case DISCHARGE:

				operationMode = Utils.getOperationMode(Math.abs(PMAXCLUSTER), Math.abs(POWER_SET), esss.size());
				System.out.println("operationMode : " + operationMode);
				switch (operationMode) {
				case ONE:
					ManagedSymmetricEss hishestSocInverter = Utils.getHighestSocInverter(esss);

					for (ManagedSymmetricEss m : esss) {
						if (m == hishestSocInverter) {
							m.setActivePowerEquals(this.config.power());
						} else {
							m.setActivePowerEquals(0);
						}
					}
					break;
				case TWO:

					// ArrayList<ArrayList<String>> x = null;

					ArrayList<ArrayList<ManagedSymmetricEss>> wantedUnwantedEss = Utils
							.getWantedUnwantedEssDischarging(esss, targetSoc, POWER_SET);

					// ArrayList<ManagedSymmetricEss> wantedEss =
					// Utils.getEssGreaterThenTargetSoc(esss, targetSoc);
					// ArrayList<ManagedSymmetricEss> unWantedEss =
					// Utils.getEssLesserThenTargetSoc(esss, targetSoc);

					double[] socRatioList = null;
					if (Math.abs(POWER_SET) <= PMAXCLUSTER) {
						socRatioList = Utils.getSocRatioList(wantedUnwantedEss.get(0), POWER_SET);
					}

					for (int i = 0; i < wantedUnwantedEss.get(0).size(); i++) {
						wantedUnwantedEss.get(0).get(i).setActivePowerEquals((int) socRatioList[i]);
					}
					for (ManagedSymmetricEss m : wantedUnwantedEss.get(1)) {
						m.setActivePowerEquals(0);
					}

					break;
				case THREE:

					for (ManagedSymmetricEss m : esss) {
						m.setActivePowerEquals((int) (POWER_SET / esss.size()));
					}
					break;
				}

				break;
			case ZERO:
				break;

			}

			break;

		case HOMOGENOUS:

			switch (targetDirection) {
			case CHARGE:
				// operation mode

				PMAXCLUSTER = Utils.getPMaxChargeOfCluster(esss);

				operationMode = Utils.getOperationMode(Math.abs(PMAXCLUSTER), Math.abs(POWER_SET), esss.size());
				System.out.println("operationMode : " + operationMode);
				switch (operationMode) {
				case ONE:

					ManagedSymmetricEss lowestSocInverter = Utils.getLowestSocInverter(esss);

					for (ManagedSymmetricEss m : esss) {
						if (m == lowestSocInverter) {
							m.setActivePowerEquals(this.config.power());
						} else {
							m.setActivePowerEquals(0);
						}
					}

					break;
				case TWO:

					PMAXCLUSTER = Utils.getPMaxChargeOfCluster(esss);

					// double twetypercentofMaxKVA = 0.0;
					// twetypercentofMaxKVA = firstKey.getMaxApparentPower().get() * 0.2;

					double n;
					boolean takeall = false;
					if (Math.abs(POWER_SET) <= (PMAXCLUSTER / 2)) {
						n = esss.size() / 2;
					} else {
						takeall = true;
						n = esss.size();
					}

					Collections.sort(esss, (o1, o2) -> o1.getSoc().get().compareTo(o2.getSoc().get()));

					if (takeall) {
						for (ManagedSymmetricEss m : esss) {
							m.setActivePowerEquals((int) (POWER_SET / n));
						}
					} else {
						for (int i = 0; i < esss.size(); i++) {
							if (i < n) {
								esss.get(i).setActivePowerEquals((int) (POWER_SET / n));
							} else {
								esss.get(i).setActivePowerEquals(0);
							}
						}

					}

					break;
				case THREE:

					for (ManagedSymmetricEss m : esss) {
						m.setActivePowerEquals((int) (POWER_SET / esss.size()));
					}
					break;

				}

				break;
			case DISCHARGE:

				// operation mode

				PMAXCLUSTER = Utils.getPMaxChargeOfCluster(esss);

				operationMode = Utils.getOperationMode(Math.abs(PMAXCLUSTER), Math.abs(POWER_SET), esss.size());
				System.out.println("operationMode : " + operationMode);
				switch (operationMode) {
				case ONE:

					ManagedSymmetricEss highestSocInverter = Utils.getHighestSocInverter(esss);

					for (ManagedSymmetricEss m : esss) {
						if (m == highestSocInverter) {
							m.setActivePowerEquals(this.config.power());
						} else {
							m.setActivePowerEquals(0);
						}
					}

					break;
				case TWO:

					PMAXCLUSTER = Utils.getPMaxChargeOfCluster(esss);

					// double twetypercentofMaxKVA = 0.0;
					// twetypercentofMaxKVA = firstKey.getMaxApparentPower().get() * 0.2;

					double n;
					boolean takeall = false;
					if (POWER_SET <= (PMAXCLUSTER / 2)) {
						n = esss.size() / 2;
					} else {
						takeall = true;
						n = esss.size();
					}

					Collections.sort(esss, (o1, o2) -> o1.getSoc().get().compareTo(o2.getSoc().get()));

					if (takeall) {
						for (ManagedSymmetricEss m : esss) {
							m.setActivePowerEquals((int) (POWER_SET / n));
						}
					} else {
						for (int i = 0; i < esss.size(); i++) {
							if (i >= n) {
								esss.get(i).setActivePowerEquals((int) (POWER_SET / n));
							} else {
								esss.get(i).setActivePowerEquals(0);
							}
						}

					}

					break;
				case THREE:

					for (ManagedSymmetricEss m : esss) {
						m.setActivePowerEquals((int) (POWER_SET / esss.size()));
					}
					break;
				}
			case ZERO:
				for (ManagedSymmetricEss m : esss) {
					m.setActivePowerEquals(0);
				}
				break;

			}
			break;
		}

	}

	/**
	 * This is a helper class, which helps to get the manipulated data for the main
	 * logic
	 */
	static class Utils {

		/**
		 * Get the Solution using the LUDecomposition
		 * 
		 * @param size
		 * @param socRatioList
		 * @param powerSet
		 * @return double[] solution
		 */
		private static double[] getSocRatioList(ArrayList<ManagedSymmetricEss> wantedEss, int pOWER_SET) {

			ArrayList<Double> socRatioList = new ArrayList<Double>();
			List<Double> socList = new ArrayList<Double>();

			for (ManagedSymmetricEss m : wantedEss) {
				socList.add((double) m.getSoc().get());
			}

			DecimalFormat df = new DecimalFormat("0.000");
			df.setRoundingMode(RoundingMode.CEILING);

			socRatioList.add(1.0);
			for (int i = 0; i < socList.size() - 1; i++) {
				int j = i + 1;
				double ratio = Double.valueOf(df.format(socList.get(j) / socList.get(i)));
				socRatioList.add(-ratio);
			}

			return getSolution(wantedEss.size(), socRatioList, pOWER_SET);

		}

		/**
		 * Helper method to get the solution using LUDecomposition
		 * 
		 * @param size
		 * @param socRatioList
		 * @param powerSet
		 * @return double[] solution
		 */
		private static double[] getSolution(int size, ArrayList<Double> socRatioList, double powerSet) {
			double[][] coeffData = new double[size][size];

			for (int i = 0; i < size; i++) {
				for (int j = 0; j < size; j++) {
					if (i == 0) {
						coeffData[i][j] = 1;
					} else if (i == j) {
						coeffData[i][j] = socRatioList.get(j);
					} else if (i != 0 && i == j + 1) {
						coeffData[i][j] = 1;
					} else {
						coeffData[i][j] = 0;
					}
				}
			}

			// TODO Run this for printing the matrix
			// printMatrix(coeffData);

			DecompositionSolver solver = new LUDecomposition(new Array2DRowRealMatrix(coeffData)).getSolver();
			double[] constants = new double[size];
			for (int i = 0; i < size; i++) {
				if (i == 0) {
					constants[i] = powerSet;
				} else {
					constants[i] = 0;
				}
			}

			// TODO Run this for printing the constants
			// printVector(constants);

			RealVector constant = new ArrayRealVector(constants, false);

			System.out.println(solver.solve(constant));
			return solver.solve(constant).toArray();

		}

		/**
		 * Get the type of cluster based. Comparing with the difference Soc (ess with
		 * the max soc value subtract with ess with the min soc value) and the static
		 * SOC value.
		 * 
		 * @param esss
		 * @return {@code TypeOfcluster}
		 */
		public static TypeOfcluster getClusterType(ArrayList<ManagedSymmetricEss> esss) {

			List<Integer> socList = new ArrayList<Integer>();
			int clusterDifferentiator = 5;

			for (ManagedSymmetricEss m : esss) {
				socList.add(m.getSoc().get());
			}

			int differenceSoc = Collections.max(socList) - Collections.min(socList);

			if (differenceSoc > clusterDifferentiator) {
				return TypeOfcluster.HETEROGENOUS;
			} else {
				return TypeOfcluster.HOMOGENOUS;
			}
		}

		public static ArrayList<ArrayList<ManagedSymmetricEss>> getWantedUnwantedEssDischarging(
				ArrayList<ManagedSymmetricEss> esss, int targetSoc, int pOWER_SET) {

			for (ManagedSymmetricEss m : esss) {
				System.out.print(m.getSoc().get() + " , ");
			}

			ArrayList<ArrayList<ManagedSymmetricEss>> WantedUnwantedEss = new ArrayList<ArrayList<ManagedSymmetricEss>>();
			ArrayList<ManagedSymmetricEss> wantedEss = new ArrayList<ManagedSymmetricEss>();
			ArrayList<ManagedSymmetricEss> unWantedEss = new ArrayList<ManagedSymmetricEss>();
			int n = esss.size() - 1;
			for (int i = esss.size() - 1; i >= 0; i--) {
				if (esss.get(i).getSoc().get() > targetSoc) {
					wantedEss.add(esss.get(i));
					n = n - 1;
				}
			}

			double pmax = getPMaxChargeOfCluster(wantedEss);
			if (Math.abs(pOWER_SET) >= pmax) {
				wantedEss.add(esss.get(n));
				n = n - 1;
			}
			for (int i = n; i >= 0; i--) {
				unWantedEss.add(esss.get(i));
			}
			WantedUnwantedEss.add(wantedEss);
			WantedUnwantedEss.add(unWantedEss);

			return WantedUnwantedEss;
		}

		public static ArrayList<ArrayList<ManagedSymmetricEss>> getWantedUnwantedEssCharging(
				ArrayList<ManagedSymmetricEss> esss, int targetSoc, int pOWER_SET) {

			ArrayList<ArrayList<ManagedSymmetricEss>> WantedUnwantedEss = new ArrayList<ArrayList<ManagedSymmetricEss>>();
			ArrayList<ManagedSymmetricEss> wantedEss = new ArrayList<ManagedSymmetricEss>();
			ArrayList<ManagedSymmetricEss> unWantedEss = new ArrayList<ManagedSymmetricEss>();
			int n = 0;
			for (int i = 0; i < esss.size(); i++) {
				if (esss.get(i).getSoc().get() < targetSoc) {
					wantedEss.add(esss.get(i));
					n = n + 1;
				}
			}

			double pmax = getPMaxChargeOfCluster(wantedEss);
			// System.out.println(pmax);
			System.out.println("pOWER_SET " + Math.abs(pOWER_SET) + " pmax of n cluster " + pmax);
			while (Math.abs(pOWER_SET) >= pmax /* && ! (n > esss.size() - 1) */) {
				wantedEss.add(esss.get(n));
				n = n + 1;
			}
			for (int i = n; i < esss.size(); i++) {
				unWantedEss.add(esss.get(i));
			}

			System.out.println(
					"pOWER_SET " + Math.abs(pOWER_SET) + " pmax of n cluster " + getPMaxChargeOfCluster(wantedEss));
			WantedUnwantedEss.add(wantedEss);
			WantedUnwantedEss.add(unWantedEss);

			System.out.print("Wanted Ess: ");
			for (ManagedSymmetricEss m : wantedEss) {
				System.out.print(m.id() + " , ");
			}
			System.out.println();
			System.out.print("Un Wanted Ess: ");
			for (ManagedSymmetricEss m : unWantedEss) {
				System.out.print(m.id() + " , ");
			}

			return WantedUnwantedEss;
		}

		/**
		 * Get the list of Ess's which have Soc greater the targetSoc
		 * 
		 * @param esss
		 * @param targetSoc
		 * @return ArrayList<ManagedSymmetricEss> wantedEss
		 */
		public static ArrayList<ManagedSymmetricEss> getEssGreaterThenTargetSoc(ArrayList<ManagedSymmetricEss> esss,
				int targetSoc) {

			ArrayList<ManagedSymmetricEss> wantedEss = new ArrayList<ManagedSymmetricEss>();

			for (ManagedSymmetricEss m : esss) {
				if (m.getSoc().get() > targetSoc) {
					wantedEss.add(m);
				}
			}

			return wantedEss;
		}

		/**
		 * Get the list of Ess's which have Soc lesser the targetSoc
		 * 
		 * @param esss
		 * @param targetSoc
		 * @return ArrayList<ManagedSymmetricEss> WantedEss
		 */
		public static ArrayList<ManagedSymmetricEss> getEssLesserThenTargetSoc(ArrayList<ManagedSymmetricEss> esss,
				int targetSoc) {

			ArrayList<ManagedSymmetricEss> WantedEss = new ArrayList<ManagedSymmetricEss>();

			for (ManagedSymmetricEss m : esss) {
				if (m.getSoc().get() < targetSoc) {
					WantedEss.add(m);
				}
			}

			return WantedEss;
		}

		/**
		 * Get the Pmax of the cluster
		 * 
		 * @param esss
		 * @return double Pmax
		 */
		public static double getPMaxChargeOfCluster(ArrayList<ManagedSymmetricEss> esss) {

			double Pmax = 0;

			for (ManagedSymmetricEss m : esss) {
				Pmax = Pmax + m.getMaxApparentPower().get();
			}

			return Pmax;
		}

		/**
		 * return the operation Mode
		 * 
		 * @param pmaxCharge
		 * @param pOWER_SET
		 * @return operationMode {@code OperationMode}
		 */
		private static OperationMode getOperationMode(double pmaxCharge, double pOWER_SET, int numOfInverters) {

			double fortyPercentPower = 0.40 * (pmaxCharge / numOfInverters);

			if (0 < pOWER_SET && pOWER_SET < fortyPercentPower) {
				return OperationMode.ONE;
			} else if (fortyPercentPower <= pOWER_SET && pOWER_SET < pmaxCharge) {
				return OperationMode.TWO;
			} else {
				return OperationMode.THREE;
			}

		}

		/**
		 * Get the ess with the Lowest Soc in the cluster.
		 * 
		 * @param esss
		 * @return ManagedSymmetricEss ess
		 */
		private static ManagedSymmetricEss getLowestSocInverter(ArrayList<ManagedSymmetricEss> esss) {

			List<Integer> socList = new ArrayList<Integer>();

			for (ManagedSymmetricEss m : esss) {
				socList.add(m.getSoc().get());
			}

			int lowestSoc = Collections.min(socList);

			for (ManagedSymmetricEss m : esss) {
				if (m.getSoc().get() == lowestSoc) {
					return m;
				}
			}

			return esss.get(0);
		}

		/**
		 * Get the ess with the Highest Soc in the cluster.
		 * 
		 * @param esss
		 * @return ManagedSymmetricEss ess
		 */
		private static ManagedSymmetricEss getHighestSocInverter(ArrayList<ManagedSymmetricEss> esss) {

			List<Integer> socList = new ArrayList<Integer>();

			for (ManagedSymmetricEss m : esss) {
				socList.add(m.getSoc().get());
			}

			int highestSoc = Collections.max(socList);

			for (ManagedSymmetricEss m : esss) {
				if (m.getSoc().get() == highestSoc) {
					return m;
				}
			}

			return esss.get(0);
		}

		/**
		 * Simple to string method for printing the matrix.
		 * 
		 * @param coeffData
		 */
		public static void printMatrix(double[][] coeffData) {
			System.out.println("coeffData ");
			for (int i = 0; i < coeffData.length; i++) { // this equals to the row in our matrix.
				for (int j = 0; j < coeffData[i].length; j++) { // this equals to the column in each row.
					System.out.print(coeffData[i][j] + " | ");
				}
				System.out.println(); // change line on console as row comes to end in the matrix.
			}
		}

		/**
		 * Simple toString method for printing the vector.
		 * 
		 * @param constants
		 */
		public static void printVector(double[] constants) {
			System.out.println("constants ");
			for (int i = 0; i < constants.length; i++) {
				System.out.print(constants[i] + " | ");
			}
			System.out.println();

		}
	}

}
