package market.square.api;

import java.util.Date;

import org.osgi.service.event.EventHandler;

import market.diagram.api.Diagram;
import market.diagram.price.PriceDiagram;

/**
 * A MarketSquare simulates a electricity-market. This enables components or
 * devices to communicate with each other about when they are going to
 * consume/produce power, how much they are going to need/provide and how urgent
 * their demand is/how much it costs them to provide energy.
 * 
 * Each market participant (agent) can issue multiple Diagrams representing its
 * demand and/or supply over time.
 * 
 * The MarketSquare sums up the demand and tries to satisfy it by using the
 * cheapest supply-sources. It then calculates the average price and the "sold"
 * energy. These values are then stored as a PriceDiagram (market state) and can
 * be red by agents, so that they can adjust their issued Diagrams based on the
 * market's reaction.
 * 
 * @author FENECON GmbH
 */

public interface MarketSquare extends EventHandler {

	/**
	 * Returns a PriceDiagram depicting the sold power and the according average
	 * price over time.
	 * 
	 * @return this market's "state". This value is NOT consistent, it may change
	 *         over time
	 */
	PriceDiagram getMarketState();

	/**
	 * Returns a PriceDiagram depicting the sold power and the according average
	 * price over time.
	 * 
	 * @return this market's "state". This value IS consistent, returns a deep-copy
	 *         of getMarketState()'s return
	 */
	PriceDiagram getConsistentMarketState();

	/**
	 * Returns an ID that agents can use to identify themselves.
	 * 
	 * @return random Agent-ID
	 */
	Long registerAgent();

	/**
	 * Deletes all Diagrams issued by the agent with the given ID
	 * 
	 * @param agentID
	 *            all of this agent's entries shall be deleted
	 */
	void unregisterAgent(Long agentID);

	/**
	 * Adds a Diagram to the according agent's account.
	 * 
	 * @param agentID
	 *            the ID of the agent to whose account the Diagram shall be added
	 * @param d
	 *            a Diagram representing the agent's demand or supply. If the
	 *            Diagram represents demand, the Diagram MUST be a LoadDiagram as
	 *            defined in this bundle. Negative values are positive demand.
	 *            Positive values are forced supply. If the Diagram represents
	 *            supply, the Diagram MUST be a PriceDiagram as defined in this
	 *            Bundle.
	 * @return diagramID, that can be used to delete the diagram from the agent's
	 *         account later on
	 */
	@SuppressWarnings("rawtypes")
	Long addDiagram(Long agentID, Diagram d);

	/**
	 * Removes the addressed Diagram from the addressed agent's account.
	 * 
	 * @param agentID
	 *            the ID of the agent from whose account the Diagram shall be
	 *            removed
	 * @param diagramID
	 *            the ID of the diagram to remove
	 */
	void removeDiagram(Long agentID, Long diagramID);

	/**
	 * The "market reactivity" indicates, how much influence a change in demand or
	 * supply of power will have on the price at a given time-period. It basically
	 * describes how much power it takes to increase the price by 10% or 0.01€, if
	 * the current price is 0.00€.
	 * 
	 * @param from
	 *            starting time
	 * @param to
	 *            ending time
	 * @return power-demand needed to increase price by 10% or 0.01€, if the current
	 *         price is 0.00€. This value MUST be positive
	 */
	double getMarketReactivity(Date from, Date to);

}