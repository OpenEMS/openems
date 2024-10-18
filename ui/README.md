# OpenEMS UI

This project was generated with [angular-cli](https://github.com/angular/angular-cli).

## Theme OpenEMS

- OpenEMS Edge - expects a Edge *Controller.Api.Websocket* on default port `8085`

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

## Testing
- Testing
   `ng test`
- Testing with Karma UI
   `ng test -c "local"`

## Further help

## Creating a Theme

- Create new folder under `/src/themes`
   - Files in `root` will be copied to `/` of the OpenEMS UI
   - `scss/variables.scss` will be used for styling
   - `environments/*.ts` define settings for Backend/Edge and development/production environments
- Generate contents of `root` folder using https://realfavicongenerator.net.
   Place them in `root` subdirectory
- Add entries in `angular.json` according to the original openems-configurations

### Create Android & iOS App

* Add Configuration to [capacitor](capacitor.config.ts)
* Build app:
`NODE_ENV=$theme ionic cap build android -c "$theme,$theme-backend-deploy-app"`
* Extend [build.gradle](android/app/build.gradle)
* Extend [capacitor](capacitor.config.ts) 
* Build your assets: `npx @capacitor/assets generate --logoSplashScale 0.3 --pwaManifestPath src/manifest.webmanifest`
> [!IMPORTANT]  
> Crucial information necessary for users to succeed. Only provide /resources/logo-dark.png and logo.png
* Move the files from res(except values and xml) to ```/android/app/src/$theme/``` (```/main``` acts as default)
* Build apps (execute in order):
   - `NODE_ENV="{$theme}" ./node_modules/.bin/ionic cap build android -c "$theme,$theme-backend-deploy-app" --no-open;`
   - `THEME="{$theme}" gradlew bundleThemeRelease`

Important (if not generated, can be copied and adjusted from existing theme):
- `ui\android\app\src\{$theme}\res\xml\file_paths.xml`
- `ui\android\app\src\{$theme}\res\values`

### Debugging

Use `gradlew install{$theme}Release to install it on any device`

- Available Tasks: `gradlew tasks`
- list available devices + emulators: `$npx native-run android --list --json`
- use Android Studio for Debugging: `$ionic cap open android`

## i18n - internationalization

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

#### Debugging Angular PWA Via USB-Connection
Please follow this: https://medium.com/nerd-for-tech/google-chrome-how-to-inspect-websites-on-mobile-devices-804677f863ce
