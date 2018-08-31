import { enableProdMode } from '@angular/core';
import { Component , OnInit} from '@angular/core';
declare var device;
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';

import { AppModule } from './app/app.module';
import { environment } from './environments/environment';

if (environment.production) {
  enableProdMode();
}
export class AppComponent implements OnInit{ 
 ngOnInit() { 
 document.addEventListener("deviceready", function() { 
 alert(device.platform); 
 }, false); 
 } 
}
platformBrowserDynamic().bootstrapModule(AppModule)
  .catch(err => console.log(err));
