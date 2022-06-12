package io.openems.edge.controller.ess.powersolvercluster;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
	private ManagedSymmetricEss esss;

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
		int POWER_SET = this.config.power();

		for (OpenemsComponent component : this.componentManager.getEnabledComponents()) {
			if (component instanceof SymmetricEss) {

				ManagedSymmetricEss managedSymmetricEss = (ManagedSymmetricEss) component;
				if (managedSymmetricEss instanceof MetaEss) {
					continue;
				}
				esss.add(managedSymmetricEss);
			}
		}

		Collections.sort(esss, (o1, o2) -> o1.getSoc().get().compareTo(o2.getSoc().get()));

		TypeOfcluster typeOfcluster = Utils.getClusterType(esss);

		if (POWER_SET < 0) {
			targetDirection = TargetDirection.CHARGE;
		} else if (POWER_SET > 0) {
			targetDirection = TargetDirection.DISCHARGE;
		} else {
			targetDirection = TargetDirection.ZERO;
		}

		double PmaxCharge = Utils.getPMaxChargeOfCluster(esss);
		System.out.println("typeOfcluster : " + typeOfcluster);
		switch (typeOfcluster) {

		case HETEROGENOUS:
			System.out.println("targetDirection : " + targetDirection);
			switch (targetDirection) {
			case CHARGE:
				System.out.println("operationMode : " + operationMode);
				operationMode = Utils.getOperationMode(Math.abs(PmaxCharge), Math.abs(POWER_SET), esss.size());

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
					// -----------------------------------------

					ArrayList<ManagedSymmetricEss> wantedEss = Utils.getEssWantedForCharging(esss, targetSoc);
					ArrayList<ManagedSymmetricEss> unWantedEss = Utils.getEssUnwantedForCharging(esss, targetSoc);

					double[] socRatioList = null;
					if (POWER_SET <= PmaxCharge) {
						socRatioList = Utils.getSocRatioList(wantedEss, POWER_SET);
					}

					for (int i = 0; i < wantedEss.size(); i++) {
						wantedEss.get(i).setActivePowerEquals((int) socRatioList[i]);
					}
					for (ManagedSymmetricEss m : unWantedEss) {
						m.setActivePowerEquals(0);
					}

					// -----------------------------------------
					break;
				case THREE:

					for (ManagedSymmetricEss m : esss) {
						m.setActivePowerEquals((int) (POWER_SET / esss.size()));
					}
					break;
				}

				break;
			case DISCHARGE:
				
				operationMode = Utils.getOperationMode(Math.abs(PmaxCharge), Math.abs(POWER_SET), esss.size());
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
					// -----------------------------------------

					ArrayList<ManagedSymmetricEss> wantedEss = Utils.getEssUnwantedForCharging(esss, targetSoc);
					ArrayList<ManagedSymmetricEss> unWantedEss = Utils.getEssWantedForCharging(esss, targetSoc);

					double[] socRatioList = null;
					if (POWER_SET <= PmaxCharge) {
						socRatioList = Utils.getSocRatioList(wantedEss, POWER_SET);
					}

					for (int i = 0; i < wantedEss.size(); i++) {
						wantedEss.get(i).setActivePowerEquals((int) socRatioList[i]);
					}
					for (ManagedSymmetricEss m : unWantedEss) {
						m.setActivePowerEquals(0);
					}

					// -----------------------------------------
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
			System.out.println("targetDirection : " + targetDirection);
			switch (targetDirection) {
			case CHARGE:
				// operation mode

				PmaxCharge = Utils.getPMaxChargeOfCluster(esss);

				operationMode = Utils.getOperationMode(Math.abs(PmaxCharge), Math.abs(POWER_SET), esss.size());
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

					PmaxCharge = Utils.getPMaxChargeOfCluster(esss);
					ManagedSymmetricEss firstKey = null;
					double twetypercentofMaxKVA = 0.0;

					firstKey = esss.get(0);

					twetypercentofMaxKVA = firstKey.getMaxApparentPower().get() * 0.2;

					double n;
					boolean takeall = false;
					if (POWER_SET <= (PmaxCharge / 2)) {
						n = esss.size() / 2;
					} else {
						takeall = true;
						n = esss.size();
					}

					// System.out.println("hahah " + esss);
					Collections.sort(esss, (o1, o2) -> o1.getSoc().get().compareTo(o2.getSoc().get()));
					// System.out.println("Booyaaa " + esss);

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

				PmaxCharge = Utils.getPMaxChargeOfCluster(esss);

				operationMode = Utils.getOperationMode(Math.abs(PmaxCharge), Math.abs(POWER_SET), esss.size());
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

					PmaxCharge = Utils.getPMaxChargeOfCluster(esss);
					ManagedSymmetricEss firstKey = null;
					double twetypercentofMaxKVA = 0.0;

					firstKey = esss.get(0);

					twetypercentofMaxKVA = firstKey.getMaxApparentPower().get() * 0.2;

					double n;
					boolean takeall = false;
					if (POWER_SET <= (PmaxCharge / 2)) {
						n = esss.size() / 2;
					} else {
						takeall = true;
						n = esss.size();
					}

					// System.out.println("hahah " + esss);
					Collections.sort(esss, (o1, o2) -> o1.getSoc().get().compareTo(o2.getSoc().get()));
					
					// System.out.println("Booyaaa " + esss);

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

	static class Utils {

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

			System.out.println(socRatioList);

			double[] solution = getSolution(wantedEss.size(), socRatioList, pOWER_SET);

			return solution;
		}

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
//			System.out.println("coeffData ");
//			for (int i = 0; i < coeffData.length; i++) { // this equals to the row in our matrix.
//				for (int j = 0; j < coeffData[i].length; j++) { // this equals to the column in each row.
//					System.out.print(coeffData[i][j] + " ");
//				}
//				System.out.println(); // change line on console as row comes to end in the matrix.
//			}

			DecompositionSolver solver = new LUDecomposition(new Array2DRowRealMatrix(coeffData)).getSolver();
			double[] constants = new double[size];
			for (int i = 0; i < size; i++) {
				if (i == 0) {
					constants[i] = powerSet;
				} else {
					constants[i] = 0;
				}
			}
//			System.out.println("constants ");
//			for (int i = 0; i < constants.length; i++) {
//				System.out.print(constants[i] + " ");
//			}
//			System.out.println();

			RealVector constant = new ArrayRealVector(constants, false);
			System.out.println(solver.solve(constant));

			double[] solution = solver.solve(constant).toArray();

			// System.out.println("sum : " + sum);
			return solution;
		}

		public static TypeOfcluster getClusterType(ArrayList<ManagedSymmetricEss> esss) {

			List<Integer> socList = new ArrayList<Integer>();

			for (ManagedSymmetricEss m : esss) {
				socList.add(m.getSoc().get());
			}

			int differenceSoc = Collections.max(socList) - Collections.min(socList);

			if (differenceSoc > 5) {
				return TypeOfcluster.HETEROGENOUS;
			} else {
				return TypeOfcluster.HOMOGENOUS;
			}
		}

		public static ArrayList<ManagedSymmetricEss> getEssUnwantedForCharging(ArrayList<ManagedSymmetricEss> esss,
				int targetSoc) {
			ArrayList<ManagedSymmetricEss> wantedEss = new ArrayList<ManagedSymmetricEss>();
			for (ManagedSymmetricEss m : esss) {
				if (m.getSoc().get() > targetSoc) {
					wantedEss.add(m);
				}
			}

			return wantedEss;
		}

		public static ArrayList<ManagedSymmetricEss> getEssWantedForCharging(ArrayList<ManagedSymmetricEss> esss,
				int targetSoc) {
			ArrayList<ManagedSymmetricEss> unWantedEss = new ArrayList<ManagedSymmetricEss>();
			for (ManagedSymmetricEss m : esss) {
				if (m.getSoc().get() < targetSoc) {
					unWantedEss.add(m);
				}
			}

			return unWantedEss;
		}

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
			} else if (fortyPercentPower < pOWER_SET && pOWER_SET < pmaxCharge) {
				return OperationMode.TWO;
			} else {
				return OperationMode.THREE;
			}

		}

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
		
		

	}
}
