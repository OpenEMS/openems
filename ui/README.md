# OpenEMS UI

This project was generated with [angular-cli](https://github.com/angular/angular-cli).

## Theme OpenEMS

- OpenEMS Edge - expects a Edge *Controller.Api.Websocket* on default port `8075`

   - Serve to port `4200`
   
      `ng serve`

      `ng serve -o -c openems-edge-dev`

   - Build Development

      `ng build`

      `ng build -c "openems,openems-edge-dev"`

   - Build Production

      `ng build -c "openems,openems-edge-prod,prod"`

- OpenEMS Backend - expects a Backend *Ui.Websocket* on default port `8082`

   - Serve to port `4200`
   
      `ng serve -o -c openems-backend-dev`

   - Build Development

      `ng build -c "openems,openems-backend-dev"`

   - Build Production

      `ng build -c "openems,openems-backend-prod,prod"`

## Theme FENECON

- OpenEMS Edge

   - Serve to port `4200`
   
      `ng serve -o -c fenecon-edge-dev`

   - Build Development

      `ng build -c "fenecon,fenecon-edge-dev"`

   - Build Production

      `ng build -c "fenecon,fenecon-edge-prod,prod"`

- OpenEMS Backend

   - Serve to port `4200`
   
      `ng serve -o -c fenecon-backend-dev`

   - Build Development

      `ng build -c "fenecon,fenecon-backend-dev"`

   - Build Production

      `ng build -c "fenecon,fenecon-backend-prod,prod"`

## Theme Heckert

- OpenEMS Edge

   - Serve to port `4200`
   
      `ng serve -o -c heckert-edge-dev`

   - Build Development

      `ng build -c "heckert,heckert-edge-dev"`

   - Build Production

      `ng build -c "heckert,heckert-edge-prod,prod"`

- OpenEMS Backend

   - Serve to port `4200`
   
      `ng serve -o -c heckert-backend-dev`

   - Build Development

      `ng build -c "heckert,heckert-backend-dev"`

   - Build Production

      `ng build -c "heckert,heckert-backend-prod,prod"`

## Further help

#### Creating a Theme

- Create new folder under `/src/themes`
   - Files in `root` will be copied to `/` of the OpenEMS UI
   - `scss/variables.scss` will be used for styling
   - `environments/*.ts` define settings for Backend/Edge and development/production environments
- Generate contents of `root` folder using https://realfavicongenerator.net Place them in `root` subdirectory
- Add entries in `angular.json`

#### i18n - internationalization

Translation is based on [ngx-translate](https://github.com/ngx-translate). The language can be changed at runtime in the "About UI" dialog.

##### In HTML template use:

`<p translate>General.storageSystem</p>`

* add attribute 'translate'
* content of the tag is the path to translation in [translate.ts](app/shared/translate.ts) file

##### In typescript code use:
```
import { TranslateService } from '@ngx-translate/core';
constructor(translate: TranslateService) {}
this.translate.instant('General.storageSystem')
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


### Creating a UI-Widget

There are many examples of how ui widgets are created and used in [LiveComponent](src/app/edge/live/live.component.html) and [HistoryComponent](src/app/edge/history/history.component.html).

UI-Widgets created for the Live-View, mainly consist of two components: 
- `FlatWidget`: directly visible in Live-View:
- `ModalComponent`: Popover, that can be opened when clicking on a `FlatWidget`
- `Module`: Every `FlatWidget` and his corresponding `ModalComponent` should be wrapped inside their own module.



Step 1: Copy an existing Controller/Folder e.g. [FixActivePower](src/app/edge/live/Controller/Ess/FixActivePower/Ess_FixActivePower.ts).

Step 2: Rename the [Module](src/app/edge/live/Controller/Ess/FixActivePower/Ess_FixActivePower.ts) and import it in [LiveModule](src/app/edge/live/live.module.ts).

Step 3: Change the `@Component` selector and use this selector inside [LiveComponent](src/app/edge/live/live.component.html#L135). Widgets in this view are shown if they are either part of the `EdgeConfig` and the corresponding factoryId matches or are a `Common` Widget.

Step 4: The Widget should now be visible in the Live-View

If you looked at the code of e.g. [FixActivePower](src/app/edge/live/Controller/Ess/FixActivePower/Ess_FixActivePower.ts), you will notice that different widgets have been used.