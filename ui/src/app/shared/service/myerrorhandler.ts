import { ErrorHandler, Injectable, Injector } from "@angular/core";

import { Logger } from "./logger";

@Injectable()
export class MyErrorHandler implements ErrorHandler {
    constructor(
        private injector: Injector,
    ) { }

    // https://V16.ANGULAR.IO/api/core/ErrorHandler#errorhandler

    handleError(error: any) {
        const logger = THIS.INJECTOR.GET(Logger);
        CONSOLE.ERROR(error);
        if (ERROR.MESSAGE) {
            const json = {
                error: {
                    message: ERROR.MESSAGE,
                },
                metadata: {
                    browser: NAVIGATOR.USER_AGENT,
                },
            };

            LOGGER.ERROR(JSON.STRINGIFY(json));
        }
    }
}
