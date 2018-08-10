package market.diagram.price;

import java.math.BigDecimal;

import market.diagram.api.Value;

public class ValuePrice implements Value<ValuePrice> {

	private static final long serialVersionUID = -1125126358112158818L;

	private BigDecimal price;
	private BigDecimal power;

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
		try {
			return new ValuePrice(
					(this.price.multiply(this.power).add(v.price.multiply(v.power))).divide((this.power.add(v.power))),
					power.add(v.power));
		} catch (ArithmeticException e) {
			return new ValuePrice(this.price, power.add(v.power));
		}
	}

	@Override
	public ValuePrice subtract(ValuePrice v) {
		try {
			return new ValuePrice(this.price.multiply(this.power).subtract(v.price.multiply(v.power))
					.divide(this.power.subtract(v.power)), power.subtract(v.power));
		} catch (ArithmeticException e) {
			return new ValuePrice(this.price, power.subtract(v.power));
		}
	}

	@Override
	public ValuePrice multiply(long v) {
		return new ValuePrice(price, power.multiply(new BigDecimal(v)));
	}

	@Override
	public ValuePrice divide(long v) {
		return new ValuePrice(price, power.divide(new BigDecimal(v), 10, BigDecimal.ROUND_HALF_UP));
	}

	@Override
	public ValuePrice clone() {
		return new ValuePrice(this.price.doubleValue(), this.power.doubleValue());
	}

	public BigDecimal getPrice() {
		return price;
	}

	public double getPriceDouble() {
		return price.doubleValue();
	}

	public BigDecimal getPower() {
		return power;
	}

	public double getPowerDouble() {
		return power.doubleValue();
	}
}
