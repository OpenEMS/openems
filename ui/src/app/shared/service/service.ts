import { Injectable, ErrorHandler } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { Cookie } from 'ng2-cookies';

import { DefaultTypes } from './defaulttypes';
import { ActivatedRoute } from '@angular/router';

@Injectable()
export class Service implements ErrorHandler {
    public notificationEvent: Subject<DefaultTypes.Notification> = new Subject<DefaultTypes.Notification>();

    constructor(
        public translate: TranslateService
    ) {
        // add language
        translate.addLangs(["de", "en", "cz", "nl"]);
        // this language will be used as a fallback when a translation isn't found in the current language
        translate.setDefaultLang('de');
    }

    /**
     * Sets the application language
     */
    public setLang(id: DefaultTypes.LanguageTag) {
        this.translate.use(id);
        // TODO set locale for date-fns: https://date-fns.org/docs/I18n
    }

    /**
     * Gets the token from the cookie
     */
    public getToken(): string {
        return Cookie.get("token");
    }

    /**
     * Sets the token in the cookie
     */
    public setToken(token: string) {
        Cookie.set("token", token);
    }

    /**
     * Removes the token from the cookie
     */
    public removeToken() {
        Cookie.delete("token");
    }

    /**
     * Shows a nofication using toastr
     */
    public notify(notification: DefaultTypes.Notification) {
        this.notificationEvent.next(notification);
    }

    /**
     * Handles an application error
     */
    public handleError(error: any) {
        console.error(error);
        // let notification: Notification = {
        //     type: "error",
        //     message: error
        // };
        // this.notify(notification);
    }
}