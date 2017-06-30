import { TranslateLoader } from '@ngx-translate/core';
import { Observable } from 'rxjs/Observable';

export class MyTranslateLoader implements TranslateLoader {

    constructor() { }

    getTranslation(lang: string): Observable<any> {
        if (lang === 'de') {
            return Observable.of(
                /*
                 * German translation
                 */
                {
                    Overview: {
                        ConnectionSuccessful: "Verbindung zu {{value}} hergestellt.",
                        ConnectionFailed: "Verbindung zu {{value}} getrennt."
                    }
                }
            );
        } else {
            return Observable.of(
                /*
                 * English translation
                 */
                {
                    Overview: {
                        ConnectionSuccessful: "Successfully connected to {{value}}.",
                        ConnectionFailed: "Connection to {{value}} failed."
                    }
                }
            );
        }
    }
}
