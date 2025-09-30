// @ts-strict-ignore
import { Component, OnDestroy, OnInit } from "@angular/core";
import { Meta, Title } from "@angular/platform-browser";
import { NavigationEnd, Router } from "@angular/router";
import { SplashScreen } from "@capacitor/splash-screen";
import { MenuController, ModalController, NavController, Platform, ToastController } from "@ionic/angular";
import { Subject, Subscription } from "rxjs";
import { filter, takeUntil } from "rxjs/operators";
import { environment } from "../environments";
import { PlatFormService } from "./PLATFORM.SERVICE";
import { NavigationService } from "./shared/components/navigation/service/NAVIGATION.SERVICE";
import { AppStateTracker } from "./shared/ngrx-store/states";
import { GlobalRouteChangeHandler } from "./shared/service/globalRouteChangeHandler";
import { UserService } from "./shared/service/USER.SERVICE";
import { Service, UserPermission, Websocket } from "./shared/shared";
import { Language } from "./shared/type/language";

@Component({
  selector: "app-root",
  templateUrl: "APP.COMPONENT.HTML",
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

  private ngUnsubscribe: Subject<void> = new Subject<void>();
  private subscription: Subscription = new Subscription();

  constructor(
    private platform: Platform,
    public menu: MenuController,
    public modalCtrl: ModalController,
    public router: Router,
    public service: Service,
    private userService: UserService,
    public toastController: ToastController,
    public websocket: Websocket,
    private globalRouteChangeHandler: GlobalRouteChangeHandler,
    private meta: Meta,
    private appService: PlatFormService,
    private title: Title,
    private stateService: AppStateTracker,
    protected navigationService: NavigationService,
    protected navCtrl: NavController
  ) {
    SERVICE.SET_LANG(LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.GET_BY_BROWSER_LANG(NAVIGATOR.LANGUAGE));

    THIS.SUBSCRIPTION.ADD(
      THIS.SERVICE.METADATA.PIPE(filter(metadata => !!metadata)).subscribe(metadata => {
        THIS.IS_USER_ALLOWED_TO_SEE_OVERVIEW = USER_PERMISSION.IS_USER_ALLOWED_TO_SEE_OVERVIEW(METADATA.USER);
        THIS.IS_USER_ALLOWED_TO_SEE_FOOTER = USER_PERMISSION.IS_USER_ALLOWED_TO_SEE_FOOTER(METADATA.USER);
      }));

    THIS.SUBSCRIPTION.ADD(
      THIS.ROUTER.EVENTS.PIPE(filter(event => event instanceof NavigationEnd)).subscribe((e: NavigationEnd) => {
        // Hide footer for history detail views
        const segments = E.URL.SPLIT("/");
        THIS.IS_HISTORY_DETAIL_VIEW = SEGMENTS.SLICE(0, -1).includes("history");
      }));

    THIS.APP_SERVICE.LISTEN();
    SPLASH_SCREEN.HIDE();
  }

  ngOnDestroy() {
    THIS.NG_UNSUBSCRIBE.NEXT();
    THIS.NG_UNSUBSCRIBE.COMPLETE();
    THIS.SUBSCRIPTION.UNSUBSCRIBE();
  }

  ngOnInit() {

    // Checks if sessionStorage is not null, undefined or empty string
    if (LOCAL_STORAGE.GET_ITEM("DEBUGMODE")) {
      THIS.ENVIRONMENT.DEBUG_MODE = JSON.PARSE(LOCAL_STORAGE.GET_ITEM("DEBUGMODE"));
    }

    THIS.SERVICE.NOTIFICATION_EVENT.PIPE(takeUntil(THIS.NG_UNSUBSCRIBE)).subscribe(async notification => {
      const toast = await THIS.TOAST_CONTROLLER.CREATE({
        message: NOTIFICATION.MESSAGE,
        position: "top",
        duration: 2000,
        buttons: [
          {
            text: "Ok",
            role: "cancel",
          },
        ],
      });
      TOAST.PRESENT();
    });

    THIS.PLATFORM.READY().then(() => {
      // OEM colors exist only after ionic is initialized, so the notch color has to be set here
      const notchColor = getComputedStyle(DOCUMENT.DOCUMENT_ELEMENT).getPropertyValue("--ion-color-background");
      THIS.META.UPDATE_TAG(
        { name: "theme-color", content: notchColor },
      );
      THIS.SERVICE.DEVICE_HEIGHT = THIS.PLATFORM.HEIGHT();
      THIS.SERVICE.DEVICE_WIDTH = THIS.PLATFORM.WIDTH();
      THIS.CHECK_SMARTPHONE_RESOLUTION(true);
      THIS.PLATFORM.RESIZE.PIPE(takeUntil(THIS.NG_UNSUBSCRIBE)).subscribe(() => {
        THIS.SERVICE.DEVICE_HEIGHT = THIS.PLATFORM.HEIGHT();
        THIS.SERVICE.DEVICE_WIDTH = THIS.PLATFORM.WIDTH();
        THIS.CHECK_SMARTPHONE_RESOLUTION(false);
      });
    });

    THIS.TITLE.SET_TITLE(ENVIRONMENT.EDGE_SHORT_NAME);
  }

  private checkSmartphoneResolution(init: boolean): void {
    if (init == true) {
      if (THIS.PLATFORM.WIDTH() <= 576) {
        THIS.SERVICE.IS_SMARTPHONE_RESOLUTION = true;
        THIS.SERVICE.IS_SMARTPHONE_RESOLUTION_SUBJECT.NEXT(true);
      } else if (THIS.PLATFORM.WIDTH() > 576) {
        THIS.SERVICE.IS_SMARTPHONE_RESOLUTION = false;
        THIS.SERVICE.IS_SMARTPHONE_RESOLUTION_SUBJECT.NEXT(false);
      }
    } else {
      if (THIS.PLATFORM.WIDTH() <= 576 && THIS.SERVICE.IS_SMARTPHONE_RESOLUTION == false) {
        THIS.SERVICE.IS_SMARTPHONE_RESOLUTION = true;
        THIS.SERVICE.IS_SMARTPHONE_RESOLUTION_SUBJECT.NEXT(true);
      } else if (THIS.PLATFORM.WIDTH() > 576 && THIS.SERVICE.IS_SMARTPHONE_RESOLUTION == true) {
        THIS.SERVICE.IS_SMARTPHONE_RESOLUTION = false;
        THIS.SERVICE.IS_SMARTPHONE_RESOLUTION_SUBJECT.NEXT(false);
      }
    }
  }
}
