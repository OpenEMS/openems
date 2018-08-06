package market.square.api;

import java.util.Date;

import market.diagram.api.Diagram;
import market.diagram.price.PriceDiagram;

public interface MarketSquare extends Runnable {

	PriceDiagram getMarketState();

	PriceDiagram getConsistentMarketState();

	Long registerAgent();

	void unregisterAgent(Long agentID);

	@SuppressWarnings("rawtypes")
	Long addDiagram(Long agentID, Diagram d);

	void removeDiagram(Long agentID, Long diagramID);

	double getMarketReactivity(Date from, Date to);

}