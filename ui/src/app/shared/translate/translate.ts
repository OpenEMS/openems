import { TranslateLoader } from '@ngx-translate/core';
import { Observable } from 'rxjs/Observable';

import { TRANSLATION as DE } from './de';
import { TRANSLATION as EN } from './en';
import { TRANSLATION as CZ } from './cz';

export class MyTranslateLoader implements TranslateLoader {

    constructor() { }

    getTranslation(lang: string): Observable<any> {
        if (lang == 'de') {
            return Observable.of(DE);
        } else if (lang == 'cz') {
            return Observable.of(CZ);
        } else {
            return Observable.of(EN);
        }
    }
}
