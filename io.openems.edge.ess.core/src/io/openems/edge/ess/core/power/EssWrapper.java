package io.openems.edge.ess.core.power;

import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.expression.discrete.arithmetic.ArExpression;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

public class EssWrapper {

	protected final ManagedSymmetricEss ess;

	private IntVar p = null;
	private IntVar p_L1 = null;
	private IntVar p_L2 = null;
	private IntVar p_L3 = null;
	private IntVar q = null;
	private IntVar q_L1 = null;
	private IntVar q_L2 = null;
	private IntVar q_L3 = null;

	private int lastP = 0;
	private int lastP_L1 = 0;
	private int lastP_L2 = 0;
	private int lastP_L3 = 0;
	private int lastQ = 0;
	private int lastQ_L1 = 0;
	private int lastQ_L2 = 0;
	private int lastQ_L3 = 0;

	public EssWrapper(ManagedSymmetricEss ess) {
		this.ess = ess;
	}

	public ManagedSymmetricEss getEss() {
		return ess;
	}

	public int getPrecision() {
		return this.ess.getPowerPrecision();
	}

	/**
	 * Is called before every Cycle
	 * 
	 * @param power
	 * @param model
	 */
	public void initialize(ChocoPower power, Model model) {
		// TODO add relation between P and Q <= maxApparent
		int maxApparent = this.ess.getMaxApparentPower().value().orElse(0);
		int allowedCharge = Math.max(maxApparent * -1, this.ess.getAllowedCharge().value().orElse(0));
		int allowedDischarge = Math.min(maxApparent, this.ess.getAllowedDischarge().value().orElse(0));

		if (this.ess instanceof ManagedAsymmetricEss && !power.parent.isSymmetricMode()) {
			/*
			 * ManagedAsymmetricEss
			 */
			// L1 + L2 + L3 = P
			int min = allowedCharge / 3;
			int max = allowedDischarge / 3;
			this.p_L1 = model.intVar(this.ess.id() + "P_L1", min, max, false);
			this.p_L2 = model.intVar(this.ess.id() + "P_L2", min, max, false);
			this.p_L3 = model.intVar(this.ess.id() + "P_L3", min, max, false);
			// hold the error that happens when distributing "p / 3" to L1/L2/L3
			BoolVar p_Lerror = model.boolVar(this.ess.id() + "p_Lerror");
			this.p = this.p_L1.add(this.p_L2.add(this.p_L3.add(p_Lerror))).intVar();
			// L1 + L2 + L3 = Q
			this.q_L1 = model.intVar(this.ess.id() + "Q_L1", min, max, false);
			this.q_L2 = model.intVar(this.ess.id() + "Q_L2", min, max, false);
			this.q_L3 = model.intVar(this.ess.id() + "Q_L3", min, max, false);
			// hold the error that happens when distributing "q / 3" to L1/L2/L3
			BoolVar q_Lerror = model.boolVar(this.ess.id() + "q_Lerror");
			this.q = this.q_L1.add(this.q_L2.add(this.q_L3.add(q_Lerror))).intVar();
		} else {
			/*
			 * ManagedSymmetricEss
			 */
			int negative = allowedCharge / this.getPrecision();
			int positive = allowedDischarge / this.getPrecision();
			int count = negative * -1 + positive + 1;
			int[] allValues = new int[count];
			for (int i = 0; i < count; i++) {
				allValues[i] = (negative + i) * this.getPrecision();
			}
			this.p = model.intVar(this.ess.id() + "P", allValues);
			// L1 = L2 = L3 = P / 3
			this.p_L1 = this.p.div(3).intVar();
			this.p_L2 = this.p_L1;
			this.p_L3 = this.p_L1;
			// Q
			this.q = model.intVar(this.ess.id() + "Q", allValues);
			// L1 = L2 = L3 = Q / 3
			this.q_L1 = this.q.div(3).intVar();
			this.q_L2 = this.q_L1;
			this.q_L3 = this.q_L1;
		}
	}

