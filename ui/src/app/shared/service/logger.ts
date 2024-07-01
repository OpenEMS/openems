import { Injectable } from "@angular/core";
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

    public constructor(private service: Service) { }

    /**
     * Sends the given message via a websocket request.
     *
     * @param level the log level
     * @param msg the message to be logged
     */
    private sendLogMessageNotification(level: Level, msg: string) {

        if (environment.production == true) {
            this.service.websocket.sendNotification(new LogMessageNotification({ level: level, msg: msg }));
        }
    }

    /**
     * Log a messag at the DEBUG level.
     *
     * @param msg the message to be logged
     */
    public debug(msg: string) {
        this.sendLogMessageNotification(Level.DEBUG, msg);
    }

    /**
     * Log a messag at the INFO level.
     *
     * @param msg the message to be logged
     */
    public info(msg: string) {
        this.sendLogMessageNotification(Level.INFO, msg);
    }

    /**
     * Log a messag at the WARNING level.
     *
     * @param msg the message to be logged
     */
    public warn(msg: string) {
        this.sendLogMessageNotification(Level.WARNING, msg);
    }

    /**
     * Log a messag at the ERROR level.
     *
     * @param msg the message to be logged
     */
    public error(msg: string) {
        this.sendLogMessageNotification(Level.ERROR, msg);
    }

}
