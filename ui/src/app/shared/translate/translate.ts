import { TranslateLoader } from '@ngx-translate/core';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/of';

import { TRANSLATION as DE } from './de';
import { TRANSLATION as EN } from './en';
import { TRANSLATION as CZ } from './cz';
import { TRANSLATION as NL } from './nl';

export class MyTranslateLoader implements TranslateLoader {

    constructor() { }

    getTranslation(lang: string): Observable<any> {
        if (lang == 'de') {
            return Observable.of(DE);
        } else if (lang == 'cz') {
            return Observable.of(CZ);
        } else if (lang == 'nl') {
            return Observable.of(NL);
        } else {
            return Observable.of(EN);
        }
    }
}
