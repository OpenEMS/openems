package market.agent.api;

import market.square.api.PriceCurve;

public interface MarketAgent {

	void receivePriceCurve(PriceCurve offer);
	
}
