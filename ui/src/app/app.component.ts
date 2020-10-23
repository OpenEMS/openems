import { Component } from '@angular/core';
import { environment } from '../environments';
import { MenuController, ModalController, Platform, ToastController } from '@ionic/angular';
import { Router } from '@angular/router';
import { Service, Websocket } from './shared/shared';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html'
})
export class AppComponent {

  public env = environment;
  public backUrl: string | boolean = '/';
  public enableSideMenu: boolean;
  public currentPage: 'EdgeSettings' | 'Other' | 'IndexLive' | 'IndexHistory' = 'Other';
  public isSystemLogEnabled: boolean = false;
  private ngUnsubscribe: Subject<void> = new Subject<void>();

  constructor(
    private platform: Platform,
    public menu: MenuController,
    public modalCtrl: ModalController,
    public router: Router,
    public service: Service,
    public toastController: ToastController,
    public websocket: Websocket,
  ) {
    service.setLang(this.service.browserLangToLangTag(navigator.language));
    this.platform.backButton.subscribe(() => {
      console.log("BackUrl: " + this.backUrl);
      if (this.backUrl) {
        this.router.navigate([this.backUrl]);

      } else {
        navigator['app'].exitApp();

      }

    });

  }

  ngOnInit() {
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

  public doLogout() {
    this.websocket.logOut();
  }
}
