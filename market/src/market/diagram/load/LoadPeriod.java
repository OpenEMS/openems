package market.diagram.load;

import java.util.Date;

import market.diagram.api.Period;
import market.diagram.universal.UniversalPeriod;

public class LoadPeriod extends UniversalPeriod<ValueDecimal> {

	private static final long serialVersionUID = -3523262346595045931L;

	public LoadPeriod(Date from, Date to, ValueDecimal value) {
		super(from, to, value);
	}

	public LoadPeriod(Period<ValueDecimal> period) {
		super(period.getStart(), period.getEnd(), period.getValue());
	}

	@Override
	public ValueDecimal getValue() {
		return (ValueDecimal) super.getValue();
	}

}
