This is the Consolinno Passing Control Center module. It manages the hierarchy of heating controllers.
* The output of a heating controller is a temperature and a boolean to signal if the controller wants to heat or not.
* This controller polls three heating controllers by hierarchy and passes on the result (heating or not heating plus the temperature) to the next module(s).

