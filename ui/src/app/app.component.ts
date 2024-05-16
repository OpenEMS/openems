// @ts-strict-ignore
import { Component, OnDestroy, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { MenuController, ModalController, Platform, ToastController } from '@ionic/angular';
import { Subject, Subscription } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';

import { Meta } from '@angular/platform-browser';
import { environment } from '../environments';
import { GlobalRouteChangeHandler } from './shared/service/globalRouteChangeHandler';
import { Service, UserPermission, Websocket } from './shared/shared';
import { Language } from './shared/type/language';

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html',
})
export class AppComponent implements OnInit, OnDestroy {

  public environment = environment;
  public backUrl: string | boolean = '/';
  public enableSideMenu: boolean;
  public isSystemLogEnabled: boolean = false;
  private ngUnsubscribe: Subject<void> = new Subject<void>();
  private subscription: Subscription = new Subscription();

  protected isUserAllowedToSeeOverview: boolean = false;
  protected isUserAllowedToSeeFooter: boolean = false;
  protected isHistoryDetailView: boolean = false;

  constructor(
    private platform: Platform,
    public menu: MenuController,
    public modalCtrl: ModalController,
    public router: Router,
    public service: Service,
    public toastController: ToastController,
    public websocket: Websocket,
    private globalRouteChangeHandler: GlobalRouteChangeHandler,
    private meta: Meta,
  ) {
    service.setLang(Language.getByKey(localStorage.LANGUAGE) ?? Language.getByBrowserLang(navigator.language));


    this.subscription.add(
      this.service.metadata.pipe(filter(metadata => !!metadata)).subscribe(metadata => {
        this.isUserAllowedToSeeOverview = UserPermission.isUserAllowedToSeeOverview(metadata.user);
        this.isUserAllowedToSeeFooter = UserPermission.isUserAllowedToSeeFooter(metadata.user);
      }));

    this.subscription.add(
      this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe((e: NavigationEnd) => {
        // Hide footer for history detail views
        const segments = e.url.split('/');
        this.isHistoryDetailView = segments.slice(0, -1).includes('history');
      }));
  }

  ngOnInit() {

    // Checks if sessionStorage is not null, undefined or empty string
    if (localStorage.getItem("DEBUGMODE")) {
      this.environment.debugMode = JSON.parse(localStorage.getItem("DEBUGMODE"));
    }

    this.service.notificationEvent.pipe(takeUntil(this.ngUnsubscribe)).subscribe(async notification => {
      const toast = await this.toastController.create({
        message: notification.message,
        position: 'top',
        duration: 2000,
        buttons: [
          {
            text: 'Ok',
            role: 'cancel',
          },
        ],
      });
      toast.present();
    });

    this.platform.ready().then(() => {

      // OEM colors exist only after ionic is initialized, so the notch color has to be set here
      const notchColor = getComputedStyle(document.documentElement).getPropertyValue('--ion-color-background');
      this.meta.updateTag(
        { name: 'theme-color', content: notchColor },
      );
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
    this.subscription.unsubscribe();
  }
}
