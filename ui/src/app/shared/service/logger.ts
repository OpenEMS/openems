import { Injectable } from "@angular/core";
import { SendLogMessage } from "../jsonrpc/request/sendLogMessage";
import { Service } from "./service";

/**
 * The log level.
 */
export enum Level {
    DEBUG = "debug",
    INFO = "info",
    WARNING = "warning",
    ERROR = "error"
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
    private log(level: Level, msg: string) {
        this.service.websocket.sendRequest(new SendLogMessage({ level, msg })).catch(err => console.log(err));
    }

    /**
     * Log a messag at the DEBUG level.
     * 
     * @param msg the message to be logged
     */
    public debug(msg: string) {
        this.log(Level.DEBUG, msg);
    }

    /**
     * Log a messag at the INFO level.
     * 
     * @param msg the message to be logged
     */
    public info(msg: string) {
        this.log(Level.INFO, msg);
    }

    /**
     * Log a messag at the WARNING level.
     * 
     * @param msg the message to be logged
     */
    public warn(msg: string) {
        this.log(Level.WARNING, msg);
    }

    /**
     * Log a messag at the ERROR level.
     * 
     * @param msg the message to be logged
     */
    public error(msg: string) {
        this.log(Level.ERROR, msg);
    }

}