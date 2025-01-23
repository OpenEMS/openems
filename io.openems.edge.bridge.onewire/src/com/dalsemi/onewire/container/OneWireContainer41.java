// CHECKSTYLE:OFF
/*---------------------------------------------------------------------------
 * Copyright (C) 2002-2009 Maxim Integrated Products, All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY,  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL MAXIM INTEGRATED PRODUCTS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Maxim Integrated Products
 * shall not be used except as stated in the Maxim Integrated Products
 * Branding Policy.
 *---------------------------------------------------------------------------
 */

package com.dalsemi.onewire.container;

// imports
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Vector;

import com.dalsemi.onewire.OneWireAccessProvider;
import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.adapter.OneWireIOException;
import com.dalsemi.onewire.debug.Debug;
import com.dalsemi.onewire.utils.CRC16;
import com.dalsemi.onewire.utils.Convert;

/**
 * <P>
 * 1-Wire&reg; container for a Temperature and Humidity/A-D Logging iButton,
 * DS1922. This container encapsulates the functionality of the 1-Wire family
 * type <B>22</B> (hex).
 * </P>
 *
 * <H3>Features</H3>
 * <UL>
 * <LI>Logs up to 8192 consecutive temperature/humidity/A-D measurements in
 * nonvolatile, read-only memory
 * <LI>Real-Time clock
 * <LI>Programmable high and low temperature alarms
 * <LI>Programmable high and low humidity/A-D alarms
 * <LI>Automatically 'wakes up' and logs temperature at user-programmable
 * intervals
 * <LI>4096 bits of general-purpose read/write nonvolatile memory
 * <LI>256-bit scratchpad ensures integrity of data transfer
 * <LI>On-chip 16-bit CRC generator to verify read operations
 * </UL>
 *
 * <H3>Memory</H3>
 *
 * <P>
 * The memory can be accessed through the objects that are returned from the
 * {@link #getMemoryBanks() getMemoryBanks} method.
 * </P>
 *
 * The following is a list of the MemoryBank instances that are returned:
 *
 * <UL>
 * <LI><B> Scratchpad with CRC and Password support </B>
 * <UL>
 * <LI><I> Implements </I> {@link com.dalsemi.onewire.container.MemoryBank
 * MemoryBank}, {@link com.dalsemi.onewire.container.PagedMemoryBank
 * PagedMemoryBank}
 * <LI><I> Size </I> 32 starting at physical address 0
 * <LI><I> Features</I> Read/Write not-general-purpose volatile
 * <LI><I> Pages</I> 1 page of length 32 bytes
 * <LI><I> Page Features </I> page-device-CRC
 * <li><i> Extra information for each page</i> Target address, offset, length 3
 * <LI><i> Supports Copy Scratchpad With Password command </I>
 * </UL>
 * <LI><B> Main Memory </B>
 * <UL>
 * <LI><I> Implements </I> {@link com.dalsemi.onewire.container.MemoryBank
 * MemoryBank}, {@link com.dalsemi.onewire.container.PagedMemoryBank
 * PagedMemoryBank}
 * <LI><I> Size </I> 512 starting at physical address 0
 * <LI><I> Features</I> Read/Write general-purpose non-volatile
 * <LI><I> Pages</I> 16 pages of length 32 bytes giving 29 bytes Packet data
 * payload
 * <LI><I> Page Features </I> page-device-CRC
 * <LI><I> Read-Only and Read/Write password </I> if enabled, passwords are
 * required for reading from and writing to the device.
 * </UL>
 * <LI><B> Register control </B>
 * <UL>
 * <LI><I> Implements </I> {@link com.dalsemi.onewire.container.MemoryBank
 * MemoryBank}, {@link com.dalsemi.onewire.container.PagedMemoryBank
 * PagedMemoryBank}
 * <LI><I> Size </I> 64 starting at physical address 512
 * <LI><I> Features</I> Read/Write not-general-purpose non-volatile
 * <LI><I> Pages</I> 2 pages of length 32 bytes
 * <LI><I> Page Features </I> page-device-CRC
 * <LI><I> Read-Only and Read/Write password </I> if enabled, passwords are
 * required for reading from and writing to the device.
 * </UL>
 * <LI><B> Temperature/Humidity/A-D log </B>
 * <UL>
 * <LI><I> Implements </I> {@link com.dalsemi.onewire.container.MemoryBank
 * MemoryBank}, {@link com.dalsemi.onewire.container.PagedMemoryBank
 * PagedMemoryBank}
 * <LI><I> Size </I> 8192 starting at physical address 4096
 * <LI><I> Features</I> Read-only not-general-purpose non-volatile
 * <LI><I> Pages</I> 256 pages of length 32 bytes
 * <LI><I> Page Features </I> page-device-CRC
 * <LI><I> Read-Only and Read/Write password </I> if enabled, passwords are
 * required for reading from and writing to the device.
 * </UL>
 * </UL>
 *
 * <H3>Usage</H3>
 *
 * <p>
 * The code below starts a mission with the following characteristics:
 * <ul>
 * <li>Rollover flag enabled.</li>
 * <li>Sets both channels (temperature and humidity) to low resolution</li>
 * <li>High temperature alarm of 28.0@htmlonly &#176C @endhtmlonly and a low
 * temperature alarm of 23.0@htmlonly &#176C @endhtmlonly.</li>
 * <li>High humidity alarm of 70%RH and a low temperature alarm of 20%RH.</li>
 * <li>Sets the Real-Time Clock to the host system's clock.</li>
 * <li>The mission will start in 2 minutes.</li>
 * <li>A sample rate of 1.5 minutes.</li>
 * </ul>
 * </p>
 *
 * <pre>
 * <code>
 *       // "ID" is a byte array of size 8 with an address of a part we
 *       // have already found with family code 22 hex
 *       // "access" is a DSPortAdapter
 *       OneWireContainer41 ds1922 = (OneWireContainer41)access.getDeviceContainer(ID);
 *       ds1922.setupContainer(access,ID);
 *       //  stop the currently running mission, if there is one
 *       ds1922.stopMission();
 *       //  clear the previous mission results
 *       ds1922.clearMemory();
 *       //  set the high temperature alarm to 28 C
 *       ds1922.setMissionAlarm(ds1922.TEMPERATURE_CHANNEL, ds1922.ALARM_HIGH, 28);
 *       ds1922.setMissionAlarmEnable(ds1922.TEMPERATURE_CHANNEL,
 *          ds1922.ALARM_HIGH, true);
 *       //  set the low temperature alarm to 23 C
 *       ds1922.setMissionAlarm(ds1922.TEMPERATURE_CHANNEL, ds1922.ALARM_LOW, 23);
 *       ds1922.setMissionAlarmEnable(ds1922.TEMPERATURE_CHANNEL,
 *          ds1922.ALARM_LOW, true);
 *       //  set the high humidity alarm to 70%RH
 *       ds1922.setMissionAlarm(ds1922.DATA_CHANNEL, ds1922.ALARM_HIGH, 70);
 *       ds1922.setMissionAlarmEnable(ds1922.DATA_CHANNEL,
 *          ds1922.ALARM_HIGH, true);
 *       //  set the low humidity alarm to 20%RH
 *       ds1922.setMissionAlarm(ds1922.DATA_CHANNEL, ds1922.ALARM_LOW, 20);
 *       ds1922.setMissionAlarmEnable(ds1922.DATA_CHANNEL,
 *          ds1922.ALARM_LOW, true);
 *       // set both channels to low resolution.
 *       ds1922.setMissionResolution(ds1922.TEMPERATURE_CHANNEL,
 *          ds1922.getMissionResolutions()[0]);
 *       ds1922.setMissionResolution(ds1922.DATA_CHANNEL,
 *          ds1922.getMissionResolutions()[0]);
 *       // enable both channels
 *       boolean[] enableChannel = new boolean[ds1922.getNumberMissionChannels()];
 *       enableChannel[ds1922.TEMPERATURE_CHANNEL] = true;
 *       enableChannel[ds1922.DATA_CHANNEL] = true;
 *       //  now start the mission with a sample rate of 1 minute
 *       ds1922.startNewMission(90, 2, true, true, enableChannel);
 * </code>
 * </pre>
 * <p>
 * The following code processes the mission log:
 * </p>
 * <code><pre>
 *       System.out.println("Temperature Readings");
 *       if(ds1922.getMissionChannelEnable(owc.TEMPERATURE_CHANNEL))
 *       {
 *          int dataCount =
 *             ds1922.getMissionSampleCount(ds1922.TEMPERATURE_CHANNEL);
 *          System.out.println("SampleCount = " + dataCount);
 *          for(int i=0; i&lt;dataCount; i++)
 *          {
 *             System.out.println(
 *                ds1922.getMissionSample(ds1922.TEMPERATURE_CHANNEL, i));
 *          }
 *       }
 *       System.out.println("Humidity Readings");
 *       if(ds1922.getMissionChannelEnable(owc.DATA_CHANNEL))
 *       {
 *          int dataCount =
 *             ds1922.getMissionSampleCount(ds1922.DATA_CHANNEL);
 *          System.out.println("SampleCount = " + dataCount);
 *          for(int i=0; i&lt;dataCount; i++)
 *          {
 *             System.out.println(
 *                ds1922.getMissionSample(ds1922.DATA_CHANNEL, i));
 *          }
 *       }
 * </pre></code>
 *
 * <p>
 * Also see the usage examples in the
 * {@link com.dalsemi.onewire.container.TemperatureContainer
 * TemperatureContainer} and {@link com.dalsemi.onewire.container.ClockContainer
 * ClockContainer} and {@link com.dalsemi.onewire.container.ADContainer
 * ADContainer} interfaces.
 * </p>
 *
 * For examples regarding memory operations,
 * <uL>
 * <li>See the usage example in
 * {@link com.dalsemi.onewire.container.OneWireContainer OneWireContainer} to
 * enumerate the MemoryBanks.
 * <li>See the usage examples in {@link com.dalsemi.onewire.container.MemoryBank
 * MemoryBank} and {@link com.dalsemi.onewire.container.PagedMemoryBank
 * PagedMemoryBank} for bank specific operations.
 * </uL>
 *
 * <H3>DataSheet</H3>
 * <P>
 * DataSheet link is unavailable at time of publication. Please visit the
 * website and search for DS1922 or DS2422 to find the current datasheet.
 * <DL>
 * <DD><A HREF="http://www.maxim-ic.com/">Maxim Website</A>
 * </DL>
 *
 * @see com.dalsemi.onewire.container.OneWireSensor
 * @see com.dalsemi.onewire.container.SwitchContainer
 * @see com.dalsemi.onewire.container.TemperatureContainer
 * @see com.dalsemi.onewire.container.ADContainer
 * @see com.dalsemi.onewire.container.MissionContainer
 * @see com.dalsemi.onewire.container.PasswordContainer
 *
 * @version 1.02, 30 November 2010
 * @author Maxim Integrated Products
 *
 */
