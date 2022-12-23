import { ErrorHandler, Injectable, Injector } from "@angular/core";
import { Logger } from "./logger";

@Injectable()
export class MyErrorHandler implements ErrorHandler {
    constructor(
        private injector: Injector
    ) { }

    handleError(error: any) {
        let logger = this.injector.get(Logger);
        console.error(error);
        if (error.message) {
            logger.error(error.message);
        }
    }
}