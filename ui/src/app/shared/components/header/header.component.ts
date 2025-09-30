// @ts-strict-ignore
import { AfterViewChecked, ChangeDetectorRef, Component, effect, Input, OnDestroy, OnInit, ViewChild } from "@angular/core";
import { NavigationEnd, Router } from "@angular/router";
import { MenuController, ModalController, NavController } from "@ionic/angular";
import { Subject } from "rxjs";
import { filter, takeUntil } from "rxjs/operators";
import { environment } from "src/environments";

import { RouteService } from "../../service/ROUTE.SERVICE";
import { Edge, Service, Websocket } from "../../shared";
import { NavigationService } from "../navigation/service/NAVIGATION.SERVICE";
import { PickDateComponent } from "../pickdate/PICKDATE.COMPONENT";
import { StatusSingleComponent } from "../status/single/STATUS.COMPONENT";

@Component({
    selector: "header",
    templateUrl: "./HEADER.COMPONENT.HTML",
    standalone: false,
})
export class HeaderComponent implements OnInit, OnDestroy, AfterViewChecked {

    @ViewChild(PickDateComponent, { static: false }) public PickDateComponent: PickDateComponent;

    public environment = environment;
    public backUrl: string | boolean = "/";
    public enableSideMenu: boolean;
    public currentPage: "EdgeSettings" | "Other" | "IndexLive" | "IndexHistory" = "Other";
    public isSystemLogEnabled: boolean = false;

    protected isHeaderAllowed: boolean = true;
    protected showBackButton: boolean = false;

    private ngUnsubscribe: Subject<void> = new Subject<void>();
    private _customBackUrl: string | null = null;

    constructor(
        private cdRef: ChangeDetectorRef,
        public menu: MenuController,
        public modalCtrl: ModalController,
        public router: Router,
        public routeService: RouteService,
        public service: Service,
        public websocket: Websocket,
        protected navigationService: NavigationService,
        protected navCtrl: NavController,
        private menuCtrl: MenuController,
    ) {

        effect(() => {
            THIS.SHOW_BACK_BUTTON = THIS.NAVIGATION_SERVICE.HEADER_OPTIONS().showBackButton;
        });
    }

    @Input() public set customBackUrl(url: string | null) {
        if (!url) {
            return;
        }
        this._customBackUrl = url;
        THIS.UPDATE_BACK_URL(url);
    }

    ngOnInit() {
        // set inital URL
        THIS.UPDATE_URL(THIS.ROUTER.ROUTER_STATE.SNAPSHOT.URL);
        // update backUrl on navigation events
        THIS.ROUTER.EVENTS.PIPE(
            takeUntil(THIS.NG_UNSUBSCRIBE),
            filter(event => event instanceof NavigationEnd),
        ).subscribe(event => {
            WINDOW.SCROLL_TO(0, 0);
            THIS.UPDATE_URL((<NavigationEnd>event).urlAfterRedirects);
        });

    }

    // used to prevent 'Expression has changed after it was checked' error
    ngAfterViewChecked() {
        THIS.CD_REF.DETECT_CHANGES();
    }

    updateUrl(url: string) {
        THIS.UPDATE_BACK_URL(url);
        THIS.UPDATE_ENABLE_SIDE_MENU(url);
        THIS.UPDATE_CURRENT_PAGE(url);
    }

    updateEnableSideMenu(url: string) {
        const urlArray = URL.SPLIT("/");
        const file = URL_ARRAY.POP();

        if (file == "user" || file == "settings" || file == "changelog" || file == "login" || file == "index" || URL_ARRAY.LENGTH > 3) {
            // disable side-menu; show back-button instead
            THIS.ENABLE_SIDE_MENU = false;
        } else {
            // enable side-menu if back-button is not needed
            THIS.ENABLE_SIDE_MENU = true;
        }
    }

