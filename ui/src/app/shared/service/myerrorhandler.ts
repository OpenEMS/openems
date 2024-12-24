import { ErrorHandler, Injectable, Injector } from "@angular/core";

import { Logger } from "./logger";

@Injectable()
export class MyErrorHandler implements ErrorHandler {
    constructor(
        private injector: Injector,
    ) { }

    // https://v16.angular.io/api/core/ErrorHandler#errorhandler
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    handleError(error: any) {
        const logger = this.injector.get(Logger);
        console.error(error);
        if (error.message) {
            const json = {
                error: {
                    message: error.message,
                },
                metadata: {
                    browser: navigator.userAgent,
                },
            };

            logger.error(JSON.stringify(json));
        }
    }
}
