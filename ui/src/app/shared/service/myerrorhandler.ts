import { ErrorHandler, Injectable, Injector } from "@angular/core";

import { Logger } from "./logger";

@Injectable()
export class MyErrorHandler implements ErrorHandler {
    constructor(
        private injector: Injector,
    ) { }

    // https://v16.angular.io/api/core/ErrorHandler#errorhandler

    handleError(error: any) {
        const chunkFailedMessage = /Loading chunk [\d]+ failed/;
        const logger = this.injector.get(Logger);
        if (chunkFailedMessage.test(error.message)) {
            logger.error(error.message);
            window.location.reload();
        }

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
