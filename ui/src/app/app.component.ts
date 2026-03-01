// @ts-strict-ignore
import { Component, OnDestroy, OnInit } from "@angular/core";
import { Meta, Title } from "@angular/platform-browser";
import { NavigationEnd, Router } from "@angular/router";
import { SplashScreen } from "@capacitor/splash-screen";
import { MenuController, ModalController, NavController, Platform, ToastController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Subject, Subscription } from "rxjs";
import { filter, takeUntil } from "rxjs/operators";
import { environment } from "../environments";
import { PlatFormService } from "./platform.service";
import { NavigationService } from "./shared/components/navigation/service/navigation.service";
import { GlobalRouteChangeHandler } from "./shared/service/globalRouteChangeHandler";
import { LayoutRefreshService } from "./shared/service/layoutRefreshService";
import { RouteService } from "./shared/service/route.service";
import { Service, UserPermission, Websocket } from "./shared/shared";
import { Language } from "./shared/type/language";

@Component({
    selector: "app-root",
    templateUrl: "app.component.html",
    standalone: false,
})
export class AppComponent implements OnInit, OnDestroy {

    public environment = environment;
    public backUrl: string | boolean = "/";
    public enableSideMenu: boolean;
    public isSystemLogEnabled: boolean = false;

    protected isUserAllowedToSeeOverview: boolean = false;
    protected isUserAllowedToSeeFooter: boolean = false;
    protected isHistoryDetailView: boolean = false;
    protected latestIncident: { message: string | null, id: string } | null = null;

    private ngUnsubscribe: Subject<void> = new Subject<void>();
    private subscription: Subscription = new Subscription();

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
        private appService: PlatFormService,
        private title: Title,
        protected navigationService: NavigationService,
        protected navCtrl: NavController,
        private translate: TranslateService,
        private routeService: RouteService,
        private layoutRefresh: LayoutRefreshService,
    ) {
        service.setLang(Language.getCurrentLanguage());

        this.subscription.add(
            this.service.metadata.pipe(filter(metadata => !!metadata)).subscribe(metadata => {
                this.isUserAllowedToSeeOverview = UserPermission.isUserAllowedToSeeOverview(metadata.user);
                this.isUserAllowedToSeeFooter = UserPermission.isUserAllowedToSeeFooter(metadata.user);
            }));

        this.subscription.add(
            this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe((e: NavigationEnd) => {
                // Hide footer for history detail views
                const segments = e.url.split("/");
                this.isHistoryDetailView = segments.slice(0, -1).includes("history");
            }));

        this.appService.listen();
        SplashScreen.hide();
    }

    public navigateToUser() {
        const prev = this.routeService.getCurrentUrl();
        const base = prev.replace(/^\//, "");
        const userUrl = base + "/user";

        this.navCtrl.navigateRoot(userUrl);
        this.menu.close();
    }

    ngOnDestroy() {
        this.ngUnsubscribe.next();
        this.ngUnsubscribe.complete();
        this.subscription.unsubscribe();
    }

    ngOnInit() {
        // Checks if sessionStorage is not null, undefined or empty string
        if (localStorage.getItem("DEBUGMODE")) {
            this.environment.debugMode = JSON.parse(localStorage.getItem("DEBUGMODE"));
        }

        this.service.notificationEvent.pipe(takeUntil(this.ngUnsubscribe)).subscribe(async notification => {
            const toast = await this.toastController.create({
                message: notification.message,
                position: "top",
                duration: 2000,
                buttons: [
                    {
                        text: "Ok",
                        role: "cancel",
                    },
                ],
            });
            toast.present();
        });

        this.platform.ready().then(() => {
            // OEM colors exist only after ionic is initialized, so the notch color has to be set here
            const notchColor = getComputedStyle(document.documentElement)
                .getPropertyValue("--ion-color-background");
            this.meta.updateTag({ name: "theme-color", content: notchColor });

            this.appService.handleResize(this.platform, this.service, this.ngUnsubscribe);


        });

        this.title.setTitle(environment.edgeShortName);
    }

    /**
     * Called by the router-outlet (activate) event on every route change.
     * Triggers a delayed window resize so chart components recalculate their
     * dimensions (WCAG 1.4.4 compliance).
     */
    public onActivate(_event: any): void {
        this.layoutRefresh.request(200);
    }
}
