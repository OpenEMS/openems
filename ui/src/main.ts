import { enableProdMode } from "@angular/core";
import { platformBrowser } from "@angular/platform-browser";
import { AppModule } from "src/app/app.module";
import { environment } from "src/environments";

if (environment.production) {
    enableProdMode();
}

platformBrowser().bootstrapModule(AppModule)
    .catch(err => console.log(err));
