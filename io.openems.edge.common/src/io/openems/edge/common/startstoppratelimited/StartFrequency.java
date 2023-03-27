package io.openems.edge.common.startstoppratelimited;

import java.time.Duration;

public class StartFrequency {

	private int times;
	private Duration time;
	
	private StartFrequency(int times, Duration time) {
		this.times = times;
		this.time = time;
	}
	
	public static StartFrequencyBuilder builder() {
		return new StartFrequencyBuilder();
	}
	
	
	
	public int getTimes() {
		return times;
	}



	public Duration getDuration() {
		return time;
	}



	public static class StartFrequencyBuilder {
		private int times;
		private Duration duration;
		
		public StartFrequencyBuilder withOccurence(int occurence) {
			if(occurence <= 0) {
				throw new IllegalArgumentException("Frequency requires positiove number, " + occurence + " was given.");
			}
			this.times = occurence;
			return this;
		}
		
		public StartFrequencyBuilder withDuration(Duration duration) {
			if(duration.isNegative() || duration.isZero())
				throw new IllegalArgumentException("Frequency requires a positive, non zero duration, " + duration.getSeconds() + "s was given.");
			this.duration = duration;
			return this;
		}
		
		public StartFrequency build() {
			return new StartFrequency(this.times, this.duration);
		}
	}
}
