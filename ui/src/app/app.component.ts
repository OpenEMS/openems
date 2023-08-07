import { Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { MenuController, ModalController, Platform, ToastController } from '@ionic/angular';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';

import { environment } from '../environments';
import { GlobalRouteChangeHandler } from './shared/service/globalRouteChangeHandler';
import { Service, UserPermission, Websocket } from './shared/shared';
import { Language } from './shared/type/language';

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html'
})
export class AppComponent implements OnInit, OnDestroy {

  public environment = environment;
  public backUrl: string | boolean = '/';
  public enableSideMenu: boolean;
  public isSystemLogEnabled: boolean = false;
  private ngUnsubscribe: Subject<void> = new Subject<void>();

  protected isUserAllowedToSeeOverview: boolean = false;

  constructor(
    private platform: Platform,
    public menu: MenuController,
    public modalCtrl: ModalController,
    public router: Router,
    public service: Service,
    public toastController: ToastController,
    public websocket: Websocket,
    private globalRouteChangeHandler: GlobalRouteChangeHandler,
    private titleService: Title
  ) {
    service.setLang(Language.getByKey(localStorage.LANGUAGE) ?? Language.getByBrowserLang(navigator.language));

    this.service.metadata.pipe(filter(metadata => !!metadata)).subscribe(metadata => {
      this.isUserAllowedToSeeOverview = UserPermission.isUserAllowedToSeeOverview(metadata.user);
    });
  }

  ngOnInit() {

    // Checks if sessionStorage is not null, undefined or empty string
    if (localStorage.getItem("DEBUGMODE")) {
      this.environment.debugMode = JSON.parse(localStorage.getItem("DEBUGMODE"));
    }

    this.titleService.setTitle(environment.edgeShortName);
    this.service.notificationEvent.pipe(takeUntil(this.ngUnsubscribe)).subscribe(async notification => {
      const toast = await this.toastController.create({
        message: notification.message,
        position: 'top',
        duration: 2000,
        buttons: [
          {
            text: 'Ok',
            role: 'cancel'
          }
        ]
      });
      toast.present();
    });

    this.platform.ready().then(() => {
      this.service.deviceHeight = this.platform.height();
      this.service.deviceWidth = this.platform.width();
      this.checkSmartphoneResolution(true);
      this.platform.resize.pipe(takeUntil(this.ngUnsubscribe)).subscribe(() => {
        this.service.deviceHeight = this.platform.height();
        this.service.deviceWidth = this.platform.width();
        this.checkSmartphoneResolution(false);
      });
    });
  }

  private checkSmartphoneResolution(init: boolean): void {
    if (init == true) {
      if (this.platform.width() <= 576) {
        this.service.isSmartphoneResolution = true;
        this.service.isSmartphoneResolutionSubject.next(true);
      } else if (this.platform.width() > 576) {
        this.service.isSmartphoneResolution = false;
        this.service.isSmartphoneResolutionSubject.next(false);
      }
    } else {
      if (this.platform.width() <= 576 && this.service.isSmartphoneResolution == false) {
        this.service.isSmartphoneResolution = true;
        this.service.isSmartphoneResolutionSubject.next(true);
      } else if (this.platform.width() > 576 && this.service.isSmartphoneResolution == true) {
        this.service.isSmartphoneResolution = false;
        this.service.isSmartphoneResolutionSubject.next(false);
      }
    }
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }
}
