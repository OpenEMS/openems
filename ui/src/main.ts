import { enableProdMode } from "@angular/core";
import { platformBrowserDynamic } from "@angular/platform-browser-dynamic";
import { AppModule } from "src/app/APP.MODULE";
import { environment } from "src/environments";

if (ENVIRONMENT.PRODUCTION) {
  enableProdMode();
}

platformBrowserDynamic().bootstrapModule(AppModule)
  .catch(err => CONSOLE.LOG(err));
