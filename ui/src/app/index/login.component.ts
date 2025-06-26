// @ts-strict-ignore
import { AfterContentChecked, ChangeDetectorRef, Component, effect, OnDestroy, OnInit } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { Capacitor } from "@capacitor/core";
import { ModalController, ViewWillEnter } from "@ionic/angular";
import { Subject } from "rxjs";
import { environment } from "src/environments";

import { Theme as UserTheme } from "../edge/history/shared";
import { PlatFormService } from "../platform.service";
import { AuthenticateWithPasswordRequest } from "../shared/jsonrpc/request/authenticateWithPasswordRequest";
import { GetEdgesRequest } from "../shared/jsonrpc/request/getEdgesRequest";
import { User } from "../shared/jsonrpc/shared";
import { States } from "../shared/ngrx-store/states";
import { UserService } from "../shared/service/user.service";
import { Edge, Service, Utils, Websocket } from "../shared/shared";


@Component({
  selector: "login",
  templateUrl: "./login.component.html",
  standalone: false,
})
export class LoginComponent implements ViewWillEnter, AfterContentChecked, OnDestroy, OnInit {
  private static readonly DEFAULT_THEME: UserTheme = UserTheme.LIGHT;
  public currentThemeMode: UserTheme;
  public environment = environment;
  public form: FormGroup;
  protected formIsDisabled: boolean = false;
  protected popoverActive: "android" | "ios" | null = null;
  protected showPassword: boolean = false;
  protected readonly operatingSystem = PlatFormService.deviceInfo.os;
  protected readonly isApp: boolean = Capacitor.getPlatform() !== "web";
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
      const user = this.userService.currentUser();
      this.currentThemeMode = userService.getValidBrowserTheme(user?.getThemeFromSettings() ?? localStorage.getItem("THEME") as UserTheme);
    });
  }

  public static getCurrentTheme(user: User): UserTheme {
    return user?.settings["theme"] ?? localStorage.getItem("THEME") ?? this.DEFAULT_THEME;
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
    this.cdref.detectChanges();
  }

  ngOnInit() {
    const interval = setInterval(() => {
      if (this.websocket.status === "online" && !this.router.url.split("/").includes("live")) {
        this.router.navigate(["/overview"]);
        clearInterval(interval);
      }
    }, 1000);
  }


  async ionViewWillEnter() {
    // Execute Login-Request if url path matches 'demo'
    if (this.route.snapshot.routeConfig.path == "demo") {

      await new Promise((resolve) => setTimeout(() => {

        // Wait for Websocket
        if (this.websocket.status == "waiting for credentials") {
          this.service.startSpinner("loginspinner");
          const lang = this.route.snapshot.queryParamMap.get("lang") ?? null;
          if (lang) {
            localStorage.DEMO_LANGUAGE = lang;
          }
          resolve(
            this.doDemoLogin({ username: "demo", password: "demo" }));
        }
      }, 2000));
    } else {
      localStorage.removeItem("DEMO_LANGUAGE");
    }
  }

  /**
   * Login to OpenEMS Edge or Backend.
   *
   * @param param data provided in login form
   */
  public doLogin(param: { username?: string, password: string }) {

    this.websocket.state.set(States.AUTHENTICATION_WITH_CREDENTIALS);
    param = LoginComponent.preprocessCredentials(param.password, param.username);

    // Prevent that user submits via keyevent 'enter' multiple times
    if (this.formIsDisabled) {
      return;
    }

    this.formIsDisabled = true;
    this.websocket.login(new AuthenticateWithPasswordRequest(param))
      .finally(() => {
        this.ionViewWillEnter();
        this.formIsDisabled = false;
      });
  }

  /**
  * Login to OpenEMS Edge or Backend for demo user.
  *
  * @param param data provided in login form
  */
  public doDemoLogin(param: { username?: string, password: string }) {

    this.websocket.login(new AuthenticateWithPasswordRequest(param)).then(() => {
      this.service.stopSpinner("loginspinner");
    });

    return new Promise<Edge[]>((resolve, reject) => {

      const req = new GetEdgesRequest({ page: this.page });

      this.service.getEdges(req)
        .then((edges) => {
          setTimeout(() => {
            this.router.navigate(["/device", edges[0].id]);
          }, 100);
          resolve(edges);
        }).catch((err) => {
          reject(err);
        });
    }).finally(() => {
      this.service.stopSpinner("loginspinner");
    },
    );
  }

  ngOnDestroy() {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }

  protected async showPopoverOrRedirectToStore(operatingSystem: "android" | "ios") {
    const link: string | null = PlatFormService.getAppStoreLink();
    if (link) {
      window.open(link, "_blank");
    } else {
      this.popoverActive = operatingSystem;
    }
  }

}
