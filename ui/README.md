# OpenemsGui

This project was generated with [angular-cli](https://github.com/angular/angular-cli).

## Development server

 - connect to live OpenEMS Backend server

    `ng serve -c backend-dev-live` (Expects openems-backend on `wss://localhost:443/openems-backend-ui`)

 - connect to local OpenEMS Backend server

    `ng serve -c backend-dev-local` (Expects openems-backend on `ws://localhost:8078`)

 - connect to local OpenEMS Edge

	`ng serve`  (Expects openems-edge on `ws://localhost:8075`)

## Build using angular-cli

Run `ng build` to build the project. The build artifacts will be stored in the `target` directory. Use the `-prod` flag for a production build.

Build for OpenEMS Backend:

`ng build -c backend`

Build for OpenEMS Edge:

`ng build -c edge`

Be aware that there is currently a [bug](https://github.com/angular/angular-cli/issues/11208) in how angular-cli generates the service-worker configuration file (ngsw-config.json). After running the above command it is required to fix 'regex' statements in the file, replacing double '`\\`' by single '`\`' to avoid wrong escaping.

## Further help

To get more help on the `angular-cli` use `ng help` or go check out the [Angular-CLI README](https://github.com/angular/angular-cli/blob/master/README.md).

### Development hints

#### i18n - internationalization

Translation is based on [ngx-translate](https://github.com/ngx-translate). The language can be changed at runtime in the "Über FEMS-UI" dialog.

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
private stopOnDestroy: Subject<void> = new Subject<void>();
ngOnInit() {
    /*subject*/.takeUntil(this.stopOnDestroy).subscribe(/*variable*/ => {
        ...
    });
}
ngOnDestroy() {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
}
```