import { Component } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { SplashScreen } from '@ionic-native/splash-screen/ngx';
import { StatusBar } from '@ionic-native/status-bar/ngx';
import { Platform, ToastController, MenuController } from '@ionic/angular';
import { Subject, fromEvent } from 'rxjs';
import { filter, takeUntil, debounceTime, delay } from 'rxjs/operators';
import { environment } from '../environments';
import { Service, Websocket, Edge } from './shared/shared';
import { LanguageTag } from './shared/translate/language';

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html'
})
export class AppComponent {
  public env = environment;
  public backUrl: string | boolean = '/';
  public enableSideMenu: boolean;
  public currentPage: 'Other' | 'IndexLive' | 'IndexHistory' = 'Other';
  public isSystemLogEnabled: boolean = false;

  private ngUnsubscribe: Subject<void> = new Subject<void>();

  constructor(
    private platform: Platform,
    private splashScreen: SplashScreen,
    private statusBar: StatusBar,
    public websocket: Websocket,
    public service: Service,
    public router: Router,
    public toastController: ToastController,
    public menu: MenuController
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
    // set inital URL
    this.updateUrl(window.location.pathname);
    // update backUrl on navigation events
    this.router.events.pipe(
      takeUntil(this.ngUnsubscribe),
      filter(event => event instanceof NavigationEnd)
    ).subscribe(event => {
      this.updateUrl((<NavigationEnd>event).urlAfterRedirects);
    })
  }

  updateUrl(url: string) {
    this.updateBackUrl(url);
    this.updateEnableSideMenu(url);
    this.updateCurrentPage(url);
  }

  updateEnableSideMenu(url: string) {
    let urlArray = url.split('/');
    let file = urlArray.pop();

    if (file == 'settings' || file == 'about' || urlArray.length > 3) {
      // disable side-menu; show back-button instead
      this.enableSideMenu = false;
    } else {
      // enable side-menu if back-button is not needed 
      this.enableSideMenu = true;
    }
  }

  updateBackUrl(url: string) {
    // disable backUrl & Segment Navigation on initial 'index' page
    if (url === '/index') {
      this.backUrl = false;
      return;
    }

    // set backUrl for general settings when an Edge had been selected before
    let currentEdge: Edge = this.service.currentEdge.value;
    if (url === '/settings' && currentEdge != null) {
      this.backUrl = '/device/' + currentEdge.id + "/live"
      return;
    }

    let urlArray = url.split('/');
    let backUrl: string | boolean = '/';
    let file = urlArray.pop();

    // disable backUrl for History & EdgeIndex Component ++ Enable Segment Navigation
    if ((file == 'history' || file == 'live') && urlArray.length == 3) {
      this.backUrl = false;
      return;
    } else {
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

  updateCurrentPage(url: string) {
    let urlArray = url.split('/');
    let file = urlArray.pop();

    // Enable Segment Navigation for Edge-Index-Page
    if ((file == 'history' || file == 'live') && urlArray.length == 3) {
      if (file == 'history') {
        this.currentPage = 'IndexHistory';
      } else {
        this.currentPage = 'IndexLive';
      }
    } else {
      this.currentPage = 'Other';
    }
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }
}
