// CHECKSTYLE:OFF
/*
*   EDCOM 8.1 is a java cross platform library for communication with 10kW
*   hybrid Inverter (Katek Memmingen GmbH).
*   Copyright (C) 2022 Katek Memmingen GmbH
*
*   This program is free software: you can redistribute it and/or modify
*   it under the terms of the GNU Lesser General Public License as published by
*   the Free Software Foundation, either version 3 of the License, or
*   (at your option) any later version.
*
*   This program is distributed in the hope that it will be useful,
*   but WITHOUT ANY WARRANTY; without even the implied warranty of
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*   GNU General Public License for more details.
*   
*   You should have received a copy of the GNU Lesser General Public License
*   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.ed.data;

import java.util.Calendar;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

import com.ed.data.history.ADataItem;
import com.ed.data.history.DayDataItem;
import com.ed.data.history.HourDataItem;
import com.ed.data.history.MonthDataItem;
import com.ed.data.history.YearDataItem;
import com.ed.edcom.Client;
import com.ed.edcom.DspVar;

/**
 * Inverter history
 */
public final class History implements DataSet {

	public static final int HOUR = 0;
	public static final int DAY = 1;
	public static final int MONTH = 2;
	public static final int YEAR = 3;

	// Size of EEPROM Ring buffers
	private static final int HOURS_MAX_CNT = 168;
	private static final int DAYS_MAX_CNT = 64;
	private static final int MONTH_MAX_CNT = 16;
	private static final int YEARS_MAX_CNT = 32;

	private final DspVar hourBlock;
	private final DspVar dayBlock;
	private final DspVar monthBlock;
	private final DspVar yearBlock;
	private final DspVar dbAck;

	private int blockIx;
	private DspVar curBlock;

	/**
	 * Creates a object representing inverter history
	 *
	 * @throws java.lang.Exception wrong parameters
	 */
	public History() throws Exception {
		this.dbAck = new DspVar("dd.db_asc", DspVar.TYPE_UINT16, 1, null, 0);
		this.hourBlock = new DspVar("dd.hour_block_ext", DspVar.TYPE_INT8, 276, null, 0);
		this.hourBlock.setSynchReadDspVar(this.dbAck);
		this.hourBlock.enableReqOptionalField(true);
		this.dayBlock = new DspVar("dd.day_block_ext", DspVar.TYPE_INT8, 24, null, 0);
		this.dayBlock.setSynchReadDspVar(this.dbAck);
		this.dayBlock.enableReqOptionalField(true);
		this.monthBlock = new DspVar("dd.month_block_ext", DspVar.TYPE_INT8, 24, null, 0);
		this.monthBlock.setSynchReadDspVar(this.dbAck);
		this.monthBlock.enableReqOptionalField(true);
		this.yearBlock = new DspVar("dd.year_block_ext", DspVar.TYPE_INT8, 36, null, 0);
		this.yearBlock.setSynchReadDspVar(this.dbAck);
		this.yearBlock.enableReqOptionalField(true);
		this.curBlock = hourBlock;
	}

	/**
	 * Set required history data block
	 *
	 * @param type History.HOUR, History.DAY, History.MONTH or History.YEAR
	 */
	public void setRequiredData(int type) {
		switch (type) {
		case History.HOUR:
			this.curBlock = this.hourBlock;
			break;
		case History.DAY:
			this.curBlock = this.dayBlock;
			break;
		case History.MONTH:
			this.curBlock = this.monthBlock;
			break;
		case History.YEAR:
			this.curBlock = this.yearBlock;
			break;
		}
	}

	public boolean hasNext() {
		int maxIx = 0;
		if (this.curBlock == this.hourBlock) {
			maxIx = HOURS_MAX_CNT;
		}
		if (this.curBlock == this.dayBlock) {
			maxIx = DAYS_MAX_CNT;
		}
		if (this.curBlock == this.monthBlock) {
			maxIx = MONTH_MAX_CNT;
		}
		if (this.curBlock == this.yearBlock) {
			maxIx = YEARS_MAX_CNT;
		}
		return (this.blockIx < maxIx);
	}

	public void setNext() {
		this.blockIx++;
	}

	public void setDataIndex(int ix) {
		this.blockIx = ix;
	}

	@Override
	public void registerData(Client cl) {
		cl.addDspVar(this.dbAck);
		cl.addDspVar(this.hourBlock);
		cl.addDspVar(this.dayBlock);
		cl.addDspVar(this.monthBlock);
		cl.addDspVar(this.yearBlock);
	}

	@Override
	public void refresh() {
		this.curBlock.setReqOptionalField(blockIx + 1);
		this.curBlock.refresh();
	}

