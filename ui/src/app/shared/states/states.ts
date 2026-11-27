export enum States {
    WEBSOCKET_CONNECTION_CLOSED,
    WEBSOCKET_NOT_YET_CONNECTED,
    WEBSOCKET_CONNECTING,
    WEBSOCKET_CONNECTED,
    AUTHENTICATING,

    // TODO substates
    AUTHENTICATION_FAILED,
    NOT_AUTHENTICATED,
    AUTHENTICATING_WITH_TOKEN,
    AUTHENTICATION_WITH_CREDENTIALS,
    AUTHENTICATED,
    EDGE_SELECTED,
    EDGE_SUBSCRIBED,
}

export namespace States {
    /**
     * Evaluates whether "State 1" is equal or more privileged than "State 2".
     *
     * @param state1 The State 1
     * @param state2 The State 2
     * @returns True if "State 1" is equal or more privileged than "State 2"
     */
    export function isAtLeast(state1: States | null, state2: States | null): boolean {
        if (state1 == null || state2 == null) {
            return false;
        }
        return state1 >= state2;
    }

    /**
     * Evaluates whether "State 1" is equal or less privileged than "State 2".
     *
     * @param state1 The State 1
     * @param state2 The State 2
     * @returns True if "State 1" is equal or less privileged than "State 2"
     */
    export function isAtMost(state1: States | null, state2: States | null): boolean {
        if (state1 == null || state2 == null) {
            return false;
        }
        return state1 <= state2;
    }
}
