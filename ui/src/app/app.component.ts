import { Component } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { SplashScreen } from '@ionic-native/splash-screen/ngx';
import { StatusBar } from '@ionic-native/status-bar/ngx';
import { Platform, ToastController, MenuController } from '@ionic/angular';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { environment } from '../environments';
import { Service, Websocket } from './shared/shared';
import { LanguageTag } from './shared/translate/language';

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html'
})
export class AppComponent {
  public env = environment;
  public backUrl: string | boolean = '/';
  public sideMenu: boolean;
  public navigation: boolean = false;

  private ngUnsubscribe: Subject<void> = new Subject<void>();

  constructor(
    private platform: Platform,
    private splashScreen: SplashScreen,
    private statusBar: StatusBar,
    public websocket: Websocket,
    public service: Service,
    public router: Router,
    public toastController: ToastController,
    public menu: MenuController,
  ) {
    // this.initializeApp();
    service.setLang(LanguageTag.DE);
  }

  initializeApp() {
    this.platform.ready().then(() => {
      this.statusBar.styleDefault();
      this.splashScreen.hide();
    });
  }

  ngOnInit() {
    this.service.notificationEvent.pipe(takeUntil(this.ngUnsubscribe)).subscribe(async notification => {
      const toast = await this.toastController.create({
        message: notification.message,
        showCloseButton: true,
        position: 'top',
        closeButtonText: 'Ok',
        duration: 2000
      });
      toast.present();
    });
    // set inital Side Menu
    this.updateMenu(window.location.pathname);
    // set initial backUrl
    this.updateBackUrl(window.location.pathname);
    // update backUrl on navigation events
    this.router.events.pipe(
      takeUntil(this.ngUnsubscribe),
      filter(event => event instanceof NavigationEnd)
    ).subscribe(event => {
      let url = (<NavigationEnd>event).urlAfterRedirects;
      this.updateBackUrl(url);
      this.updateMenu(url);
    })
  }

  updateMenu(url: string) {
    let urlArray = url.split('/');
    let file = urlArray.pop();

    if (file == 'settings' || file == 'about' || urlArray.length > 3) {
      this.sideMenu = false;
    } else {
      this.sideMenu = true;
    }
  }

  updateBackUrl(url: string) {
    // disable backUrl & Segment Navigation on initial 'index' page
    if (url === '/index') {
      this.backUrl = false;
      this.navigation = false;
      return;
    }

    let urlArray = url.split('/');
    let backUrl: string | boolean = '/';
    let file = urlArray.pop();

    // disable backUrl for History & EdgeIndex Component ++ Enable Segment Navigation
    if ((file == 'history' || file == 'live') && urlArray.length == 3) {
      this.backUrl = false;
      this.navigation = true;
      return;
    } else {
      this.navigation = false;
    }

    // disable backUrl to first 'index' page from Edge index if there is only one Edge in the system
    if (file === 'live' && urlArray.length == 3 && this.env.backend === "OpenEMS Edge") {
      this.backUrl = false;
      return;
    }

    // remove one part of the url for 'index'
    if (file === 'live') {
      urlArray.pop();
    }
    // re-join the url
    backUrl = urlArray.join('/') || '/';

    // correct path for '/device/[edgeId]/index'
    if (backUrl === '/device') {
      backUrl = '/';
    }
    this.backUrl = backUrl;
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }
}
