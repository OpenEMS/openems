package market.diagram.load;

import java.util.Date;

import market.diagram.api.Period;
import market.diagram.universal.ArrayListDiagram;

public class LoadDiagram extends ArrayListDiagram<ValueDecimal> {

	private static final long serialVersionUID = 5265230054197284740L;

	public LoadDiagram() {
		super(new ValueDecimalFactory());
	}

	public void setValue(Date at, ValueDecimal value) {
		setValue(at, 1, value);
	}

	public void setValue(Date from, long duration, ValueDecimal value) {
		setValue(from, new Date(from.getTime() + duration - 1), value);
	}

	public void setValue(long from, long duration, ValueDecimal value) {
		setValue(new Date(from), new Date(from + duration - 1), value);
	}

	public void setValue(Date from, Date to, ValueDecimal value) {
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

	public ValueDecimal getValue(Date at) {
		return super.getValue(at);
	}

	public ValueDecimal getValue(long at) {
		return getValue(new Date(at));
	}

	public ValueDecimal getAvg(Date from, long duration) {
		return getAvg(from, new Date(duration + from.getTime() - 1));
	}

	public ValueDecimal getAvg(long from, long duration) {
		return getAvg(new Date(from), duration);

	}

	public ValueDecimal getAvg(Date from, Date to) {
		return super.getAvg(from, to);
	}

	public void print() {
		super.print();
	}

	public LoadDiagram getCopy() {
		return (LoadDiagram) super.getCopy();
	}

	public LoadPeriod getNext() {
		Period<ValueDecimal> next = super.getNext();
		if (next == null) {
			return null;
		}
		return new LoadPeriod(next);
	}
}
