// @ts-strict-ignore
import { AfterContentChecked, ChangeDetectorRef, Component, effect, OnDestroy, OnInit } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { Capacitor } from "@capacitor/core";
import { ModalController, ViewWillEnter } from "@ionic/angular";
import { Subject } from "rxjs";
import { environment } from "src/environments";

import { Theme as UserTheme } from "../edge/history/shared";
import { PlatFormService } from "../PLATFORM.SERVICE";
import { AuthenticateWithPasswordRequest } from "../shared/jsonrpc/request/authenticateWithPasswordRequest";
import { GetEdgesRequest } from "../shared/jsonrpc/request/getEdgesRequest";
import { User, UserSettings } from "../shared/jsonrpc/shared";
import { States } from "../shared/ngrx-store/states";
import { UserService } from "../shared/service/USER.SERVICE";
import { Edge, Service, Utils, Websocket } from "../shared/shared";


@Component({
  selector: "login",
  templateUrl: "./LOGIN.COMPONENT.HTML",
  standalone: false,
})
export class LoginComponent implements ViewWillEnter, AfterContentChecked, OnDestroy, OnInit {
  private static readonly DEFAULT_THEME: UserTheme = USER_THEME.LIGHT;
  public currentThemeMode: UserTheme;
  public environment = environment;
  public form: FormGroup;
  protected formIsDisabled: boolean = false;
  protected popoverActive: "android" | "ios" | null = null;
  protected showPassword: boolean = false;
  protected readonly operatingSystem = PLAT_FORM_SERVICE.DEVICE_INFO.OS;
  protected readonly isApp: boolean = CAPACITOR.GET_PLATFORM() !== "web";
  private stopOnDestroy: Subject<void> = new Subject<void>();
  private page = 0;

  constructor(
    public service: Service,
    public websocket: Websocket,
    public utils: Utils,
    private router: Router,
    private route: ActivatedRoute,
    private cdref: ChangeDetectorRef,
    protected modalCtrl: ModalController,
    private userService: UserService,
  ) {
    effect(() => {
      const user = THIS.USER_SERVICE.CURRENT_USER();
      THIS.CURRENT_THEME_MODE = USER_SERVICE.GET_VALID_BROWSER_THEME(user?.getThemeFromSettings() ?? LOCAL_STORAGE.GET_ITEM("THEME") as UserTheme);
    });
  }

  public static getCurrentTheme(user: User): UserTheme {
    return (user?.settings[USER_SETTINGS.THEME] ?? LOCAL_STORAGE.GET_ITEM("THEME") ?? this.DEFAULT_THEME) as UserTheme;
  }
  /**
   * Preprocesses the credentials
   *
   * @param password the password
   * @param username the username
   * @returns trimmed credentials
   */
  public static preprocessCredentials(password: string, username?: string): { password: string, username?: string } {
    return {
      password: password?.trim(),
      ...(username && { username: username?.trim().toLowerCase() }),
    };
  }

  ngAfterContentChecked() {
    THIS.CDREF.DETECT_CHANGES();
  }

  ngOnInit() {
    const interval = setInterval(() => {
      if (THIS.WEBSOCKET.STATUS === "online" && !THIS.ROUTER.URL.SPLIT("/").includes("live")) {
        THIS.ROUTER.NAVIGATE(["/overview"]);
        clearInterval(interval);
      }
    }, 1000);
  }


  async ionViewWillEnter() {
    // Execute Login-Request if url path matches 'demo'
    if (THIS.ROUTE.SNAPSHOT.ROUTE_CONFIG.PATH == "demo") {

      await new Promise((resolve) => setTimeout(() => {

        // Wait for Websocket
        if (THIS.WEBSOCKET.STATUS == "waiting for credentials") {
          THIS.SERVICE.START_SPINNER("loginspinner");
          const lang = THIS.ROUTE.SNAPSHOT.QUERY_PARAM_MAP.GET("lang") ?? null;
          if (lang) {
            localStorage.DEMO_LANGUAGE = lang;
          }
          resolve(
            THIS.DO_DEMO_LOGIN({ username: "demo", password: "demo" }));
        }
      }, 2000));
    } else {
      LOCAL_STORAGE.REMOVE_ITEM("DEMO_LANGUAGE");
    }
  }

  /**
   * Login to OpenEMS Edge or Backend.
   *
   * @param param data provided in login form
   */
  public doLogin(param: { username?: string, password: string }) {

    THIS.WEBSOCKET.STATE.SET(States.AUTHENTICATION_WITH_CREDENTIALS);
    param = LOGIN_COMPONENT.PREPROCESS_CREDENTIALS(PARAM.PASSWORD, PARAM.USERNAME);

    // Prevent that user submits via keyevent 'enter' multiple times
    if (THIS.FORM_IS_DISABLED) {
      return;
    }

    THIS.FORM_IS_DISABLED = true;
    THIS.WEBSOCKET.LOGIN(new AuthenticateWithPasswordRequest(param))
      .finally(() => {
        THIS.ION_VIEW_WILL_ENTER();
        THIS.FORM_IS_DISABLED = false;
      });
  }

  /**
  * Login to OpenEMS Edge or Backend for demo user.
  *
  * @param param data provided in login form
  */
  public doDemoLogin(param: { username?: string, password: string }) {

    THIS.WEBSOCKET.LOGIN(new AuthenticateWithPasswordRequest(param)).then(() => {
      THIS.SERVICE.STOP_SPINNER("loginspinner");
    });

    return new Promise<Edge[]>((resolve, reject) => {

      const req = new GetEdgesRequest({ page: THIS.PAGE });

      THIS.SERVICE.GET_EDGES(req)
        .then((edges) => {
          setTimeout(() => {
            THIS.ROUTER.NAVIGATE(["/device", edges[0].id]);
          }, 100);
          resolve(edges);
        }).catch((err) => {
          reject(err);
        });
    }).finally(() => {
      THIS.SERVICE.STOP_SPINNER("loginspinner");
    },
    );
  }

  ngOnDestroy() {
    THIS.STOP_ON_DESTROY.NEXT();
    THIS.STOP_ON_DESTROY.COMPLETE();
  }

  protected async showPopoverOrRedirectToStore(operatingSystem: "android" | "ios") {
    const link: string | null = PLAT_FORM_SERVICE.GET_APP_STORE_LINK();
    if (link) {
      WINDOW.OPEN(link, "_blank");
    } else {
      THIS.POPOVER_ACTIVE = operatingSystem;
    }
  }

}
