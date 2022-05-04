package io.openems.edge.ess.core.power;

public class Sorter {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int k;  
		//initializing an array  
		int EssSoC[] = {19, 45, 25, 70, 23, 89, 21, 56, 27};  // how do we get this data? From BMS ? through Modbus? 
			System.out.print("ESSs' SoCs before sorting: \n");  
		for(k = 0; k < EssSoC.length; k++)  
			System.out.println(EssSoC[k]);        
		//user defined method for sorting in ascending            
		sortSoC_ascending(EssSoC, EssSoC.length);  
		System.out.print("ESSs' SoCs after sorting: \n");      
		//accessing elements of the sorted array     
		for(k = 0; k <EssSoC.length; k++)  
			{  
			System.out.println(EssSoC[k]);  
			}
		
		//user defined method for sorting in ascending            
		sortSoC_descending(EssSoC, EssSoC.length);  
		System.out.print("ESSs' SoCs after sorting: \n");      
		//accessing elements of the sorted array     
		for(k = 0; k <EssSoC.length; k++)  
				{  
				System.out.println(EssSoC[k]);  
				}  
		}  
		//user defined method to sort an array in ascending order  
		private static void sortSoC_ascending(int array[], int n)   
			{  
			for (int k = 1; k < n; k++)  
				{  
					int j = k;  
					int a = array[k];  
					while ((j > 0) && (array[j-1] > a))   //returns true when both conditions are true  
						{  
							array[j] = array[j-1];  
							j--;  
						}  
					array[j] = a;
				}
			}  
		
		//user defined method to sort an array in ascending order  
		private static void sortSoC_descending(int array[], int n)   
		{  
			for (int k = 1; k < n; k++)  
				{  
					int j = k;  
					int a = array[k];  
						while ((j > 0) && (array[j-1] < a))   //returns true when both conditions are true  
							{  
								array[j] = array[j-1];  
								j--;  
							}  
					array[j] = a;  
				}  
	}

}
