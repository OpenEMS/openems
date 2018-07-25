package market.diagram.price;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class PriceDiagram {

	private List<PriceNode> tl;
	
	public PriceDiagram() {
		tl = new ArrayList<PriceNode>();
	}
	
	public void setValue(Date at, double price, double power) {
		setValue(at, 1, price, power);
	}
	
	public void setValue(Date from, long duration, double price, double power) {
		setValue(from, new Date(from.getTime() + duration - 1), price, power);
	}
	
	public void setValue(long from, long duration, double price, double power) {
		setValue(new Date(from), new Date(from + duration - 1), price, power);
	}
	
	public void setValue(Date from, Date to, double price, double power) {
		if (from.getTime() > to.getTime()) {
			return;
		}
		int s = binaryInsert(new PriceNode(from.getTime(), price, power, true));
		int e = binaryInsert(new PriceNode(to.getTime(), price, power, false));
		
		// overwrite old nodes
		PriceNode cutOffEnd = null;
		PriceNode cutOffStart = null;
		int latestType = 0; // 0 = undefined; 1 = start; 2 = end
		while (s < e - 1) {
			PriceNode cur = tl.get(s+1);
			if (cur.isStart()) {
				cutOffStart = cur;
				latestType = 1;
			} else {
				if (latestType == 0) {
					cutOffEnd = cur;
				}
				latestType = 2;
			}
			tl.remove(s+1);
			e--;
		}
		if (latestType == 1) {			
				tl.add(e+1, new PriceNode(to.getTime()+1, cutOffStart.getPrice(), cutOffStart.getPower(), true));
		}
		if (cutOffEnd != null) {
			// there must be a start, if a cut-off-end exists -> tl.get(s-1) can't be null
			tl.add(s, new PriceNode(from.getTime()-1, cutOffEnd.getPrice(), cutOffEnd.getPower(), false));				
		}
		// no nodes inside new time-range -> new time-range could be completely inside an old one
		if (latestType != 1 && cutOffEnd == null) {
			try {
				PriceNode before = tl.get(s-1);
				if (before.isStart()) {
					// suspicion true
					tl.add(s, new PriceNode(from.getTime()-1, before.getPrice(), before.getPower(), false));
					s++;
					e++;
					tl.add(e+1, new PriceNode(to.getTime()+1, tl.get(e+1).getPrice(), tl.get(e+1).getPower(), true));
				}
			} catch (IndexOutOfBoundsException exc) {
				
			}
		}
	}
	
	public double getPrice(long at) {
		int index = -1-Collections.binarySearch(tl,  new PriceNode(at, 0, 0, false));
		try {
			PriceNode before = tl.get(index - 1);
			if (before.isStart()) {
				return before.getPrice();
			} else if (at == before.getTime()) {
				return before.getPrice();
			}
		} catch(IndexOutOfBoundsException e) {
		}
		return 0.0;
	}
	
	public double getPrice(Date at) {
		return getPrice(at.getTime());
	}
	
	
	public double getPower(long at) {
		int index = -1-Collections.binarySearch(tl,  new PriceNode(at, 0, 0, false));
		try {
			PriceNode before = tl.get(index - 1);
			if (before.isStart()) {
				return before.getPower();
			} else if (at == before.getTime()) {
				return before.getPower();
			}
		} catch(IndexOutOfBoundsException e) {
		}
		return 0.0;
	}
	
	public double getPower(Date at) {
		return getPower(at.getTime());
	}
	
	public double getAvgPrice(Date from, long duration) {
		return getAvgPrice(from.getTime(), duration);
	}
	
	public double getAvgPrice(long from, long duration) {
		if (duration < 0) {
			return -1;
		}
		if (duration == 0) {
			return 0;
		}
		if (duration == 1) {
			return getPrice(from);
		}
		
		double sum = 0;
		int index = -1-Collections.binarySearch(tl, new PriceNode(from, 0.0, 0.0, true));
		PriceNode curStart = null;
		PriceNode curEnd = null;
		while(index < tl.size() && (curStart == null || curStart.getTime() <= from + duration - 1)) {
			if (tl.get(index).isStart()) {
				// count time since curEnd as 0.0
				curStart = tl.get(index);
			} else {
				curEnd = tl.get(index);
				long startTime = from;
				long endTime = from + duration - 1;
				if (curStart != null) {
					startTime = curStart.getTime();
				}
				if (curEnd.getTime() < endTime) {
					endTime = curEnd.getTime();
				}
				sum += (endTime - startTime + 1) * curEnd.getPrice();
			}
			index++;
		}		
		
		return sum/(double) duration;
	}
	
	public double getAvgPrice(Date from, Date to) {
		return getAvgPrice(from.getTime(), to.getTime() - from.getTime() + 1); // both included
	}
	
	public double getAvgPower(Date from, long duration) {
		return getAvgPower(from.getTime(), duration);
	}
	
	public double getAvgPower(long from, long duration) {
		if (duration < 0) {
			return -1;
		}
		if (duration == 0) {
			return 0;
		}
		if (duration == 1) {
			return getPower(from);
		}
		
		double sum = 0;
		int index = -1-Collections.binarySearch(tl, new PriceNode(from, 0.0, 0.0, true));
		PriceNode curStart = null;
		PriceNode curEnd = null;
		while(index < tl.size() && (curStart == null || curStart.getTime() <= from + duration - 1)) {
			if (tl.get(index).isStart()) {
				// count time since curEnd as 0.0
				curStart = tl.get(index);
			} else {
				curEnd = tl.get(index);
				long startTime = from;
				long endTime = from + duration - 1;
				if (curStart != null) {
					startTime = curStart.getTime();
				}
				if (curEnd.getTime() < endTime) {
					endTime = curEnd.getTime();
				}
				sum += (endTime - startTime + 1) * curEnd.getPower();
			}
			index++;
		}		
		
		return sum/(double) duration;
	}
	
	public double getAvgPower(Date from, Date to) {
		return getAvgPower(from.getTime(), to.getTime() - from.getTime() + 1); // both included
	}
	
	private int binaryInsert(PriceNode node) {		
		int index = -1-Collections.binarySearch(tl, node);
		tl.add(index, node);
		return index;		
 	}
	
}
