package market.diagram.load;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class LoadDiagram {

	private List<LoadNode> tl;
	
	public LoadDiagram() {
		tl = new ArrayList<LoadNode>();
	}
	
	public void setValue(Date at, double value) {
		setValue(at, 1, value);
	}
	
	public void setValue(Date from, long duration, double value) {
		setValue(from, new Date(from.getTime() + duration - 1), value);
	}
	
	public void setValue(long from, long duration, double value) {
		setValue(new Date(from), new Date(from + duration - 1), value);
	}
	
	public void setValue(Date from, Date to, double value) {
		if (from.getTime() > to.getTime()) {
			return;
		}
		int s = binaryInsert(new LoadNode(from.getTime(), value, true));
		int e = binaryInsert(new LoadNode(to.getTime(), value, false));
		
		// overwrite old nodes
		LoadNode cutOffEnd = null;
		LoadNode cutOffStart = null;
		int latestType = 0; // 0 = undefined; 1 = start; 2 = end
		while (s < e - 1) {
			LoadNode cur = tl.get(s+1);
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
				tl.add(e+1, new LoadNode(to.getTime()+1, cutOffStart.getValue(), true));
		}
		if (cutOffEnd != null) {
			// there must be a start, if a cut-off-end exists -> tl.get(s-1) can't be null
			tl.add(s, new LoadNode(from.getTime()-1, cutOffEnd.getValue(), false));				
		}
		// no nodes inside new time-range -> new time-range could be completely inside an old one
		if (latestType != 1 && cutOffEnd == null) {
			try {
				LoadNode before = tl.get(s-1);
				if (before.isStart()) {
					// suspicion true
					tl.add(s, new LoadNode(from.getTime()-1, before.getValue(), false));
					s++;
					e++;
					tl.add(e+1, new LoadNode(to.getTime()+1, tl.get(e+1).getValue(), true));
				}
			} catch (IndexOutOfBoundsException exc) {
				
			}
		}
	}
	
	public double getValue(long at) {
		int index = -1-Collections.binarySearch(tl,  new LoadNode(at, 0, false));
		try {
			LoadNode before = tl.get(index - 1);
			if (before.isStart()) {
				return before.getValue();
			} else if (at == before.getTime()) {
				return before.getValue();
			}
		} catch(IndexOutOfBoundsException e) {
		}
		return 0.0;
	}
	
	public double getValue(Date at) {
		return getValue(at.getTime());
	}
	
	public double getAvg(Date from, long duration) {
		return getAvg(from.getTime(), duration);
	}
	
	public double getAvg(long from, long duration) {
		if (duration < 0) {
			return -1;
		}
		if (duration == 0) {
			return 0;
		}
		if (duration == 1) {
			return getValue(from);
		}
		
		double sum = 0;
		int index = -1-Collections.binarySearch(tl, new LoadNode(from, 0.0, true));
		LoadNode curStart = null;
		LoadNode curEnd = null;
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
				sum += (endTime - startTime + 1) * curEnd.getValue();
			}
			index++;
		}		
		
		return sum/(double) duration;
	}
	
	public double getAvg(Date from, Date to) {
		return getAvg(from.getTime(), to.getTime() - from.getTime() + 1); // both included
	}
	
	private int binaryInsert(LoadNode node) {		
		int index = -1-Collections.binarySearch(tl, node);
		tl.add(index, node);
		return index;		
 	}
	
}
