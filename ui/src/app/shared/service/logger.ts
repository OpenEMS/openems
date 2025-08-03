import { Injectable, inject } from "@angular/core";
import { Router } from "@angular/router";
import { Changelog } from "src/app/changelog/view/component/changelog.constants";
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
    private service = inject(Service);
    private router = inject(Router);


    private previousMessage: LogMessageNotification["params"] | undefined;
    private messageCounter: number = 0;

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);

    public constructor() { }

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

    /**
     * Sends the given message via a websocket request.
     *
     * @param level the log level
     * @param msg the message to be logged
     */
    private sendLogMessageNotification(level: Level, msg: string) {
        if (environment.production == false) {
            return;
        }

        const message: LogMessageNotification["params"] = { level: level, msg: msg };
        if (!this.previousMessage
            || message.level !== this.previousMessage.level
            || message.msg !== this.previousMessage.msg) {
            this.previousMessage = message;
            this.messageCounter = 0;
        }
        this.messageCounter++;
        if (!isPowerOf2(this.messageCounter) && this.messageCounter % 1024 !== 0) {
            return;
        }

        const page = this.router.url;
        this.service.websocket.sendNotification(new LogMessageNotification({
            level: message.level,
            msg: "[count=" + this.messageCounter
                + ";page=" + page
                + ";version=" + Changelog.UI_VERSION
                + "] " + message.msg,
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
