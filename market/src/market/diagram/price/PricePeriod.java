package market.diagram.price;

import java.util.Date;

import market.diagram.api.Period;
import market.diagram.universal.UniversalPeriod;

public class PricePeriod extends UniversalPeriod<ValuePrice> {

	private static final long serialVersionUID = 607175830054883755L;

	public PricePeriod(Date from, Date to, ValuePrice value) {
		super(from, to, value);
	}

	public PricePeriod(Period<ValuePrice> period) {
		super(period.getStart(), period.getEnd(), period.getValue());
	}

	@Override
	public ValuePrice getValue() {
		return (ValuePrice) super.getValue();
	}

}
