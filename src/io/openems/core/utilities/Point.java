package io.openems.core.utilities;

public class Point {
	final long x;
	final long y;

	public Point(long x, long y) {
		this.x = x;
		this.y = y;
	}

	@Override public String toString() {
		return "Point x[" + x + "] y[" + y + "]";
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Point other = (Point) obj;
		if (x != other.x) {
			return false;
		}
		if (y != other.y) {
			return false;
		}
		return true;
	}

}
