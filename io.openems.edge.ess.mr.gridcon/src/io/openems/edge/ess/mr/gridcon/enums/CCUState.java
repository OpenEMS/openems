package io.openems.edge.ess.mr.gridcon.enums;

/**
 * This enum describes the overall total state of a gridcon.
 */
public enum CCUState {
	INIT(), //  = 1, // Initialisierung / Booting in Progress
	IDLE(), // = 2, // System waiting for Instructions, MainContactor open, IGBT are not working
	PRECHARGE(), // = 3, // IPU State
	GO_IDLE(), // = 4,  // System changes STATE_IDLE
	CHARGED(), // = 5,  // IPU State
	READY(), //  = 6,  // IPU State
	RUN(), //  = 7,  // IPU State
	ERROR(), // = 8,  // System in ErrorState
	PAUSE(), //  = 9,  // System Pause, Maincontactor closed, IGBTs are switching"
	UNDEFINED(),
	;

}
