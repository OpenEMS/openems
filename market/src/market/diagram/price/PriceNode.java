package market.diagram.price;

public class PriceNode implements Comparable<PriceNode> {
	
	private long time;
	private double price;
	private double power;
	private boolean isStart;
	
	public PriceNode(long time, double price, double power, boolean isStart) {
		this.time = time;
		this.price = price;
		this.power = power;
		this.isStart = isStart;
	}
	
	public long getTime() {
		return time;
	}
	
	public double getPrice() {
		return price;
	}

	public double getPower() {
		return power;
	}

	public boolean isStart() {
		return isStart;
	}
	
	public boolean isEnd() {
		return !isStart;
	}
	
	@Override
	public int compareTo(PriceNode key) {
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
