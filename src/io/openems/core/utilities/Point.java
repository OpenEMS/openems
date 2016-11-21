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
}
