This is the Consolinno Warmup Heating Controller.
 * It is a heating controller that outputs a temperature based on a predefined program.
 * The controller behaves very much like a media player. It has a play/pause button, an "elapsed time" display, a 
   "go to minute" function and a "load program" function.
 * There is also an "auto resume" function that continues from the last valvePosition if the controller was shut down
   (for whatever reason) while a program was running.
 * A basic heating program can be defined in the configuration menu. This program will then be saved in the default
   config file "warmupcontroller.json" in the same directory as the "openems.jar". If you want a warmup program that
   cannot be created with the provided functionality in the configuration menu, edit the config file. You can leave
   the modified file or save it under a different name and load it into the controller with the "load program" function.
 * More detailed descriptions of the functionality of this controller can be found in the interface 
   "ControllerWarmupChannel".

