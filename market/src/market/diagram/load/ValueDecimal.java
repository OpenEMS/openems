package market.diagram.load;

import java.math.BigDecimal;

import market.diagram.api.Value;

public class ValueDecimal extends BigDecimal implements Value<ValueDecimal> {

	private static final long serialVersionUID = -1056094065002926124L;

	public ValueDecimal(double val) {
		super(val);
	}

	public ValueDecimal(BigDecimal val) {
		super(val.doubleValue());
	}

	@Override
	public ValueDecimal add(ValueDecimal v) {
		return new ValueDecimal(super.add(v));
	}

	@Override
	public ValueDecimal subtract(ValueDecimal v) {
		return new ValueDecimal(super.subtract(v));
	}

	@Override
	public ValueDecimal multiply(long v) {
		return new ValueDecimal(super.multiply(new BigDecimal(v)));
	}

	@Override
	public ValueDecimal divide(long v) {
		return new ValueDecimal(super.divide(new BigDecimal(v), 10, BigDecimal.ROUND_HALF_UP));
	}

	@Override
	public ValueDecimal clone() {
		return new ValueDecimal(this.doubleValue());
	}

	public BigDecimal getDecimal() {
		return (BigDecimal) this;
	}

	public double getDecimalDouble() {
		return super.doubleValue();
	}
}
