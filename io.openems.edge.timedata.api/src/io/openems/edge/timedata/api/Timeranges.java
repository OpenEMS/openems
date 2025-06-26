package io.openems.edge.timedata.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Bundle class of multiple {@link Timerange}. Used to get the timeranges to
 * resend data from.
 * 
 * <p>
 * All {@link Timerange} inside a {@link Timeranges} are separated by the
 * {@link Timeranges#MAX_CONCAT_TIME}. When inserting a new timestamp the
 * timestamp gets inserted to the nearest {@link Timerange} or if the nearest
 * one is more than the {@link Timeranges#MAX_CONCAT_TIME} away a new
 * {@link Timerange} gets inserted in the current {@link Timeranges}.
 * 
 * <p>
 * Usage example:
 * 
 * <pre>
 * final var timeranges = timedata.getResendTimeranges([CHANNEL_ADDRESS], [LAST_RESEND_TIMESTAMP]);
 * for (var timerange : timeranges.maxDataInTime([MAX_RESEND_TIMESPAN_SECONDS])) {
 * 	final var from = timerange.getMinTimestamp();
 * 	final var to = timerange.getMaxTimestamp();
 * 	...fetch resend data
 * 	...send data
 * }
 * </pre>
 *
 * @see Timedata#getResendTimeranges(io.openems.common.types.ChannelAddress,
 *      long)
 */
public class Timeranges {

	protected static final long MAX_CONCAT_TIME = 300;

	protected static final Comparator<Timerange> TIMERANGE_COMPARATOR = (o1, o2) -> {
		if (o2.getMaxTimestamp() < o1.getMinTimestamp()) {
			return -1;
		}
		if (o2.getMinTimestamp() > o1.getMaxTimestamp()) {
			return 1;
		}
		return 0;
	};

	public static class Timerange {
		private SortedSet<Long> timestamps = new TreeSet<Long>();

		/**
		 * Constructor of a {@link Timerange}.
		 * 
		 * @param timestamp the initial value of the {@link Timerange} in seconds
		 */
		public Timerange(long timestamp) {
			super();
			this.timestamps.add(timestamp);
		}

		/**
		 * Inserts a timestamp in seconds into this {@link Timerange}.
		 * 
		 * @param timestamp the timestamp to inserts in seconds
		 */
		public void insert(long timestamp) {
			this.timestamps.add(timestamp);
		}

		public long getTimespan() {
			return this.getMaxTimestamp() - this.getMinTimestamp();
		}

		public long getMaxTimestamp() {
			return this.timestamps.last();
		}

		public long getMinTimestamp() {
			return this.timestamps.first();
		}

		public List<Long> getTimestamps() {
			return this.timestamps.stream().toList();
		}

		/**
		 * Creates an {@link Iterable} which iterates over new {@link Timerange
		 * Timeranges} where the max size of one {@link Timerange} is the given
		 * maxTimerange.
		 * 
		 * @param maxTimerange the maximal timerange of one {@link Timerange} in seconds
		 * @return a new {@link Iterable}
		 */
		public Iterable<Timerange> maxRange(long maxTimerange) {
			return new Iterable<Timerange>() {

				@Override
				public Iterator<Timerange> iterator() {
					return new Iterator<Timerange>() {

						private final Iterator<Long> iterator = Timerange.this.timestamps.iterator();
						private Long next;

						@Override
						public Timerange next() {
							final Timerange timerange;
							if (this.next == null) {
								timerange = new Timerange(this.iterator.next());
							} else {
								timerange = new Timerange(this.next);
								this.next = null;
							}
							while (this.iterator.hasNext()) {
								final var timestamp = this.next = this.iterator.next();
								if (timerange.getMinTimestamp() + maxTimerange < timestamp) {
									break;
								}
								this.next = null;
								timerange.insert(timestamp);
							}
							return timerange;
						}

						@Override
						public boolean hasNext() {
							return this.iterator.hasNext() || this.next != null;
						}
					};

				}
			};
		}

	}

	private final SortedSet<Timerange> timeranges = new TreeSet<>(TIMERANGE_COMPARATOR);

	/**
	 * Inserts a timestamp in seconds into this {@link Timeranges}.
	 * 
	 * @param timestamp the timestamp to inserts in seconds
	 */
	public void insert(long timestamp) {
		final var subSet = this.timeranges.subSet(new Timerange(timestamp + MAX_CONCAT_TIME),
				new Timerange(timestamp - MAX_CONCAT_TIME));
		if (subSet.isEmpty()) {
			this.timeranges.add(new Timerange(timestamp));
		} else {
			subSet.first().insert(timestamp);
		}
	}

	/**
	 * Gets the {@link Timerange Timeranges} in ascending order.
	 * 
	 * @return a list of the {@link Timerange Timeranges}
	 */
	public List<Timerange> getTimerangeAscending() {
		final var newTimerangeList = new ArrayList<>(this.timeranges);
		Collections.reverse(newTimerangeList);
		return newTimerangeList;
	}

	/**
	 * Creates a new {@link Timeranges} with the given buffers before and after
	 * every {@link Timerange}.
	 * 
	 * @param beforeBuffer the buffer before every {@link Timerange}
	 * @param afterBuffer  the buffer after every {@link Timerange}
	 * @return the new {@link Timeranges} with the buffer
	 */
	public Timeranges withBuffer(long beforeBuffer, long afterBuffer) {
		final var withBuffer = new Timeranges();

		for (final var timerange : this.timeranges) {
			withBuffer.insert(timerange.getMinTimestamp() - beforeBuffer);
			for (final var timestamps : timerange.timestamps) {
				withBuffer.insert(timestamps);
			}
			withBuffer.insert(timerange.getMaxTimestamp() + afterBuffer);
		}

		return withBuffer;
	}

	/**
	 * Creates an {@link Iterable} which iterates over new {@link Timerange
	 * Timeranges} where the max size of one {@link Timerange} is the given
	 * maxTimerange.
	 * 
	 * @param maxTimerange the maximal timerange of one {@link Timerange} in seconds
	 * @return a new {@link Iterable}
	 */
	public Iterable<Timerange> maxDataInTime(long maxTimerange) {
		return new Iterable<Timerange>() {

			@Override
			public Iterator<Timerange> iterator() {
				return new Iterator<Timerange>() {

					private final Iterator<Timerange> iterator = Timeranges.this.getTimerangeAscending().iterator();
					private Timerange current = this.nextOrNull();
					private Iterator<Timerange> partIterator = null;

					@Override
					public Timerange next() {
						Timerange returnTimerange;
						if (this.current.getTimespan() < maxTimerange) {
							// take whole timerange
							returnTimerange = this.current;
							this.current = this.nextOrNull();
						} else {
							// take only part from timerange
							if (this.partIterator == null) {
								this.partIterator = this.current.maxRange(maxTimerange).iterator();
							}
							returnTimerange = this.partIterator.next();
							if (!this.partIterator.hasNext()) {
								this.partIterator = null;
								this.current = this.nextOrNull();
							}
						}

						return returnTimerange;
					}

					@Override
					public boolean hasNext() {
						return this.current != null;
					}

					private Timerange nextOrNull() {
						return this.iterator.hasNext() ? this.iterator.next() : null;
					}

				};
			}

		};
	}

}
