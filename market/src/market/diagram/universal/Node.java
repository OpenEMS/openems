package market.diagram.universal;

import java.io.Serializable;
import java.util.Date;

public class Node<V> implements Comparable<Node<V>>, Serializable {

	private static final long serialVersionUID = -5287328839768200606L;

	private Date time;
	private V value;
	private boolean isStart;

	// only for comparison
	Node(Date time, boolean isStart) {
		this.time = time;
		this.isStart = isStart;
		value = null;
	}

	public Node(Date time, V value, boolean isStart) {
		this.time = time;
		this.value = value;
		this.isStart = isStart;
	}

	public long getTime() {
		return time.getTime();
	}

	public V getValue() {
		return value;
	}

	public boolean isStart() {
		return isStart;
	}

	public boolean isEnd() {
		return !isStart;
	}

	@Override
	public int compareTo(Node<V> key) {
		long timediff = this.time.getTime() - key.getTime();
		if (timediff == 0L) {
			// make sure, that old nodes always stay between two newly inserted nodes
			if (key.isStart()) {
				return 1;
			} else {
				return -1;
			}
		}
		return (int) timediff;
	}
}
