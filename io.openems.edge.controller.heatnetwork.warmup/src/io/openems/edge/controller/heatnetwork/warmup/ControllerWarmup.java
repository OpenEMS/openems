package io.openems.edge.controller.heatnetwork.warmup;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;


/**
 * This is the Consolinno Warmup Heating Controller.
 * - It is a heating controller that outputs a temperature based on a predefined program.
 * - The controller behaves very much like a media player. It has a play/pause button, an "elapsed time" display, a
 *   "go to minute" function and a "load program" function.
 * - There is also an "auto resume" function that continues from the last position if the controller was shut down
 *   (for whatever reason) while a program was running.
 * - A basic heating program can be defined in the configuration menu. This program will then be saved in the default
 *   config file "warmupcontroller.json" in the same directory as the "openems.jar". If you want a warmup program that
 *   cannot be created with the provided functionality in the configuration menu, edit the config file. You can leave
 *   the modified file or save it under a different name and load it into the controller with the "load program" function.
 * - More detailed descriptions of the functionality of this controller can be found in the interface
 *   "ControllerWarmupChannel".
 *
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Warmup", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ControllerWarmup extends AbstractOpenemsComponent implements OpenemsComponent, io.openems.edge.controller.heatnetwork.warmup.api.ControllerWarmup, Controller {

	private final Logger log = LoggerFactory.getLogger(ControllerWarmup.class);

	@Reference
	protected ComponentManager cpm;

	private JsonObject warmupstate;		// Container that is used to save to file and read from file. Identical to file content.
	private final File storage = new File("warmupcontroller.json");	// Name of file the container warmupstate is saved in, location is same directory the .jar is in.
	private static final DateTimeFormatter timeformat = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");	// How date+time is formatted in the file.
	private LocalDateTime lastTimestampRuntime, adjustedStartDate;	// Timestamps needed for elapsed time calculations.
	private final int activationInterval = 1;	// Interval in minutes between activation of recurring code in run() method. Done this way because recurring code saves to file every time it is run. Doing that every second would increase system load.
	private boolean isSwitchedOn;   // This is used to remember last state when playpause channel is changed.
	private int totalLengthMinutes;	// Total length of the heating program loaded in minutes.
	//private int testcounter;	// Only used for testing the controller.

    // Variables for channel readout
    private boolean goToButtonHasData;
    private int goToMinute;
    private boolean playPauseButtonHasData;
    private boolean playButtonPressed;
    private boolean loadProgramButtonHasData;
    private String loadProgramChannel;


    public ControllerWarmup() {
		super(OpenemsComponent.ChannelId.values(),
				io.openems.edge.controller.heatnetwork.warmup.api.ControllerWarmup.ChannelId.values(),
				Controller.ChannelId.values());
	}

	@Activate
	public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		//testcounter = 0;

		this.noError().setNextValue(true);
		isSwitchedOn = false;
		if (storage.isFile()) {		// Check if file exists, if not create it with parameters from config.
			loadConfigFile(config);
			if (this.noError().getNextValue().get()) {
				if(config.auto_resume() == false) { // When auto resume is disabled, set elapsed time to 0 upon loading.
					warmupstate.addProperty("elapsedTime", 0);
				}
				if (warmupstate.get("elapsedTime").getAsInt() == 0){     // No heating run was in progress.
					this.logInfo(this.log, "Ready to start next heating run.");
				} else {		// If elapsedTime is not 0, a heating run was in progress and has been interrupted. Try to resume.
					this.playPauseWarmupController().setNextValue(true);
				}
			}
		} else {
			createDefaultConfigFile(config);
		}

		// Put variables from config file in the channels.
		this.getElapsedTimeWarmupProgram().setNextValue(warmupstate.get("elapsedTime").getAsInt());
		this.getLengthWarmupProgram().setNextValue(totalLengthMinutes);

		if (config.start_on_activation()) {
			this.playPauseWarmupController().setNextValue(true);
		}
	}

	@Deactivate
	public void deactivate() {
		super.deactivate();
	}

	private void createDefaultConfigFile(Config config) {
		this.logInfo(this.log, "No config file found. Creating default config file. Temperature list of heating program:");
		warmupstate = new JsonObject();
		try {
			warmupstate.addProperty("startDate", LocalDateTime.now().format(timeformat));
			warmupstate.addProperty("lastTimestamp", LocalDateTime.now().format(timeformat));
		} catch (DateTimeException exc) {
            jsonTimeFormatError();
			throw exc;
		}
		warmupstate.addProperty("elapsedTime", 0);
		this.getElapsedTimeWarmupProgram().setNextValue(0);
		warmupstate.addProperty("timeStepDurationMinutes", config.step_length());	//Duration of each heating step, in minutes. Default 1 day.
		int count = 0;
		while (count < config.step_number()) {		//Create default entries for temperature list. Unit is dezidegree celsius.
			warmupstate.addProperty("temp" + count, (config.start_temp() + (count * config.temp_increase())) * 10);
			this.logInfo(this.log, "Temp" + count + " = " + warmupstate.get("temp" + count).getAsInt() * 0.1 + "°C");
			count++;
		}
		totalLengthMinutes = config.step_number() * warmupstate.get("timeStepDurationMinutes").getAsInt();
		this.logInfo(this.log, "There are " + count + " temperature entries. Each entry is set to run for "
				+ warmupstate.get("timeStepDurationMinutes").getAsInt() / 60 + "h " + warmupstate.get("timeStepDurationMinutes").getAsInt() % 60
				+ "m, for a total running time of " + totalLengthMinutes / 1440 + "d " + totalLengthMinutes / 60 + "h "
				+ totalLengthMinutes % 60 + "m.");
		saveToConfigFile();
		this.logInfo(this.log, "Ready to start next heating run.");
	}

	private void saveToConfigFile() {
		try (FileWriter writer = new FileWriter(storage)) {		//Write JSON file
			writer.write(warmupstate.toString());
			writer.flush();
			this.logInfo(this.log, "Saving to config file");
		} catch (IOException e) {
			this.noError().setNextValue(false);
			this.logError(this.log, "Error, couldn't write to config file " + storage);
			e.printStackTrace();
		}
	}

	private void loadConfigFile(Config config) throws OpenemsError.OpenemsNamedException {
		this.logInfo(this.log, "Loading config file ");
		try (FileReader reader = new FileReader(storage)) {
			JsonParser jParser = new JsonParser();
			Object obj = jParser.parse(reader);
			warmupstate = (JsonObject) obj;
		} catch (FileNotFoundException e) {
			this.noError().setNextValue(false);
			this.logError(this.log, "Error, couldn't find file " + storage);
			e.printStackTrace();
		} catch (IOException e) {
			this.noError().setNextValue(false);
			this.logError(this.log, "Error, couldn't read from file " + storage + ". File may be corrupted, "
                    + "please replace/delete and try again. If you delete the file, the controller will create an "
                    + "appropriate file with standard parameters.");
			e.printStackTrace();
		} catch (JsonParseException js) {
			this.noError().setNextValue(false);
			this.logError(this.log, "Error, couldn't translate contents of " + storage + ". Incompatible JSON "
                    + "format or file may be corrupted, please replace/delete and try again. If you delete the file, the "
                    + "controller will create an appropriate file with standard parameters.");
			this.logError(this.log, js.toString());
		}

		try {
			lastTimestampRuntime = LocalDateTime.parse(warmupstate.get("lastTimestamp").getAsString(), timeformat);
		} catch (DateTimeParseException exc) {
//				this.noError().setNextValue(false);
			this.logError(this.log, "Error reading timestamp from file. Trying to recover by setting new "
                    + "timestamp, may result in incorrect timing of program.");
			lastTimestampRuntime = LocalDateTime.now();
			throw exc;
		}

		if (this.noError().getNextValue().get()) {
			if (config.override_program()) {
				this.logInfo(this.log, "Overriding last heating program. Temperature list:");
				warmupstate.addProperty("timeStepDurationMinutes", config.step_length());	// Duration of each heating step, in minutes. Default 1 day.
				int count = 0;
				while (count < config.step_number()) {		// Create default entries for temperature list. Unit is dezidegree celsius.
					warmupstate.addProperty("temp" + count, (config.start_temp() + (count * config.temp_increase())) * 10);
					this.logInfo(this.log, "Temp" + count + " = " + warmupstate.get("temp" + count).getAsInt() * 0.1 + "°C");
					count++;
				}
				totalLengthMinutes = config.step_number() * warmupstate.get("timeStepDurationMinutes").getAsInt();
				this.logInfo(this.log, "There are " + count + " temperature entries. Each entry is set to run for "
						+ warmupstate.get("timeStepDurationMinutes").getAsInt() / 60 + "h "
						+ warmupstate.get("timeStepDurationMinutes").getAsInt() % 60 + "m, for a total running time of "
						+ totalLengthMinutes / 1440 + "d " + totalLengthMinutes / 60 + "h " + totalLengthMinutes % 60 + "m.");

				while (warmupstate.has("temp" + count)) {   // In case warmupstate had more temperature entries than the new program, delete them.
					warmupstate.remove("temp" + count);
					count++;
				}

			} else {
				this.logInfo(this.log, "Successfully read config file. Temperature list:");
				int count = 0;
				while (warmupstate.has("temp" + count)) {   //read all available temperature entries.
					this.logInfo(this.log, "Temp" + count + " = " + warmupstate.get("temp" + count).getAsInt() * 0.1 + "°C");
					count++;
				}
				if (count == 0) {
					this.noError().setNextValue(false);
					this.logError(this.log, "Error, could not find temperature entries in file " + storage);
				}
				totalLengthMinutes = warmupstate.get("timeStepDurationMinutes").getAsInt()*count;
				this.logInfo(this.log, "There are " + count + " temperature entries. Each entry is set to run for "
						+ warmupstate.get("timeStepDurationMinutes").getAsInt() / 60 + "h "
						+ warmupstate.get("timeStepDurationMinutes").getAsInt() % 60 + "m, for a total running time of "
						+ totalLengthMinutes / 1440 + "d " + totalLengthMinutes / 60 + "h " + totalLengthMinutes % 60 + "m.");
			}

			if (warmupstate.get("elapsedTime").getAsInt() < 0 || warmupstate.get("elapsedTime").getAsInt() >= totalLengthMinutes) {
				warmupstate.addProperty("elapsedTime", 0);  //Make sure elapsed time is not out of bounds.
			}
		} else {
			this.playPauseWarmupController().setNextValue(false);
			this.logError(this.log, "Encountered an error, deactivating.");
		}
	}

	private void loadHeatingProgramFromFile(String filepath) throws OpenemsError.OpenemsNamedException {
		this.logInfo(this.log, "Trying to load new heating program from file " + filepath + ".");
		JsonObject newProgram, newConfigFile;
		boolean nofileloaderror = true; // Need separate error tracker, failing to load a new heating program should not stop the controller.
		File filetoload = new File(filepath);
		if (filetoload.isFile()) {	//Check if file exists
			try (FileReader reader = new FileReader(filetoload)) {
				JsonParser jParser = new JsonParser();
				Object obj = jParser.parse(reader);
				newProgram = (JsonObject) obj;
				// Everything in that file is now loaded. Might be a lot of junk too, so only pick the necessary data and copy that to warmupstate.

				// Check if the data loaded contains a program
				if (newProgram.has("timeStepDurationMinutes") && newProgram.has("temp0")) {   // Minimum requirement for a heating program.
					this.logInfo(this.log, "Successfully loaded new heating program. Temperature list:");
					warmupstate.addProperty("timeStepDurationMinutes", newProgram.get("timeStepDurationMinutes").getAsInt()); // Copy relevant data to warmupstate.
					int count = 0;
					while (newProgram.has("temp" + count)) {   // Read all available temperature entries, copy them to warmupstate.
						warmupstate.addProperty("temp" + count, newProgram.get("temp" + count).getAsInt());
						this.logInfo(this.log, "Temp" + count + " = " + warmupstate.get("temp" + count).getAsInt() * 0.1 + "°C");
						count++;
					}
					totalLengthMinutes = warmupstate.get("timeStepDurationMinutes").getAsInt() * count;
					this.logInfo(this.log, "There are " + count + " temperature entries. Each entry is set to run for "
                            + warmupstate.get("timeStepDurationMinutes").getAsInt() / 60 + "h "
                            + warmupstate.get("timeStepDurationMinutes").getAsInt() % 60 + "m, for a total running time of "
                            + totalLengthMinutes / 1440 + "d " + totalLengthMinutes / 60 + "h " + totalLengthMinutes % 60 + "m.");

					while (warmupstate.has("temp" + count)) {   // In case warmupstate had more temperature entries than the new program, delete them.
						warmupstate.remove("temp" + count);
						count++;
					}

					// Reset elapsed time if current elapsed time is longer than length of new program.
                    if (warmupstate.get("elapsedTime").getAsInt() > totalLengthMinutes) {
                        warmupstate.addProperty("elapsedTime", 0);
                    }
                    // The elapsed time is left untouched if possible, to make temperature adjustments of a heating run
                    // in progress easier. You can modifying the temperatures in the config file and then load it with
                    // the loadWarmupProgram channel. The elapsed time won't change.
                    // If you want to change the elapsed time, use the goToMinute channel.

					saveToConfigFile();

				} else {
					nofileloaderror = false;
					this.logError(this.log, "Error, couldn't find a heating program in file " + filetoload);
				}
			} catch (FileNotFoundException e) {
				nofileloaderror = false;
				this.logError(this.log, "Error, couldn't find file " + filetoload);
				e.printStackTrace();
			} catch (IOException e) {
				nofileloaderror = false;
				this.logError(this.log, "Error, couldn't read from file " + filetoload);
				e.printStackTrace();
			} catch (JsonParseException js) {
				nofileloaderror = false;
				this.logError(this.log, "Error, couldn't translate contents of " + filetoload + ". Incompatible JSON format or file may be corrupted.");
				this.logError(this.log, js.toString());
			}

		} else {
			nofileloaderror = false;
			this.logError(this.log, "Error, found no such file: " + filepath);
		}

		// Write "done" or "failed" in the loadWarmupProgram channel to reset the load program function. The load program
        // function will trigger if anything else but "done" or "failed" is in that channel.
		if (nofileloaderror) {
            // Reset error in case one was active. If the error was caused by a faulty config file, successfully loading
            // a new config should solve that error. If the error was from something else, it will trigger again.
            this.noError().setNextValue(true);
			this.loadWarmupProgram().setNextValue("done");
		} else {
			this.loadWarmupProgram().setNextValue("failed");
		}

	}


	@Override
	public void run() throws OpenemsError.OpenemsNamedException {
/*
	    //For testing:
        testcounter++;
        if (testcounter == 5) {
            this.playPauseWarmupController().setNextValue(false);
			this.goToMinuteWarmupProgram().setNextValue(2);
        }
        if (testcounter == 10) {
            this.playPauseWarmupController().setNextValue(true);
        }
        if (testcounter == 15) {
            this.playPauseWarmupController().setNextValue(false);
            this.loadWarmupProgram().setNextValue("loadprogram.json");
        }
		if (testcounter == 20) {
			this.playPauseWarmupController().setNextValue(true);
		}
		if (testcounter == 30) {
			this.goToMinuteWarmupProgram().setNextValue(4);
		}
*/

        // Transfer channel data to local variables for better readability of logic code.
        goToButtonHasData = this.goToMinuteWarmupProgram().value().isDefined();
        if (goToButtonHasData) {
			goToMinute = this.goToMinuteWarmupProgram().value().get();
		}
        playPauseButtonHasData = this.playPauseWarmupController().value().isDefined();
        if (playPauseButtonHasData) {
			playButtonPressed = this.playPauseWarmupController().value().get();
		}
        loadProgramButtonHasData = this.loadWarmupProgram().value().isDefined();
        if (loadProgramButtonHasData) {
			loadProgramChannel = this.loadWarmupProgram().value().get();
		}



		// Forward/rewind button
		if (goToButtonHasData) {
			if (goToMinute >= 0) {	// Negative values mean "do nothing". Do not need to check if >totalLength, code will execute "heating finished" branch in that case.
				warmupstate.addProperty("elapsedTime", goToMinute);
				lastTimestampRuntime = LocalDateTime.now().minusMinutes(activationInterval);   // Adjust this so that recurring code executes immediately and updates the temperature.
				adjustedStartDate = LocalDateTime.now().minusMinutes(goToMinute);   // Need to adjust this so that elapsed time is calculated correctly.
				this.logInfo(this.log, "Heating program elapsed time moved to " + goToMinute / 1440 + "d "
						+ goToMinute / 60 + "h " + goToMinute % 60
						+ "m, total length is " + totalLengthMinutes / 1440 + "d " + totalLengthMinutes / 60 + "h "
                        + totalLengthMinutes % 60 + "m.");
                this.goToMinuteWarmupProgram().setNextValue(-1);	// Deactivate forward/rewind button
				saveToConfigFile();		// Save changed parameters to file.
			}
		}

		// Need getNextValue() here instead of value() for the error channel, as the controller should react to an
		// error immediately and not one cycle later.
		if (playPauseButtonHasData && this.noError().getNextValue().get()) {

			// Pause has just been pressed == play just switched to false.
			if (isSwitchedOn && (playButtonPressed == false)) {
				lastTimestampRuntime = LocalDateTime.now();
				isSwitchedOn = false;   // Track state.
				this.logInfo(this.log, "Heating program paused, heating turned off.");
			}

			// Play has just been pressed.
			if ((isSwitchedOn == false) && playButtonPressed) {
				if (warmupstate.get("elapsedTime").getAsInt() == 0) {     // Check if this is the start of a new heating run.
					try {
						lastTimestampRuntime = LocalDateTime.now();
						warmupstate.addProperty("startDate", lastTimestampRuntime.format(timeformat));		// Save start time.
						warmupstate.addProperty("lastTimestamp", lastTimestampRuntime.format(timeformat));	// This is used to track interruptions.
						adjustedStartDate = LocalDateTime.now();    // This timestamp is used to calculate elapsed time
						this.getWarmupTemperature().setNextValue(warmupstate.get("temp0").getAsInt());		//Set temperature value to first temp entry. This one should always exist.
						this.logInfo(this.log, "Starting heating run at " + (warmupstate.get("temp0").getAsInt() * 0.1)
								+ "°C. Duration is " + totalLengthMinutes / 60 + "h " + totalLengthMinutes % 60 + "m.");
					} catch (DateTimeException exc) {
                        jsonTimeFormatError();
						throw exc;
					}
					saveToConfigFile();
				} else {    // This branch executes when a heating run is resumed.
					this.logInfo(this.log, "Resuming heating run that was started at " + warmupstate.get("startDate").getAsString() + ".");
					this.logInfo(this.log, "Heating was paused/interrupted at " + warmupstate.get("lastTimestamp").getAsString()
							+ ". The pause lasted for " + ChronoUnit.HOURS.between(lastTimestampRuntime, LocalDateTime.now()) + "h "
							+ ChronoUnit.MINUTES.between(lastTimestampRuntime, LocalDateTime.now()) % 60 + "m.");
					adjustedStartDate = LocalDateTime.now().minusMinutes(warmupstate.get("elapsedTime").getAsInt());
					lastTimestampRuntime = LocalDateTime.now().minusMinutes(activationInterval);   //Adjust this so that recurring code executes immediately.
				}
				isSwitchedOn = true;    // Track state
			}

			// Recurring code during heating run. Activates every activationInterval. SaveToConfigFile() is part of this
            // code, so it should not run every second to reduce system load.
			if (playButtonPressed && ChronoUnit.MINUTES.between(lastTimestampRuntime, LocalDateTime.now()) >= activationInterval) {
				lastTimestampRuntime = LocalDateTime.now();
				try {
					warmupstate.addProperty("lastTimestamp", lastTimestampRuntime.format(timeformat)); // Store timestamp
				} catch (DateTimeException exc) {
                    jsonTimeFormatError();
					throw exc;
				}
				warmupstate.addProperty("elapsedTime", ChronoUnit.MINUTES.between(adjustedStartDate, LocalDateTime.now()));

                // Check that end is not reached yet.
                if (warmupstate.get("elapsedTime").getAsInt() < totalLengthMinutes) {
					int tempEntry = warmupstate.get("elapsedTime").getAsInt() / warmupstate.get("timeStepDurationMinutes").getAsInt();    // Gives position in temperature list
					this.getWarmupTemperature().setNextValue(warmupstate.get("temp" + tempEntry).getAsInt());		// Set temperature according to temperature list.
					this.logInfo(this.log, "Setting temperature entry temp" + tempEntry + ", which is "
                            + (warmupstate.get("temp" + tempEntry).getAsInt() * 0.1) + "°C.");
					this.logInfo(this.log, "Elapsed time is " + warmupstate.get("elapsedTime").getAsInt() / 1440 + "d "
							+ warmupstate.get("elapsedTime").getAsInt() / 60 + "h " + warmupstate.get("elapsedTime").getAsInt() % 60
							+ "m, total length is " + totalLengthMinutes / 1440 + "d " + totalLengthMinutes / 60 + "h "
							+ totalLengthMinutes % 60 + "m.");
				} else {    // Heating run has reached end.
					this.logInfo(this.log, "Heating run that was started at " + warmupstate.get("startDate").getAsString() + " has finished.");
					this.logInfo(this.log, "Ready to start next heating run.");

					// Reset everything
					warmupstate.addProperty("elapsedTime", 0);
					this.playPauseWarmupController().setNextValue(false);
					isSwitchedOn = false;
				}

                this.getElapsedTimeWarmupProgram().setNextValue(warmupstate.get("elapsedTime").getAsInt());     // Update elapsed time channel
				saveToConfigFile();		// Save changed parameters to file.
			}

		}

		// Load program button. Executes when there is a string (= file path) in the loadProgramChannel that is not "done" or "failed".
		if (loadProgramButtonHasData) {
			if ((loadProgramChannel.equals("done") == false) && (loadProgramChannel.equals("failed") == false)) {

			    // When you want to load a program, the controller needs to be paused or not yet active.
			    if (playPauseButtonHasData) {
					if (playButtonPressed == false) {
						loadHeatingProgramFromFile(loadProgramChannel);
					} else {
                        this.logInfo(this.log, "You need to pause the controller to load a warmup program. "
                                + "Pause the controller or write \"done\" in the loadWarmupProgram channel to stop this message.");
                    }
				} else {
					loadHeatingProgramFromFile(loadProgramChannel);
				}
			}
		}

	}

	private void jsonTimeFormatError() {
        this.noError().setNextValue(false);
        this.logError(this.log, "Time format error, couldn't store lastTimestamp in Json object.");
    }

}
