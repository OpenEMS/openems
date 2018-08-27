import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ToasterService } from 'angular2-toaster';
import { filter } from 'rxjs/operators';

import { Platform, PopoverController } from '@ionic/angular';
import { SplashScreen } from '@ionic-native/splash-screen/ngx';
import { StatusBar } from '@ionic-native/status-bar/ngx';

import { environment } from '../environments';
import { Service, Websocket } from './shared/shared';

import { PopoverPage } from './shared/popover/popover.component';
import { Router, NavigationEnd } from '@angular/router';
import { Location } from '@angular/common';


@Component({
  selector: 'root',
  templateUrl: 'app.component.html'
})
export class AppComponent {
  public env = environment;
  public backUrl: string | boolean = '/';

  private ngUnsubscribe: Subject<void> = new Subject<void>();

  constructor(
    public websocket: Websocket,
    public service: Service,
    private toaster: ToasterService,
    private popoverController: PopoverController,
    public router: Router,
  ) {
    service.setLang('de');

    /*
     * Parse URL for global 'back' button
     */
    router.events.pipe(
      takeUntil(this.ngUnsubscribe),
      filter(event => event instanceof NavigationEnd)
    ).subscribe(event => {
      let url = (<NavigationEnd>event).urlAfterRedirects;
      let urlArray = url.split('/');
      let file = urlArray.pop();
      // remove one more part of the url for 'index'
      if (file === 'index') {
        urlArray.pop();
      }
      // re-join the url
      let backUrl: string | boolean = urlArray.join('/') || '/';
      // correct path for '/device/[edgeName]/index'
      if (backUrl === '/device') {
        backUrl = '/';
      }
      // disable backUrl to first 'index' page if there is only one Edge in the system
      if (Object.keys(this.websocket.edges.getValue()).length == 1 && backUrl === '/') {
        backUrl = false;
      }
      this.backUrl = backUrl;
    })
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

}
