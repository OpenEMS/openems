# OpenEMS UI

This project was generated with [angular-cli](https://github.com/angular/angular-cli).

## Development server

 - connect to live OpenEMS Backend server

    `ng serve -c backend-dev-live` (Expects openems-backend on `wss://localhost:443/openems-backend-ui`)

 - connect to local OpenEMS Backend server

    `ng serve -c backend-dev-local` (Expects openems-backend on `ws://localhost:8078`)

 - connect to local OpenEMS Edge

	`ng serve`  (Expects openems-edge on `ws://localhost:8075`)

## Further help

#### i18n - internationalization

Translation is based on [ngx-translate](https://github.com/ngx-translate). The language can be changed at runtime in the "About UI" dialog.

##### In HTML template use:

`<p translate>General.StorageSystem</p>`

* add attribute 'translate'
* content of the tag is the path to translation in [translate.ts](app/shared/translate.ts) file

##### In typescript code use:
```
import { TranslateService } from '@ngx-translate/core';
constructor(translate: TranslateService) {}
this.translate.instant('General.StorageSystem')
```

#### Subscribe
For "subscribe" please follow this: https://stackoverflow.com/questions/38008334/angular-rxjs-when-should-i-unsubscribe-from-subscription
```
import { Subject } from 'rxjs/Subject';
import { takeUntil } from 'rxjs/operators';
private stopOnDestroy: Subject<void> = new Subject<void>();
ngOnInit() {
    /*subject*/.pipe(takeUntil(this.stopOnDestroy)).subscribe(/*variable*/ => {
        ...
    });
}
ngOnDestroy() {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
}
```