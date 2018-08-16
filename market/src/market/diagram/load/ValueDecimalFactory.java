package market.diagram.load;

import market.diagram.api.ValueFactory;

public class ValueDecimalFactory implements ValueFactory<ValueDecimal> {

	private static final long serialVersionUID = -3471611969292192058L;

	@Override
	public ValueDecimal NewZero() {
		return new ValueDecimal(0);
	}

	@Override
	public ValueDecimal NewMax() {
		return new ValueDecimal(Double.MAX_VALUE);
	}
}
