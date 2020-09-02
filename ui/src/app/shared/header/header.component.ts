import { Component, Input, ChangeDetectorRef } from '@angular/core';
import { Platform, MenuController, ModalController, ToastController } from '@ionic/angular';
import { SplashScreen } from '@ionic-native/splash-screen/ngx';
import { StatusBar } from '@ionic-native/status-bar/ngx';
import { Router, NavigationEnd } from '@angular/router';
import { Service, Websocket, ChannelAddress, Edge } from '../shared';
import { takeUntil, filter } from 'rxjs/operators';
import { environment } from 'src/environments';
import { Subject } from 'rxjs';
import { StatusSingleComponent } from '../status/single/status.component';


@Component({
    selector: 'header',
    templateUrl: './header.component.html'
})
export class HeaderComponent {

    public env = environment;
    public backUrl: string | boolean = '/';
    public enableSideMenu: boolean | null = null;
    public currentPage: 'EdgeSettings' | 'Other' | 'IndexLive' | 'IndexHistory' = 'Other';
    public isSystemLogEnabled: boolean = false;
    private ngUnsubscribe: Subject<void> = new Subject<void>();

    constructor(
        private cdRef: ChangeDetectorRef,
        public menu: MenuController,
        public modalCtrl: ModalController,
        public router: Router,
        public service: Service,
        public toastController: ToastController,
        public websocket: Websocket,
    ) { }

    ngOnInit() {
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

    public updateLiveHistorySegment(navigateTo) {
        if (navigateTo == "IndexLive") {
            this.router.navigateByUrl("/device/" + this.service.currentEdge.value.id + "/live");

        }
        if (navigateTo == "IndexHistory") {
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