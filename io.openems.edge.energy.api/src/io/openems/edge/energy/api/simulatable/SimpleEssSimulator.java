//package io.openems.edge.energy.api.simulatable;
//
//import io.openems.edge.energy.api.ExecutionPlan.Period;
//
//public class SimpleEssSimulator implements Simulator {
//
//	private final String componentId;
//	private final int capacity;
//	private float energy;
//
//	public SimpleEssSimulator(String componentId, int capacity, int soc) {
//		this.componentId = componentId;
//		this.capacity = capacity;
//		this.energy = (soc / 100F) * this.capacity;
//	}
//
//	@Override
//	public void simulate(Period period) {
//		// TODO take max apparent power in consideration
//		var storage = period.getStorageOrZero();
//		if (storage > 0) {
//			// Discharge
//			if (storage > this.energy) {
//				period.setStorage(this.componentId, Math.round(this.energy));
//				this.energy = 0F;
//				// TODO add cost for non-optimal solution
//			} else {
//				this.energy -= storage;
//			}
//		} else {
//			// Charge
//			storage *= -1; // convert to positive
//			if (this.energy + storage >= this.capacity) {
//				period.setStorage(this.componentId, Math.round(this.energy - this.capacity /* invert */));
//				this.energy = this.capacity;
//				// TODO add cost for non-optimal solution
//			} else {
//				this.energy += storage;
//			}
//		}
//		period.setValue("ess/Soc", this.energy / this.capacity * 100);
//		period.addLog("ESS:" + this.energy);
//	}
//
//}
