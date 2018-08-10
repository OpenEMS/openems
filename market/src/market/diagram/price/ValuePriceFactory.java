package market.diagram.price;

import market.diagram.api.ValueFactory;

public class ValuePriceFactory implements ValueFactory<ValuePrice> {

	private static final long serialVersionUID = 5785567776356206224L;

	@Override
	public ValuePrice NewZero() {
		return new ValuePrice(0, 0);
	}

	@Override
	public ValuePrice NewMax() {
		return new ValuePrice(Double.MAX_VALUE, Double.MAX_VALUE);
	}
}
