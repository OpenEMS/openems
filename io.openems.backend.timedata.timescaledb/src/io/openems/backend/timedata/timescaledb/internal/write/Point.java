package io.openems.backend.timedata.timescaledb.internal.write;

import java.time.ZonedDateTime;

import de.bytefish.pgbulkinsert.row.SimpleRow;

public abstract class Point {

	public final int channelId;
	public final ZonedDateTime timestamp;

	private Point(int channelId, ZonedDateTime timestamp) {
		this.channelId = channelId;
		this.timestamp = timestamp;
	}

	public static final class IntPoint extends Point {

		public final int value;

		public IntPoint(int channelId, ZonedDateTime timestamp, int value) {
			super(channelId, timestamp);
			this.value = value;
		}

		@Override
		public void addToSimpleRow(SimpleRow row, int column) {
			row.setInteger(column, this.value);
		}

		@Override
		public String toString() {
			return "IntPoint [" + this.timestamp + ": " + this.channelId + "=" + this.value + "]";
		}
	}

	public static final class LongPoint extends Point {

		public final long value;

		public LongPoint(int channelId, ZonedDateTime timestamp, long value) {
			super(channelId, timestamp);
			this.value = value;
		}

		@Override
		public void addToSimpleRow(SimpleRow row, int column) {
			row.setLong(column, this.value);
		}

		@Override
		public String toString() {
			return "LongPoint [" + this.timestamp + ": " + this.channelId + "=" + this.value + "]";
		}
	}

	public static final class FloatPoint extends Point {

		public final float value;

		public FloatPoint(int channelId, ZonedDateTime timestamp, float value) {
			super(channelId, timestamp);
			this.value = value;
		}

		@Override
		public void addToSimpleRow(SimpleRow row, int column) {
			row.setFloat(column, this.value);
		}

		@Override
		public String toString() {
			return "FloatPoint [" + this.timestamp + ": " + this.channelId + "=" + this.value + "]";
		}
	}

	public static final class StringPoint extends Point {

		public final String value;

		public StringPoint(int channelId, ZonedDateTime timestamp, String value) {
			super(channelId, timestamp);
			this.value = value;
		}

		@Override
		public void addToSimpleRow(SimpleRow row, int column) {
			row.setText(column, this.value);
		}

		@Override
		public String toString() {
			return "StringPoint [" + this.timestamp + ": " + this.channelId + "=" + this.value + "]";
		}
	}

	/**
	 * Adds the Point to a {@link SimpleRow} for pgBulkInsert.
	 * 
	 * @param row    the {@link SimpleRow}
	 * @param column the index of the column in the SimpleRow
	 */
	public abstract void addToSimpleRow(SimpleRow row, int column);

}