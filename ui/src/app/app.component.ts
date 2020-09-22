import { Component } from '@angular/core';
import { Service, Websocket } from './shared/shared';
import { environment } from '../environments';
import { takeUntil } from 'rxjs/operators';
import { MenuController, Platform, ToastController, ModalController } from '@ionic/angular';
import { Router } from '@angular/router';
import { SplashScreen } from '@ionic-native/splash-screen/ngx';
import { StatusBar } from '@ionic-native/status-bar/ngx';
import { Subject } from 'rxjs';

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
    private splashScreen: SplashScreen,
    private statusBar: StatusBar,
    public menu: MenuController,
    public modalCtrl: ModalController,
    public router: Router,
    public service: Service,
    public toastController: ToastController,
    public websocket: Websocket,
  ) {
    // this.initializeApp();
    service.setLang(this.service.browserLangToLangTag(navigator.language));
  }

  // initializeApp() {
  //   this.platform.ready().then(() => {
  //     this.statusBar.styleDefault();
  //     this.splashScreen.hide();
  //   })
  // }

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

      this.platform.resize.pipe(takeUntil(this.ngUnsubscribe)).subscribe(() => {
        this.service.deviceHeight = this.platform.height();
        this.service.deviceWidth = this.platform.width();
        if (this.platform.width() <= 576) {
          this.service.isSmartphoneResolution = true;
        } else {
          this.service.isSmartphoneResolution = false;
        }
      })
    })
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }
}
