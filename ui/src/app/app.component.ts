import { Component, ChangeDetectorRef } from '@angular/core';
import { Edge, Service, Websocket, ChannelAddress } from './shared/shared';
import { environment } from '../environments';
import { filter, takeUntil } from 'rxjs/operators';
import { LanguageTag } from './shared/translate/language';
import { MenuController, Platform, ToastController, ModalController } from '@ionic/angular';
import { NavigationEnd, Router } from '@angular/router';
import { SplashScreen } from '@ionic-native/splash-screen/ngx';
import { StatusBar } from '@ionic-native/status-bar/ngx';
import { StatusSingleComponent } from './shared/status/single/status.component';
import { Subject, Subscription } from 'rxjs';

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html'
})
export class AppComponent {

  public env = environment;
  public backUrl: string | boolean = '/';
  public showChartPageOnly: boolean = true;
  public enableSideMenu: boolean;
  public currentPage: 'EdgeSettings' | 'Other' | 'IndexLive' | 'IndexHistory' = 'Other';
  public isSystemLogEnabled: boolean = false;
  private ngUnsubscribe: Subject<void> = new Subject<void>();

  constructor(
    private cdRef: ChangeDetectorRef,
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
    // set inital URL
    this.updateUrl(window.location.pathname);
    // update backUrl on navigation events
    this.router.events.pipe(
      takeUntil(this.ngUnsubscribe),
      filter(event => event instanceof NavigationEnd)
    ).subscribe(event => {
      window.scrollTo(0, 0);
      this.updateUrl((<NavigationEnd>event).urlAfterRedirects);
    })

    // subscribe for single status component
    this.service.currentEdge.pipe(takeUntil(this.ngUnsubscribe)).subscribe(edge => {
      if (edge != null) {
        edge.subscribeChannels(this.websocket, '', [
          new ChannelAddress('_sum', 'State'),
        ]);
      }
    })
  }

  // used to prevent 'Expression has changed after it was checked' error
  ngAfterViewChecked() {
    this.cdRef.detectChanges()
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

    // hides header for history charts
    if (file.endsWith('chart')) {
      this.showChartPageOnly = true;
    } else {
      this.showChartPageOnly = false;
    }

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
    if (urlArray.length >= 4) {
      file = urlArray[3];
    }
    // Enable Segment Navigation for Edge-Index-Page
    if ((file == 'history' || file == 'live') && urlArray.length == 3) {
      if (file == 'history') {
        this.currentPage = 'IndexHistory';
      } else {
        this.currentPage = 'IndexLive';
      }
    } else if (file == 'settings' && urlArray.length > 1) {
      this.currentPage = 'EdgeSettings';
    }
    else {
      this.currentPage = 'Other';
    }
  }

  updateLiveHistorySegment(event) {
    if (event.detail.value == "IndexLive") {
      this.router.navigateByUrl("/device/" + this.service.currentEdge.value.id + "/live");
    }
    if (event.detail.value == "IndexHistory") {
      this.router.navigateByUrl("/device/" + this.service.currentEdge.value.id + "/history");
    }
  }

  async presentSingleStatusModal() {
    const modal = await this.modalCtrl.create({
      component: StatusSingleComponent,
    });
    return await modal.present();
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }
}
