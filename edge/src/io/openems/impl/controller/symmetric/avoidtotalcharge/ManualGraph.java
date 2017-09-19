package io.openems.impl.controller.symmetric.avoidtotalcharge;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * Created by maxo2 on 29.08.2017.
 */
public class ManualGraph implements ChargingGraph {

    private Map<Integer, Double> percentages;

    public ManualGraph(Map<Integer, Double> percentages) {
        for(Map.Entry<Integer, Double> entry: percentages.entrySet()){
            if(entry.getKey() < 0 || entry.getKey() >= 24 || entry.getValue() < 0 || entry.getValue() > 1){
                throw new IllegalArgumentException();
            }
        }
        this.percentages = percentages;
    }

    public Double getAccordingVal(Calendar c){
        return calcVal(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    public Double getCurrentVal(){
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        return calcVal(c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE));
    }

    private Double calcVal(int h, int m){
        Double v1 = percentages.get(h);
        Double v2 = percentages.get(h < 23 ? h++ : 0);
        double diff = v2 - v1;
        return v1 + (m / 60) * diff;
    }

    public void setPercentage(int hourOfDay, Double percentage) throws IllegalArgumentException {
        if(hourOfDay < 0 || hourOfDay >= 24 || percentage < 0 || percentage > 1){
            throw new IllegalArgumentException();
        }else{
            percentages.put(hourOfDay,percentage);
        }
    }

}
