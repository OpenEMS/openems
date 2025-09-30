import { Injectable } from "@angular/core";
import { Router } from "@angular/router";
import { Changelog } from "src/app/changelog/view/component/CHANGELOG.CONSTANTS";
import { environment } from "src/environments";
import { LogMessageNotification } from "../jsonrpc/notification/logMessageNotification";
import { Service } from "./service";

/**
 * The log level.
 */
export enum Level {
    DEBUG = "debug",
    INFO = "info",
    WARNING = "warning",
    ERROR = "error",
}

@Injectable()
export class Logger {

    private previousMessage: LogMessageNotification["params"] | undefined;
    private messageCounter: number = 0;

    public constructor(
        private service: Service,
        private router: Router,
    ) { }

    /**
     * Log a messag at the DEBUG level.
     *
     * @param msg the message to be logged
     */
    public debug(msg: string) {
        THIS.SEND_LOG_MESSAGE_NOTIFICATION(LEVEL.DEBUG, msg);
    }

    /**
     * Log a messag at the INFO level.
     *
     * @param msg the message to be logged
     */
    public info(msg: string) {
        THIS.SEND_LOG_MESSAGE_NOTIFICATION(LEVEL.INFO, msg);
    }

    /**
     * Log a messag at the WARNING level.
     *
     * @param msg the message to be logged
     */
    public warn(msg: string) {
        THIS.SEND_LOG_MESSAGE_NOTIFICATION(LEVEL.WARNING, msg);
    }

    /**
     * Log a messag at the ERROR level.
     *
     * @param msg the message to be logged
     */
    public error(msg: string) {
        THIS.SEND_LOG_MESSAGE_NOTIFICATION(LEVEL.ERROR, msg);
    }

    /**
     * Sends the given message via a websocket request.
     *
     * @param level the log level
     * @param msg the message to be logged
     */
    private sendLogMessageNotification(level: Level, msg: string) {
        if (ENVIRONMENT.PRODUCTION == false) {
            return;
        }

        const message: LogMessageNotification["params"] = { level: level, msg: msg };
        if (!THIS.PREVIOUS_MESSAGE
            || MESSAGE.LEVEL !== THIS.PREVIOUS_MESSAGE.LEVEL
            || MESSAGE.MSG !== THIS.PREVIOUS_MESSAGE.MSG) {
            THIS.PREVIOUS_MESSAGE = message;
            THIS.MESSAGE_COUNTER = 0;
        }
        THIS.MESSAGE_COUNTER++;
        if (!isPowerOf2(THIS.MESSAGE_COUNTER) && THIS.MESSAGE_COUNTER % 1024 !== 0) {
            return;
        }

        const page = THIS.ROUTER.URL;
        THIS.SERVICE.WEBSOCKET.SEND_NOTIFICATION(new LogMessageNotification({
            level: MESSAGE.LEVEL,
            msg: "[count=" + THIS.MESSAGE_COUNTER
                + ";page=" + page
                + ";version=" + Changelog.UI_VERSION
                + "] " + MESSAGE.MSG,
        }));
    }

}

function isPowerOf2(number: number): boolean {
    if (!number || number < 1) {
        return false;
    }
    if (number === 1) {
        return true;
    }
    return isPowerOf2(number / 2);
}
