package io.openems.edge.predictor.lstm.adddata;

import java.time.OffsetDateTime;
//mport java.time.ZonedDateTime;
import java.util.ArrayList;



public class AddHourData {
	
	public static ArrayList<Double> getCorrespondingMinuteData(ArrayList<Double> data,ArrayList<OffsetDateTime>date,OffsetDateTime target){
		
		
		
		return searchAndReturn(data,date,getHourDataPoints(target,15));
		
	}
	
	public static ArrayList<OffsetDateTime> getHourDataPoints(OffsetDateTime targetDate,int interval){
		ArrayList<OffsetDateTime> timeList = new ArrayList<OffsetDateTime>();
		
		
		for (int i= 0; i< (Integer)(60/interval);i++) {
			targetDate = targetDate.minusMinutes(interval);
			
			
		timeList.add(targetDate);
		
		reverseArray(timeList);
			
		}
		
		//System.out.println(timeList);
		return reverseArray(timeList);
		
	}
	
		
	public static ArrayList<Double> searchAndReturn(ArrayList<Double> doubleList, ArrayList<OffsetDateTime> bigList, ArrayList<OffsetDateTime> smallList) {
		 ArrayList<Double> results = new ArrayList<>();

	        for (OffsetDateTime dateTime : smallList) {
	            int index = bigList.indexOf(dateTime);
	            if (index != -1 && index < doubleList.size()) {
	                results.add(doubleList.get(index));
	               
	            }
	            else {results.add(0.0);}
	        }

	        return results;
    }


 
     
     public static ArrayList<OffsetDateTime> reverseArray(ArrayList<OffsetDateTime> data){
     ArrayList<OffsetDateTime> ordered = new ArrayList<OffsetDateTime>();
     for (int i = data.size() - 1; i >= 0; i--) {
    	 
         
        ordered.add(data.get(i));
     }

  
     return ordered;
 }
    public static ArrayList<Double> merg(ArrayList<Double>list1, ArrayList<Double>list2){
    	
    	ArrayList<Double> mergedList = new ArrayList<Double>();
    	mergedList.addAll(list1);
    	mergedList.addAll(list2);
    		
    	
    	return mergedList;
    }
     
	

}
