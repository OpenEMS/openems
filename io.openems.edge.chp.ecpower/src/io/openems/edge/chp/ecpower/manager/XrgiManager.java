package io.openems.edge.chp.ecpower.manager;

import io.openems.edge.common.channel.Doc;

import io.openems.edge.common.component.OpenemsComponent;


public interface XrgiManager extends  OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
	


		
		
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
	
	
	void applyPower(int activePowerTarget);

	void applyPower(Integer activePowerTarget);		
	
	//public default Integer getActivePower() {
	//	return null;		
	//}
	
}