	public void applyPower(ChocoPower power, Solution solution) {
		if (solution == null) {
			/*
			 * No Solution
			 */
			this.lastP = 0;
			this.lastP_L1 = 0;
			this.lastP_L2 = 0;
			this.lastP_L3 = 0;
			this.lastQ = 0;
			this.lastQ_L1 = 0;
			this.lastQ_L2 = 0;
			this.lastQ_L3 = 0;
			this.ess.applyPower(0, 0);

		} else if (this.ess instanceof ManagedAsymmetricEss && !power.parent.isSymmetricMode()) {
			/*
			 * ManagedAsymmetricEss
			 */
			ManagedAsymmetricEss e = (ManagedAsymmetricEss) ess;
			this.lastP_L1 = solution.getIntVal(this.p_L1);
			this.lastP_L2 = solution.getIntVal(this.p_L2);
			this.lastP_L3 = solution.getIntVal(this.p_L3);
			this.lastP = this.lastP_L1 + this.lastP_L2 + this.lastP_L3;
			this.lastQ_L1 = solution.getIntVal(this.q_L1);
			this.lastQ_L2 = solution.getIntVal(this.q_L2);
			this.lastQ_L3 = solution.getIntVal(this.q_L3);
			this.lastQ = this.lastQ_L1 + this.lastQ_L2 + this.lastQ_L3;

			// set debug channels on Ess
			ess.channel(ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER).setNextValue(this.lastP);
			ess.channel(ManagedSymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER).setNextValue(this.lastQ);
			ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER_L1).setNextValue(this.lastP_L1);
			ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER_L1).setNextValue(this.lastQ_L1);
			ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER_L2).setNextValue(this.lastP_L2);
			ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER_L2).setNextValue(this.lastQ_L2);
			ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER_L3).setNextValue(this.lastP_L3);
			ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER_L3).setNextValue(this.lastQ_L3);

			// call Ess
			e.applyPower( //
					this.lastP_L1, //
					this.lastQ_L1, //
					this.lastP_L2, //
					this.lastQ_L2, //
					this.lastP_L3, //
					this.lastQ_L3 //
			);

		} else {
			/*
			 * ManagedSymmetricEss
			 */
			this.lastP_L1 = solution.getIntVal(this.p_L1);
			this.lastP_L2 = solution.getIntVal(this.p_L2);
			this.lastP_L3 = solution.getIntVal(this.p_L3);
			this.lastP = this.lastP_L1 + this.lastP_L2 + this.lastP_L3;
			this.lastQ_L1 = solution.getIntVal(this.q_L1);
			this.lastQ_L2 = solution.getIntVal(this.q_L2);
			this.lastQ_L3 = solution.getIntVal(this.q_L3);
			this.lastQ = this.lastQ_L1 + this.lastQ_L2 + this.lastQ_L3;

			// set debug channels on Ess
			ess.channel(ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER).setNextValue(this.lastP);
			ess.channel(ManagedSymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER).setNextValue(this.lastQ);

			// call Ess
			ess.applyPower( //
					solution.getIntVal(this.p), //
					solution.getIntVal(this.q) //
			);
		}
	}

	public IntVar getP() {
		return this.p;
	}

	public IntVar getP_L1() {
		return this.p_L1;
	}

	public IntVar getP_L2() {
		return this.p_L2;
	}

	public IntVar getP_L3() {
		return this.p_L3;
	}

	public int getLastP() {
		return this.lastP;
	}

	public int getLastP_L1() {
		return this.lastP_L1;
	}

	public int getLastP_L2() {
		return this.lastP_L2;
	}

	public int getLastP_L3() {
		return this.lastP_L3;
	}

	public IntVar getQ() {
		return this.q;
	}

	public IntVar getQ_L1() {
		return this.q_L1;
	}

	public IntVar getQ_L2() {
		return this.q_L2;
	}

	public IntVar getQ_L3() {
		return this.q_L3;
	}

	public int getLastQ() {
		return this.lastQ;
	}

	public int getLastQ_L1() {
		return this.lastQ_L1;
	}

	public int getLastQ_L2() {
		return this.lastQ_L2;
	}

	public int getLastQ_L3() {
		return this.lastQ_L3;
	}

	public ArExpression optimizePDiffToLast() {
		int precisionL = Math.max(this.getPrecision() / 3, 1); // at least "1"
		return this.getP().dist(this.getLastP()).div(this.getPrecision()) //
				.add( //
						this.getP_L1().dist(this.getLastP_L1()).div(precisionL) //
								.add( //
										this.getP_L2().dist(this.getLastP_L2()).div(precisionL) //
												.add( //
														this.getP_L3().dist(this.getLastP_L3()).div(precisionL) //
												)));
	};

	public ArExpression optimizeQDiffToLast() {
		int precisionL = Math.max(this.getPrecision() / 3, 1); // at least "1"
		return this.getQ().dist(this.getLastQ()).div(this.getPrecision()) //
				.add( //
						this.getQ_L1().dist(this.getLastQ_L1()).div(precisionL) //
								.add( //
										this.getQ_L2().dist(this.getLastQ_L2()).div(precisionL) //
												.add( //
														this.getQ_L3().dist(this.getLastQ_L3()).div(precisionL) //
												)));
	};
}