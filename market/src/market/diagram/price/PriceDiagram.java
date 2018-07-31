package market.diagram.price;

import java.util.Date;

import market.diagram.api.Period;
import market.diagram.universal.ArrayListDiagram;

public class PriceDiagram extends ArrayListDiagram<ValuePrice> {

	private static final long serialVersionUID = 5907453111456542062L;

	public PriceDiagram() {
		super();
	}

	public void setValue(Date at, ValuePrice value) {
		setValue(at, 1, value);
	}

	public void setValue(Date from, long duration, ValuePrice value) {
		setValue(from, new Date(from.getTime() + duration - 1), value);
	}

	public void setValue(long from, long duration, ValuePrice value) {
		setValue(new Date(from), new Date(from + duration - 1), value);
	}

	public void setValue(Date from, Date to, ValuePrice value) {
		super.setValue(from, to, value);
	}

	public void erasePeriod(Date at) {
		erasePeriod(at, 1);
	}

	public void erasePeriod(Date from, long duration) {
		erasePeriod(from, new Date(from.getTime() + duration - 1));
	}

	public void erasePeriod(long from, long duration) {
		erasePeriod(new Date(from), new Date(from + duration - 1));
	}

	public void erasePeriod(Date from, Date to) {
		super.erasePeriod(from, to);
	}

	public ValuePrice getValue(Date at) {
		return super.getValue(at);
	}

	public ValuePrice getValue(long at) {
		return getValue(new Date(at));
	}

	public ValuePrice getAvg(Date from, long duration) {
		return getAvg(from, new Date(duration + from.getTime() - 1));
	}

	public ValuePrice getAvg(long from, long duration) {
		return getAvg(new Date(from), duration);

	}

	public ValuePrice getAvg(Date from, Date to) {
		return super.getAvg(from, to);
	}

	public PriceDiagram getCopy() {
		return (PriceDiagram) super.getCopy();
	}

	public PricePeriod getNext() {
		Period<ValuePrice> next = super.getNext();
		if (next == null) {
			return null;
		}
		return new PricePeriod(next);
	}
}
