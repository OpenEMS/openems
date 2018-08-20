import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ToasterService } from 'angular2-toaster';

import { Platform, PopoverController } from '@ionic/angular';
import { SplashScreen } from '@ionic-native/splash-screen/ngx';
import { StatusBar } from '@ionic-native/status-bar/ngx';

import { environment } from '../environments';
import { Service, Websocket } from './shared/shared';

import { PopoverPage } from './shared/popover/popover.component';
import { Router } from '@angular/router';
import { Location } from '@angular/common';


@Component({
  selector: 'root',
  templateUrl: 'app.component.html'
})
export class AppComponent {
  public env = environment;
  private navCollapsed: boolean = true;
  private ngUnsubscribe: Subject<void> = new Subject<void>();

  constructor(
    private platform: Platform,
    private splashScreen: SplashScreen,
    private statusBar: StatusBar,
    public websocket: Websocket,
    public service: Service,
    private toaster: ToasterService,
    private popoverController: PopoverController,
    public router: Router,
    private location: Location,
  ) {
    service.setLang('de');
  }

  ngOnInit() {
    this.service.notificationEvent.pipe(takeUntil(this.ngUnsubscribe)).subscribe(notification => {
      this.toaster.pop({ type: notification.type, body: notification.message });
    });
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  //Presents Popovermenu for Navbar
  async presentPopover(event: any) {
    const popover = await this.popoverController.create({
      component: PopoverPage,
      event: event,
      translucent: false
    });
    return await popover.present();
  }


  //Todo: Optimize Back Button for Stack Method
  navBack() {
    this.location.back();
  }

}