@SuppressWarnings({ "unused" })
public class OneWireContainer41 extends OneWireContainer implements PasswordContainer, MissionContainer, ClockContainer,
		TemperatureContainer, ADContainer, HumidityContainer {
	// enables/disables debugging
	private static final boolean DEBUG = false;

	// when reading a page, the memory bank may throw a crc exception if the device
	// is sampling or starts sampling during the read. This value sets how many
	// times the device retries before passing the exception on to the application.
	private static final int MAX_READ_RETRY_CNT = 10;

	// the length of the Read-Only and Read/Write password registers
	private static final int PASSWORD_LENGTH = 8;

	// indicates whether or not the device configuration has been read
	// and all the ranges for the part have been set.
	private boolean isContainerVariablesSet = false;

	// memory bank for scratchpad
	private MemoryBankScratchCRCPW scratch = null;
	// memory bank for general-purpose user data
	private MemoryBankNVCRCPW userDataMemory = null;
	// memory bank for control register
	private MemoryBankNVCRCPW register = null;
	// memory bank for mission log
	private MemoryBankNVCRCPW log = null;

	// Maxim/Maxim Integrated Products Part number
	private String partNumber = null;

	// Device Configuration Byte
	private byte deviceConfigByte = (byte) 0xFF;

	// Temperature range low temperaturein degrees Celsius
	private double temperatureRangeLow = -40.0;

	// Temperature range width in degrees Celsius
	private double temperatureRangeWidth = 125.0;

	// A-D Reference voltage
	private double adReferenceVoltage = 5.02d;
	// Number of valid bits in A-D Result
	private int adDeviceBits = 10;
	// Force mission results to return value as A-D, not humidity
	private boolean adForceResults = false;

	// should we update the Real time clock?
	private boolean updatertc = false;

	// should we check the speed
	private boolean doSpeedEnable = true;

	/** The current password for readingfrom this device. */
	private final byte[] readPassword = new byte[8];
	private boolean readPasswordSet = false;
	private boolean readOnlyPasswordEnabled = false;

	/** The current password for reading/writing from/to this device. */
	private final byte[] readWritePassword = new byte[8];
	private boolean readWritePasswordSet = false;
	private boolean readWritePasswordEnabled = false;

	/** indicates whether or not the results of a mission are successfully loaded */
	private boolean isMissionLoaded = false;
	/**
	 * holds the missionRegister, which details the status of the current mission
	 */
	private byte[] missionRegister = null;
	/** The mission logs */
	private byte[] dataLog = null, temperatureLog = null;
	/** Number of bytes used to store temperature values (0, 1, or 2) */
	private int temperatureBytes = 0;
	/** Number of bytes used to store data valuas (0, 1, or 2) */
	private int dataBytes = 0;
	/** indicates whether or not the log has rolled over */
	private boolean rolledOver = false;
	/** start time offset for the first sample, if rollover occurred */
	private int timeOffset = 0;
	/** the time (unix time) when mission started */
	private long missionTimeStamp = -1;
	/** The rate at which samples are taken, and the number of samples */
	private int sampleRate = -1, sampleCount = -1;
	/** total number of samples, including rollover */
	private int sampleCountTotal;

	// indicates whether or not to use calibration for the humidity values
	private boolean useHumdCalibrationRegisters = false;
	// reference humidities that the calibration was calculated over
	private double Href2 = 60, Href3 = 90;
	// the average value for each reference point
	private double Hread1 = 0, Hread2 = 0, Hread3 = 0;
	// the average error for each reference point
	private double Herror1 = 0, Herror2 = 0, Herror3 = 0;
	// the coefficients for calibration
	private double humdCoeffA, humdCoeffB, humdCoeffC;

	// indicates whether or not to use calibration for the temperature values
	private boolean useTempCalibrationRegisters = false;
	private double Tref2 = 0;

	private double Tref3 = 0;
	// the average value for each reference point
	private double Tread1 = 0, Tread2 = 0, Tread3 = 0;
	// the average error for each reference point
	private double Terror1 = 0, Terror2 = 0, Terror3 = 0;
	// the coefficients for calibration of temperature
	private double tempCoeffA, tempCoeffB, tempCoeffC;

	// indicates whether or not to temperature compensate the humidity values
	private boolean useTemperatureCompensation = false;
	// indicates whether or not to use the temperature log for compensation
	private boolean overrideTemperatureLog = false;
	// default temperature in case of no log or override log
	private double defaultTempCompensationValue = 25;

	// indicates whether or not this is a DS1923
	private boolean hasHumiditySensor = false;

	// temperature is 8-bit or 11-bit
	private static final double temperatureResolutions[] = { .5d, .0625d };
	// data is 10-bit or 16-bit
	private static final double dataResolutions[] = { .5d, 0.001953125 };
	private static final double humidityResolutions[] = { .6d, .04d };

	private String descriptionString = DESCRIPTION_UNKNOWN;

	// first year that calendar starts counting years from
	private static final int FIRST_YEAR_EVER = 2000;

	// used to 'enable' passwords
	private static final byte ENABLE_BYTE = (byte) 0xAA;
	// used to 'disable' passwords
	private static final byte DISABLE_BYTE = 0x00;

// *****************************************************************************
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// 1-Wire Commands
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// *****************************************************************************

	/** Refers to the Temperature Channel for this device */
	public static final int TEMPERATURE_CHANNEL = 0;
	/** Refers to the Humidity/A-D Channel for this device */
	public static final int DATA_CHANNEL = 1;

	/** 1-Wire command for Write Scratchpad */
	public static final byte WRITE_SCRATCHPAD_COMMAND = (byte) 0x0F;
	/** 1-Wire command for Read Scratchpad */
	public static final byte READ_SCRATCHPAD_COMMAND = (byte) 0xAA;
	/** 1-Wire command for Copy Scratchpad With Password */
	public static final byte COPY_SCRATCHPAD_PW_COMMAND = (byte) 0x99;
	/** 1-Wire command for Read Memory CRC With Password */
	public static final byte READ_MEMORY_CRC_PW_COMMAND = (byte) 0x69;
	/** 1-Wire command for Clear Memory With Password */
	public static final byte CLEAR_MEMORY_PW_COMMAND = (byte) 0x96;
	/** 1-Wire command for Start Mission With Password */
	public static final byte START_MISSION_PW_COMMAND = (byte) 0xCC;
	/** 1-Wire command for Stop Mission With Password */
	public static final byte STOP_MISSION_PW_COMMAND = (byte) 0x33;
	/** 1-Wire command for Forced Conversion */
	public static final byte FORCED_CONVERSION = (byte) 0x55;

// *****************************************************************************
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// Register addresses and control bits
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// *****************************************************************************

	/** Address of the Real-time Clock Time value */
	public static final int RTC_TIME = 0x200;
	/** Address of the Real-time Clock Date value */
	public static final int RTC_DATE = 0x203;

	/** Address of the Sample Rate Register */
	public static final int SAMPLE_RATE = 0x206;// 2 bytes, LSB first, MSB no greater than 0x3F

	/** Address of the Temperature Low Alarm Register */
	public static final int TEMPERATURE_LOW_ALARM_THRESHOLD = 0x208;
	/** Address of the Temperature High Alarm Register */
	public static final int TEMPERATURE_HIGH_ALARM_THRESHOLD = 0x209;

	/** Address of the Data Low Alarm Register */
	public static final int DATA_LOW_ALARM_THRESHOLD = 0x20A;
	/** Address of the Data High Alarm Register */
	public static final int DATA_HIGH_ALARM_THRESHOLD = 0x20B;

	/** Address of the last temperature conversion's LSB */
	public static final int LAST_TEMPERATURE_CONVERSION_LSB = 0x20C;
	/** Address of the last temperature conversion's MSB */
	public static final int LAST_TEMPERATURE_CONVERSION_MSB = 0x20D;

	/** Address of the last data conversion's LSB */
	public static final int LAST_DATA_CONVERSION_LSB = 0x20E;
	/** Address of the last data conversion's MSB */
	public static final int LAST_DATA_CONVERSION_MSB = 0x20F;

	/** Address of Temperature Control Register */
	public static final int TEMPERATURE_CONTROL_REGISTER = 0x210;
	/** Temperature Control Register Bit: Enable Data Low Alarm */
	public static final byte TCR_BIT_ENABLE_TEMPERATURE_LOW_ALARM = (byte) 0x01;
	/** Temperature Control Register Bit: Enable Data Low Alarm */
	public static final byte TCR_BIT_ENABLE_TEMPERATURE_HIGH_ALARM = (byte) 0x02;

	/** Address of Data Control Register */
	public static final int DATA_CONTROL_REGISTER = 0x211;
	/** Data Control Register Bit: Enable Data Low Alarm */
	public static final byte DCR_BIT_ENABLE_DATA_LOW_ALARM = (byte) 0x01;
	/** Data Control Register Bit: Enable Data High Alarm */
	public static final byte DCR_BIT_ENABLE_DATA_HIGH_ALARM = (byte) 0x02;

	/** Address of Real-Time Clock Control Register */
	public static final int RTC_CONTROL_REGISTER = 0x212;
	/** Real-Time Clock Control Register Bit: Enable Oscillator */
	public static final byte RCR_BIT_ENABLE_OSCILLATOR = (byte) 0x01;
	/** Real-Time Clock Control Register Bit: Enable High Speed Sample */
	public static final byte RCR_BIT_ENABLE_HIGH_SPEED_SAMPLE = (byte) 0x02;

	/** Address of Mission Control Register */
	public static final int MISSION_CONTROL_REGISTER = (byte) 0x213;
	/** Mission Control Register Bit: Enable Temperature Logging */
	public static final byte MCR_BIT_ENABLE_TEMPERATURE_LOGGING = (byte) 0x01;
	/** Mission Control Register Bit: Enable Data Logging */
	public static final byte MCR_BIT_ENABLE_DATA_LOGGING = (byte) 0x02;
	/** Mission Control Register Bit: Set Temperature Resolution */
	public static final byte MCR_BIT_TEMPERATURE_RESOLUTION = (byte) 0x04;
	/** Mission Control Register Bit: Set Data Resolution */
	public static final byte MCR_BIT_DATA_RESOLUTION = (byte) 0x08;
	/** Mission Control Register Bit: Enable Rollover */
	public static final byte MCR_BIT_ENABLE_ROLLOVER = (byte) 0x10;
	/** Mission Control Register Bit: Start Mission on Temperature Alarm */
	public static final byte MCR_BIT_START_MISSION_ON_TEMPERATURE_ALARM = (byte) 0x20;

	/** Address of Alarm Status Register */
	public static final int ALARM_STATUS_REGISTER = 0x214;
	/** Alarm Status Register Bit: Temperature Low Alarm */
	public static final byte ASR_BIT_TEMPERATURE_LOW_ALARM = (byte) 0x01;
	/** Alarm Status Register Bit: Temperature High Alarm */
	public static final byte ASR_BIT_TEMPERATURE_HIGH_ALARM = (byte) 0x02;
	/** Alarm Status Register Bit: Data Low Alarm */
	public static final byte ASR_BIT_DATA_LOW_ALARM = (byte) 0x04;
	/** Alarm Status Register Bit: Data High Alarm */
	public static final byte ASR_BIT_DATA_HIGH_ALARM = (byte) 0x08;
	/** Alarm Status Register Bit: Battery On Reset */
	public static final byte ASR_BIT_BATTERY_ON_RESET = (byte) 0x80;

	/** Address of General Status Register */
	public static final int GENERAL_STATUS_REGISTER = 0x215;
	/** General Status Register Bit: Sample In Progress */
	public static final byte GSR_BIT_SAMPLE_IN_PROGRESS = (byte) 0x01;
	/** General Status Register Bit: Mission In Progress */
	public static final byte GSR_BIT_MISSION_IN_PROGRESS = (byte) 0x02;
	/** General Status Register Bit: Conversion In Progress */
	public static final byte GSR_BIT_CONVERSION_IN_PROGRESS = (byte) 0x04;
	/** General Status Register Bit: Memory Cleared */
	public static final byte GSR_BIT_MEMORY_CLEARED = (byte) 0x08;
	/** General Status Register Bit: Waiting for Temperature Alarm */
	public static final byte GSR_BIT_WAITING_FOR_TEMPERATURE_ALARM = (byte) 0x10;
	/** General Status Register Bit: Forced Conversion In Progress */
	public static final byte GSR_BIT_FORCED_CONVERSION_IN_PROGRESS = (byte) 0x20;

	/** Address of the Mission Start Delay */
	public static final int MISSION_START_DELAY = 0x216; // 3 bytes, LSB first

	/** Address of the Mission Timestamp Time value */
	public static final int MISSION_TIMESTAMP_TIME = 0x219;
	/** Address of the Mission Timestamp Date value */
	public static final int MISSION_TIMESTAMP_DATE = 0x21C;

	/** Address of Device Configuration Register */
	public static final int DEVICE_CONFIGURATION_BYTE = 0x226;
	/** Value of Device Configuration Register for DS1922S */
	public static final byte DCB_DS2422 = 0x00;
	/** Value of Device Configuration Register for DS1923 */
	public static final byte DCB_DS1923 = 0x20;
	/** Value of Device Configuration Register for DS1922L */
	public static final byte DCB_DS1922L = 0x40;
	/** Value of Device Configuration Register for DS1922T */
	public static final byte DCB_DS1922T = 0x60;
	/** Value of Device Configuration Register for DS1922E */
	public static final byte DCB_DS1922E = (byte) 0x80;
	/** Value of Device Configuration Register for DS1922S */
	public static final byte DCB_DS1922S = (byte) 0xA0;

	// 1 byte, alternating ones and zeroes indicates passwords are enabled
	/** Address of the Password Control Register. */
	public static final int PASSWORD_CONTROL_REGISTER = 0x227;

	// 8 bytes, write only, for setting the Read Access Password
	/** Address of Read Access Password. */
	public static final int READ_ACCESS_PASSWORD = 0x228;

	// 8 bytes, write only, for setting the Read Access Password
	/** Address of the Read Write Access Password. */
	public static final int READ_WRITE_ACCESS_PASSWORD = 0x230;

	// 3 bytes, LSB first
	/** Address of the Mission Sample Count */
	public static final int MISSION_SAMPLE_COUNT = 0x220;

	// 3 bytes, LSB first
	/** Address of the Device Sample Count */
	public static final int DEVICE_SAMPLE_COUNT = 0x223;

	/** maximum size of the mission log */
	public static final int MISSION_LOG_SIZE = 8192;

	/**
	 * mission log size for odd combination of resolutions (i.e. 8-bit temperature &
	 * 16-bit data or 16-bit temperature & 8-bit data
	 */
	public static final int ODD_MISSION_LOG_SIZE = 7680;

	private static final String PART_NUMBER_DS1923 = "DS1923";
	private static final String PART_NUMBER_DS2422 = "DS2422";
	private static final String PART_NUMBER_DS1922L = "DS1922L";
	private static final String PART_NUMBER_DS1922T = "DS1922T";
	private static final String PART_NUMBER_DS1922E = "DS1922E";
	private static final String PART_NUMBER_DS1922S = "DS1922S";
	private static final String PART_NUMBER_UNKNOWN = "DS1922/DS1923/DS2422";

	private static final String DESCRIPTION_DS1923 = """
			The DS1923 Temperature/Humidity Logger iButton is a rugged, \
			self-sufficient system that measures temperature and/or humidity \
			and records the result in a protected memory section. The recording \
			is done at a user-defined rate. A total of 8192 8-bit readings or \
			4096 16-bit readings taken at equidistant intervals ranging from 1 \
			second to 273 hours can be stored. In addition to this, there are 512 \
			bytes of SRAM for storing application specific information and 64 \
			bytes for calibration data. A mission to collect data can be \
			programmed to begin immediately, or after a user-defined delay or \
			after a temperature alarm. Access to the memory and control functions \
			can be password-protected.""";

	private static final String DESCRIPTION_DS1922 = """
			The DS1922L/T/E/S Temperature Logger iButtons are rugged, \
			self-sufficient systems that measure temperature and record the \
			result in a protected memory section. The recording is done at a \
			user-defined rate. A total of 8192 8-bit readings or 4096 16-bit \
			readings taken at equidistant intervals ranging from 1s to 273hrs \
			can be stored. In addition to this, there are 512 bytes of SRAM for \
			storing application-specific information and 64 bytes for calibration \
			data. A mission to collect data can be programmed to begin \
			immediately, or after a user-defined delay or after a temperature \
			alarm. Access to the memory and control functions can be password \
			protected.""";

	private static final String DESCRIPTION_DS2422 = """
			The DS2422 temperature/datalogger combines the core functions of a \
			fully featured datalogger in a single chip. It includes a temperature \
			sensor, realtime clock (RTC), memory, 1-Wire(R) interface, and serial \
			interface for an analog-to-digital converter (ADC) as well as control \
			circuitry for a charge pump. The ADC and the charge pump are \
			peripherals that can be added to build application-specific \
			dataloggers. The MAX1086 is an example of a compatible serial ADC. \
			Without external ADC, the DS2422 functions as a temperature logger \
			only. The DS2422 measures the temperature and/or reads the ADC at a \
			user-defined rate. A total of 8192 8-bit readings or 4096 16-bit \
			readings taken at equidistant intervals ranging from 1s to 273hrs can \
			be stored.""";

	private static final String DESCRIPTION_UNKNOWN = """
			Rugged, self-sufficient 1-Wire device that, once setup for \
			a mission, will measure temperature and A-to-D/Humidity, with the \
			result recorded in a protected memory section. It stores up \
			to 8192 1-byte measurements, which can be filled with 1- or \
			2-byte temperature readings and 1- or 2-byte A-to-D/Humidity readings \
			taken at a user-specified rate.""";

// *****************************************************************************
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// Constructors and Initializers
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// *****************************************************************************

	/**
	 * Creates a new <code>OneWireContainer</code> for communication with a DS1922.
	 * Note that the method <code>setupContainer(DSPortAdapter,byte[])</code> must
	 * be called to set the correct <code>DSPortAdapter</code> device address.
	 *
	 * @see com.dalsemi.onewire.container.OneWireContainer#setupContainer(com.dalsemi.onewire.adapter.DSPortAdapter,byte[])
	 *      setupContainer(DSPortAdapter,byte[])
	 * @see #OneWireContainer41(com.dalsemi.onewire.adapter.DSPortAdapter,byte[])
	 *      OneWireContainer41(DSPortAdapter,byte[])
	 * @see #OneWireContainer41(com.dalsemi.onewire.adapter.DSPortAdapter,long)
	 *      OneWireContainer41(DSPortAdapter,long)
	 * @see #OneWireContainer41(com.dalsemi.onewire.adapter.DSPortAdapter,java.lang.String)
	 *      OneWireContainer41(DSPortAdapter,String)
	 */
	public OneWireContainer41() {
		// initialize the memory banks
		this.initMem();
		this.setContainerVariables(null);
	}

	/**
	 * Creates a new <code>OneWireContainer</code> for communication with a DS1922.
	 *
	 * @param sourceAdapter adapter object required to communicate with this iButton
	 * @param newAddress    address of this DS1922
	 *
	 * @see #OneWireContainer41()
	 * @see #OneWireContainer41(com.dalsemi.onewire.adapter.DSPortAdapter,long)
	 *      OneWireContainer41(DSPortAdapter,long)
	 * @see #OneWireContainer41(com.dalsemi.onewire.adapter.DSPortAdapter,java.lang.String)
	 *      OneWireContainer41(DSPortAdapter,String)
	 */
	public OneWireContainer41(DSPortAdapter sourceAdapter, byte[] newAddress) {
		super(sourceAdapter, newAddress);

		// initialize the memory banks
		this.initMem();
		this.setContainerVariables(null);
	}

	/**
	 * Creates a new <code>OneWireContainer</code> for communication with a DS1922.
	 *
	 * @param sourceAdapter adapter object required to communicate with this iButton
	 * @param newAddress    address of this DS1922
	 *
	 * @see #OneWireContainer41()
	 * @see #OneWireContainer41(com.dalsemi.onewire.adapter.DSPortAdapter,byte[])
	 *      OneWireContainer41(DSPortAdapter,byte[])
	 * @see #OneWireContainer41(com.dalsemi.onewire.adapter.DSPortAdapter,java.lang.String)
	 *      OneWireContainer41(DSPortAdapter,String)
	 */
	public OneWireContainer41(DSPortAdapter sourceAdapter, long newAddress) {
		super(sourceAdapter, newAddress);

		// initialize the memory banks
		this.initMem();
		this.setContainerVariables(null);
	}

	/**
	 * Creates a new <code>OneWireContainer</code> for communication with a DS1922.
	 *
	 * @param sourceAdapter adapter object required to communicate with this iButton
	 * @param newAddress    address of this DS1922
	 *
	 * @see #OneWireContainer41()
	 * @see #OneWireContainer41(com.dalsemi.onewire.adapter.DSPortAdapter,long)
	 *      OneWireContainer41(DSPortAdapter,long)
	 * @see #OneWireContainer41(com.dalsemi.onewire.adapter.DSPortAdapter,java.lang.String)
	 *      OneWireContainer41(DSPortAdapter,String)
	 */
	public OneWireContainer41(DSPortAdapter sourceAdapter, String newAddress) {
		super(sourceAdapter, newAddress);

		// initialize the memory banks
		this.initMem();
		this.setContainerVariables(null);
	}

	/**
	 * Provides this container with the adapter object used to access this device
	 * and the address of the iButton or 1-Wire device.
	 *
	 * @param sourceAdapter adapter object required to communicate with this iButton
	 * @param newAddress    address of this 1-Wire device
	 * @see com.dalsemi.onewire.utils.Address
	 */
	@Override
	public void setupContainer(DSPortAdapter sourceAdapter, byte[] newAddress) {
		super.setupContainer(sourceAdapter, newAddress);

		// initialize the memory banks
		this.initMem();
		this.setContainerVariables(null);
	}

	/**
	 * Provides this container with the adapter object used to access this device
	 * and the address of the iButton or 1-Wire device.
	 *
	 * @param sourceAdapter adapter object required to communicate with this iButton
	 * @param newAddress    address of this 1-Wire device
	 * @see com.dalsemi.onewire.utils.Address
	 */
	@Override
	public void setupContainer(DSPortAdapter sourceAdapter, long newAddress) {
		super.setupContainer(sourceAdapter, newAddress);

		// initialize the memory banks
		this.initMem();
		this.setContainerVariables(null);
	}

	/**
	 * Provides this container with the adapter object used to access this device
	 * and the address of the iButton or 1-Wire device.
	 *
	 * @param sourceAdapter adapter object required to communicate with this iButton
	 * @param newAddress    address of this 1-Wire device
	 * @see com.dalsemi.onewire.utils.Address
	 */
	@Override
	public void setupContainer(DSPortAdapter sourceAdapter, String newAddress) {
		super.setupContainer(sourceAdapter, newAddress);

		// initialize the memory banks
		this.initMem();
		this.setContainerVariables(null);
	}

// *****************************************************************************
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// Sensor read/write
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// *****************************************************************************

	/**
	 * Retrieves the 1-Wire device sensor state. This state is returned as a byte
	 * array. Pass this byte array to the 'get' and 'set' methods. If the device
	 * state needs to be changed then call the 'writeDevice' to finalize the
	 * changes.
	 *
	 * @return 1-Wire device sensor state
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from a 1-Wire device. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 */
	@Override
	public byte[] readDevice() throws OneWireIOException, OneWireException {
		var buffer = new byte[96];

		var retryCnt = MAX_READ_RETRY_CNT;
		var page = 0;
		do {
			try {
				switch (page) {
				default:
					break;
				case 0:
					this.register.readPageCRC(0, false, buffer, 0);
					page++;
				case 1:
					this.register.readPageCRC(1, retryCnt == MAX_READ_RETRY_CNT, buffer, 32);
					page++;
				case 2:
					this.register.readPageCRC(2, retryCnt == MAX_READ_RETRY_CNT, buffer, 64);
					page++;
				}
				retryCnt = MAX_READ_RETRY_CNT;
			} catch (OneWireIOException owioe) {
				// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
				if (DEBUG) {
					Debug.debug("readDevice exc, retryCnt=" + retryCnt, owioe);
				}
				// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
				/*
				 * // this "workaround" is broken. Idea was based on suggestion // that
				 * scratchpad and data memory had different buses. So, // Should be possible to
				 * read scratchpad while data memory is // written by logger. Experiments show
				 * this isn't true. try { scratch.readPageCRC(0, false, buffer, 0); }
				 * catch(Exception e) { throw new
				 * OneWireIOException("Invalid CRC16 read from device, battery may be dead."); }
				 */
				if (--retryCnt == 0) {
					throw owioe;
				}
			} catch (OneWireException owe) {
				// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
				if (DEBUG) {
					Debug.debug("readDevice exc, retryCnt=" + retryCnt, owe);
				}
				// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
				if (--retryCnt == 0) {
					throw owe;
				}
			}
		} while (page < 3);

		if (!this.isContainerVariablesSet) {
			this.setContainerVariables(buffer);
		}

		return buffer;
	}

	/**
	 * Writes the 1-Wire device sensor state that have been changed by 'set'
	 * methods. Only the state registers that changed are updated. This is done by
	 * referencing a field information appended to the state data.
	 *
	 * @param state 1-Wire device sensor state
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from a 1-Wire device. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 */
	@Override
	public void writeDevice(byte[] state) throws OneWireIOException, OneWireException {
		var start = this.updatertc ? 0 : 6;

		this.register.write(start, state, start, 32 - start);

		synchronized (this) {
			this.updatertc = false;
		}
	}

	/**
	 * Reads a single byte from the DS1922. Note that the preferred manner of
	 * reading from the DS1922 Thermocron is through the <code>readDevice()</code>
	 * method or through the <code>MemoryBank</code> objects returned in the
	 * <code>getMemoryBanks()</code> method.
	 *
	 * @param memAddr the address to read from (in the range of 0x200-0x21F)
	 *
	 * @return the data byte read
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from a 1-Wire device. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 *
	 * @see #readDevice()
	 * @see #getMemoryBanks()
	 */
	public byte readByte(int memAddr) throws OneWireIOException, OneWireException {
		// break the address up into bytes
		var msbAddress = (byte) (memAddr >> 8 & 0x0ff);
		var lsbAddress = (byte) (memAddr & 0x0ff);

		/* check the validity of the address */
		if (msbAddress > 0x2F || msbAddress < 0) {
			throw new IllegalArgumentException("OneWireContainer41-Address for read out of range.");
		}

		var numBytesToEndOfPage = 32 - (lsbAddress & 0x1F);
		var buffer = new byte[11 + numBytesToEndOfPage + 2];

		if (this.doSpeedEnable) {
			this.doSpeed();
		}

		if (!this.adapter.select(this.address)) {
			throw new OneWireException("OneWireContainer41-Device not present.");
		}
		buffer[0] = READ_MEMORY_CRC_PW_COMMAND;
		buffer[1] = lsbAddress;
		buffer[2] = msbAddress;

		if (this.isContainerReadWritePasswordSet()) {
			this.getContainerReadWritePassword(buffer, 3);
		} else {
			this.getContainerReadOnlyPassword(buffer, 3);
		}

		for (var i = 11; i < buffer.length; i++) {
			buffer[i] = (byte) 0x0ff;
		}

		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		if (DEBUG) {
			Debug.debug("Send-> ", buffer);
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		this.adapter.dataBlock(buffer, 0, buffer.length);
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		if (DEBUG) {
			Debug.debug("Recv<- ", buffer);
			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		}

		// exclude password from CRC 16
		if (CRC16.compute(buffer, 11, buffer.length - 11, CRC16.compute(buffer, 0, 3, 0)) != 0x0000B001) {
			throw new OneWireIOException(
					"Invalid CRC16 read from device.  Password may be incorrect or a sample may be in progress.");
		}

		return buffer[11];
	}

	/**
	 * <p>
	 * Gets the status of the specified flag from the specified register. This
	 * method actually communicates with the DS1922. To improve performance if you
	 * intend to make multiple calls to this method, first call
	 * <code>readDevice()</code> and use the <code>getFlag(int, byte, byte[])</code>
	 * method instead.
	 * </p>
	 *
	 * <p>
	 * The DS1922 has several sets of flags.
	 * </p>
	 * <ul>
	 * <LI>Register: <CODE> TEMPERATURE_CONTROL_REGISTER </CODE><BR>
	 * Flags:
	 * <UL>
	 * <li><code> TCR_BIT_ENABLE_TEMPERATURE_LOW_ALARM  </code></li>
	 * <li><code> TCR_BIT_ENABLE_TEMPERATURE_HIGH_ALARM </code></li>
	 * </UL>
	 * </LI>
	 * <LI>Register: <CODE> DATA_CONTROL_REGISTER </CODE><BR>
	 * Flags:
	 * <UL>
	 * <li><code> DCR_BIT_ENABLE_DATA_LOW_ALARM  </code></li>
	 * <li><code> DCR_BIT_ENABLE_DATA_HIGH_ALARM </code></li>
	 * </UL>
	 * </LI>
	 * <LI>Register: <CODE> RTC_CONTROL_REGISTER </CODE><BR>
	 * Flags:
	 * <UL>
	 * <li><code> RCR_BIT_ENABLE_OSCILLATOR        </code></li>
	 * <li><code> RCR_BIT_ENABLE_HIGH_SPEED_SAMPLE </code></li>
	 * </UL>
	 * </LI>
	 * <LI>Register: <CODE> MISSION_CONTROL_REGISTER </CODE><BR>
	 * Flags:
	 * <UL>
	 * <li><code> MCR_BIT_ENABLE_TEMPERATURE_LOGGING           </code></li>
	 * <li><code> MCR_BIT_ENABLE_DATA_LOGGING                  </code></li>
	 * <li><code> MCR_BIT_TEMPERATURE_RESOLUTION               </code></li>
	 * <li><code> MCR_BIT_DATA_RESOLUTION                      </code></li>
	 * <li><code> MCR_BIT_ENABLE_ROLLOVER                      </code></li>
	 * <li><code> MCR_BIT_START_MISSION_UPON_TEMPERATURE_ALARM </code></li>
	 * </UL>
	 * </LI>
	 * <LI>Register: <CODE> ALARM_STATUS_REGISTER </CODE><BR>
	 * Flags:
	 * <UL>
	 * <li><code> ASR_BIT_TEMPERATURE_LOW_ALARM  </code></li>
	 * <li><code> ASR_BIT_TEMPERATURE_HIGH_ALARM </code></li>
	 * <li><code> ASR_BIT_DATA_LOW_ALARM         </code></li>
	 * <li><code> ASR_BIT_DATA_HIGH_ALARM        </code></li>
	 * <li><code> ASR_BIT_BATTERY_ON_RESET       </code></li>
	 * </UL>
	 * </LI>
	 * <LI>Register: <CODE> GENERAL_STATUS_REGISTER </CODE><BR>
	 * Flags:
	 * <UL>
	 * <li><code> GSR_BIT_SAMPLE_IN_PROGRESS            </code></li>
	 * <li><code> GSR_BIT_MISSION_IN_PROGRESS           </code></li>
	 * <li><code> GSR_BIT_MEMORY_CLEARED                </code></li>
	 * <li><code> GSR_BIT_WAITING_FOR_TEMPERATURE_ALARM </code></li>
	 * </UL>
	 * </LI>
	 * </ul>
	 *
	 * @param register address of register containing the flag (see above for
	 *                 available options)
	 * @param bitMask  the flag to read (see above for available options)
	 *
	 * @return the status of the flag, where <code>true</code> signifies a "1" and
	 *         <code>false</code> signifies a "0"
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from a 1-Wire device. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 *
	 * @see #getFlag(int,byte,byte[])
	 * @see #readDevice()
	 * @see #setFlag(int,byte,boolean)
	 */
	public boolean getFlag(int register, byte bitMask) throws OneWireIOException, OneWireException {
		var retryCnt = MAX_READ_RETRY_CNT;
		while (true) {
			try {
				return (this.readByte(register) & bitMask) != 0;
			} catch (OneWireException owe) {
				if (--retryCnt == 0) {
					throw owe;
				}
			}
		}
	}

	/**
	 * <p>
	 * Gets the status of the specified flag from the specified register. This
	 * method is the preferred manner of reading the control and status flags.
	 * </p>
	 *
	 * <p>
	 * For more information on valid values for the <code>bitMask</code> parameter,
	 * see the {@link #getFlag(int,byte) getFlag(int,byte)} method.
	 * </p>
	 *
	 * @param register address of register containing the flag (see
	 *                 {@link #getFlag(int,byte) getFlag(int,byte)} for available
	 *                 options)
	 * @param bitMask  the flag to read (see {@link #getFlag(int,byte)
	 *                 getFlag(int,byte)} for available options)
	 * @param state    current state of the device returned from
	 *                 <code>readDevice()</code>
	 *
	 * @return the status of the flag, where <code>true</code> signifies a "1" and
	 *         <code>false</code> signifies a "0"
	 *
	 * @see #getFlag(int,byte)
	 * @see #readDevice()
	 * @see #setFlag(int,byte,boolean,byte[])
	 */
	public boolean getFlag(int register, byte bitMask, byte[] state) {
		return (state[register & 0x3F] & bitMask) != 0;
	}

	/**
	 * <p>
	 * Sets the status of the specified flag in the specified register. If a mission
	 * is in progress a <code>OneWireIOException</code> will be thrown (one cannot
	 * write to the registers while a mission is commencing). This method actually
	 * communicates with the DS1922. To improve performance if you intend to make
	 * multiple calls to this method, first call <code>readDevice()</code> and use
	 * the <code>setFlag(int,byte,boolean,byte[])</code> method instead.
	 * </p>
	 *
	 * <p>
	 * For more information on valid values for the <code>bitMask</code> parameter,
	 * see the {@link #getFlag(int,byte) getFlag(int,byte)} method.
	 * </p>
	 *
	 * @param register  address of register containing the flag (see
	 *                  {@link #getFlag(int,byte) getFlag(int,byte)} for available
	 *                  options)
	 * @param bitMask   the flag to read (see {@link #getFlag(int,byte)
	 *                  getFlag(int,byte)} for available options)
	 * @param flagValue new value for the flag (<code>true</code> is logic "1")
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from a 1-Wire device. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'. In the case of
	 *                            the DS1922, this could also be due to a currently
	 *                            running mission.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 *
	 * @see #getFlag(int,byte)
	 * @see #getFlag(int,byte,byte[])
	 * @see #setFlag(int,byte,boolean,byte[])
	 * @see #readDevice()
	 */
	public void setFlag(int register, byte bitMask, boolean flagValue) throws OneWireIOException, OneWireException {
		var state = this.readDevice();

		this.setFlag(register, bitMask, flagValue, state);

		this.writeDevice(state);
	}

	/**
	 * <p>
	 * Sets the status of the specified flag in the specified register. If a mission
	 * is in progress a <code>OneWireIOException</code> will be thrown (one cannot
	 * write to the registers while a mission is commencing). This method is the
	 * preferred manner of setting the DS1922 status and control flags. The method
	 * <code>writeDevice()</code> must be called to finalize changes to the device.
	 * Note that multiple 'set' methods can be called before one call to
	 * <code>writeDevice()</code>.
	 * </p>
	 *
	 * <p>
	 * For more information on valid values for the <code>bitMask</code> parameter,
	 * see the {@link #getFlag(int,byte) getFlag(int,byte)} method.
	 * </p>
	 *
	 * @param register  address of register containing the flag (see
	 *                  {@link #getFlag(int,byte) getFlag(int,byte)} for available
	 *                  options)
	 * @param bitMask   the flag to read (see {@link #getFlag(int,byte)
	 *                  getFlag(int,byte)} for available options)
	 * @param flagValue new value for the flag (<code>true</code> is logic "1")
	 * @param state     current state of the device returned from
	 *                  <code>readDevice()</code>
	 *
	 * @see #getFlag(int,byte)
	 * @see #getFlag(int,byte,byte[])
	 * @see #setFlag(int,byte,boolean)
	 * @see #readDevice()
	 * @see #writeDevice(byte[])
	 */
	public void setFlag(int register, byte bitMask, boolean flagValue, byte[] state) {
		register = register & 0x3F;

		var flags = state[register];

		if (flagValue) {
			flags = (byte) (flags | bitMask);
		} else {
			flags = (byte) (flags & ~bitMask);
		}

		// write the regs back
		state[register] = flags;
	}

// *****************************************************************************
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// Container Functions
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// *****************************************************************************

	/**
	 * Gets an enumeration of memory bank instances that implement one or more of
	 * the following interfaces: {@link com.dalsemi.onewire.container.MemoryBank
	 * MemoryBank}, {@link com.dalsemi.onewire.container.PagedMemoryBank
	 * PagedMemoryBank}, and {@link com.dalsemi.onewire.container.OTPMemoryBank
	 * OTPMemoryBank}.
	 *
	 * @return <CODE>Enumeration</CODE> of memory banks
	 */
	@Override
	public Enumeration<MemoryBank> getMemoryBanks() {
		var v = new Vector<MemoryBank>(4);

		v.addElement(this.scratch);
		v.addElement(this.userDataMemory);
		v.addElement(this.register);
		v.addElement(this.log);

		return v.elements();
	}

	/**
	 * Returns instance of the memory bank representing this device's scratchpad.
	 *
	 * @return scratchpad memory bank
	 */
	public MemoryBankScratchCRCPW getScratchpadMemoryBank() {
		return this.scratch;
	}

	/**
	 * Returns instance of the memory bank representing this device's
	 * general-purpose user data memory.
	 *
	 * @return user data memory bank
	 */
	public MemoryBankNVCRCPW getUserDataMemoryBank() {
		return this.userDataMemory;
	}

	/**
	 * Returns instance of the memory bank representing this device's data log.
	 *
	 * @return data log memory bank
	 */
	public MemoryBankNVCRCPW getDataLogMemoryBank() {
		return this.log;
	}

	/**
	 * Returns instance of the memory bank representing this device's special
	 * function registers.
	 *
	 * @return register memory bank
	 */
	public MemoryBankNVCRCPW getRegisterMemoryBank() {
		return this.register;
	}

	/**
	 * Returns the maximum speed this iButton device can communicate at.
	 *
	 * @return maximum speed
	 * @see DSPortAdapter#setSpeed
	 */
	@Override
	public int getMaxSpeed() {
		return DSPortAdapter.SPEED_OVERDRIVE;
	}

	/**
	 * Gets the Maxim Integrated Products part number of the iButton or 1-Wire
	 * Device as a <code>java.lang.String</code>. For example "DS1992".
	 *
	 * @return iButton or 1-Wire device name
	 */
	@Override
	public String getName() {
		return this.partNumber;
	}

	/**
	 * Retrieves the alternate Maxim Integrated Products part numbers or names. A
	 * 'family' of MicroLAN devices may have more than one part number depending on
	 * packaging. There can also be nicknames such as "Crypto iButton".
	 *
	 * @return the alternate names for this iButton or 1-Wire device
	 */
	@Override
	public String getAlternateNames() {
		if (this.partNumber.equals("DS1923")) {
			return "Hygrochron";
		}
		return "Thermochron8k";
	}

	/**
	 * Gets a short description of the function of this iButton or 1-Wire Device
	 * type.
	 *
	 * @return device description
	 */
	@Override
	public String getDescription() {
		return this.descriptionString;
	}

	/**
	 * Returns the Device Configuration Byte, which specifies whether or not this
	 * device is a DS1922, DS1923, or DS2422.
	 *
	 * @return the Device Configuration Byte
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	public byte getDeviceConfigByte() throws OneWireIOException, OneWireException {
		if (this.deviceConfigByte == (byte) 0xFF) {
			var state = this.readDevice();
			if (this.deviceConfigByte == (byte) 0xFF) {
				this.deviceConfigByte = state[DEVICE_CONFIGURATION_BYTE & 0x3F];
			}
		}
		return this.deviceConfigByte;
	}

	/**
	 * Directs the container to avoid the calls to doSpeed() in methods that
	 * communicate with the DS1922/DS2422. To ensure that all parts can talk to the
	 * 1-Wire bus at their desired speed, each method contains a call to
	 * <code>doSpeed()</code>. However, this is an expensive operation. If a user
	 * manages the bus speed in an application, call this method with
	 * <code>doSpeedCheck</code> as <code>false</code>. The default behavior is to
	 * call <code>doSpeed()</code>.
	 *
	 * @param doSpeedCheck <code>true</code> for <code>doSpeed()</code> to be called
	 *                     before every 1-Wire bus access, <code>false</code> to
	 *                     skip this expensive call
	 *
	 * @see OneWireContainer#doSpeed()
	 */
	public synchronized void setSpeedCheck(boolean doSpeedCheck) {
		this.doSpeedEnable = doSpeedCheck;
	}

// *****************************************************************************
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// DS1922 Device Specific Functions
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// *****************************************************************************

	/**
	 * Stops the currently running mission.
	 *
	 */
	@Override
	public void stopMission() throws OneWireException, OneWireIOException {
		/* read a user specified amount of memory and verify its validity */
		if (this.doSpeedEnable) {
			this.doSpeed();
		}

		if (!this.adapter.select(this.address)) {
			throw new OneWireException("OneWireContainer41-Device not present.");
		}

		var buffer = new byte[10];
		buffer[0] = STOP_MISSION_PW_COMMAND;
		this.getContainerReadWritePassword(buffer, 1);
		buffer[9] = (byte) 0xFF;

		this.adapter.dataBlock(buffer, 0, 10);

		if (this.getFlag(GENERAL_STATUS_REGISTER, GSR_BIT_MISSION_IN_PROGRESS)) {
			throw new OneWireException("OneWireContainer41-Stop mission failed.  Check read/write password.");
		}
	}

	/**
	 * Starts a new mission. Assumes all parameters have been set by either writing
	 * directly to the device registers, or by calling other setup methods.
	 */
	public void startMission() throws OneWireException, OneWireIOException {
		if (this.getFlag(GENERAL_STATUS_REGISTER, GSR_BIT_MISSION_IN_PROGRESS)) {
			throw new OneWireException("OneWireContainer41-Cannot start a mission while a mission is in progress.");
		}

		if (!this.getFlag(GENERAL_STATUS_REGISTER, GSR_BIT_MEMORY_CLEARED)) {
			throw new OneWireException("OneWireContainer41-Must clear memory before calling start mission.");
		}

		if (this.doSpeedEnable) {
			this.doSpeed();
		}

		if (!this.adapter.select(this.address)) {
			throw new OneWireException("OneWireContainer41-Device not present.");
		}

		var buffer = new byte[10];
		buffer[0] = START_MISSION_PW_COMMAND;
		this.getContainerReadWritePassword(buffer, 1);
		buffer[9] = (byte) 0xFF;

		this.adapter.dataBlock(buffer, 0, 10);
	}

	/**
	 * Erases the log memory from this missioning device.
	 */
	public void clearMemory() throws OneWireException, OneWireIOException {
		if (this.getFlag(GENERAL_STATUS_REGISTER, GSR_BIT_MISSION_IN_PROGRESS)) {
			throw new OneWireException("OneWireContainer41-Cannot clear memory while mission is in progress.");
		}

		if (this.doSpeedEnable) {
			this.doSpeed();
		}

		if (!this.adapter.select(this.address)) {
			throw new OneWireException("OneWireContainer41-Device not present.");
		}

		var buffer = new byte[10];
		buffer[0] = CLEAR_MEMORY_PW_COMMAND;
		this.getContainerReadWritePassword(buffer, 1);
		buffer[9] = (byte) 0xFF;

		this.adapter.dataBlock(buffer, 0, 10);

		// wait 2 ms for Clear Memory to complete
		msWait(2);

		if (!this.getFlag(GENERAL_STATUS_REGISTER, GSR_BIT_MEMORY_CLEARED)) {
			throw new OneWireException("OneWireContainer41-Clear Memory failed.  Check read/write password.");
		}
	}

// *****************************************************************************
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// Read/Write Password Functions
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// *****************************************************************************

	/**
	 * Returns the length in bytes of the Read-Only password.
	 *
	 * @return the length in bytes of the Read-Only password.
	 */
	@Override
	public int getReadOnlyPasswordLength() throws OneWireException {
		return PASSWORD_LENGTH;
	}

	/**
	 * Returns the length in bytes of the Read/Write password.
	 *
	 * @return the length in bytes of the Read/Write password.
	 */
	@Override
	public int getReadWritePasswordLength() throws OneWireException {
		return PASSWORD_LENGTH;
	}

	/**
	 * Returns the length in bytes of the Write-Only password.
	 *
	 * @return the length in bytes of the Write-Only password.
	 */
	@Override
	public int getWriteOnlyPasswordLength() throws OneWireException {
		throw new OneWireException("The DS1922 does not have a write only password.");
	}

	/**
	 * Returns the absolute address of the memory location where the Read-Only
	 * password is written.
	 *
	 * @return the absolute address of the memory location where the Read-Only
	 *         password is written.
	 */
	@Override
	public int getReadOnlyPasswordAddress() throws OneWireException {
		return READ_ACCESS_PASSWORD;
	}

	/**
	 * Returns the absolute address of the memory location where the Read/Write
	 * password is written.
	 *
	 * @return the absolute address of the memory location where the Read/Write
	 *         password is written.
	 */
	@Override
	public int getReadWritePasswordAddress() throws OneWireException {
		return READ_WRITE_ACCESS_PASSWORD;
	}

	/**
	 * Returns the absolute address of the memory location where the Write-Only
	 * password is written.
	 *
	 * @return the absolute address of the memory location where the Write-Only
	 *         password is written.
	 */
	@Override
	public int getWriteOnlyPasswordAddress() throws OneWireException {
		throw new OneWireException("The DS1922 does not have a write password.");
	}

	/**
	 * Returns true if this device has a Read-Only password. If false, all other
	 * functions dealing with the Read-Only password will throw an exception if
	 * called.
	 *
	 * @return <code>true</code> always, since DS1922 has Read-Only password.
	 */
	@Override
	public boolean hasReadOnlyPassword() {
		return true;
	}

	/**
	 * Returns true if this device has a Read/Write password. If false, all other
	 * functions dealing with the Read/Write password will throw an exception if
	 * called.
	 *
	 * @return <code>true</code> always, since DS1922 has Read/Write password.
	 */
	@Override
	public boolean hasReadWritePassword() {
		return true;
	}

	/**
	 * Returns true if this device has a Write-Only password. If false, all other
	 * functions dealing with the Write-Only password will throw an exception if
	 * called.
	 *
	 * @return <code>false</code> always, since DS1922 has no Write-Only password.
	 */
	@Override
	public boolean hasWriteOnlyPassword() {
		return false;
	}

	/**
	 * Returns true if the device's Read-Only password has been enabled.
	 *
	 * @return <code>true</code> if the device's Read-Only password has been
	 *         enabled.
	 */
	@Override
	public boolean getDeviceReadOnlyPasswordEnable() throws OneWireException {
		return this.readOnlyPasswordEnabled;
	}

	/**
	 * Returns true if the device's Read/Write password has been enabled.
	 *
	 * @return <code>true</code> if the device's Read/Write password has been
	 *         enabled.
	 */
	@Override
	public boolean getDeviceReadWritePasswordEnable() throws OneWireException {
		return this.readWritePasswordEnabled;
	}

	/**
	 * Returns true if the device's Write-Only password has been enabled.
	 *
	 * @return <code>true</code> if the device's Write-Only password has been
	 *         enabled.
	 */
	@Override
	public boolean getDeviceWriteOnlyPasswordEnable() throws OneWireException {
		throw new OneWireException("The DS1922 does not have a Write Only Password.");
	}

	/**
	 * Returns true if this device has the capability to enable one type of password
	 * while leaving another type disabled. i.e. if the device has Read-Only
	 * password protection and Write-Only password protection, this method indicates
	 * whether or not you can enable Read-Only protection while leaving the
	 * Write-Only protection disabled.
	 *
	 * @return <code>true</code> if the device has the capability to enable one type
	 *         of password while leaving another type disabled.
	 */
	@Override
	public boolean hasSinglePasswordEnable() {
		return false;
	}

	/**
	 * <p>
	 * Enables/Disables passwords for this Device. This method allows you to
	 * individually enable the different types of passwords for a particular device.
	 * If <code>hasSinglePasswordEnable()</code> returns true, you can selectively
	 * enable particular types of passwords. Otherwise, this method will throw an
	 * exception if all supported types are not enabled.
	 * </p>
	 *
	 * <p>
	 * For this to be successful, either write-protect passwords must be disabled,
	 * or the write-protect password(s) for this container must be set and must
	 * match the value of the write-protect password(s) in the device's register.
	 * </p>
	 *
	 * <P>
	 * <B> WARNING: Enabling passwords requires that both the read password and the
	 * read/write password be re-written to the part. Before calling this method,
	 * you should set the container read password and read/write password values.
	 * This will ensure that the correct value is written into the part. </B>
	 * </P>
	 *
	 * @param enableReadOnly  if <code>true</code> Read-Only passwords will be
	 *                        enabled.
	 * @param enableReadWrite if <code>true</code> Read/Write passwords will be
	 *                        enabled.
	 * @param enableWriteOnly if <code>true</code> Write-Only passwords will be
	 *                        enabled.
	 */
	@Override
	public void setDevicePasswordEnable(boolean enableReadOnly, boolean enableReadWrite, boolean enableWriteOnly)
			throws OneWireException, OneWireIOException {
		if (enableWriteOnly) {
			throw new OneWireException("The DS1922 does not have a write only password.");
		}
		if (enableReadOnly != enableReadWrite) {
			throw new OneWireException("Both read-only and read/write will be set with enable.");
		}
		if (!this.isContainerReadOnlyPasswordSet()) {
			throw new OneWireException("Container Read Password is not set");
		}
		if (!this.isContainerReadWritePasswordSet()) {
			throw new OneWireException("Container Read/Write Password is not set");
		}

		// must write both passwords for this to work
		var bothPasswordsEnable = new byte[17];
		bothPasswordsEnable[0] = enableReadOnly ? ENABLE_BYTE : DISABLE_BYTE;
		this.getContainerReadOnlyPassword(bothPasswordsEnable, 1);
		this.getContainerReadWritePassword(bothPasswordsEnable, 9);

		this.register.write(PASSWORD_CONTROL_REGISTER & 0x3F, bothPasswordsEnable, 0, 17);

		if (enableReadOnly) {
			this.readOnlyPasswordEnabled = true;
			this.readWritePasswordEnabled = true;
		} else {
			this.readOnlyPasswordEnabled = false;
			this.readWritePasswordEnabled = false;
		}
	}

	/**
	 * <p>
	 * Enables/Disables passwords for this device. If the part has more than one
	 * type of password (Read-Only, Write-Only, or Read/Write), all passwords will
	 * be enabled. This function is equivalent to the following:
	 * <code> owc41.setDevicePasswordEnable(
	 *                    owc41.hasReadOnlyPassword(),
	 *                    owc41.hasReadWritePassword(),
	 *                    owc41.hasWriteOnlyPassword() ); </code>
	 * </p>
	 *
	 * <p>
	 * For this to be successful, either write-protect passwords must be disabled,
	 * or the write-protect password(s) for this container must be set and must
	 * match the value of the write-protect password(s) in the device's register.
	 * </P>
	 *
	 * <P>
	 * <B> WARNING: Enabling passwords requires that both the read password and the
	 * read/write password be re-written to the part. Before calling this method,
	 * you should set the container read password and read/write password values.
	 * This will ensure that the correct value is written into the part. </B>
	 * </P>
	 *
	 * @param enableAll if <code>true</code>, all passwords are enabled. Otherwise,
	 *                  all passwords are disabled.
	 */
	@Override
	public void setDevicePasswordEnableAll(boolean enableAll) throws OneWireException, OneWireIOException {
		this.setDevicePasswordEnable(enableAll, enableAll, false);
	}

	/**
	 * <p>
	 * Writes the given password to the device's Read-Only password register. Note
	 * that this function does not enable the password, just writes the value to the
	 * appropriate memory location.
	 * </p>
	 *
	 * <p>
	 * For this to be successful, either write-protect passwords must be disabled,
	 * or the write-protect password(s) for this container must be set and must
	 * match the value of the write-protect password(s) in the device's register.
	 * </p>
	 *
	 * <P>
	 * <B> WARNING: Setting the read password requires that both the read password
	 * and the read/write password be written to the part. Before calling this
	 * method, you should set the container read/write password value. This will
	 * ensure that the correct value is written into the part. </B>
	 * </P>
	 *
	 * @param password the new password to be written to the device's Read-Only
	 *                 password register. Length must be
	 *                 <code>(offset + getReadOnlyPasswordLength)</code>
	 * @param offset   the starting point for copying from the given password array
	 */
	@Override
	public void setDeviceReadOnlyPassword(byte[] password, int offset) throws OneWireException, OneWireIOException {
		if (this.getFlag(GENERAL_STATUS_REGISTER, GSR_BIT_MISSION_IN_PROGRESS)) {
			throw new OneWireIOException("OneWireContainer41-Cannot change password while mission is in progress.");
		}

		if (!this.isContainerReadWritePasswordSet()) {
			throw new OneWireException("Container Read/Write Password is not set");
		}

		// must write both passwords for this to work
		var bothPasswords = new byte[16];
		System.arraycopy(password, offset, bothPasswords, 0, 8);
		this.getContainerReadWritePassword(bothPasswords, 8);

		this.register.write(READ_ACCESS_PASSWORD & 0x3F, bothPasswords, 0, 16);
		this.setContainerReadOnlyPassword(password, offset);
	}

	/**
	 * <p>
	 * Writes the given password to the device's Read/Write password register. Note
	 * that this function does not enable the password, just writes the value to the
	 * appropriate memory location.
	 * </p>
	 *
	 * <p>
	 * For this to be successful, either write-protect passwords must be disabled,
	 * or the write-protect password(s) for this container must be set and must
	 * match the value of the write-protect password(s) in the device's register.
	 * </p>
	 *
	 * @param password the new password to be written to the device's Read-Write
	 *                 password register. Length must be
	 *                 <code>(offset + getReadWritePasswordLength)</code>
	 * @param offset   the starting point for copying from the given password array
	 */
	@Override
	public void setDeviceReadWritePassword(byte[] password, int offset) throws OneWireException, OneWireIOException {
		if (this.getFlag(GENERAL_STATUS_REGISTER, GSR_BIT_MISSION_IN_PROGRESS)) {
			throw new OneWireIOException("OneWireContainer41-Cannot change password while mission is in progress.");
		}

		this.register.write(READ_WRITE_ACCESS_PASSWORD & 0x3F, password, offset, 8);
		this.setContainerReadWritePassword(password, offset);
	}

	/**
	 * <p>
	 * Writes the given password to the device's Write-Only password register. Note
	 * that this function does not enable the password, just writes the value to the
	 * appropriate memory location.
	 * </p>
	 *
	 * <p>
	 * For this to be successful, either write-protect passwords must be disabled,
	 * or the write-protect password(s) for this container must be set and must
	 * match the value of the write-protect password(s) in the device's register.
	 * </p>
	 *
	 * @param password the new password to be written to the device's Write-Only
	 *                 password register. Length must be
	 *                 <code>(offset + getWriteOnlyPasswordLength)</code>
	 * @param offset   the starting point for copying from the given password array
	 */
	@Override
	public void setDeviceWriteOnlyPassword(byte[] password, int offset) throws OneWireException, OneWireIOException {
		throw new OneWireException("The DS1922 does not have a write only password.");
	}

	/**
	 * Sets the Read-Only password used by the API when reading from the device's
	 * memory. This password is not written to the device's Read-Only password
	 * register. It is the password used by the software for interacting with the
	 * device only.
	 *
	 * @param password the new password to be used by the API when reading from the
	 *                 device's memory. Length must be
	 *                 <code>(offset + getReadOnlyPasswordLength)</code>
	 * @param offset   the starting point for copying from the given password array
	 */
	@Override
	public void setContainerReadOnlyPassword(byte[] password, int offset) throws OneWireException {
		System.arraycopy(password, offset, this.readPassword, 0, PASSWORD_LENGTH);
		this.readPasswordSet = true;
	}

	/**
	 * Sets the Read/Write password used by the API when reading from or writing to
	 * the device's memory. This password is not written to the device's Read/Write
	 * password register. It is the password used by the software for interacting
	 * with the device only.
	 *
	 * @param password the new password to be used by the API when reading from or
	 *                 writing to the device's memory. Length must be
	 *                 <code>(offset + getReadWritePasswordLength)</code>
	 * @param offset   the starting point for copying from the given password array
	 */
	@Override
	public void setContainerReadWritePassword(byte[] password, int offset) throws OneWireException {
		System.arraycopy(password, offset, this.readWritePassword, 0, 8);
		this.readWritePasswordSet = true;
	}

	/**
	 * Sets the Write-Only password used by the API when writing to the device's
	 * memory. This password is not written to the device's Write-Only password
	 * register. It is the password used by the software for interacting with the
	 * device only.
	 *
	 * @param password the new password to be used by the API when writing to the
	 *                 device's memory. Length must be
	 *                 <code>(offset + getWriteOnlyPasswordLength)</code>
	 * @param offset   the starting point for copying from the given password array
	 */
	@Override
	public void setContainerWriteOnlyPassword(byte[] password, int offset) throws OneWireException {
		throw new OneWireException("The DS1922 does not have a write only password.");
	}

	/**
	 * Returns true if the password used by the API for reading from the device's
	 * memory has been set. The return value is not affected by whether or not the
	 * read password of the container actually matches the value in the device's
	 * password register
	 *
	 * @return <code>true</code> if the password used by the API for reading from
	 *         the device's memory has been set.
	 */
	@Override
	public boolean isContainerReadOnlyPasswordSet() throws OneWireException {
		return this.readPasswordSet;
	}

	/**
	 * Returns true if the password used by the API for reading from or writing to
	 * the device's memory has been set. The return value is not affected by whether
	 * or not the read/write password of the container actually matches the value in
	 * the device's password register.
	 *
	 * @return <code>true</code> if the password used by the API for reading from or
	 *         writing to the device's memory has been set.
	 */
	@Override
	public boolean isContainerReadWritePasswordSet() throws OneWireException {
		return this.readWritePasswordSet;
	}

	/**
	 * Returns true if the password used by the API for writing to the device's
	 * memory has been set. The return value is not affected by whether or not the
	 * write password of the container actually matches the value in the device's
	 * password register.
	 *
	 * @return <code>true</code> if the password used by the API for writing to the
	 *         device's memory has been set.
	 */
	@Override
	public boolean isContainerWriteOnlyPasswordSet() throws OneWireException {
		throw new OneWireException("The DS1922 does not have a write only password");
	}

	/**
	 * Gets the Read-Only password used by the API when reading from the device's
	 * memory. This password is not read from the device's Read-Only password
	 * register. It is the password used by the software for interacting with the
	 * device only and must have been set using the
	 * <code>setContainerReadOnlyPassword</code> method.
	 *
	 * @param password array for holding the password that is used by the API when
	 *                 reading from the device's memory. Length must be
	 *                 <code>(offset + getWriteOnlyPasswordLength)</code>
	 * @param offset   the starting point for copying into the given password array
	 */
	@Override
	public void getContainerReadOnlyPassword(byte[] password, int offset) throws OneWireException {
		System.arraycopy(this.readPassword, 0, password, offset, PASSWORD_LENGTH);
	}

	/**
	 * Gets the Read/Write password used by the API when reading from or writing to
	 * the device's memory. This password is not read from the device's Read/Write
	 * password register. It is the password used by the software for interacting
	 * with the device only and must have been set using the
	 * <code>setContainerReadWritePassword</code> method.
	 *
	 * @param password array for holding the password that is used by the API when
	 *                 reading from or writing to the device's memory. Length must
	 *                 be <code>(offset + getReadWritePasswordLength)</code>
	 * @param offset   the starting point for copying into the given password array
	 */
	@Override
	public void getContainerReadWritePassword(byte[] password, int offset) throws OneWireException {
		System.arraycopy(this.readWritePassword, 0, password, offset, PASSWORD_LENGTH);
	}

	/**
	 * Gets the Write-Only password used by the API when writing to the device's
	 * memory. This password is not read from the device's Write-Only password
	 * register. It is the password used by the software for interacting with the
	 * device only and must have been set using the
	 * <code>setContainerWriteOnlyPassword</code> method.
	 *
	 * @param password array for holding the password that is used by the API when
	 *                 writing to the device's memory. Length must be
	 *                 <code>(offset + getWriteOnlyPasswordLength)</code>
	 * @param offset   the starting point for copying into the given password array
	 */
	@Override
	public void getContainerWriteOnlyPassword(byte[] password, int offset) throws OneWireException {
		throw new OneWireException("The DS1922 does not have a write only password");
	}

// *****************************************************************************
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// Mission Interface Functions
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// *****************************************************************************

	/**
	 * Returns a default friendly label for each channel supported by this
	 * Missioning device.
	 *
	 * @param channel the mission channel, between <code>0</code> and
	 *                <code>(getNumberOfMissionChannels()-1)</code>
	 * @return friendly label for the specified channel
	 */
	@Override
	public String getMissionLabel(int channel) throws OneWireException, OneWireIOException {
		if (channel == TEMPERATURE_CHANNEL) {
			return "Temperature";
		}
		if (channel == DATA_CHANNEL) {
			if (this.hasHumiditySensor && !this.adForceResults) {
				return "Humidity";
			} else {
				return "Data";
			}
		} else {
			throw new OneWireException("Invalid Channel");
		}
	}

	/**
	 * Sets the SUTA (Start Upon Temperature Alarm) bit in the Mission Control
	 * register. This method will communicate with the device directly.
	 *
	 * @param enable sets/clears the SUTA bit in the Mission Control register.
	 */
	public void setStartUponTemperatureAlarmEnable(boolean enable) throws OneWireException, OneWireIOException {
		this.setFlag(MISSION_CONTROL_REGISTER, MCR_BIT_START_MISSION_ON_TEMPERATURE_ALARM, enable);
	}

	/**
	 * Sets the SUTA (Start Upon Temperature Alarm) bit in the Mission Control
	 * register. This method will set the bit in the provided 'state' array, which
	 * should be acquired through a call to <code>readDevice()</code>. After
	 * updating the 'state', the method <code>writeDevice(byte[])</code> should be
	 * called to commit your changes.
	 *
	 * @param enable sets/clears the SUTA bit in the Mission Control register.
	 * @param state  current state of the device returned from
	 *               <code>readDevice()</code>
	 */
	public void setStartUponTemperatureAlarmEnable(boolean enable, byte[] state)
			throws OneWireException, OneWireIOException {
		this.setFlag(MISSION_CONTROL_REGISTER, MCR_BIT_START_MISSION_ON_TEMPERATURE_ALARM, enable, state);
	}

	/**
	 * Returns true if the SUTA (Start Upon Temperature Alarm) bit in the Mission
	 * Control register is set. This method will communicate with the device to read
	 * the status of the SUTA bit.
	 *
	 * @return <code>true</code> if the SUTA bit in the Mission Control register is
	 *         set.
	 */
	public boolean isStartUponTemperatureAlarmEnabled() throws OneWireException, OneWireIOException {
		return this.getFlag(MISSION_CONTROL_REGISTER, MCR_BIT_START_MISSION_ON_TEMPERATURE_ALARM);
	}

	/**
	 * Returns true if the SUTA (Start Upon Temperature Alarm) bit in the Mission
	 * Control register is set. This method will check for the bit in the provided
	 * 'state' array, which should be acquired through a call to
	 * <code>readDevice()</code>.
	 *
	 * @param state current state of the device returned from
	 *              <code>readDevice()</code>
	 * @return <code>true</code> if the SUTA bit in the Mission Control register is
	 *         set.
	 */
	public boolean isStartUponTemperatureAlarmEnabled(byte[] state) throws OneWireException, OneWireIOException {
		return this.getFlag(MISSION_CONTROL_REGISTER, MCR_BIT_START_MISSION_ON_TEMPERATURE_ALARM, state);
	}

	/**
	 * Returns true if the currently loaded mission results indicate that this
	 * mission has the SUTA bit enabled.
	 *
	 * @return <code>true</code> if the currently loaded mission results indicate
	 *         that this mission has the SUTA bit enabled.
	 */
	public boolean isMissionSUTA() throws OneWireException, OneWireIOException {
		if (this.isMissionLoaded) {
			return this.getFlag(MISSION_CONTROL_REGISTER, MCR_BIT_START_MISSION_ON_TEMPERATURE_ALARM,
					this.missionRegister);
		}
		return this.getFlag(MISSION_CONTROL_REGISTER, MCR_BIT_START_MISSION_ON_TEMPERATURE_ALARM);
	}

	/**
	 * Returns true if the currently loaded mission results indicate that this
	 * mission has the SUTA bit enabled and is still Waiting For Temperature Alarm
	 * (WFTA).
	 *
	 * @return <code>true</code> if the currently loaded mission results indicate
	 *         that this mission has the SUTA bit enabled and is still Waiting For
	 *         Temperature Alarm (WFTA).
	 */
	public boolean isMissionWFTA() throws OneWireException, OneWireIOException {
		// check for MIP=1 and SUTA=1 before returning value of WFTA.
		// if MIP=0 or SUTA=0, WFTA could be in invalid state if previous
		// mission did not get a temperature alarm. Clear Memory should
		// clear this bit, so this is the workaround.
		if (this.isMissionRunning() && this.isMissionSUTA()) {
			if (this.isMissionLoaded) {
				return this.getFlag(GENERAL_STATUS_REGISTER, GSR_BIT_WAITING_FOR_TEMPERATURE_ALARM,
						this.missionRegister);
			}
			return this.getFlag(GENERAL_STATUS_REGISTER, GSR_BIT_WAITING_FOR_TEMPERATURE_ALARM);
		}
		return false;
	}

	/**
	 * Begins a new mission on this missioning device.
	 *
	 * @param sampleRate        indicates the sampling rate, in seconds, that this
	 *                          missioning device should log samples.
	 * @param missionStartDelay indicates the amount of time, in minutes, that
	 *                          should pass before the mission begins.
	 * @param rolloverEnabled   if <code>false</code>, this device will stop
	 *                          recording new samples after the data log is full.
	 *                          Otherwise, it will replace samples starting at the
	 *                          beginning.
	 * @param syncClock         if <code>true</code>, the real-time clock of this
	 *                          missioning device will be synchronized with the
	 *                          current time according to this
	 *                          <code>java.util.Date</code>.
	 */
	@Override
	public void startNewMission(int sampleRate, int missionStartDelay, boolean rolloverEnabled, boolean syncClock,
			boolean[] channelEnabled) throws OneWireException, OneWireIOException {
		var state = this.readDevice();
		// if(isMissionLoaded)
		// state = missionRegister;
		// else
		// state = readDevice();

//      System.out.println("startNewMission: before state=" + Convert.toHexString(state));

		for (var i = 0; i < this.getNumberMissionChannels(); i++) {
			this.setMissionChannelEnable(i, channelEnabled[i], state);
		}

		if (sampleRate % 60 == 0 || sampleRate > 0x03FFF) {
			// convert to minutes
			sampleRate = sampleRate / 60 & 0x03FFF;
			this.setFlag(RTC_CONTROL_REGISTER, RCR_BIT_ENABLE_HIGH_SPEED_SAMPLE, false, state);
		} else {
			this.setFlag(RTC_CONTROL_REGISTER, RCR_BIT_ENABLE_HIGH_SPEED_SAMPLE, true, state);
		}

		Convert.toByteArray(sampleRate, state, SAMPLE_RATE & 0x3F, 2);

		Convert.toByteArray(missionStartDelay, state, MISSION_START_DELAY & 0x3F, 3);

		this.setFlag(MISSION_CONTROL_REGISTER, MCR_BIT_ENABLE_ROLLOVER, rolloverEnabled, state);

		if (syncClock) {
			this.setClock(new Date().getTime(), state);
		} else if (!this.getFlag(RTC_CONTROL_REGISTER, RCR_BIT_ENABLE_OSCILLATOR, state)) {
			this.setFlag(RTC_CONTROL_REGISTER, RCR_BIT_ENABLE_OSCILLATOR, true, state);
		}
//      System.out.println("startNewMission: after  state=" + Convert.toHexString(state));

		this.clearMemory();
		this.writeDevice(state);
		this.startMission();
	}

	/**
	 * Loads the results of the currently running mission. Must be called before all
	 * mission result/status methods.
	 */
	@Override
	public synchronized void loadMissionResults() throws OneWireException, OneWireIOException {
		// read the register contents
		this.missionRegister = this.readDevice();

		// get the number of samples
		this.sampleCount = Convert.toInt(this.missionRegister, MISSION_SAMPLE_COUNT & 0x3F, 3);
		this.sampleCountTotal = this.sampleCount;

		// sample rate, in seconds
		this.sampleRate = Convert.toInt(this.missionRegister, SAMPLE_RATE & 0x3F, 2);
		if (!this.getFlag(RTC_CONTROL_REGISTER, RCR_BIT_ENABLE_HIGH_SPEED_SAMPLE, this.missionRegister)) {
			// if sample rate is in minutes, convert to seconds
			this.sampleRate *= 60;
		}

		// grab the time
		var time = this.getTime(MISSION_TIMESTAMP_TIME & 0x3F, this.missionRegister);
		// grab the date
		var date = this.getDate(MISSION_TIMESTAMP_DATE & 0x3F, this.missionRegister);

		// date[1] - 1 because Java months are 0 offset
		Calendar d = new GregorianCalendar(date[0], date[1] - 1, date[2], time[2], time[1], time[0]);

		this.missionTimeStamp = d.getTime().getTime();

		// figure out how many bytes for each temperature sample
		this.temperatureBytes = 0;
		// if it's being logged, add 1 to the size
		if (this.getFlag(MISSION_CONTROL_REGISTER, MCR_BIT_ENABLE_TEMPERATURE_LOGGING, this.missionRegister)) {
			this.temperatureBytes += 1;
			// if it's 16-bit resolution, add another 1 to the size
			if (this.getFlag(MISSION_CONTROL_REGISTER, MCR_BIT_TEMPERATURE_RESOLUTION, this.missionRegister)) {
				this.temperatureBytes += 1;
			}
		}

		// figure out how many bytes for each data sample
		this.dataBytes = 0;
		// if it's being logged, add 1 to the size
		if (this.getFlag(MISSION_CONTROL_REGISTER, MCR_BIT_ENABLE_DATA_LOGGING, this.missionRegister)) {
			this.dataBytes += 1;
			// if it's 16-bit resolution, add another 1 to the size
			if (this.getFlag(MISSION_CONTROL_REGISTER, MCR_BIT_DATA_RESOLUTION, this.missionRegister)) {
				this.dataBytes += 1;
			}
		}

		// default size of the log, could be different if using an odd
		// sample size combination.
		var logSize = MISSION_LOG_SIZE;

		// figure max number of samples
		var maxSamples = 0;
		switch (this.temperatureBytes + this.dataBytes) {
		case 1:
			maxSamples = 8192;
			break;
		case 2:
			maxSamples = 4096;
			break;
		case 3:
			maxSamples = 2560;
			logSize = ODD_MISSION_LOG_SIZE;
			break;
		case 4:
			maxSamples = 2048;
			break;
		default:
		case 0:
			// assert! should never, ever get here
			break;
		}

		// check for rollover
		int wrapCount = 0, offsetDepth = 0;
		if (this.getFlag(MISSION_CONTROL_REGISTER, MCR_BIT_ENABLE_ROLLOVER, this.missionRegister)
				&& (this.rolledOver = this.sampleCount > maxSamples))// intentional assignment
		{
			wrapCount = this.sampleCount / maxSamples - 1;
			offsetDepth = this.sampleCount % maxSamples;
			this.sampleCount = maxSamples;
		}

		// DEBUG: For bad SOICS
		if (!this.getFlag(MISSION_CONTROL_REGISTER, MCR_BIT_ENABLE_ROLLOVER, this.missionRegister) && this.rolledOver) {
			throw new OneWireException("Device Error: rollover was not enabled, but it did occur.");
		}

		// figure out where the temperature bytes end, that's where
		// the data bytes begin
		var temperatureLogSize = this.temperatureBytes * maxSamples;

		// calculate first log entry time offset, in samples
		this.timeOffset = wrapCount * maxSamples + offsetDepth;

		// temperature log
		this.temperatureLog = new byte[this.sampleCount * this.temperatureBytes];
		// data log
		this.dataLog = new byte[this.sampleCount * this.dataBytes];
		// cache for entire log
		var missionLogBuffer = new byte[Math.max(this.temperatureLog.length, this.dataLog.length)];
		var pagebuffer = new byte[32];

		if (this.temperatureLog.length > 0) {
			// read the data log for temperature
			var numPages = this.temperatureLog.length / 32 + (this.temperatureLog.length % 32 > 0 ? 1 : 0);
			var retryCnt = MAX_READ_RETRY_CNT;
			for (var i = 0; i < numPages;) {
				try {
					this.log.readPageCRC(i, i > 0 && retryCnt == MAX_READ_RETRY_CNT, pagebuffer, 0);
					System.arraycopy(pagebuffer, 0, missionLogBuffer, i * 32,
							Math.min(32, this.temperatureLog.length - i * 32));
					retryCnt = MAX_READ_RETRY_CNT;
					i++;
				} catch (OneWireIOException owioe) {
					if (--retryCnt == 0) {
						throw owioe;
					}
				} catch (OneWireException owe) {
					if (--retryCnt == 0) {
						throw owe;
					}
				}
			}

			// get the temperature bytes in order
			var offsetIndex = offsetDepth * this.temperatureBytes;
			System.arraycopy(missionLogBuffer, offsetIndex, this.temperatureLog, 0,
					this.temperatureLog.length - offsetIndex);
			System.arraycopy(missionLogBuffer, 0, this.temperatureLog, this.temperatureLog.length - offsetIndex,
					offsetIndex);
		}

		if (this.dataLog.length > 0) {
			// read the data log for humidity
			var numPages = this.dataLog.length / 32 + (this.dataLog.length % 32 > 0 ? 1 : 0);
			var retryCnt = MAX_READ_RETRY_CNT;
			for (var i = 0; i < numPages;) {
				try {
					this.log.readPageCRC(temperatureLogSize / 32 + i, i > 0 && retryCnt == MAX_READ_RETRY_CNT,
							pagebuffer, 0);
					System.arraycopy(pagebuffer, 0, missionLogBuffer, i * 32,
							Math.min(32, this.dataLog.length - i * 32));
					retryCnt = MAX_READ_RETRY_CNT;
					i++;
				} catch (OneWireIOException owioe) {
					if (--retryCnt == 0) {
						throw owioe;
					}
				} catch (OneWireException owe) {
					if (--retryCnt == 0) {
						throw owe;
					}
				}
			}

			// get the data bytes in order
			var offsetIndex = offsetDepth * this.dataBytes;
			System.arraycopy(missionLogBuffer, offsetIndex, this.dataLog, 0, this.dataLog.length - offsetIndex);
			System.arraycopy(missionLogBuffer, 0, this.dataLog, this.dataLog.length - offsetIndex, offsetIndex);
		}

		this.isMissionLoaded = true;
	}

	/**
	 * Returns true if the mission results have been loaded from the device.
	 *
	 * @return <code>true</code> if the mission results have been loaded.
	 */
	@Override
	public boolean isMissionLoaded() {
		return this.isMissionLoaded;
	}

	/**
	 * Gets the number of channels supported by this Missioning device. Channel
	 * specific methods will use a channel number specified by an integer from [0 to
	 * (<code>getNumberOfMissionChannels()</code> - 1)].
	 *
	 * @return the number of channels
	 */
	@Override
	public int getNumberMissionChannels() {
		if (this.deviceConfigByte == DCB_DS1922L || this.deviceConfigByte == DCB_DS1922T
				|| this.deviceConfigByte == DCB_DS1922E || this.deviceConfigByte == DCB_DS1922S) {
			return 1; // temperature only
		}
		return 2; // temperature and data/voltage/humidity
	}

	/**
	 * Enables/disables the specified mission channel, indicating whether or not the
	 * channel's readings will be recorded in the mission log.
	 *
	 * @param channel the channel to enable/disable
	 * @param enable  if true, the channel is enabled
	 */
	@Override
	public void setMissionChannelEnable(int channel, boolean enable) throws OneWireException, OneWireIOException {
		if (!this.isMissionLoaded) {
			this.missionRegister = this.readDevice();
		}
		this.setMissionChannelEnable(channel, enable, this.missionRegister);
		this.writeDevice(this.missionRegister);
	}

	/**
	 * Enables/disables the specified mission channel, indicating whether or not the
	 * channel's readings will be recorded in the mission log.
	 *
	 * @param channel the channel to enable/disable
	 * @param enable  if true, the channel is enabled
	 * @param state   the state as returned from readDevice, for cached writes
	 */
	public void setMissionChannelEnable(int channel, boolean enable, byte[] state)
			throws OneWireException, OneWireIOException {
		if (channel == TEMPERATURE_CHANNEL) {
			this.setFlag(MISSION_CONTROL_REGISTER, MCR_BIT_ENABLE_TEMPERATURE_LOGGING, enable, state);
		} else if (channel == DATA_CHANNEL) {
			this.setFlag(MISSION_CONTROL_REGISTER, MCR_BIT_ENABLE_DATA_LOGGING, enable, state);
		} else {
			throw new IllegalArgumentException("Invalid Channel");
		}
	}

	/**
	 * Returns true if the specified mission channel is enabled, indicating that the
	 * channel's readings will be recorded in the mission log.
	 *
	 * @param channel the channel to enable/disable
	 * @param enable  if true, the channel is enabled
	 */
	@Override
	public boolean getMissionChannelEnable(int channel) throws OneWireException, OneWireIOException {
		if (!this.isMissionLoaded) {
			this.missionRegister = this.readDevice();
		}

		return this.getMissionChannelEnable(channel, this.missionRegister);
	}

	/**
	 * Returns true if the specified mission channel is enabled, indicating that the
	 * channel's readings will be recorded in the mission log.
	 *
	 * @param channel the channel to enable/disable
	 * @param enable  if true, the channel is enabled
	 */
	public boolean getMissionChannelEnable(int channel, byte[] state) throws OneWireException, OneWireIOException {
		if (channel == TEMPERATURE_CHANNEL) {
			return this.getFlag(MISSION_CONTROL_REGISTER, MCR_BIT_ENABLE_TEMPERATURE_LOGGING, state);
		}
		if (channel == DATA_CHANNEL) {
			return this.getFlag(MISSION_CONTROL_REGISTER, MCR_BIT_ENABLE_DATA_LOGGING, state);
		} else {
			throw new IllegalArgumentException("Invalid Channel");
		}
	}

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	// - Mission Results
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * Returns the amount of time, in seconds, between samples taken by this
	 * missioning device.
	 *
	 * @param channel the mission channel, between <code>0</code> and
	 *                <code>(getNumberOfMissionChannels()-1)</code>
	 * @return time, in seconds, between sampling
	 */
	@Override
	public int getMissionSampleRate(int channel) throws OneWireException, OneWireIOException {
		if (!this.isMissionLoaded) {
			throw new OneWireException("Must load mission results first.");
		}

		return this.sampleRate;
	}

	/**
	 * Returns the number of samples available for the specified channel during the
	 * current mission.
	 *
	 * @param channel the mission channel, between <code>0</code> and
	 *                <code>(getNumberOfMissionChannels()-1)</code>
	 * @return number of samples available for the specified channel
	 */
	@Override
	public int getMissionSampleCount(int channel) throws OneWireException, OneWireIOException {
		if (!this.isMissionLoaded) {
			throw new OneWireException("Must load mission results first.");
		}

		return this.sampleCount;
	}

	/**
	 * Reads the device and returns the total number of samples logged since the
	 * first power-on of this device.
	 *
	 * @return the total number of samples logged since the first power-on of this
	 *         device.
	 */
	public int getDeviceSampleCount() throws OneWireException, OneWireIOException {
		return this.getDeviceSampleCount(this.readDevice());
	}

	/**
	 * Returns the total number of samples logged since the first power-on of this
	 * device.
	 *
	 * @param state The current state of the device as return from
	 *              <code>readDevice()</code>
	 * @return the total number of samples logged since the first power-on of this
	 *         device.
	 */
	public int getDeviceSampleCount(byte[] state) throws OneWireException, OneWireIOException {
		return Convert.toInt(state, DEVICE_SAMPLE_COUNT & 0x3F, 3);
	}

	/**
	 * Returns the total number of samples taken for the specified channel during
	 * the current mission. This number can be more than the actual sample count if
	 * rollover is enabled and the log has been filled.
	 *
	 * @param channel the mission channel, between <code>0</code> and
	 *                <code>(getNumberOfMissionChannels()-1)</code>
	 * @return number of samples taken for the specified channel
	 */
	@Override
	public int getMissionSampleCountTotal(int channel) throws OneWireException, OneWireIOException {
		if (!this.isMissionLoaded) {
			throw new OneWireException("Must load mission results first.");
		}

		return this.sampleCountTotal;
	}

	/**
	 * Returns the sample as degrees celsius if temperature channel is specified or
	 * as percent relative humidity if data channel is specified. If the device is a
	 * DS2422 configuration (or A-D results are forced on the DS1923), the data
	 * channel will return samples as the voltage measured.
	 *
	 * @param channel   the mission channel, between <code>0</code> and
	 *                  <code>(getNumberOfMissionChannels()-1)</code>
	 * @param sampleNum the sample number to return, between <code>0</code> and
	 *                  <code>(getMissionSampleCount(channel)-1)</code>
	 * @return the sample's value in degrees Celsius or percent RH.
	 */
	@Override
	public double getMissionSample(int channel, int sampleNum) throws OneWireException, OneWireIOException {
		if (!this.isMissionLoaded) {
			throw new OneWireException("Must load mission results first.");
		}

		if (sampleNum >= this.sampleCount || sampleNum < 0) {
			throw new IllegalArgumentException("Invalid sample number");
		}

		var val = 0D;
		if (channel == TEMPERATURE_CHANNEL) {
			val = this.decodeTemperature(this.temperatureLog, sampleNum * this.temperatureBytes, this.temperatureBytes,
					true);
			if (this.useTempCalibrationRegisters && this.partNumber != PART_NUMBER_DS1922E) // DS1922E does not use
																							// calibration
			// registers
			{
				var valsq = val * val;
				var error = this.tempCoeffA * valsq + this.tempCoeffB * val + this.tempCoeffC;
				val = val - error;
			}

		} else if (channel == DATA_CHANNEL) {
			if (this.hasHumiditySensor && !this.adForceResults) {
				val = this.decodeHumidity(this.dataLog, sampleNum * this.dataBytes, this.dataBytes, true);

				if (this.useTemperatureCompensation) {
					double T;
					if (!this.overrideTemperatureLog && this.getMissionSampleCount(TEMPERATURE_CHANNEL) > 0) {
						T = this.getMissionSample(TEMPERATURE_CHANNEL, sampleNum);
					} else {
						T = this.defaultTempCompensationValue;
					}
					var gamma = T > 15 ? 0.00001 : -0.00005;
					T -= 25;
					val = (val * 0.0307 + .0035 * T - 0.000043 * T * T) / (0.0307 + gamma * T - 0.000002 * T * T);
				}
			} else {
				val = this.getADVoltage(this.dataLog, sampleNum * this.dataBytes, this.dataBytes, true);
			}
		} else {
			throw new IllegalArgumentException("Invalid Channel");
		}

		return val;
	}

	/**
	 * Returns the sample as an integer value. This value is not converted to
	 * degrees Celsius for temperature or to percent RH for Humidity. It is simply
	 * the 8 or 16 bits of digital data written in the mission log for this sample
	 * entry. It is up to the user to mask off the unused bits and convert this
	 * value to it's proper units. This method is primarily for users of the DS2422
	 * who are using an input device which is not an A-D or have an A-D wholly
	 * dissimilar to the one specified in the datasheet.
	 *
	 * @param channel   the mission channel, between <code>0</code> and
	 *                  <code>(getNumberOfMissionChannels()-1)</code>
	 * @param sampleNum the sample number to return, between <code>0</code> and
	 *                  <code>(getMissionSampleCount(channel)-1)</code>
	 * @return the sample as a whole integer
	 */
	@Override
	public int getMissionSampleAsInteger(int channel, int sampleNum) throws OneWireException, OneWireIOException {
		if (!this.isMissionLoaded) {
			throw new OneWireException("Must load mission results first.");
		}

		if (sampleNum >= this.sampleCount || sampleNum < 0) {
			throw new IllegalArgumentException("Invalid sample number");
		}

		var i = 0;
		if (channel == TEMPERATURE_CHANNEL) {
			if (this.temperatureBytes == 2) {
				i = (0x0FF & this.temperatureLog[sampleNum * this.temperatureBytes]) << 8
						| 0x0FF & this.temperatureLog[sampleNum * this.temperatureBytes + 1];
			} else {
				i = 0x0FF & this.temperatureLog[sampleNum * this.temperatureBytes];
			}
		} else if (channel == DATA_CHANNEL) {
			if (this.dataBytes == 2) {
				i = (0x0FF & this.dataLog[sampleNum * this.dataBytes]) << 8
						| 0x0FF & this.dataLog[sampleNum * this.dataBytes + 1];
			} else {
				i = 0x0FF & this.dataLog[sampleNum * this.dataBytes];
			}
		} else {
			throw new IllegalArgumentException("Invalid Channel");
		}

		return i;
	}

	/**
	 * Returns the time, in milliseconds, that each sample was taken by the current
	 * mission.
	 *
	 * @param channel   the mission channel, between <code>0</code> and
	 *                  <code>(getNumberOfMissionChannels()-1)</code>
	 * @param sampleNum the sample number to return, between <code>0</code> and
	 *                  <code>(getMissionSampleCount(channel)-1)</code>
	 * @return the sample's timestamp, in milliseconds
	 */
	@Override
	public long getMissionSampleTimeStamp(int channel, int sampleNum) throws OneWireException, OneWireIOException {
		if (!this.isMissionLoaded) {
			throw new OneWireException("Must load mission results first.");
		}

		var delta = ((long) this.timeOffset + (long) sampleNum) * this.sampleRate;
		return delta * 1000L + this.missionTimeStamp;
	}

	/**
	 * Returns <code>true</code> if a mission is currently running.
	 *
	 * @return <code>true</code> if a mission is currently running.
	 */
	@Override
	public boolean isMissionRunning() throws OneWireException, OneWireIOException {
		return this.getFlag(GENERAL_STATUS_REGISTER, GSR_BIT_MISSION_IN_PROGRESS);
	}

	/**
	 * Returns <code>true</code> if a rollover is enabled.
	 *
	 * @return <code>true</code> if a rollover is enabled.
	 */
	@Override
	public boolean isMissionRolloverEnabled() throws OneWireException, OneWireIOException {
		if (this.isMissionLoaded) {
			return this.getFlag(MISSION_CONTROL_REGISTER, MCR_BIT_ENABLE_ROLLOVER, this.missionRegister);
		}
		return this.getFlag(MISSION_CONTROL_REGISTER, MCR_BIT_ENABLE_ROLLOVER);
	}

	/**
	 * Returns <code>true</code> if a mission has rolled over.
	 *
	 * @return <code>true</code> if a mission has rolled over.
	 */
	@Override
	public boolean hasMissionRolloverOccurred() throws OneWireException, OneWireIOException {
		if (!this.isMissionLoaded) {
			throw new OneWireException("Must load mission results first.");
		}

		return this.rolledOver;
	}

	/**
	 * Clears the mission results and erases the log memory from this missioning
	 * device.
	 */
	@Override
	public void clearMissionResults() throws OneWireException, OneWireIOException {
		this.clearMemory();
		this.isMissionLoaded = false;
	}

	/**
	 * Returns the time, in milliseconds, that the mission began.
	 *
	 * @param channel the mission channel, between <code>0</code> and
	 *                <code>(getNumberOfMissionChannels()-1)</code>
	 * @return time, in milliseconds, that the mission began
	 */
	@Override
	public long getMissionTimeStamp(int channel) throws OneWireException, OneWireIOException {
		if (!this.isMissionLoaded) {
			throw new OneWireException("Must load mission results first.");
		}

		return this.missionTimeStamp;
	}

	/**
	 * Returns the amount of time, in milliseconds, before the first sample
	 * occurred. If rollover disabled, or datalog didn't fill up, this will be 0.
	 *
	 * @param channel the mission channel, between <code>0</code> and
	 *                <code>(getNumberOfMissionChannels()-1)</code>
	 * @return time, in milliseconds, before first sample occurred
	 */
	@Override
	public long getFirstSampleOffset(int channel) throws OneWireException, OneWireIOException {
		if (!this.isMissionLoaded) {
			throw new OneWireException("Must load mission results first.");
		}

		return this.timeOffset * this.sampleRate * 1000L;
	}

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	// - Mission Resolutions
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * Returns all available resolutions for the specified mission channel.
	 *
	 * @param channel the mission channel, between <code>0</code> and
	 *                <code>(getNumberOfMissionChannels()-1)</code>
	 * @return all available resolutions for the specified mission channel.
	 */
	@Override
	public double[] getMissionResolutions(int channel) throws OneWireException, OneWireIOException {
		if (channel == TEMPERATURE_CHANNEL) {
			return new double[] { temperatureResolutions[0], temperatureResolutions[1] };
		}
		if (channel == DATA_CHANNEL) {
			if (this.hasHumiditySensor && !this.adForceResults) {
				return new double[] { humidityResolutions[0], humidityResolutions[1] };
			} else if (this.adForceResults) {
				return new double[] { dataResolutions[0], dataResolutions[1] };
			} else {
				return new double[] { 8, 16 };
			}
		} else {
			throw new IllegalArgumentException("Invalid Channel");
		}
	}

	/**
	 * Returns the currently selected resolution for the specified channel.
	 *
	 * @param channel the mission channel, between <code>0</code> and
	 *                <code>(getNumberOfMissionChannels()-1)</code>
	 * @return the currently selected resolution for the specified channel.
	 */
	@Override
	public double getMissionResolution(int channel) throws OneWireException, OneWireIOException {
		if (!this.isMissionLoaded) {
			throw new OneWireException("Must load mission results first.");
		}

		var resolution = 0D;
		if (channel == TEMPERATURE_CHANNEL) {
			if (this.getFlag(MISSION_CONTROL_REGISTER, MCR_BIT_TEMPERATURE_RESOLUTION, this.missionRegister)) {
				resolution = temperatureResolutions[1];
			} else {
				resolution = temperatureResolutions[0];
			}
		} else if (channel == DATA_CHANNEL) {
			if (this.getFlag(MISSION_CONTROL_REGISTER, MCR_BIT_DATA_RESOLUTION, this.missionRegister)) {
				if (this.hasHumiditySensor && !this.adForceResults) {
					resolution = humidityResolutions[1];
				} else if (this.adForceResults) {
					resolution = dataResolutions[1];
				} else {
					resolution = 16;
				}
			} else if (this.hasHumiditySensor && !this.adForceResults) {
				resolution = humidityResolutions[0];
			} else if (this.adForceResults) {
				resolution = dataResolutions[0];
			} else {
				resolution = 8;
			}
		} else {
			throw new IllegalArgumentException("Invalid Channel");
		}
		return resolution;
	}

	/**
	 * Sets the selected resolution for the specified channel.
	 *
	 * @param channel    the mission channel, between <code>0</code> and
	 *                   <code>(getNumberOfMissionChannels()-1)</code>
	 * @param resolution the new resolution for the specified channel.
	 */
	@Override
	public void setMissionResolution(int channel, double resolution) throws OneWireException, OneWireIOException {
		if (!this.isMissionLoaded) {
			this.missionRegister = this.readDevice();
		}

		if (channel == TEMPERATURE_CHANNEL) {
			this.setFlag(MISSION_CONTROL_REGISTER, MCR_BIT_TEMPERATURE_RESOLUTION,
					resolution == temperatureResolutions[1], this.missionRegister);
		} else if (channel == DATA_CHANNEL) {
			if (this.hasHumiditySensor && !this.adForceResults) {
				this.setFlag(MISSION_CONTROL_REGISTER, MCR_BIT_DATA_RESOLUTION, resolution == humidityResolutions[1],
						this.missionRegister);
			} else if (this.adForceResults) {
				this.setFlag(MISSION_CONTROL_REGISTER, MCR_BIT_DATA_RESOLUTION, resolution == dataResolutions[1],
						this.missionRegister);
			} else {
				this.setFlag(MISSION_CONTROL_REGISTER, MCR_BIT_DATA_RESOLUTION, resolution == 16, this.missionRegister);
			}
		} else {
			throw new IllegalArgumentException("Invalid Channel");
		}

		this.writeDevice(this.missionRegister);
	}

	/**
	 * Enables/Disables the usage of calibration registers. Only applies to the
	 * DS1923 configuration. The calibration depends on an average error at 3 known
	 * reference points. This average error is written to 3 registers on the DS1922.
	 * The container use these values to calibrate the recorded humidity values and
	 * improve the accuracy of the device. This method allows you to turn off
	 * calibration so that you may download the actual data recorded to the device's
	 * memory and perform a manual calibration.
	 *
	 * @param use if <code>true</code>, all humidity values read from device will be
	 *            calibrated.
	 *
	 */
	public void setTemperatureCalibrationRegisterUsage(boolean use) {
		this.useTempCalibrationRegisters = use;
	}

	/**
	 * Enables/Disables the usage of the humidity calibration registers. Only
	 * applies to the DS1923 configuration. The calibration depends on an average
	 * error at 3 known reference points. This average error is written to 3
	 * registers on the DS1922. The container use these values to calibrate the
	 * recorded humidity values and improve the accuracy of the device. This method
	 * allows you to turn off calibration so that you may download the actual data
	 * recorded to the device's memory and perform a manual calibration.
	 *
	 * @param use if <code>true</code>, all humidity values read from device will be
	 *            calibrated.
	 */
	public void setHumidityCalibrationRegisterUsage(boolean use) {
		this.useHumdCalibrationRegisters = use;
	}

	/**
	 * Enables/Disables the usage of temperature compensation. Only applies to the
	 * DS1923 configuration. The temperature compensation adjusts the humidity
	 * values based on the known effects of temperature on the humidity sensor. If
	 * this is a joint humidity and temperature mission, the temperature values used
	 * could (should?) come from the temperature log itself. If, however, there is
	 * no temperature log the default temperature value can be set for the mission
	 * using the <code>setDefaultTemperatureCompensationValue</code> method.
	 *
	 * @param use if <code>true</code>, all humidity values read from device will be
	 *            compensated for temperature.
	 *
	 * @see #setDefaultTemperatureCompensationValue
	 */
	public void setTemperatureCompensationUsage(boolean use) {
		this.useTemperatureCompensation = use;
	}

	/**
	 * Sets the default temperature value for temperature compensation. This value
	 * will be used if there is no temperature log data or if the
	 * <code>override</code> parameter is true.
	 *
	 * @param temperatureValue the default temperature value for temperature
	 *                         compensation.
	 * @param override         if <code>true</code>, the default temperature value
	 *                         will always be used (instead of the temperature log
	 *                         data).
	 *
	 * @see #setDefaultTemperatureCompensationValue
	 */
	public void setDefaultTemperatureCompensationValue(double temperatureValue, boolean override) {
		this.defaultTempCompensationValue = temperatureValue;
		this.overrideTemperatureLog = override;
	}

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	// - Mission Alarms
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * Indicates whether or not the specified channel of this missioning device has
	 * mission alarm capabilities.
	 *
	 * @param channel the mission channel, between <code>0</code> and
	 *                <code>(getNumberOfMissionChannels()-1)</code>
	 * @return true if the device has mission alarms for the specified channel.
	 */
	@Override
	public boolean hasMissionAlarms(int channel) {
		return true;
	}

	/**
	 * Returns true if the specified channel's alarm value of the specified type has
	 * been triggered during the mission.
	 *
	 * @param channel   the mission channel, between <code>0</code> and
	 *                  <code>(getNumberOfMissionChannels()-1)</code>
	 * @param alarmType valid value: <code>ALARM_HIGH</code> or
	 *                  <code>ALARM_LOW</code>
	 * @return true if the alarm was triggered.
	 */
	@Override
	public boolean hasMissionAlarmed(int channel, int alarmType) throws OneWireException, OneWireIOException {
		if (!this.isMissionLoaded) {
			throw new OneWireException("Must load mission results first.");
		}

		if (channel == TEMPERATURE_CHANNEL) {
			if (alarmType == MissionContainer.ALARM_HIGH) {
				return this.getFlag(ALARM_STATUS_REGISTER, ASR_BIT_TEMPERATURE_HIGH_ALARM, this.missionRegister);
			} else {
				return this.getFlag(ALARM_STATUS_REGISTER, ASR_BIT_TEMPERATURE_LOW_ALARM, this.missionRegister);
			}
		}
		if (channel == DATA_CHANNEL) {
			if (alarmType == MissionContainer.ALARM_HIGH) {
				return this.getFlag(ALARM_STATUS_REGISTER, ASR_BIT_DATA_HIGH_ALARM, this.missionRegister);
			} else {
				return this.getFlag(ALARM_STATUS_REGISTER, ASR_BIT_DATA_LOW_ALARM, this.missionRegister);
			}
		} else {
			throw new IllegalArgumentException("Invalid Channel");
		}
	}

	/**
	 * Returns true if the alarm of the specified type has been enabled for the
	 * specified channel.
	 *
	 * @param channel   the mission channel, between <code>0</code> and
	 *                  <code>(getNumberOfMissionChannels()-1)</code>
	 * @param alarmType valid value: <code>ALARM_HIGH</code> or
	 *                  <code>ALARM_LOW</code>
	 * @return true if the alarm of the specified type has been enabled for the
	 *         specified channel.
	 */
	@Override
	public boolean getMissionAlarmEnable(int channel, int alarmType) throws OneWireException, OneWireIOException {
		if (!this.isMissionLoaded) {
			throw new OneWireException("Must load mission results first.");
		}

		if (channel == TEMPERATURE_CHANNEL) {
			if (alarmType == MissionContainer.ALARM_HIGH) {
				return this.getFlag(TEMPERATURE_CONTROL_REGISTER, TCR_BIT_ENABLE_TEMPERATURE_HIGH_ALARM,
						this.missionRegister);
			} else {
				return this.getFlag(TEMPERATURE_CONTROL_REGISTER, TCR_BIT_ENABLE_TEMPERATURE_LOW_ALARM,
						this.missionRegister);
			}
		}
		if (channel == DATA_CHANNEL) {
			if (alarmType == MissionContainer.ALARM_HIGH) {
				return this.getFlag(DATA_CONTROL_REGISTER, DCR_BIT_ENABLE_DATA_HIGH_ALARM, this.missionRegister);
			} else {
				return this.getFlag(DATA_CONTROL_REGISTER, DCR_BIT_ENABLE_DATA_LOW_ALARM, this.missionRegister);
			}
		} else {
			throw new IllegalArgumentException("Invalid Channel");
		}
	}

	/**
	 * Enables/disables the alarm of the specified type for the specified channel
	 *
	 * @param channel   the mission channel, between <code>0</code> and
	 *                  <code>(getNumberOfMissionChannels()-1)</code>
	 * @param alarmType valid value: <code>ALARM_HIGH</code> or
	 *                  <code>ALARM_LOW</code>
	 * @param enable    if true, alarm is enabled.
	 */
	@Override
	public void setMissionAlarmEnable(int channel, int alarmType, boolean enable)
			throws OneWireException, OneWireIOException {
		if (!this.isMissionLoaded) {
			this.missionRegister = this.readDevice();
		}

//      System.out.println("setMissionAlarmEnable: channel=" + channel +", alarmType=" + alarmType + ", enable=" + enable);
//      System.out.println("setMissionAlarmEnable: before state=" + Convert.toHexString(missionRegister));
		if (channel == TEMPERATURE_CHANNEL) {
			if (alarmType == MissionContainer.ALARM_HIGH) {
				this.setFlag(TEMPERATURE_CONTROL_REGISTER, TCR_BIT_ENABLE_TEMPERATURE_HIGH_ALARM, enable,
						this.missionRegister);
			} else {
				this.setFlag(TEMPERATURE_CONTROL_REGISTER, TCR_BIT_ENABLE_TEMPERATURE_LOW_ALARM, enable,
						this.missionRegister);
			}
		} else if (channel == DATA_CHANNEL) {
			if (alarmType == MissionContainer.ALARM_HIGH) {
				this.setFlag(DATA_CONTROL_REGISTER, DCR_BIT_ENABLE_DATA_HIGH_ALARM, enable, this.missionRegister);
			} else {
				this.setFlag(DATA_CONTROL_REGISTER, DCR_BIT_ENABLE_DATA_LOW_ALARM, enable, this.missionRegister);
			}
		} else {
			throw new IllegalArgumentException("Invalid Channel");
		}
//      System.out.println("setMissionAlarmEnable: after  state=" + Convert.toHexString(missionRegister));
		this.writeDevice(this.missionRegister);
	}

	/**
	 * Returns the threshold value which will trigger the alarm of the specified
	 * type on the specified channel.
	 *
	 * @param channel   the mission channel, between <code>0</code> and
	 *                  <code>(getNumberOfMissionChannels()-1)</code>
	 * @param alarmType valid value: <code>ALARM_HIGH</code> or
	 *                  <code>ALARM_LOW</code>
	 * @return the threshold value which will trigger the alarm
	 */
	@Override
	public double getMissionAlarm(int channel, int alarmType) throws OneWireException, OneWireIOException {
		if (!this.isMissionLoaded) {
			throw new OneWireException("Must load mission results first.");
		}

		var th = 0D;
		if (channel == TEMPERATURE_CHANNEL) {
			if (alarmType == MissionContainer.ALARM_HIGH) {
				th = this.decodeTemperature(this.missionRegister, TEMPERATURE_HIGH_ALARM_THRESHOLD & 0x3F, 1, false);
			} else {
				th = this.decodeTemperature(this.missionRegister, TEMPERATURE_LOW_ALARM_THRESHOLD & 0x3F, 1, false);
			}
		} else if (channel == DATA_CHANNEL) {
			if (alarmType == MissionContainer.ALARM_HIGH) {
				if (this.hasHumiditySensor && !this.adForceResults) {
					th = this.decodeHumidity(this.missionRegister, DATA_HIGH_ALARM_THRESHOLD & 0x3F, 1, false);
				} else if (this.adForceResults) {
					th = this.getADVoltage(this.missionRegister, DATA_HIGH_ALARM_THRESHOLD & 0x3F, 1, false);
				} else {
					th = 0x0FF & this.missionRegister[DATA_HIGH_ALARM_THRESHOLD & 0x3F];
				}
			} else if (this.hasHumiditySensor && !this.adForceResults) {
				th = this.decodeHumidity(this.missionRegister, DATA_LOW_ALARM_THRESHOLD & 0x3F, 1, false);
			} else if (this.adForceResults) {
				th = this.getADVoltage(this.missionRegister, DATA_LOW_ALARM_THRESHOLD & 0x3F, 1, false);
			} else {
				th = 0x0FF & this.missionRegister[DATA_LOW_ALARM_THRESHOLD & 0x3F];
			}
		} else {
			throw new IllegalArgumentException("Invalid Channel");
		}
		return th;
	}

	/**
	 * Sets the threshold value which will trigger the alarm of the specified type
	 * on the specified channel.
	 *
	 * @param channel   the mission channel, between <code>0</code> and
	 *                  <code>(getNumberOfMissionChannels()-1)</code>
	 * @param alarmType valid value: <code>ALARM_HIGH</code> or
	 *                  <code>ALARM_LOW</code>
	 * @param threshold the threshold value which will trigger the alarm
	 */
	@Override
	public void setMissionAlarm(int channel, int alarmType, double threshold)
			throws OneWireException, OneWireIOException {
		if (!this.isMissionLoaded) {
			this.missionRegister = this.readDevice();
		}

//      System.out.println("setMissionAlarm: channel=" + channel +", alarmType=" + alarmType + ", threshold=" + threshold);
//      System.out.println("setMissionAlarm: before state=" + Convert.toHexString(missionRegister));

		if (channel == TEMPERATURE_CHANNEL) {
			if (alarmType == MissionContainer.ALARM_HIGH) {
				this.encodeTemperature(threshold, this.missionRegister, TEMPERATURE_HIGH_ALARM_THRESHOLD & 0x3F, 1,
						false);
			} else {
				this.encodeTemperature(threshold, this.missionRegister, TEMPERATURE_LOW_ALARM_THRESHOLD & 0x3F, 1,
						false);
			}
		} else if (channel == DATA_CHANNEL) {
			if (alarmType == MissionContainer.ALARM_HIGH) {
				if (this.hasHumiditySensor && !this.adForceResults) {
					this.encodeHumidity(threshold, this.missionRegister, DATA_HIGH_ALARM_THRESHOLD & 0x3F, 1, false);
				} else if (this.adForceResults) {
					this.setADVoltage(threshold, this.missionRegister, DATA_HIGH_ALARM_THRESHOLD & 0x3F, 1, false);
				} else {
					this.missionRegister[DATA_HIGH_ALARM_THRESHOLD & 0x3F] = (byte) threshold;
				}
			} else if (this.hasHumiditySensor && !this.adForceResults) {
				this.encodeHumidity(threshold, this.missionRegister, DATA_LOW_ALARM_THRESHOLD & 0x3F, 1, false);
			} else if (this.adForceResults) {
				this.setADVoltage(threshold, this.missionRegister, DATA_LOW_ALARM_THRESHOLD & 0x3F, 1, false);
			} else {
				this.missionRegister[DATA_LOW_ALARM_THRESHOLD & 0x3F] = (byte) threshold;
			}
		} else {
			throw new IllegalArgumentException("Invalid Channel");
		}
//      System.out.println("setMissionAlarm: after state=" + Convert.toHexString(missionRegister));
		this.writeDevice(this.missionRegister);
	}

// *****************************************************************************
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// Temperature Interface Functions
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// *****************************************************************************

	/**
	 * Checks to see if this temperature measuring device has high/low trip alarms.
	 *
	 * @return <code>true</code> if this <code>TemperatureContainer</code> has
	 *         high/low trip alarms
	 *
	 * @see #getTemperatureAlarm
	 * @see #setTemperatureAlarm
	 */
	@Override
	public boolean hasTemperatureAlarms() {
		return true;
	}

	/**
	 * Checks to see if this device has selectable temperature resolution.
	 *
	 * @return <code>true</code> if this <code>TemperatureContainer</code> has
	 *         selectable temperature resolution
	 *
	 * @see #getTemperatureResolution
	 * @see #getTemperatureResolutions
	 * @see #setTemperatureResolution
	 */
	@Override
	public boolean hasSelectableTemperatureResolution() {
		return false;
	}

	/**
	 * Get an array of available temperature resolutions in Celsius.
	 *
	 * @return byte array of available temperature resolutions in Celsius with
	 *         minimum resolution as the first element and maximum resolution as the
	 *         last element
	 *
	 * @see #hasSelectableTemperatureResolution
	 * @see #getTemperatureResolution
	 * @see #setTemperatureResolution
	 */
	@Override
	public double[] getTemperatureResolutions() {
		var d = new double[1];

		d[0] = temperatureResolutions[1];

		return d;
	}

	/**
	 * Gets the temperature alarm resolution in Celsius.
	 *
	 * @return temperature alarm resolution in Celsius for this 1-wire device
	 *
	 * @see #hasTemperatureAlarms
	 * @see #getTemperatureAlarm
	 * @see #setTemperatureAlarm
	 *
	 */
	@Override
	public double getTemperatureAlarmResolution() {
		return temperatureResolutions[0];
	}

	/**
	 * Gets the maximum temperature in Celsius.
	 *
	 * @return maximum temperature in Celsius for this 1-wire device
	 *
	 * @see #getMinTemperature()
	 */
	@Override
	public double getMaxTemperature() {
		return this.temperatureRangeLow + this.temperatureRangeWidth;
	}

	/**
	 * Gets the minimum temperature in Celsius.
	 *
	 * @return minimum temperature in Celsius for this 1-wire device
	 *
	 * @see #getMaxTemperature()
	 */
	@Override
	public double getMinTemperature() {
		return this.temperatureRangeLow;
	}

	/**
	 * Performs a temperature conversion. Use the <code>state</code> information to
	 * calculate the conversion time.
	 *
	 * @param state byte array with device state information
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from a 1-Wire device. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'. In the case of
	 *                            the DS1922 Thermocron, this could also be due to a
	 *                            currently running mission.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 */
	@Override
	public void doTemperatureConvert(byte[] state) throws OneWireIOException, OneWireException {
		/* check for mission in progress */
		if (this.getFlag(GENERAL_STATUS_REGISTER, GSR_BIT_MISSION_IN_PROGRESS, state)) {
			throw new OneWireIOException("OneWireContainer41-Cant force " + "temperature read during a mission.");
		}
		/* check that the RTC is running */
		if (!this.getFlag(RTC_CONTROL_REGISTER, RCR_BIT_ENABLE_OSCILLATOR, state)) {
			throw new OneWireIOException(
					"OneWireContainer41-Cant force " + "temperature conversion if the oscillator is not enabled");
		}

		/* get the temperature */
		if (this.doSpeedEnable) {
			this.doSpeed(); // we aren't worried about how long this takes...we're sleeping for 750 ms!
		}

		this.adapter.reset();

		if (!this.adapter.select(this.address)) {
			throw new OneWireException("OneWireContainer41-Device not found!");
		}
		// perform the temperature conversion
		byte[] buffer = { FORCED_CONVERSION, (byte) 0xFF };
		this.adapter.dataBlock(buffer, 0, 2);

		msWait(750);

		// grab the temperature
		state[LAST_TEMPERATURE_CONVERSION_LSB & 0x3F] = this.readByte(LAST_TEMPERATURE_CONVERSION_LSB);
		state[LAST_TEMPERATURE_CONVERSION_MSB & 0x3F] = this.readByte(LAST_TEMPERATURE_CONVERSION_MSB);
	}

	/**
	 * Gets the temperature value in Celsius from the <code>state</code> data
	 * retrieved from the <code>readDevice()</code> method.
	 *
	 * @param state byte array with device state information
	 *
	 * @return temperature in Celsius from the last
	 *         <code>doTemperatureConvert()</code>
	 */
	@Override
	public double getTemperature(byte[] state) {
		var val = this.decodeTemperature(state, LAST_TEMPERATURE_CONVERSION_LSB & 0x3F, 2, false);
		if (this.useTempCalibrationRegisters && this.partNumber != PART_NUMBER_DS1922E) // DS1922E does not use
																						// calibration
		// registers
		{
			var valsq = val * val;
			var error = this.tempCoeffA * valsq + this.tempCoeffB * val + this.tempCoeffC;
			val = val - error;
		}
		return val;
	}

	/**
	 * Gets the specified temperature alarm value in Celsius from the
	 * <code>state</code> data retrieved from the <code>readDevice()</code> method.
	 *
	 * @param alarmType valid value: <code>ALARM_HIGH</code> or
	 *                  <code>ALARM_LOW</code>
	 * @param state     byte array with device state information
	 *
	 * @return temperature alarm trip values in Celsius for this 1-wire device
	 *
	 * @see #hasTemperatureAlarms
	 * @see #setTemperatureAlarm
	 */
	@Override
	public double getTemperatureAlarm(int alarmType, byte[] state) {
		var th = 0D;
		if (alarmType == TemperatureContainer.ALARM_HIGH) {
			th = this.decodeTemperature(state, TEMPERATURE_HIGH_ALARM_THRESHOLD & 0x3F, 1, false);
		} else {
			th = this.decodeTemperature(state, TEMPERATURE_LOW_ALARM_THRESHOLD & 0x3F, 1, false);
		}
		if (this.useTempCalibrationRegisters && this.partNumber != PART_NUMBER_DS1922E) // DS1922E does not use
																						// calibration
		// registers
		{
			var thsq = th * th;
			var error = this.tempCoeffA * thsq + this.tempCoeffB * th + this.tempCoeffC;
			th = th - error;
		}
		return th;
	}

	/**
	 * Gets the current temperature resolution in Celsius from the
	 * <code>state</code> data retrieved from the <code>readDevice()</code> method.
	 *
	 * @param state byte array with device state information
	 *
	 * @return temperature resolution in Celsius for this 1-wire device
	 *
	 * @see #hasSelectableTemperatureResolution
	 * @see #getTemperatureResolutions
	 * @see #setTemperatureResolution
	 */
	@Override
	public double getTemperatureResolution(byte[] state) {
		return temperatureResolutions[1];
	}

	/**
	 * Sets the temperature alarm value in Celsius in the provided
	 * <code>state</code> data. Use the method <code>writeDevice()</code> with this
	 * data to finalize the change to the device.
	 *
	 * @param alarmType  valid value: <code>ALARM_HIGH</code> or
	 *                   <code>ALARM_LOW</code>
	 * @param alarmValue alarm trip value in Celsius
	 * @param state      byte array with device state information
	 *
	 * @see #hasTemperatureAlarms
	 * @see #getTemperatureAlarm
	 */
	@Override
	public void setTemperatureAlarm(int alarmType, double alarmValue, byte[] state) {
		if (this.useTempCalibrationRegisters && this.partNumber != PART_NUMBER_DS1922E) // DS1922E does not use
																						// calibration
		// registers
		{
			alarmValue = (1 - this.tempCoeffB - Math.sqrt((this.tempCoeffB - 1) * (this.tempCoeffB - 1)
					- 4 * this.tempCoeffA * (this.tempCoeffC + alarmValue))) / (2 * this.tempCoeffA);
		}

		if (alarmType == TemperatureContainer.ALARM_HIGH) {
			this.encodeTemperature(alarmValue, state, TEMPERATURE_HIGH_ALARM_THRESHOLD & 0x3F, 1, false);
		} else {
			this.encodeTemperature(alarmValue, state, TEMPERATURE_LOW_ALARM_THRESHOLD & 0x3F, 1, false);
		}
	}

	/**
	 * Sets the current temperature resolution in Celsius in the provided
	 * <code>state</code> data. Use the method <code>writeDevice()</code> with this
	 * data to finalize the change to the device.
	 *
	 * @param resolution temperature resolution in Celsius
	 * @param state      byte array with device state information
	 *
	 * @throws OneWireException if the device does not support selectable
	 *                          temperature resolution
	 *
	 * @see #hasSelectableTemperatureResolution
	 * @see #getTemperatureResolution
	 * @see #getTemperatureResolutions
	 */
	@Override
	public void setTemperatureResolution(double resolution, byte[] state) throws OneWireException {
		throw new OneWireException("Selectable Temperature Resolution Not Supported");
	}

// *****************************************************************************
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// Humidity Interface Functions
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// *****************************************************************************

	/**
	 * Checks to see if humidity value given is a 'relative' humidity value.
	 *
	 * @return <code>true</code> if this <code>HumidityContainer</code> provides a
	 *         relative humidity reading
	 *
	 * @see #getHumidityResolution
	 * @see #getHumidityResolutions
	 * @see #setHumidityResolution
	 */
	@Override
	public boolean isRelative() {
		return true;
	}

	/**
	 * Checks to see if this Humidity measuring device has high/low trip alarms.
	 *
	 * @return <code>true</code> if this <code>HumidityContainer</code> has high/low
	 *         trip alarms
	 *
	 * @see #getHumidityAlarm
	 * @see #setHumidityAlarm
	 */
	@Override
	public boolean hasHumidityAlarms() {
		return true;
	}

	/**
	 * Checks to see if this device has selectable Humidity resolution.
	 *
	 * @return <code>true</code> if this <code>HumidityContainer</code> has
	 *         selectable Humidity resolution
	 *
	 * @see #getHumidityResolution
	 * @see #getHumidityResolutions
	 * @see #setHumidityResolution
	 */
	@Override
	public boolean hasSelectableHumidityResolution() {
		return false;
	}

	/**
	 * Get an array of available Humidity resolutions in percent humidity (0 to
	 * 100).
	 *
	 * @return byte array of available Humidity resolutions in percent with minimum
	 *         resolution as the first element and maximum resolution as the last
	 *         element.
	 *
	 * @see #hasSelectableHumidityResolution
	 * @see #getHumidityResolution
	 * @see #setHumidityResolution
	 */
	@Override
	public double[] getHumidityResolutions() {
		var d = new double[1];

		d[0] = humidityResolutions[1];

		return d;
	}

	/**
	 * Gets the Humidity alarm resolution in percent.
	 *
	 * @return Humidity alarm resolution in percent for this 1-wire device
	 *
	 * @throws OneWireException Device does not support Humidity alarms
	 *
	 * @see #hasHumidityAlarms
	 * @see #getHumidityAlarm
	 * @see #setHumidityAlarm
	 *
	 */
	@Override
	public double getHumidityAlarmResolution() throws OneWireException {
		return humidityResolutions[0];
	}

	// --------
	// -------- Humidity I/O Methods
	// --------

	/**
	 * Performs a Humidity conversion.
	 *
	 * @param state byte array with device state information
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as reading an
	 *                            incorrect CRC from a 1-Wire device. This could be
	 *                            caused by a physical interruption in the 1-Wire
	 *                            Network due to shorts or a newly arriving 1-Wire
	 *                            device issuing a 'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 */
	@Override
	public void doHumidityConvert(byte[] state) throws OneWireIOException, OneWireException {
		/* check for mission in progress */
		if (this.getFlag(GENERAL_STATUS_REGISTER, GSR_BIT_MISSION_IN_PROGRESS, state)) {
			throw new OneWireIOException("OneWireContainer41-Cant force " + "Humidity read during a mission.");
		}

		/* check that the RTC is running */
		if (!this.getFlag(RTC_CONTROL_REGISTER, RCR_BIT_ENABLE_OSCILLATOR, state)) {
			throw new OneWireIOException(
					"OneWireContainer41-Cant force " + "Humidity conversion if the oscillator is not enabled");
		}

		/* get the temperature */
		if (this.doSpeedEnable) {
			this.doSpeed(); // we aren't worried about how long this takes...we're sleeping for 750 ms!
		}

		this.adapter.reset();

		if (!this.adapter.select(this.address)) {
			throw new OneWireException("OneWireContainer41-Device not found!");
		}
		// perform the temperature conversion
		byte[] buffer = { FORCED_CONVERSION, (byte) 0xFF };
		this.adapter.dataBlock(buffer, 0, 2);

		msWait(750);

		// grab the temperature
		state[LAST_DATA_CONVERSION_LSB & 0x3F] = this.readByte(LAST_DATA_CONVERSION_LSB);
		state[LAST_DATA_CONVERSION_MSB & 0x3F] = this.readByte(LAST_DATA_CONVERSION_MSB);
	}

	// --------
	// -------- Humidity 'get' Methods
	// --------

	/**
	 * Gets the humidity expressed as a percent value (0.0 to 100.0) of humidity.
	 *
	 * @param state byte array with device state information
	 * @return humidity expressed as a percent
	 *
	 * @see #hasSelectableHumidityResolution
	 * @see #getHumidityResolution
	 * @see #setHumidityResolution
	 */
	@Override
	public double getHumidity(byte[] state) {
		var val = this.decodeHumidity(state, LAST_DATA_CONVERSION_LSB & 0x3F, 2, false);
		if (this.useTemperatureCompensation) {
			var T = this.decodeTemperature(state, LAST_TEMPERATURE_CONVERSION_LSB & 0x3F, 2, false);
			var gamma = T > 15 ? 0.00001 : -0.00005;
			T -= 25;
			val = (val * 0.0307 + .0035 * T - 0.000043 * T * T) / (0.0307 + gamma * T - 0.000002 * T * T);
		}
		return val;
	}

	/**
	 * Gets the current Humidity resolution in percent from the <code>state</code>
	 * data retrieved from the <code>readDevice()</code> method.
	 *
	 * @param state byte array with device state information
	 *
	 * @return Humidity resolution in percent for this 1-wire device
	 *
	 * @see #hasSelectableHumidityResolution
	 * @see #getHumidityResolutions
	 * @see #setHumidityResolution
	 */
	@Override
	public double getHumidityResolution(byte[] state) {
		return humidityResolutions[1];
	}

	/**
	 * Gets the specified Humidity alarm value in percent from the
	 * <code>state</code> data retrieved from the <code>readDevice()</code> method.
	 *
	 * @param alarmType valid value: <code>ALARM_HIGH</code> or
	 *                  <code>ALARM_LOW</code>
	 * @param state     byte array with device state information
	 *
	 * @return Humidity alarm trip values in percent for this 1-wire device
	 *
	 * @throws OneWireException Device does not support Humidity alarms
	 *
	 * @see #hasHumidityAlarms
	 * @see #setHumidityAlarm
	 */
	@Override
	public double getHumidityAlarm(int alarmType, byte[] state) throws OneWireException {
		double th;
		if (alarmType == HumidityContainer.ALARM_HIGH) {
			th = this.decodeHumidity(state, DATA_HIGH_ALARM_THRESHOLD & 0x3F, 1, false);
		} else {
			th = this.decodeHumidity(state, DATA_LOW_ALARM_THRESHOLD & 0x3F, 1, false);
		}
		return th;
	}

	// --------
	// -------- Humidity 'set' Methods
	// --------

	/**
	 * Sets the Humidity alarm value in percent in the provided <code>state</code>
	 * data. Use the method <code>writeDevice()</code> with this data to finalize
	 * the change to the device.
	 *
	 * @param alarmType  valid value: <code>ALARM_HIGH</code> or
	 *                   <code>ALARM_LOW</code>
	 * @param alarmValue alarm trip value in percent
	 * @param state      byte array with device state information
	 *
	 * @throws OneWireException Device does not support Humidity alarms
	 *
	 * @see #hasHumidityAlarms
	 * @see #getHumidityAlarm
	 */
	@Override
	public void setHumidityAlarm(int alarmType, double alarmValue, byte[] state) throws OneWireException {
		if (alarmType == HumidityContainer.ALARM_HIGH) {
			this.encodeHumidity(alarmValue, state, DATA_HIGH_ALARM_THRESHOLD & 0x3F, 1, false);
		} else {
			this.encodeHumidity(alarmValue, state, DATA_LOW_ALARM_THRESHOLD & 0x3F, 1, false);
		}
	}

	/**
	 * Sets the current Humidity resolution in percent in the provided
	 * <code>state</code> data. Use the method <code>writeDevice()</code> with this
	 * data to finalize the change to the device.
	 *
	 * @param resolution Humidity resolution in percent
	 * @param state      byte array with device state information
	 *
	 * @throws OneWireException Device does not support selectable Humidity
	 *                          resolution
	 *
	 * @see #hasSelectableHumidityResolution
	 * @see #getHumidityResolution
	 * @see #getHumidityResolutions
	 */
	@Override
	public void setHumidityResolution(double resolution, byte[] state) throws OneWireException {
		throw new OneWireException("Selectable Humidity Resolution Not Supported");
	}

// *****************************************************************************
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// A-to-D Interface Functions
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// *****************************************************************************
	/**
	 * Gets the number of channels supported by this A/D. Channel specific methods
	 * will use a channel number specified by an integer from [0 to
	 * (<code>getNumberADChannels()</code> - 1)].
	 *
	 * @return the number of channels
	 */
	@Override
	public int getNumberADChannels() {
		return 1;
	}

	/**
	 * Checks to see if this A/D measuring device has high/low alarms.
	 *
	 * @return true if this device has high/low trip alarms
	 */
	@Override
	public boolean hasADAlarms() {
		return true;
	}

	/**
	 * Gets an array of available ranges for the specified A/D channel.
	 *
	 * @param channel channel number in the range [0 to
	 *                (<code>getNumberADChannels()</code> - 1)]
	 *
	 * @return array indicating the available ranges starting from the largest range
	 *         to the smallest range
	 *
	 * @see #getNumberADChannels()
	 */
	@Override
	public double[] getADRanges(int channel) {
		return new double[] { 127 };
	}

	/**
	 * Gets an array of available resolutions based on the specified range on the
	 * specified A/D channel.
	 *
	 * @param channel channel number in the range [0 to
	 *                (<code>getNumberADChannels()</code> - 1)]
	 * @param range   A/D range setting from the <code>getADRanges(int)</code>
	 *                method
	 *
	 * @return array indicating the available resolutions on this
	 *         <code>channel</code> for this <code>range</code>
	 *
	 * @see #getNumberADChannels()
	 * @see #getADRanges(int)
	 */
	@Override
	public double[] getADResolutions(int channel, double range) {
		return new double[] { dataResolutions[1] };
	}

	/**
	 * Checks to see if this A/D supports doing multiple voltage conversions at the
	 * same time.
	 *
	 * @return true if the device can do multi-channel voltage reads
	 *
	 * @see #doADConvert(boolean[],byte[])
	 */
	@Override
	public boolean canADMultiChannelRead() {
		return true;
	}

	/**
	 * Performs a voltage conversion on one specified channel. Use the method
	 * <code>getADVoltage(int,byte[])</code> to read the result of this conversion,
	 * using the same <code>channel</code> argument as this method uses.
	 *
	 * @param channel channel number in the range [0 to
	 *                (<code>getNumberADChannels()</code> - 1)]
	 * @param state   current state of the device returned from
	 *                <code>readDevice()</code>
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as no 1-Wire
	 *                            device present. This could be caused by a physical
	 *                            interruption in the 1-Wire Network due to shorts
	 *                            or a newly arriving 1-Wire device issuing a
	 *                            'presence pulse'. This is usually a recoverable
	 *                            error.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter. This is usually a non-recoverable error.
	 *
	 * @see com.dalsemi.onewire.container.OneWireSensor#readDevice()
	 * @see #getADVoltage(int,byte[])
	 */
	@Override
	public void doADConvert(int channel, byte[] state) throws OneWireIOException, OneWireException {
		/* check for mission in progress */
		if (this.getFlag(GENERAL_STATUS_REGISTER, GSR_BIT_MISSION_IN_PROGRESS, state)) {
			throw new OneWireIOException("OneWireContainer41-Cant force " + "temperature read during a mission.");
		}

		/* check that the RTC is running */
		if (!this.getFlag(RTC_CONTROL_REGISTER, RCR_BIT_ENABLE_OSCILLATOR, state)) {
			throw new OneWireIOException(
					"OneWireContainer41-Cant force " + "A/D conversion if the oscillator is not enabled");
		}

		/* get the temperature */
		if (this.doSpeedEnable) {
			this.doSpeed(); // we aren't worried about how long this takes...we're sleeping for 750 ms!
		}

		this.adapter.reset();

		if (!this.adapter.select(this.address)) {
			throw new OneWireException("OneWireContainer41-Device not found!");
		}
		// perform the conversion
		byte[] buffer = { FORCED_CONVERSION, (byte) 0xFF };
		this.adapter.dataBlock(buffer, 0, 2);

		msWait(750);

		// grab the data
		state[LAST_DATA_CONVERSION_LSB & 0x3F] = this.readByte(LAST_DATA_CONVERSION_LSB);
		state[LAST_DATA_CONVERSION_MSB & 0x3F] = this.readByte(LAST_DATA_CONVERSION_MSB);
	}

	/**
	 * Performs voltage conversion on one or more specified channels. The method
	 * <code>getADVoltage(byte[])</code> can be used to read the result of the
	 * conversion(s). This A/D must support multi-channel read, reported by
	 * <code>canADMultiChannelRead()</code>, if more then 1 channel is specified.
	 *
	 * @param doConvert array of size <code>getNumberADChannels()</code>
	 *                  representing which channels should perform conversions
	 * @param state     current state of the device returned from
	 *                  <code>readDevice()</code>
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as no 1-Wire
	 *                            device present. This could be caused by a physical
	 *                            interruption in the 1-Wire Network due to shorts
	 *                            or a newly arriving 1-Wire device issuing a
	 *                            'presence pulse'. This is usually a recoverable
	 *                            error.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter. This is usually a non-recoverable error.
	 *
	 * @see com.dalsemi.onewire.container.OneWireSensor#readDevice()
	 * @see #getADVoltage(byte[])
	 * @see #canADMultiChannelRead()
	 */
	@Override
	public void doADConvert(boolean[] doConvert, byte[] state) throws OneWireIOException, OneWireException {
		this.doADConvert(DATA_CHANNEL, state);
	}

	/**
	 * Reads the value of the voltages after a
	 * <code>doADConvert(boolean[],byte[])</code> method call. This A/D device must
	 * support multi-channel reading, reported by
	 * <code>canADMultiChannelRead()</code>, if more than 1 channel conversion was
	 * attempted by <code>doADConvert()</code>.
	 *
	 * @param state current state of the device returned from
	 *              <code>readDevice()</code>
	 *
	 * @return array with the voltage values for all channels
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as no 1-Wire
	 *                            device present. This could be caused by a physical
	 *                            interruption in the 1-Wire Network due to shorts
	 *                            or a newly arriving 1-Wire device issuing a
	 *                            'presence pulse'. This is usually a recoverable
	 *                            error.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter. This is usually a non-recoverable error.
	 *
	 * @see #doADConvert(boolean[],byte[])
	 */
	@Override
	public double[] getADVoltage(byte[] state) throws OneWireIOException, OneWireException {
		return new double[] { this.getADVoltage(DATA_CHANNEL, state) };
	}

	/**
	 * Reads the value of the voltages after a <code>doADConvert(int,byte[])</code>
	 * method call. If more than one channel has been read it is more efficient to
	 * use the <code>getADVoltage(byte[])</code> method that returns all channel
	 * voltage values.
	 *
	 * @param channel channel number in the range [0 to
	 *                (<code>getNumberADChannels()</code> - 1)]
	 * @param state   current state of the device returned from
	 *                <code>readDevice()</code>
	 *
	 * @return the voltage value for the specified channel
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as no 1-Wire
	 *                            device present. This could be caused by a physical
	 *                            interruption in the 1-Wire Network due to shorts
	 *                            or a newly arriving 1-Wire device issuing a
	 *                            'presence pulse'. This is usually a recoverable
	 *                            error.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter. This is usually a non-recoverable error.
	 *
	 * @see #doADConvert(int,byte[])
	 * @see #getADVoltage(byte[])
	 */
	@Override
	public double getADVoltage(int channel, byte[] state) throws OneWireIOException, OneWireException {
		return this.getADVoltage(state, LAST_DATA_CONVERSION_LSB & 0x3F, 2, false);
	}

	/**
	 * Reads the value of the specified A/D alarm on the specified channel. Not all
	 * A/D devices have alarms. Check to see if this device has alarms first by
	 * calling the <code>hasADAlarms()</code> method.
	 *
	 * @param channel   channel number in the range [0 to
	 *                  (<code>getNumberADChannels()</code> - 1)]
	 * @param alarmType the desired alarm, <code>ALARM_HIGH</code> or
	 *                  <code>ALARM_LOW</code>
	 * @param state     current state of the device returned from
	 *                  <code>readDevice()</code>
	 *
	 * @return the alarm value in volts
	 *
	 * @throws OneWireException if this device does not have A/D alarms
	 *
	 * @see com.dalsemi.onewire.container.OneWireSensor#readDevice()
	 * @see #hasADAlarms()
	 */
	@Override
	public double getADAlarm(int channel, int alarmType, byte[] state) throws OneWireException {
		var th = 0D;
		if (alarmType == ADContainer.ALARM_HIGH) {
			th = this.getADVoltage(state, DATA_HIGH_ALARM_THRESHOLD & 0x3F, 1, false);
		} else {
			th = this.getADVoltage(state, DATA_LOW_ALARM_THRESHOLD & 0x3F, 1, false);
		}
		return th;
	}

	/**
	 * Checks to see if the specified alarm on the specified channel is enabled. Not
	 * all A/D devices have alarms. Check to see if this device has alarms first by
	 * calling the <code>hasADAlarms()</code> method.
	 *
	 * @param channel   channel number in the range [0 to
	 *                  (<code>getNumberADChannels()</code> - 1)]
	 * @param alarmType the desired alarm, <code>ALARM_HIGH</code> or
	 *                  <code>ALARM_LOW</code>
	 * @param state     current state of the device returned from
	 *                  <code>readDevice()</code>
	 *
	 * @return true if specified alarm is enabled
	 *
	 * @throws OneWireException if this device does not have A/D alarms
	 *
	 * @see com.dalsemi.onewire.container.OneWireSensor#readDevice()
	 * @see #hasADAlarms()
	 */
	@Override
	public boolean getADAlarmEnable(int channel, int alarmType, byte[] state) throws OneWireException {
		var b = false;
		if (alarmType == ADContainer.ALARM_HIGH) {
			b = this.getFlag(DATA_CONTROL_REGISTER, DCR_BIT_ENABLE_DATA_HIGH_ALARM, state);
		} else {
			b = this.getFlag(DATA_CONTROL_REGISTER, DCR_BIT_ENABLE_DATA_LOW_ALARM, state);
		}
		return b;
	}

	/**
	 * Checks the state of the specified alarm on the specified channel. Not all A/D
	 * devices have alarms. Check to see if this device has alarms first by calling
	 * the <code>hasADAlarms()</code> method.
	 *
	 * @param channel   channel number in the range [0 to
	 *                  (<code>getNumberADChannels()</code> - 1)]
	 * @param alarmType the desired alarm, <code>ALARM_HIGH</code> or
	 *                  <code>ALARM_LOW</code>
	 * @param state     current state of the device returned from
	 *                  <code>readDevice()</code>
	 *
	 * @return true if specified alarm occurred
	 *
	 * @throws OneWireException if this device does not have A/D alarms
	 *
	 * @see com.dalsemi.onewire.container.OneWireSensor#readDevice()
	 * @see #hasADAlarms()
	 * @see #getADAlarmEnable(int,int,byte[])
	 * @see #setADAlarmEnable(int,int,boolean,byte[])
	 */
	@Override
	public boolean hasADAlarmed(int channel, int alarmType, byte[] state) throws OneWireException {
		var b = false;
		if (alarmType == ADContainer.ALARM_HIGH) {
			b = this.getFlag(ALARM_STATUS_REGISTER, ASR_BIT_DATA_HIGH_ALARM, state);
		} else {
			b = this.getFlag(ALARM_STATUS_REGISTER, ASR_BIT_DATA_LOW_ALARM, state);
		}
		return b;
	}

	/**
	 * Returns the currently selected resolution for the specified channel. This
	 * device may not have selectable resolutions, though this method will return a
	 * valid value.
	 *
	 * @param channel channel number in the range [0 to
	 *                (<code>getNumberADChannels()</code> - 1)]
	 * @param state   current state of the device returned from
	 *                <code>readDevice()</code>
	 *
	 * @return the current resolution of <code>channel</code> in volts
	 *
	 * @see #getADResolutions(int,double)
	 * @see #setADResolution(int,double,byte[])
	 */
	@Override
	public double getADResolution(int channel, byte[] state) {
		return dataResolutions[1];
	}

	/**
	 * Returns the currently selected range for the specified channel. This device
	 * may not have selectable ranges, though this method will return a valid value.
	 *
	 * @param channel channel number in the range [0 to
	 *                (<code>getNumberADChannels()</code> - 1)]
	 * @param state   current state of the device returned from
	 *                <code>readDevice()</code>
	 *
	 * @return the input voltage range
	 *
	 * @see #getADRanges(int)
	 * @see #setADRange(int,double,byte[])
	 */
	@Override
	public double getADRange(int channel, byte[] state) {
		return 127;
	}

	/**
	 * Sets the voltage value of the specified alarm on the specified channel. The
	 * method <code>writeDevice()</code> must be called to finalize changes to the
	 * device. Note that multiple 'set' methods can be called before one call to
	 * <code>writeDevice()</code>. Also note that not all A/D devices have alarms.
	 * Check to see if this device has alarms first by calling the
	 * <code>hasADAlarms()</code> method.
	 *
	 * @param channel   channel number in the range [0 to
	 *                  (<code>getNumberADChannels()</code> - 1)]
	 * @param alarmType the desired alarm, <code>ALARM_HIGH</code> or
	 *                  <code>ALARM_LOW</code>
	 * @param alarm     new alarm value
	 * @param state     current state of the device returned from
	 *                  <code>readDevice()</code>
	 *
	 * @throws OneWireException if this device does not have A/D alarms
	 *
	 * @see com.dalsemi.onewire.container.OneWireSensor#writeDevice(byte[])
	 * @see #hasADAlarms()
	 * @see #getADAlarm(int,int,byte[])
	 * @see #getADAlarmEnable(int,int,byte[])
	 * @see #setADAlarmEnable(int,int,boolean,byte[])
	 * @see #hasADAlarmed(int,int,byte[])
	 */
	@Override
	public void setADAlarm(int channel, int alarmType, double alarm, byte[] state) throws OneWireException {
		if (alarmType == ADContainer.ALARM_HIGH) {
			this.setADVoltage(alarm, state, DATA_HIGH_ALARM_THRESHOLD & 0x3F, 1, false);
		} else {
			this.setADVoltage(alarm, state, DATA_LOW_ALARM_THRESHOLD & 0x3F, 1, false);
		}
	}

	/**
	 * Enables or disables the specified alarm on the specified channel. The method
	 * <code>writeDevice()</code> must be called to finalize changes to the device.
	 * Note that multiple 'set' methods can be called before one call to
	 * <code>writeDevice()</code>. Also note that not all A/D devices have alarms.
	 * Check to see if this device has alarms first by calling the
	 * <code>hasADAlarms()</code> method.
	 *
	 * @param channel     channel number in the range [0 to
	 *                    (<code>getNumberADChannels()</code> - 1)]
	 * @param alarmType   the desired alarm, <code>ALARM_HIGH</code> or
	 *                    <code>ALARM_LOW</code>
	 * @param alarmEnable true to enable the alarm, false to disable
	 * @param state       current state of the device returned from
	 *                    <code>readDevice()</code>
	 *
	 * @throws OneWireException if this device does not have A/D alarms
	 *
	 * @see com.dalsemi.onewire.container.OneWireSensor#writeDevice(byte[])
	 * @see #hasADAlarms()
	 * @see #getADAlarm(int,int,byte[])
	 * @see #setADAlarm(int,int,double,byte[])
	 * @see #getADAlarmEnable(int,int,byte[])
	 * @see #hasADAlarmed(int,int,byte[])
	 */
	@Override
	public void setADAlarmEnable(int channel, int alarmType, boolean alarmEnable, byte[] state)
			throws OneWireException {
		if (alarmType == ADContainer.ALARM_HIGH) {
			this.setFlag(DATA_CONTROL_REGISTER, DCR_BIT_ENABLE_DATA_HIGH_ALARM, alarmEnable, state);
		} else {
			this.setFlag(DATA_CONTROL_REGISTER, DCR_BIT_ENABLE_DATA_LOW_ALARM, alarmEnable, state);
		}
	}

	/**
	 * Sets the conversion resolution value for the specified channel. The method
	 * <code>writeDevice()</code> must be called to finalize changes to the device.
	 * Note that multiple 'set' methods can be called before one call to
	 * <code>writeDevice()</code>. Also note that not all A/D devices have alarms.
	 * Check to see if this device has alarms first by calling the
	 * <code>hasADAlarms()</code> method.
	 *
	 * @param channel    channel number in the range [0 to
	 *                   (<code>getNumberADChannels()</code> - 1)]
	 * @param resolution one of the resolutions returned by
	 *                   <code>getADResolutions(int,double)</code>
	 * @param state      current state of the device returned from
	 *                   <code>readDevice()</code>
	 *
	 * @see #getADResolutions(int,double)
	 * @see #getADResolution(int,byte[])
	 *
	 */
	@Override
	public void setADResolution(int channel, double resolution, byte[] state) {
		// throw new OneWireException("Selectable A-D Resolution Not Supported");
	}

	/**
	 * Sets the input range for the specified channel. The method
	 * <code>writeDevice()</code> must be called to finalize changes to the device.
	 * Note that multiple 'set' methods can be called before one call to
	 * <code>writeDevice()</code>. Also note that not all A/D devices have alarms.
	 * Check to see if this device has alarms first by calling the
	 * <code>hasADAlarms()</code> method.
	 *
	 * @param channel channel number in the range [0 to
	 *                (<code>getNumberADChannels()</code> - 1)]
	 * @param range   one of the ranges returned by <code>getADRanges(int)</code>
	 * @param state   current state of the device returned from
	 *                <code>readDevice()</code>
	 *
	 * @see #getADRanges(int)
	 * @see #getADRange(int,byte[])
	 */
	@Override
	public void setADRange(int channel, double range, byte[] state) {
		// throw new OneWireException("Selectable A-D Range Not Supported");
	}

	public void setADReferenceVoltage(double referenceVoltage) {
		this.adReferenceVoltage = referenceVoltage;
	}

	public double getADReferenceVoltage() {
		return this.adReferenceVoltage;
	}

	public void setADDeviceBitCount(int bits) {
		if (bits > 16) {
			bits = 16;
		}
		if (bits < 8) {
			bits = 8;
		}
		this.adDeviceBits = bits;
	}

	public int getADDeviceBitCount() {
		return this.adDeviceBits;
	}

	public void setForceADResults(boolean force) {
		this.adForceResults = force;
	}

	public boolean getForceADResults() {
		return this.adForceResults;
	}

// *****************************************************************************
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// Clock Interface Functions
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// *****************************************************************************

	/**
	 * Checks to see if the clock has an alarm feature.
	 *
	 * @return false, since this device does not have clock alarms
	 *
	 * @see #getClockAlarm(byte[])
	 * @see #isClockAlarmEnabled(byte[])
	 * @see #isClockAlarming(byte[])
	 * @see #setClockAlarm(long,byte[])
	 * @see #setClockAlarmEnable(boolean,byte[])
	 */
	@Override
	public boolean hasClockAlarm() {
		return false;
	}

	/**
	 * Checks to see if the clock can be disabled.
	 *
	 * @return true if the clock can be enabled and disabled
	 *
	 * @see #isClockRunning(byte[])
	 * @see #setClockRunEnable(boolean,byte[])
	 */
	@Override
	public boolean canDisableClock() {
		return true;
	}

	/**
	 * Gets the clock resolution in milliseconds
	 *
	 * @return the clock resolution in milliseconds
	 */
	@Override
	public long getClockResolution() {
		return 1000;
	}

	// --------
	// -------- Clock 'get' Methods
	// --------

	/**
	 * Extracts the Real-Time clock value in milliseconds.
	 *
	 * @param state current state of the device returned from
	 *              <code>readDevice()</code>
	 *
	 * @return the time represented in this clock in milliseconds since 1970
	 *
	 * @see com.dalsemi.onewire.container.OneWireSensor#readDevice()
	 * @see #setClock(long,byte[])
	 */
	@Override
	public long getClock(byte[] state) {
		// grab the time
		var time = this.getTime(RTC_TIME & 0x3F, state);
		// grab the date
		var date = this.getDate(RTC_DATE & 0x3F, state);

		// date[1] - 1 because Java months are 0 offset
		Calendar d = new GregorianCalendar(date[0], date[1] - 1, date[2], time[2], time[1], time[0]);

		return d.getTime().getTime();
	}

	/**
	 * Extracts the clock alarm value for the Real-Time clock.
	 *
	 * @param state current state of the device returned from
	 *              <code>readDevice()</code>
	 *
	 * @return milliseconds since 1970 that the clock alarm is set to
	 *
	 * @see com.dalsemi.onewire.container.OneWireSensor#readDevice()
	 * @see #hasClockAlarm()
	 * @see #isClockAlarmEnabled(byte[])
	 * @see #isClockAlarming(byte[])
	 * @see #setClockAlarm(long,byte[])
	 * @see #setClockAlarmEnable(boolean,byte[])
	 */
	@Override
	public long getClockAlarm(byte[] state) throws OneWireException {
		throw new OneWireException("Device does not support clock alarms");
	}

	/**
	 * Checks if the clock alarm flag has been set. This will occur when the value
	 * of the Real-Time clock equals the value of the clock alarm.
	 *
	 * @param state current state of the device returned from
	 *              <code>readDevice()</code>
	 *
	 * @return true if the Real-Time clock is alarming
	 *
	 * @see com.dalsemi.onewire.container.OneWireSensor#readDevice()
	 * @see #hasClockAlarm()
	 * @see #isClockAlarmEnabled(byte[])
	 * @see #getClockAlarm(byte[])
	 * @see #setClockAlarm(long,byte[])
	 * @see #setClockAlarmEnable(boolean,byte[])
	 */
	@Override
	public boolean isClockAlarming(byte[] state) {
		return false;
	}

	/**
	 * Checks if the clock alarm is enabled.
	 *
	 * @param state current state of the device returned from
	 *              <code>readDevice()</code>
	 *
	 * @return true if clock alarm is enabled
	 *
	 * @see com.dalsemi.onewire.container.OneWireSensor#readDevice()
	 * @see #hasClockAlarm()
	 * @see #isClockAlarming(byte[])
	 * @see #getClockAlarm(byte[])
	 * @see #setClockAlarm(long,byte[])
	 * @see #setClockAlarmEnable(boolean,byte[])
	 */
	@Override
	public boolean isClockAlarmEnabled(byte[] state) {
		return false;
	}

	/**
	 * Checks if the device's oscillator is enabled. The clock will not increment if
	 * the clock oscillator is not enabled.
	 *
	 * @param state current state of the device returned from
	 *              <code>readDevice()</code>
	 *
	 * @return true if the clock is running
	 *
	 * @see com.dalsemi.onewire.container.OneWireSensor#readDevice()
	 * @see #canDisableClock()
	 * @see #setClockRunEnable(boolean,byte[])
	 */
	@Override
	public boolean isClockRunning(byte[] state) {
		return this.getFlag(RTC_CONTROL_REGISTER, RCR_BIT_ENABLE_OSCILLATOR, state);
	}

	// --------
	// -------- Clock 'set' Methods
	// --------

	/**
	 * Sets the Real-Time clock. The method <code>writeDevice()</code> must be
	 * called to finalize changes to the device. Note that multiple 'set' methods
	 * can be called before one call to <code>writeDevice()</code>.
	 *
	 * @param time  new value for the Real-Time clock, in milliseconds since January
	 *              1, 1970
	 * @param state current state of the device returned from
	 *              <code>readDevice()</code>
	 *
	 * @see com.dalsemi.onewire.container.OneWireSensor#writeDevice(byte[])
	 * @see #getClock(byte[])
	 */
	@Override
	public void setClock(long time, byte[] state) {
		var x = new Date(time);
		Calendar d = new GregorianCalendar();

		d.setTime(x);
		this.setTime(RTC_TIME & 0x3F, d.get(Calendar.HOUR_OF_DAY), d.get(Calendar.MINUTE), d.get(Calendar.SECOND),
				false, state);
		this.setDate(RTC_DATE & 0x3F, d.get(Calendar.YEAR), d.get(Calendar.MONTH) + 1, d.get(Calendar.DATE), state);

		if (!this.getFlag(RTC_CONTROL_REGISTER, RCR_BIT_ENABLE_OSCILLATOR, state)) {
			this.setFlag(RTC_CONTROL_REGISTER, RCR_BIT_ENABLE_OSCILLATOR, true, state);
		}

		synchronized (this) {
			this.updatertc = true;
		}
	}

	/**
	 * Sets the clock alarm. The method <code>writeDevice()</code> must be called to
	 * finalize changes to the device. Note that multiple 'set' methods can be
	 * called before one call to <code>writeDevice()</code>. Also note that not all
	 * clock devices have alarms. Check to see if this device has alarms first by
	 * calling the <code>hasClockAlarm()</code> method.
	 *
	 * @param time  - new value for the Real-Time clock alarm, in milliseconds since
	 *              January 1, 1970
	 * @param state current state of the device returned from
	 *              <code>readDevice()</code>
	 *
	 * @throws OneWireException if this device does not have clock alarms
	 *
	 * @see com.dalsemi.onewire.container.OneWireSensor#writeDevice(byte[])
	 * @see #hasClockAlarm()
	 * @see #isClockAlarmEnabled(byte[])
	 * @see #getClockAlarm(byte[])
	 * @see #isClockAlarming(byte[])
	 * @see #setClockAlarmEnable(boolean,byte[])
	 */
	@Override
	public void setClockAlarm(long time, byte[] state) throws OneWireException {
		throw new OneWireException("Device does not support clock alarms");
	}

	/**
	 * Enables or disables the oscillator, turning the clock 'on' and 'off'. The
	 * method <code>writeDevice()</code> must be called to finalize changes to the
	 * device. Note that multiple 'set' methods can be called before one call to
	 * <code>writeDevice()</code>. Also note that not all clock devices can disable
	 * their oscillators. Check to see if this device can disable its oscillator
	 * first by calling the <code>canDisableClock()</code> method.
	 *
	 * @param runEnable true to enable the clock oscillator
	 * @param state     current state of the device returned from
	 *                  <code>readDevice()</code>
	 *
	 * @see com.dalsemi.onewire.container.OneWireSensor#writeDevice(byte[])
	 * @see #canDisableClock()
	 * @see #isClockRunning(byte[])
	 */
	@Override
	public void setClockRunEnable(boolean runEnable, byte[] state) {
		this.setFlag(RTC_CONTROL_REGISTER, RCR_BIT_ENABLE_OSCILLATOR, runEnable, state);
	}

	/**
	 * Enables or disables the clock alarm. The method <code>writeDevice()</code>
	 * must be called to finalize changes to the device. Note that multiple 'set'
	 * methods can be called before one call to <code>writeDevice()</code>. Also
	 * note that not all clock devices have alarms. Check to see if this device has
	 * alarms first by calling the <code>hasClockAlarm()</code> method.
	 *
	 * @param alarmEnable true to enable the clock alarm
	 * @param state       current state of the device returned from
	 *                    <code>readDevice()</code>
	 *
	 * @see com.dalsemi.onewire.container.OneWireSensor#writeDevice(byte[])
	 * @see #hasClockAlarm()
	 * @see #isClockAlarmEnabled(byte[])
	 * @see #getClockAlarm(byte[])
	 * @see #setClockAlarm(long,byte[])
	 * @see #isClockAlarming(byte[])
	 */
	@Override
	public void setClockAlarmEnable(boolean alarmEnable, byte[] state) throws OneWireException {
		throw new OneWireException("Device does not support clock alarms");
	}

	/**
	 * Gets the time of day fields in 24-hour time from button returns int[] =
	 * {seconds, minutes, hours}
	 *
	 * @param timeReg which register offset to pull the time from
	 * @param state   acquired from call to readDevice
	 * @return array representing {seconds, minutes, hours}
	 */
	private int[] getTime(int timeReg, byte[] state) {
		byte upper, lower;
		var result = new int[3];

		// First grab the seconds. Upper half holds the 10's of seconds
		lower = state[timeReg++];
		upper = (byte) (lower >>> 4 & 0x07);
		lower = (byte) (lower & 0x0f);
		result[0] = lower + upper * 10;

		// now grab minutes. The upper half holds the 10s of minutes
		lower = state[timeReg++];
		upper = (byte) (lower >>> 4 & 0x07);
		lower = (byte) (lower & 0x0f);
		result[1] = lower + upper * 10;

		// now grab the hours. The lower half is single hours again, but the
		// upper half of the byte is determined by the 2nd bit - specifying
		// 12/24 hour time.
		lower = state[timeReg];
		upper = (byte) (lower >>> 4 & 0x07);
		lower = (byte) (lower & 0x0f);

		byte PM = 0;
		// if the 2nd bit is 1, convert 12 hour time to 24 hour time.
		if ((upper & 0x04) != 0) {
			// extract the AM/PM byte (PM is indicated by a 1)
			if ((upper & 0x02) > 0) {
				PM = 12;
			}

			// isolate the 10s place
			upper &= 0x01;
		}

		result[2] = upper * 10 + lower + PM;

		return result;
	}

	/**
	 * Set the time in the DS1922 time register format.
	 */
	private void setTime(int timeReg, int hours, int minutes, int seconds, boolean AMPM, byte[] state) {
		byte upper, lower;

		/* format in bytes and write seconds */
		upper = (byte) (seconds / 10 << 4 & 0xf0);
		lower = (byte) (seconds % 10 & 0x0f);
		state[timeReg++] = (byte) (upper | lower);

		/* format in bytes and write minutes */
		upper = (byte) (minutes / 10 << 4 & 0xf0);
		lower = (byte) (minutes % 10 & 0x0f);
		state[timeReg++] = (byte) (upper | lower);

		/* format in bytes and write hours/(12/24) bit */
		if (AMPM) {
			upper = (byte) 0x04;

			if (hours > 11) {
				upper = (byte) (upper | 0x02);
			}

			// this next logic simply checks for a decade hour
			if (hours % 12 == 0 || hours % 12 > 9) {
				upper = (byte) (upper | 0x01);
			}

			if (hours > 12) {
				hours = hours - 12;
			}

			if (hours == 0) {
				lower = (byte) 0x02;
			} else {
				lower = (byte) (hours % 10 & 0x0f);
			}
		} else {
			upper = (byte) (hours / 10);
			lower = (byte) (hours % 10);
		}

		upper = (byte) (upper << 4 & 0xf0);
		lower = (byte) (lower & 0x0f);
		state[timeReg] = (byte) (upper | lower);
	}

	/**
	 * Grab the date from one of the time registers. returns int[] = {year, month,
	 * date}
	 *
	 * @param timeReg which register offset to pull the date from
	 * @param state   acquired from call to readDevice
	 * @return array representing {year, month, date}
	 */
	private int[] getDate(int timeReg, byte[] state) {
		byte upper, lower;
		int[] result = { 0, 0, 0 };

		/* extract the day of the month */
		lower = state[timeReg++];
		upper = (byte) (lower >>> 4 & 0x0f);
		lower = (byte) (lower & 0x0f);
		result[2] = upper * 10 + lower;

		/* extract the month */
		lower = state[timeReg++];
		if ((lower & 0x80) == 0x80) {
			result[0] = 100;
		}
		upper = (byte) (lower >>> 4 & 0x01);
		lower = (byte) (lower & 0x0f);
		result[1] = upper * 10 + lower;

		/* grab the year */
		lower = state[timeReg++];
		upper = (byte) (lower >>> 4 & 0x0f);
		lower = (byte) (lower & 0x0f);
		result[0] += upper * 10 + lower + FIRST_YEAR_EVER;

		return result;
	}

	/**
	 * Set the current date in the DS1922's real time clock.
	 *
	 * year - The year to set to, i.e. 2001. month - The month to set to, i.e. 1 for
	 * January, 12 for December. day - The day of month to set to, i.e. 1 to 31 in
	 * January, 1 to 30 in April.
	 */
	private void setDate(int timeReg, int year, int month, int day, byte[] state) {
		byte upper, lower;

		/* write the day byte (the upper holds 10s of days, lower holds single days) */
		upper = (byte) (day / 10 << 4 & 0xf0);
		lower = (byte) (day % 10 & 0x0f);
		state[timeReg++] = (byte) (upper | lower);

		/*
		 * write the month bit in the same manner, with the MSBit indicating the century
		 * (1 for 2000, 0 for 1900)
		 */
		upper = (byte) (month / 10 << 4 & 0xf0);
		lower = (byte) (month % 10 & 0x0f);
		state[timeReg++] = (byte) (upper | lower);

		// now write the year
		year = year - FIRST_YEAR_EVER;
		if (year > 100) {
			state[timeReg - 1] |= 0x80;
			year -= 100;
		}
		upper = (byte) (year / 10 << 4 & 0xf0);
		lower = (byte) (year % 10 & 0x0f);
		state[timeReg] = (byte) (upper | lower);
	}

// *****************************************************************************
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// Private initializers
//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// *****************************************************************************

	/**
	 * Construct the memory banks used for I/O.
	 */
	private void initMem() {

		// scratchpad
		this.scratch = new MemoryBankScratchCRCPW(this);

		// User Data Memory
		this.userDataMemory = new MemoryBankNVCRCPW(this, this.scratch);
		this.userDataMemory.numberPages = 16;
		this.userDataMemory.size = 512;
		this.userDataMemory.bankDescription = "User Data Memory";
		this.userDataMemory.startPhysicalAddress = 0x0000;
		this.userDataMemory.generalPurposeMemory = true;
		this.userDataMemory.readOnly = false;
		this.userDataMemory.readWrite = true;

		// Register
		this.register = new MemoryBankNVCRCPW(this, this.scratch);
		this.register.numberPages = 32;
		this.register.size = 1024;
		this.register.bankDescription = "Register control";
		this.register.startPhysicalAddress = 0x0200;
		this.register.generalPurposeMemory = false;

		// Data Log
		this.log = new MemoryBankNVCRCPW(this, this.scratch);
		this.log.numberPages = 256;
		this.log.size = 8192;
		this.log.bankDescription = "Data log";
		this.log.startPhysicalAddress = 0x1000;
		this.log.generalPurposeMemory = false;
		this.log.readOnly = true;
		this.log.readWrite = false;
	}

	/**
	 * Sets the following, calculated from the 12-bit code of the 1-Wire Net
	 * Address: 1) The part numbers: DS1923 - Temperature/Humidity iButton DS1922L -
	 * Temperature iButton DS1922T - Extended Temperature iButton DS1i22S -
	 * Temperature/A-D iButton
	 */
	private void setContainerVariables(byte[] registerPages) {
		var Tref1 = 60D;
		var autoLoadCalibration = true;

		// clear this flag.. Gets set later if registerPages!=null
		this.isContainerVariablesSet = false;

		// reset mission parameters
		this.hasHumiditySensor = false;
		this.isMissionLoaded = false;
		this.missionRegister = null;
		this.dataLog = null;
		this.temperatureLog = null;
		this.adReferenceVoltage = 5.02d;
		this.adDeviceBits = 10;
		this.adForceResults = false;

		this.deviceConfigByte = (byte) 0xFF;
		if (registerPages != null) {
			this.deviceConfigByte = registerPages[DEVICE_CONFIGURATION_BYTE & 0x03F];
		}

		switch (this.deviceConfigByte) {
		case DCB_DS2422:
			this.partNumber = PART_NUMBER_DS2422;
			this.temperatureRangeLow = -40;
			this.temperatureRangeWidth = 125;
			Tref1 = 60;
			this.descriptionString = DESCRIPTION_DS2422;
			autoLoadCalibration = false;
			break;
		case DCB_DS1923:
			this.partNumber = PART_NUMBER_DS1923;
			this.temperatureRangeLow = -40;
			this.temperatureRangeWidth = 125;
			Tref1 = 60;
			this.hasHumiditySensor = true;
			this.descriptionString = DESCRIPTION_DS1923;
			break;
		case DCB_DS1922L:
			this.partNumber = PART_NUMBER_DS1922L;
			this.temperatureRangeLow = -40;
			this.temperatureRangeWidth = 125;
			Tref1 = 60;
			this.descriptionString = DESCRIPTION_DS1922;
			break;
		case DCB_DS1922T:
			this.partNumber = PART_NUMBER_DS1922T;
			this.temperatureRangeLow = 0;
			this.temperatureRangeWidth = 125;
			Tref1 = 90;
			this.descriptionString = DESCRIPTION_DS1922;
			break;
		case DCB_DS1922E:
			this.partNumber = PART_NUMBER_DS1922E;
			this.temperatureRangeLow = 15;
			this.temperatureRangeWidth = 125;
			this.descriptionString = DESCRIPTION_DS1922;
			break;
		case DCB_DS1922S:
			this.partNumber = PART_NUMBER_DS1922S;
			this.temperatureRangeLow = -40;
			this.temperatureRangeWidth = 125;
			Tref1 = 60;
			this.descriptionString = DESCRIPTION_DS1922
					+ "\r\n\r\n* Please note that the DS1922S can be missioned only once.";
			break;
		default:
			this.partNumber = PART_NUMBER_UNKNOWN;
			this.temperatureRangeLow = -40;
			this.temperatureRangeWidth = 125;
			Tref1 = 60;
			this.descriptionString = DESCRIPTION_UNKNOWN;
			autoLoadCalibration = false;
			break;
		}

		if (registerPages != null) {
			this.isContainerVariablesSet = true;

			if (autoLoadCalibration) {
				// if humidity device, calculate the calibration coefficients
				if (this.hasHumiditySensor) {
					this.useHumdCalibrationRegisters = true;

					// DEBUG: Product samples were sent out uncalibrated. This flag
					// allows the customer to not use the temperature calibration
					var useHumdCal = OneWireAccessProvider.getProperty("DS1923.useHumidityCalibrationRegisters");
					if (useHumdCal != null && useHumdCal.toLowerCase().equals("false")) {
						this.useHumdCalibrationRegisters = false;
						if (DEBUG) {
							Debug.debug("DEBUG: Disabling Humidity Calibration Usage in Container");
						}
					}

					var Href1 = this.decodeHumidity(registerPages, 0x48, 2, true);
					this.Hread1 = this.decodeHumidity(registerPages, 0x4A, 2, true);
					this.Herror1 = this.Hread1 - Href1;
					this.Href2 = this.decodeHumidity(registerPages, 0x4C, 2, true);
					this.Hread2 = this.decodeHumidity(registerPages, 0x4E, 2, true);
					this.Herror2 = this.Hread2 - this.Href2;
					this.Href3 = this.decodeHumidity(registerPages, 0x50, 2, true);
					this.Hread3 = this.decodeHumidity(registerPages, 0x52, 2, true);
					this.Herror3 = this.Hread3 - this.Href3;

					var Href1sq = Href1 * Href1;
					var Href2sq = this.Href2 * this.Href2;
					var Href3sq = this.Href3 * this.Href3;
					this.humdCoeffB = ((Href2sq - Href1sq) * (this.Herror3 - this.Herror1)
							+ Href3sq * (this.Herror1 - this.Herror2) + Href1sq * (this.Herror2 - this.Herror1))
							/ ((Href2sq - Href1sq) * (this.Href3 - Href1) + (Href3sq - Href1sq) * (Href1 - this.Href2));
					this.humdCoeffA = (this.Herror2 - this.Herror1 + this.humdCoeffB * (Href1 - this.Href2))
							/ (Href2sq - Href1sq);
					this.humdCoeffC = this.Herror1 - this.humdCoeffA * Href1sq - this.humdCoeffB * Href1;
				}

				this.useTempCalibrationRegisters = true;

				// DEBUG: Product samples were sent out uncalibrated. This flag
				// allows the customer to not use the temperature calibration
				var useTempCal = OneWireAccessProvider.getProperty("DS1923.useTemperatureCalibrationRegisters");
				if (useTempCal != null && useTempCal.toLowerCase().equals("false")) {
					this.useTempCalibrationRegisters = false;
					if (DEBUG) {
						Debug.debug("DEBUG: Disabling Temperature Calibration Usage in Container");
					}
				}

				// if (partNumber == PART_NUMBER_DS1922E)
				// useTempCalibrationRegisters = false; // !!!

				this.Tref2 = this.decodeTemperature(registerPages, 0x40, 2, true);
				this.Tread2 = this.decodeTemperature(registerPages, 0x42, 2, true);
				this.Terror2 = this.Tread2 - this.Tref2;
				this.Tref3 = this.decodeTemperature(registerPages, 0x44, 2, true);
				this.Tread3 = this.decodeTemperature(registerPages, 0x46, 2, true);
				this.Terror3 = this.Tread3 - this.Tref3;
				this.Terror1 = this.Terror2;
				this.Tread1 = Tref1 + this.Terror1;

				if (DEBUG) {
					Debug.debug("Tref1=" + Tref1);
					Debug.debug("Tread1=" + this.Tread1);
					Debug.debug("Terror1=" + this.Terror1);
					Debug.debug("Tref2=" + this.Tref2);
					Debug.debug("Tread2=" + this.Tread2);
					Debug.debug("Terror2=" + this.Terror2);
					Debug.debug("Tref3=" + this.Tref3);
					Debug.debug("Tread3=" + this.Tread3);
					Debug.debug("Terror3=" + this.Terror3);
				}

				var Tref1sq = Tref1 * Tref1;
				var Tref2sq = this.Tref2 * this.Tref2;
				var Tref3sq = this.Tref3 * this.Tref3;
				this.tempCoeffB = ((Tref2sq - Tref1sq) * (this.Terror3 - this.Terror1)
						+ Tref3sq * (this.Terror1 - this.Terror2) + Tref1sq * (this.Terror2 - this.Terror1))
						/ ((Tref2sq - Tref1sq) * (this.Tref3 - Tref1) + (Tref3sq - Tref1sq) * (Tref1 - this.Tref2));
				this.tempCoeffA = (this.Terror2 - this.Terror1 + this.tempCoeffB * (Tref1 - this.Tref2))
						/ (Tref2sq - Tref1sq);
				this.tempCoeffC = this.Terror1 - this.tempCoeffA * Tref1sq - this.tempCoeffB * Tref1;
			}
		}
	}

	/**
	 * helper method to pause for specified milliseconds
	 */
	private static final void msWait(final long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException ie) {

		}
	}

	/**
	 * helper method for decoding temperature values
	 */
	private final double decodeTemperature(byte[] data, int offset, int length, boolean reverse) {
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		if (DEBUG) {
			Debug.debug("decodeTemperature, data", data, offset, length);
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		double whole, fraction = 0;
		if (reverse && length == 2) {
			fraction = (data[offset + 1] & 0x0FF) / 512d;
			whole = (data[offset] & 0x0FF) / 2d + (this.temperatureRangeLow - 1);
		} else if (length == 2) {
			fraction = (data[offset] & 0x0FF) / 512d;
			whole = (data[offset + 1] & 0x0FF) / 2d + (this.temperatureRangeLow - 1);
		} else {
			whole = (data[offset] & 0x0FF) / 2d + (this.temperatureRangeLow - 1);
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		if (DEBUG) {
			Debug.debug("decodeTemperature, temperatureRangeLow= " + this.temperatureRangeLow);
			Debug.debug("decodeTemperature, whole= " + whole);
			Debug.debug("decodeTemperature, fraction= " + fraction);
			Debug.debug("decodeTemperature, (whole+fraction)= " + (whole + fraction));
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		return whole + fraction;
	}

	/**
	 * helper method for encoding temperature values
	 */
	private final void encodeTemperature(double temperature, byte[] data, int offset, int length, boolean reverse) {
		var val = 2 * (temperature - (this.temperatureRangeLow - 1));
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		if (DEBUG) {
			Debug.debug("encodeTemperature, temperature=" + temperature + ", temperatureRangeLow="
					+ this.temperatureRangeLow + ", val=" + val);
			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		}

		if (reverse && length == 2) {
			data[offset + 1] = (byte) (0x0C0 & (byte) (val * 256));
			data[offset] = (byte) val;
		} else if (length == 2) {
			data[offset] = (byte) (0x0C0 & (byte) (val * 256));
			data[offset + 1] = (byte) val;
		} else {
			data[offset] = (byte) val;
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		if (DEBUG) {
			Debug.debug("encodeTemperature, data", data, offset, length);
			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		}
	}

	/**
	 * helper method for decoding humidity values
	 */
	private final double decodeHumidity(byte[] data, int offset, int length, boolean reverse) {
		// get the 10-bit value of Vout
		var val = this.getADVoltage(data, offset, length, reverse);

		// convert Vout to a humidity reading
		// this formula is from HIH-3610 sensor datasheet
		val = (val - .958) / .0307;

		if (this.useHumdCalibrationRegisters) {
			var valsq = val * val;
			var error = this.humdCoeffA * valsq + this.humdCoeffB * val + this.humdCoeffC;
			val = val - error;
		}

		return val;
	}

	/**
	 * helper method for encoding humidity values
	 */
	private final void encodeHumidity(double humidity, byte[] data, int offset, int length, boolean reverse) {
		// uncalibrate the alarm value before writing
		if (this.useHumdCalibrationRegisters) {
			humidity = (1 - this.humdCoeffB - Math.sqrt(
					(this.humdCoeffB - 1) * (this.humdCoeffB - 1) - 4 * this.humdCoeffA * (this.humdCoeffC + humidity)))
					/ (2 * this.humdCoeffA);
		}

		// convert humidity value to Vout value
		var val = humidity * .0307 + .958;
		// convert Vout to byte[]
		this.setADVoltage(val, data, offset, length, reverse);
	}

	private final double getADVoltage(byte[] data, int offset, int length, boolean reverse) {
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		if (DEBUG) {
			Debug.debug("getADVoltage, data", data, offset, length);
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		// get the 10-bit value of vout
		var ival = 0;
		if (reverse && length == 2) {
			ival = (data[offset] & 0x0FF) << this.adDeviceBits - 8;
			ival |= (data[offset + 1] & 0x0FF) >> 16 - this.adDeviceBits;
		} else if (length == 2) {
			ival = (data[offset + 1] & 0x0FF) << this.adDeviceBits - 8;
			ival |= (data[offset] & 0x0FF) >> 16 - this.adDeviceBits;
		} else {
			ival = (data[offset] & 0x0FF) << this.adDeviceBits - 8;
		}

		var dval = ival * this.adReferenceVoltage / (1 << this.adDeviceBits);

		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		if (DEBUG) {
			Debug.debug("getADVoltage, ival=" + ival);
			Debug.debug("getADVoltage, voltage=" + dval);
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		return dval;
	}

	private final void setADVoltage(double voltage, byte[] data, int offset, int length, boolean reverse) {
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		if (DEBUG) {
			Debug.debug("setADVoltage, voltage=" + voltage);
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		var val = (int) (voltage * (1 << this.adDeviceBits) / this.adReferenceVoltage);
		val = val & (1 << this.adDeviceBits) - 1;
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		if (DEBUG) {
			Debug.debug("setADVoltage, val=" + val);
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		if (reverse && length == 2) {
			data[offset] = (byte) (val >> this.adDeviceBits - 8);
			data[offset + 1] = (byte) (val << 16 - this.adDeviceBits);
		} else if (length == 2) {
			data[offset + 1] = (byte) (val >> this.adDeviceBits - 8);
			data[offset] = (byte) (val << 16 - this.adDeviceBits);
		} else {
			data[offset] = (byte) ((val & 0x3FC) >> this.adDeviceBits - 8);
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		if (DEBUG) {
			Debug.debug("setADVoltage, data", data, offset, length);
			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		}
	}
}
// CHECKSTYLE:ON
