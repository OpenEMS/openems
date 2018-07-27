package market.diagram.price;

import java.math.BigDecimal;

import market.diagram.api.Value;

public class ValuePrice implements Value<ValuePrice> {

	BigDecimal price;
	BigDecimal power;

	public ValuePrice(double price, double power) {
		this.price = new BigDecimal(price);
		this.power = new BigDecimal(power);
	}

	public ValuePrice(BigDecimal price, BigDecimal power) {
		this.price = price;
		this.power = power;
	}

	@Override
	public ValuePrice add(ValuePrice v) {
		return new ValuePrice(price.add(v.price), power.add(v.power));
	}

	@Override
	public ValuePrice multiply(long v) {
		return new ValuePrice(price.multiply(new BigDecimal(v)), price.multiply(new BigDecimal(v)));
	}

	@Override
	public ValuePrice divide(long v) {
		return new ValuePrice(price.divide(new BigDecimal(v), 10, BigDecimal.ROUND_HALF_UP),
				power.divide(new BigDecimal(v), 10, BigDecimal.ROUND_HALF_UP));
	}

	@Override
	public ValuePrice clone() {
		return new ValuePrice(this.price.doubleValue(), this.power.doubleValue());
	}
}
