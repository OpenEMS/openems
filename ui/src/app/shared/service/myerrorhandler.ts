import { ErrorHandler, Injectable, Injector, inject } from "@angular/core";

import { Logger } from "./logger";

@Injectable()
export class MyErrorHandler implements ErrorHandler {
    private injector = inject(Injector);

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);

    constructor() { }

    // https://v16.angular.io/api/core/ErrorHandler#errorhandler

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
