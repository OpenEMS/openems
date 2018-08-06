package market.diagram.universal;

import java.util.Date;

import market.diagram.api.Period;
import market.diagram.api.Value;

public class UniversalPeriod<T extends Value<T>> implements Period<T> {

	private static final long serialVersionUID = 8117762209543194112L;

	private Date start;
	private Date end;
	private T value;

	public UniversalPeriod(Date from, Date to, T value) {
		this.start = from;
		this.end = to;
		this.value = value;
	}

	public Date getStart() {
		return start;
	}

	public Date getEnd() {
		return end;
	}

	public T getValue() {
		return (T) value;
	}

}
