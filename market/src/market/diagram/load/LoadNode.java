package market.diagram.load;

public class LoadNode implements Comparable<LoadNode> {
	
	private long time;
	private double value;
	private boolean isStart;
	
	public LoadNode(long time, double value, boolean isStart) {
		this.time = time;
		this.value = value;
		this.isStart = isStart;
	}
	
	public long getTime() {
		return time;
	}

	public double getValue() {
		return value;
	}

	public boolean isStart() {
		return isStart;
	}
	
	public boolean isEnd() {
		return !isStart;
	}
	
	@Override
	public int compareTo(LoadNode key) {
		long timediff = this.time - key.time;
		if (timediff == 0L) {
			// make sure, that old nodes always stay between two newly inserted nodes
			if (key.isStart) {
				return 1;
			} else {
				return -1;
			}
		}
		return (int) timediff;
	}

}