	@Override
	public boolean dataReady() {
		if (this.curBlock.refreshTime() > 0) {
			if (this.dbAck.getLong() == (blockIx + 1)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get History data block
	 *
	 * @return required data
	 */
	public ADataItem getData() {
		if (this.curBlock == this.hourBlock) {
			return new HourDataItem(this.curBlock.getBytes());
		}
		if (this.curBlock == this.dayBlock) {
			return new DayDataItem(this.curBlock.getBytes());
		}
		if (this.curBlock == this.monthBlock) {
			return new MonthDataItem(this.curBlock.getBytes());
		}
		if (this.curBlock == this.yearBlock) {
			return new YearDataItem(this.curBlock.getBytes());
		}
		throw new RuntimeException();
	}

	@Override
	public String toString() {
		String rs;
		if (this.curBlock.refreshTime() > 0) {
			rs = this.curBlock.toString() + "\n";
		} else {
			rs = "no data\n";
		}
		return rs;
	}

	/**
	 * Get history year record
	 *
	 * @return All recorded energy valuesper year sorted by year in ascending order
	 *
	 * @throws java.lang.InterruptedException
	 */
	public synchronized SortedMap<Date, SortedMap<String, Float>> getHistoryYear() throws InterruptedException {

		SortedMap<Date, SortedMap<String, Float>> map = new TreeMap<>();

		this.setRequiredData(History.YEAR);

		Float compEng, gridCon, gridInj, invCon, invInj, batAmp;

		this.setDataIndex(0);
		this.refresh();
		while (this.hasNext()) {

			if (this.dataReady()) {
				YearDataItem yearData = (YearDataItem) this.getData();
				SortedMap<String, Float> data = new TreeMap<>();
				try {
					compEng = yearData.getCompensationEnergy();
				} catch (Exception ex) {
					compEng = 0f;
				}
				data.put("e_self_cons", compEng);
				try {
					gridCon = yearData.getGridConsEnergy();
				} catch (Exception ex) {
					gridCon = 0f;
				}
				data.put("e_grid_cons", gridCon);
				try {
					gridInj = yearData.getGridInjEnergy();
				} catch (Exception ex) {
					gridInj = 0f;
				}
				data.put("e_grid_feedin", gridInj);
				try {
					invCon = yearData.getInvConsEnergy();
				} catch (Exception ex) {
					invCon = 0f;
				}
				data.put("e_inv_cons", invCon);
				try {
					invInj = yearData.getInvInjEnergy();
				} catch (Exception ex) {
					invInj = 0f;
				}
				data.put("e_inv_feedin", invInj);
				try {
					batAmp = yearData.getQAcc();
				} catch (Exception ex) {
					batAmp = 0f;
				}
				data.put("bat_ah", batAmp);

				map.put(yearData.getTime(), data);
				this.setNext();

				this.refresh();
			}
			wait(1);
		}
		this.setDataIndex(0);
		this.refresh();
		return map;

	}

	/**
	 * Get history month record
	 *
	 * @return All recorded energy values per month sorted by month in ascending
	 *         order
	 *
	 * @throws java.lang.InterruptedException
	 */
	public synchronized SortedMap<Date, SortedMap<String, Float>> getHistoryMonth() throws InterruptedException {

		SortedMap<Date, SortedMap<String, Float>> map = new TreeMap<>();

		this.setRequiredData(History.MONTH);

		Float compEng, gridCon, gridInj, invCon, invInj, batAmp;

		this.setDataIndex(0);
		this.refresh();
		while (this.hasNext()) {

			if (this.dataReady()) {
				MonthDataItem monthData = (MonthDataItem) this.getData();
				SortedMap<String, Float> data = new TreeMap<>();
				try {
					compEng = monthData.getCompensationEnergy();
				} catch (Exception ex) {
					compEng = 0f;
				}
				data.put("e_self_cons", compEng);
				try {
					gridCon = monthData.getGridConsEnergy();
				} catch (Exception ex) {
					gridCon = 0f;
				}
				data.put("e_grid_cons", gridCon);
				try {
					gridInj = monthData.getGridInjEnergy();
				} catch (Exception ex) {
					gridInj = 0f;
				}
				data.put("e_grid_feedin", gridInj);
				try {
					invCon = monthData.getInvConsEnergy();
				} catch (Exception ex) {
					invCon = 0f;
				}
				data.put("e_inv_cons", invCon);
				try {
					invInj = monthData.getInvInjEnergy();
				} catch (Exception ex) {
					invInj = 0f;
				}
				data.put("e_inv_feedin", invInj);
				try {
					batAmp = monthData.getQAcc();
				} catch (Exception ex) {
					batAmp = 0f;
				}
				data.put("bat_ah", batAmp);

				map.put(monthData.getTime(), data);
				this.setNext();

				this.refresh();
			}
			wait(1);
		}
		this.setDataIndex(0);
		this.refresh();
		return map;

	}

	/**
	 * Get history day record
	 *
	 * @return All recorded energy values per day sorted by day in ascending order.
	 *
	 * @throws java.lang.InterruptedException
	 */
	public synchronized SortedMap<Date, SortedMap<String, Float>> getHistoryDay() throws InterruptedException {

		SortedMap<Date, SortedMap<String, Float>> map = new TreeMap<>();

		this.setRequiredData(History.DAY);

		Float compEng, gridCon, gridInj, invCon, invInj, batAmp;

		this.setDataIndex(0);
		this.refresh();
		while (this.hasNext()) {

			if (this.dataReady()) {
				DayDataItem dayData = (DayDataItem) this.getData();
				SortedMap<String, Float> data = new TreeMap<>();
				try {
					compEng = dayData.getCompensationEnergy();
				} catch (Exception ex) {
					compEng = 0f;
				}
				data.put("e_self_cons", compEng);
				try {
					gridCon = dayData.getGridConsEnergy();
				} catch (Exception ex) {
					gridCon = 0f;
				}
				data.put("e_grid_cons", gridCon);
				try {
					gridInj = dayData.getGridInjEnergy();
				} catch (Exception ex) {
					gridInj = 0f;
				}
				data.put("e_grid_feedin", gridInj);
				try {
					invCon = dayData.getInvConsEnergy();
				} catch (Exception ex) {
					invCon = 0f;
				}
				data.put("e_inv_cons", invCon);
				try {
					invInj = dayData.getInvInjEnergy();
				} catch (Exception ex) {
					invInj = 0f;
				}
				data.put("e_inv_feedin", invInj);
				try {
					batAmp = dayData.getQAcc();
				} catch (Exception ex) {
					batAmp = 0f;
				}
				data.put("bat_ah", batAmp);

				map.put(dayData.getTime(), data);
				this.setNext();

				this.refresh();
			}
			wait(1);
		}
		this.setDataIndex(0);
		this.refresh();
		return map;

	}

	/**
	 * Get history hour record
	 *
	 * @param reqDate Date of the requested day
	 * @return
	 *         <p>
	 *         All recorded performance values in per hour of the requested day
	 *         sorted by hour in ascending order. The values are stored in arrays of
	 *         lenght 30 and 12. Arrays of length 30 represent 2 minute average
	 *         values, arrays of 12 represent 5 minute average values.
	 *         </p>
	 *         <p>
	 *         For example: Date -> 06:00:00
	 *         </p>
	 *         <p>
	 *         p_pv[0] = 06:00:00, p_pv[1] = 06:02:00 ... p_pv[length - 1] =
	 *         06:58:00
	 *         </p>
	 *         <p>
	 *         soc[0] = 06:00:00, soc[1] = 06:05:00 ... soc[length - 1] = 06:55:00
	 *         </p>
	 *
	 * @throws java.lang.InterruptedException
	 *
	 *
	 */
	public synchronized SortedMap<Date, SortedMap<String, float[]>> getHistoryHour(Date reqDate)
			throws InterruptedException {

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(reqDate);

		int reqDay = calendar.get(Calendar.DAY_OF_YEAR);

		SortedMap<Date, SortedMap<String, float[]>> map = new TreeMap<>();

		this.setRequiredData(History.HOUR);
		this.setDataIndex(0);
		this.refresh();
		while (this.hasNext()) {

			if (this.dataReady()) {
				HourDataItem hourData = (HourDataItem) this.getData();
				Date hourDate = hourData.getTime();
				calendar.setTime(hourDate);

				if (calendar.get(Calendar.DAY_OF_YEAR) == reqDay) {
					float[] p_pv = new float[30];
					float[] p_cons = new float[30];
					float[] p_grid = new float[30];
					float[] p_ul1 = new float[12];
					float[] p_ul2 = new float[12];
					float[] p_ul3 = new float[12];
					float[] u_pv1 = new float[12];
					float[] u_pv2 = new float[12];
					float[] u_bat = new float[12];
					float[] soc = new float[12];
					SortedMap<String, float[]> data = new TreeMap<>();
					try {
						hourData.getPvPower(p_pv, 0);
					} catch (Exception ex) {
					}
					try {
						hourData.getHousePower(p_cons, 0);
					} catch (Exception ex) {
					}
					try {
						hourData.getGridPower(p_grid, 0);
					} catch (Exception ex) {
					}
					try {
						hourData.getUL1(p_ul1, 0);
					} catch (Exception ex) {
					}
					try {
						hourData.getUL2(p_ul2, 0);
					} catch (Exception ex) {
					}
					try {
						hourData.getUL3(p_ul3, 0);
					} catch (Exception ex) {
					}
					try {
						hourData.getUPV1(u_pv1, 0);
					} catch (Exception ex) {
					}
					try {
						hourData.getUPV2(u_pv2, 0);
					} catch (Exception ex) {
					}
					try {
						hourData.getUBat(u_bat, 0);
					} catch (Exception ex) {
					}

					try {
						hourData.getSOC(soc, 0);
					} catch (Exception ex) {
					}

					data.put("p_pv", p_pv);
					data.put("p_cons", p_cons);
					data.put("p_grid", p_grid);
					data.put("u_l1", p_ul1);
					data.put("u_l2", p_ul2);
					data.put("u_l3", p_ul3);
					data.put("u_pv1", u_pv1);
					data.put("u_pv2", u_pv2);
					data.put("u_bat", u_bat);
					data.put("soc", soc);

					map.put(hourDate, data);
				}

				this.setNext();

				this.refresh();
			}
			wait(1);
		}
		this.setDataIndex(0);
		this.refresh();
		return map;

	}

}
//CHECKSTYLE:ON
