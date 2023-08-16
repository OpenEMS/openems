package io.openems.edge.predictor.lstmmodel.predictor;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.osgi.service.component.annotations.Reference;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.timedata.api.Timedata;

public class DataQuarry {
	@Reference
	
	SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> quarryResult = new TreeMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>();
	public ArrayList<Double> data= new ArrayList<Double>();
	public ArrayList<OffsetDateTime> date= new ArrayList<OffsetDateTime>();
	private ZonedDateTime fromDate;
	private ZonedDateTime tillDate;
	private ChannelAddress address;
	private int resolution;
	
	public DataQuarry(ZonedDateTime from,ZonedDateTime till, int interval,Timedata td)
	{
	    fromDate = from;
		tillDate = till;
		resolution = interval;
		address = new ChannelAddress("_sum","ConsumptionActivePower");
		this.quarryDataForPrediction(td);
		this.getData();
		this.getDate();
	
		
	}

	
	
	public  void quarryDataForPrediction(Timedata td) {
		
		try {
			this.quarryResult = td.queryHistoricData(null, this.fromDate, this.tillDate, Sets.newHashSet(this.address),
					new Resolution(this.resolution, ChronoUnit.MINUTES));
		} catch (OpenemsNamedException e) {
			
			e.printStackTrace();
			
			
		}
		
	
		
	}
	

	public  void getData(){
		
		this.data = (ArrayList<Double>) this.quarryResult .values().stream() //
				.map(SortedMap::values) //
				.flatMap(Collection::stream) //
				.map(v -> {
					if (v.isJsonNull()) {
						return  null;
					}
					return v.getAsDouble();
				}).collect(Collectors.toList());

		if (isAllNulls(data)) {
			System.out.println("Data is all null, use different predictor");
		
		}
		
	}
	
	



	public void getDate(){
		
		
		
		quarryResult.keySet().stream().forEach(zonedDateTime -> {
		    // Process the ZonedDateTime data
			date.add(zonedDateTime.toOffsetDateTime());
			
			
			});
		
		
	}
	
	private boolean isAllNulls(ArrayList<Double> array) {
		return StreamSupport //
				.stream(array.spliterator(), true) //
				.allMatch(o -> o == null);
	
	}
	
	
}

