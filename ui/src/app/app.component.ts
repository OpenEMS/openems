import { HttpClient } from '@angular/common/http';
import { ApplicationRef, Component, Injectable, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { SwUpdate } from '@angular/service-worker';
import { MenuController, ModalController, Platform, ToastController } from '@ionic/angular';
import { concat, interval, Subject, timer } from 'rxjs';
import { first, retry, switchMap, takeUntil } from 'rxjs/operators';

import { environment } from '../environments';
import { CheckForUpdateService } from './appupdateservice';
import { Service, Websocket } from './shared/shared';
import { Language } from './shared/type/language';

export interface CachetComponentStatus {
  data: {
    status: 1 /* Operational */ | 2 /* Performance Issues */ | 3 /* Partial Outage */ | 4 /* Major Outage */
  }
}

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

  protected isSystemOutage = false;

  constructor(
    private platform: Platform,
    private http: HttpClient,
    public menu: MenuController,
    public modalCtrl: ModalController,
    public router: Router,
    public service: Service,
    public toastController: ToastController,
    public websocket: Websocket,
    private titleService: Title,
    checkForUpdateService: CheckForUpdateService
  ) {
    service.setLang(Language.getByKey(localStorage.LANGUAGE) ?? Language.getByBrowserLang(navigator.language));
    checkForUpdateService.init();
  }

  ngOnInit() {
    this.pollSystemStatus();

    // Checks if sessionStorage is not null, undefined or empty string
    if (sessionStorage.getItem("DEBUGMODE")) {
      this.environment.debugMode = JSON.parse(sessionStorage.getItem("DEBUGMODE"));
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
            role: 'cancel',
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
      })
    })
  }

  /**
   * Get system status from https://status.fenecon.de
   */
  private pollSystemStatus(): void {
    timer(1 /* immediately */, 5 * 60 * 1000 /* and then every 5 minutes */)
      .pipe(
        switchMap(() => this.http.get<CachetComponentStatus>('https://status.fenecon.de/api/v1/components/3')),
        retry())
      .subscribe((data) => {
        switch (data.data.status) {
          case 2:
          case 3:
          case 4:
            this.isSystemOutage = true;
            break;
          case 1:
          default:
            this.isSystemOutage = false;
            break;
        }
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