    updateBackUrl(url: string) {

        if (this._customBackUrl) {
            THIS.BACK_URL = this._customBackUrl;
            return;
        }

        // disable backUrl & Segment Navigation on initial 'login' page
        if (url === "/login" || url === "/overview" || url === "/index") {
            THIS.BACK_URL = false;
            return;
        }


        // set backUrl for user when an Edge had been selected before
        const currentEdge: Edge = THIS.SERVICE.CURRENT_EDGE();
        if (url === "/user" && currentEdge != null) {
            THIS.BACK_URL = "/device/" + CURRENT_EDGE.ID + "/live";
            return;
        }

        // set backUrl for user if no edge had been selected
        if (url === "/user") {
            THIS.BACK_URL = "/overview";
            return;
        }

        if (url === "/changelog" && currentEdge != null) {
            // TODO this does not work if Changelog was opened from /user
            THIS.BACK_URL = "/device/" + CURRENT_EDGE.ID + "/settings/profile";
            return;
        }

        const urlArray = URL.SPLIT("/");
        let backUrl: string | boolean = "/";
        const file = URL_ARRAY.POP();

        // disable backUrl for History & EdgeIndex Component ++ Enable Segment Navigation
        if ((file == "history" || file == "live") && URL_ARRAY.LENGTH == 3) {
            THIS.BACK_URL = false;
            return;
        }

        // disable backUrl to first 'index' page from Edge index if there is only one Edge in the system
        if (file === "live" && URL_ARRAY.LENGTH == 3 && THIS.ENVIRONMENT.BACKEND === "OpenEMS Edge") {
            THIS.BACK_URL = false;
            return;
        }

        // remove one part of the url for 'index'
        if (file === "live") {
            URL_ARRAY.POP();
        }

        // fix url for App "settings/app/install" and "settings/app/update"
        if (URL_ARRAY.SLICE(-3, -1).join("/") === "settings/app") {
            URL_ARRAY.POP();
        }

        // re-join the url
        backUrl = URL_ARRAY.JOIN("/") || "/";

        // correct path for '/device/[edgeId]/index'
        if (backUrl === "/device") {
            backUrl = "/";
        }
        THIS.BACK_URL = backUrl;
    }

    updateCurrentPage(url: string) {
        const urlArray = URL.SPLIT("/");
        let file = URL_ARRAY.POP();
        if (URL_ARRAY.LENGTH >= 4) {
            file = urlArray[3];
        }
        // Enable Segment Navigation for Edge-Index-Page
        if ((file == "history" || file == "live") && URL_ARRAY.LENGTH == 3) {
            if (file == "history") {
                THIS.CURRENT_PAGE = "IndexHistory";
            } else {
                THIS.CURRENT_PAGE = "IndexLive";
            }
        } else if (file == "settings" && URL_ARRAY.LENGTH > 1) {
            THIS.CURRENT_PAGE = "EdgeSettings";
        }
        else {
            THIS.CURRENT_PAGE = "Other";
        }
    }

    public segmentChanged(event) {
        if (EVENT.DETAIL.VALUE == "IndexLive") {
            THIS.NAV_CTRL.NAVIGATE_ROOT(["/device/" + THIS.SERVICE.CURRENT_EDGE().id + "/live"], { replaceUrl: true });
            THIS.CD_REF.DETECT_CHANGES();
        }
        if (EVENT.DETAIL.VALUE == "IndexHistory") {

            /** Creates bug of being infinite forwarded betweeen live and history, if not relatively routed  */
            // THIS.ROUTER.NAVIGATE(["../history"], { relativeTo: THIS.ROUTE });
            THIS.NAV_CTRL.NAVIGATE_ROOT(["/device/" + THIS.SERVICE.CURRENT_EDGE().id + "/history"]);
            THIS.CD_REF.DETECT_CHANGES();
        }
    }

    async presentSingleStatusModal() {
        const modal = await THIS.MODAL_CTRL.CREATE({
            component: StatusSingleComponent,
        });
        return await MODAL.PRESENT();
    }

    ngOnDestroy() {
        THIS.NG_UNSUBSCRIBE.NEXT();
        THIS.NG_UNSUBSCRIBE.COMPLETE();
    }

    protected toggleMenu() {
        THIS.MENU.TOGGLE();
    }
}
