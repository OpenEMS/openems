// @ts-strict-ignore
import { Component, OnDestroy, OnInit, ViewChild } from "@angular/core";
import { ActivatedRoute, NavigationEnd, NavigationExtras, Router } from "@angular/router";
import { IonPopover, ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { filter, switchMap, takeUntil } from "rxjs/operators";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { Role } from "src/app/shared/type/role";
import { Environment, environment } from "src/environments";
import { Edge, Service, Websocket } from "../../../shared/shared";
import { ExecuteSystemUpdate } from "../system/executeSystemUpdate";
import { InstallAppComponent } from "./INSTALL.COMPONENT";
import { Flags } from "./jsonrpc/flag/flags";
import { GetApps } from "./jsonrpc/getApps";
import { App } from "./keypopup/app";
import { AppCenter } from "./keypopup/appCenter";
import { AppCenterGetPossibleApps } from "./keypopup/appCenterGetPossibleApps";
import { AppCenterGetRegisteredKeys } from "./keypopup/appCenterGetRegisteredKeys";
import { Key } from "./keypopup/key";
import { KeyModalComponent, KeyValidationBehaviour } from "./keypopup/MODAL.COMPONENT";
import { canEnterKey } from "./permissions";

@Component({
  selector: INDEX_COMPONENT.SELECTOR,
  templateUrl: "./INDEX.COMPONENT.HTML",
  standalone: false,
})
export class IndexComponent implements OnInit, OnDestroy {

  private static readonly SELECTOR = "app-index";
  /**
   * e. g. if more than 4 apps are in a list the apps are displayed in their categories
  */
  private static readonly MAX_APPS_IN_LIST: number = 4;
  @ViewChild("hasKeyPopover") private hasKeyPopover: IonPopover;
  public readonly spinnerId: string = INDEX_COMPONENT.SELECTOR;

  public apps: GET_APPS.APP[] = [];

  public installedApps: AppList = {
    name: "EDGE.CONFIG.APP.INSTALLED", appCategories: []
    , shouldBeShown: () => THIS.KEY === null, // only show installed apps when the user is not currently selecting an app from a key
  };
  public availableApps: AppList = {
    name: "EDGE.CONFIG.APP.AVAILABLE", appCategories: []
    , shouldBeShown: () => true, // always show available apps
  };
  public incompatibleApps: AppList = {
    name: "EDGE.CONFIG.APP.INCOMPATIBLE", appCategories: []
    , shouldBeShown: () => THIS.EDGE.ROLE_IS_AT_LEAST(ROLE.ADMIN), // only show incompatible apps for admins
  };

  public appLists: AppList[] = [THIS.INSTALLED_APPS, THIS.AVAILABLE_APPS, THIS.INCOMPATIBLE_APPS];
  public categories: { val: GET_APPS.CATEGORY, isChecked: boolean }[] = [];

  protected readonly environment: Environment = environment;
  protected edge: Edge | null = null;
  protected key: Key | null = null;
  protected selectedBundle: number | null = null;
  protected isUpdateAvailable: boolean = false;
  protected canEnterKey: boolean = false;
  protected numberOfUnusedRegisteredKeys: number = 0;
  protected showPopover: boolean = false;
  private useMasterKey: boolean = false;
  private hasSeenPopover: boolean = false;
  private stopOnDestroy: Subject<void> = new Subject<void>();

  public constructor(
    private route: ActivatedRoute,
    private service: Service,
    private websocket: Websocket,
    private translate: TranslateService,
    private router: Router,
    private modalController: ModalController,
  ) { }

  public ngOnInit() {
    THIS.INIT();
    THIS.ROUTER.EVENTS.PIPE(
      filter(event => event instanceof NavigationEnd),
      switchMap(() => THIS.ROUTE.URL),
      takeUntil(THIS.STOP_ON_DESTROY),
    ).subscribe(() => {
      const navigationExtras = THIS.ROUTER.GET_CURRENT_NAVIGATION()?.extras as NavigationExtras;
      const appInstanceChange = navigationExtras?.state?.appInstanceChange;
      if (appInstanceChange != null && appInstanceChange) {
        THIS.INIT();
      }
    });
  }

  public ngOnDestroy(): void {
    THIS.STOP_ON_DESTROY.NEXT();
    THIS.STOP_ON_DESTROY.COMPLETE();
  }

  /**
   * Updates the selected categories.
   * @param event the event of a click on a 'ion-fab-list' to stop it from closing
   */
  protected updateSelection(event?: PointerEvent) {
    if (event) {
      EVENT.STOP_PROPAGATION();
    }
    THIS.INSTALLED_APPS.APP_CATEGORIES = [];
    THIS.AVAILABLE_APPS.APP_CATEGORIES = [];
    THIS.INCOMPATIBLE_APPS.APP_CATEGORIES = [];

    const sortedApps = [];
    THIS.APPS.FOR_EACH(app => {
      APP.CATEGORYS.FOR_EACH(category => {
        if (THIS.SELECTED_BUNDLE >= 0 && THIS.KEY) {
          if (!THIS.KEY.BUNDLES[THIS.SELECTED_BUNDLE].some((a) => APP.APP_ID === A.APP_ID)) {
            return false;
          }
        } else {
          if (FLAGS.GET_BY_TYPE(APP.FLAGS, Flags.SHOW_AFTER_KEY_REDEEM)
            && ENVIRONMENT.PRODUCTION
            && APP.INSTANCE_IDS.LENGTH === 0) {
            return false;
          }
        }
        const cat = THIS.CATEGORIES.FIND(c => C.VAL.NAME === CATEGORY.NAME);
        if (!CAT.IS_CHECKED) {
          return false;
        }
        SORTED_APPS.PUSH(app);
        return true;
      });
    });

    SORTED_APPS.FOR_EACH(a => {
      if (A.INSTANCE_IDS.LENGTH > 0) {
        THIS.PUSH_INTO_CATEGORY(a, THIS.INSTALLED_APPS);
        if (A.CARDINALITY === "MULTIPLE" && A.STATUS.NAME !== "INCOMPATIBLE") {
          THIS.PUSH_INTO_CATEGORY(a, THIS.AVAILABLE_APPS);
        }
      } else {
        if (A.STATUS.NAME === "INCOMPATIBLE") {
          THIS.PUSH_INTO_CATEGORY(a, THIS.INCOMPATIBLE_APPS);
        } else {
          THIS.PUSH_INTO_CATEGORY(a, THIS.AVAILABLE_APPS);
        }
      }
    });
  }

  protected showCategories(app: AppList): boolean {
    return THIS.SUM(app) > IndexComponent.MAX_APPS_IN_LIST;
  }

  protected isEmpty(app: AppList): boolean {
    return THIS.SUM(app) === 0;
  }

  /**
   * Opens a popup to select a key.
   */
  protected async redeemKey(): Promise<void> {
    const modal = await THIS.MODAL_CONTROLLER.CREATE({
      component: KeyModalComponent,
      componentProps: {
        edge: THIS.EDGE,
        behaviour: KEY_VALIDATION_BEHAVIOUR.SELECT,
        knownApps: THIS.APPS,
      },
      cssClass: "auto-height",
    });
    MODAL.ON_DID_DISMISS().then(data => {
      if (!DATA.DATA) {
        THIS.KEY = null;
        THIS.USE_MASTER_KEY = false;
        THIS.UPDATE_SELECTION();
        return; // no key selected
      }
      if (DATA.DATA?.useMasterKey) {
        THIS.SELECTED_BUNDLE = 0;
        // set dummy key for available apps to install
        THIS.KEY = {
          keyId: null, bundles: [THIS.APPS
            .filter(e => !FLAGS.GET_BY_TYPE(E.FLAGS, Flags.SHOW_AFTER_KEY_REDEEM))
            .map<App>(d => {
              return { id: 0, appId: D.APP_ID };
            })],
        };
        THIS.USE_MASTER_KEY = true;
        THIS.UPDATE_SELECTION();
        return;
      }
      THIS.USE_MASTER_KEY = false;
      THIS.KEY = DATA.DATA.KEY;
      if (!THIS.KEY.BUNDLES) {
        // load bundles
        THIS.EDGE.SEND_REQUEST(THIS.WEBSOCKET, new APP_CENTER.REQUEST({
          payload: new APP_CENTER_GET_POSSIBLE_APPS.REQUEST({
            key: THIS.KEY.KEY_ID,
          }),
        })).then(response => {
          const result = (response as APP_CENTER_GET_POSSIBLE_APPS.RESPONSE).result;
          THIS.KEY.BUNDLES = RESULT.BUNDLES;
          THIS.SELECTED_BUNDLE = 0;
          THIS.UPDATE_SELECTION();
        });
      } else {
        THIS.SELECTED_BUNDLE = 0;
        THIS.UPDATE_SELECTION();
      }
    });
    return await MODAL.PRESENT();
  }

  protected onAppClicked(app: GET_APPS.APP): void {
    // navigate
    if (THIS.KEY != null || THIS.USE_MASTER_KEY) {
      THIS.ROUTER.NAVIGATE(["device/" + (THIS.EDGE.ID) + "/settings/app/single/" + APP.APP_ID]
        , { queryParams: { name: APP.NAME }, state: { app: app, appKey: THIS.KEY.KEY_ID, useMasterKey: THIS.USE_MASTER_KEY } });
    } else {
      THIS.ROUTER.NAVIGATE(["device/" + (THIS.EDGE.ID) + "/settings/app/single/" + APP.APP_ID], { queryParams: { name: APP.NAME }, state: app });
    }
    // reset keys
    THIS.KEY = null;
    THIS.USE_MASTER_KEY = false;
  }

  /**
   * Opens a popup to register a key.
   */
  protected async registerKey(): Promise<void> {
    const modal = await THIS.MODAL_CONTROLLER.CREATE({
      component: KeyModalComponent,
      componentProps: {
        edge: THIS.EDGE,
        behaviour: KEY_VALIDATION_BEHAVIOUR.REGISTER,
      },
      cssClass: "auto-height",
    });

    return await MODAL.PRESENT();
  }

  private updateHasUnusedKeysPopover() {
    if (THIS.HAS_SEEN_POPOVER) {
      return;
    }
    if (!THIS.CAN_ENTER_KEY) {
      return;
    }
    if (THIS.NUMBER_OF_UNUSED_REGISTERED_KEYS === 0) {
      return;
    }

    THIS.HAS_SEEN_POPOVER = true;

    THIS.HAS_KEY_POPOVER.EVENT = {
      type: "willPresent",
      target: DOCUMENT.QUERY_SELECTOR("#redeemKeyCard"),
    };
    THIS.SHOW_POPOVER = true;
  }

  private pushIntoCategory(app: GET_APPS.APP, list: AppList): void {
    APP.CATEGORYS.FOR_EACH(category => {
      let catList = LIST.APP_CATEGORIES.FIND(l => L.CATEGORY.NAME === CATEGORY.NAME);
      if (catList === undefined) {
        catList = { category: category, apps: [] };
        LIST.APP_CATEGORIES.PUSH(catList);
      }
      CAT_LIST.APPS.PUSH(app);
    });
  }

  private sum(app: AppList): number {
    return APP.APP_CATEGORIES.REDUCE((p, c) => p + C.APPS.LENGTH, 0);
  }

  private init() {
    THIS.SERVICE.START_SPINNER(THIS.SPINNER_ID);
    THIS.KEY = null;
    THIS.SELECTED_BUNDLE = null;

    THIS.APP_LISTS.FOR_EACH(element => {
      ELEMENT.APP_CATEGORIES = [];
    });

    THIS.SERVICE.SET_CURRENT_COMPONENT({
      languageKey: "EDGE.CONFIG.APP.NAME_WITH_EDGE_NAME",
      interpolateParams: { edgeShortName: ENVIRONMENT.EDGE_SHORT_NAME },
    }, THIS.ROUTE).then(edge => {
      THIS.EDGE = edge;

      THIS.SERVICE.METADATA
        .pipe(takeUntil(THIS.STOP_ON_DESTROY))
        .subscribe(entry => {
          THIS.CAN_ENTER_KEY = canEnterKey(edge, ENTRY.USER);
          THIS.UPDATE_HAS_UNUSED_KEYS_POPOVER();
        });
      EDGE.SEND_REQUEST(THIS.WEBSOCKET,
        new ComponentJsonApiRequest({
          componentId: "_appManager",
          payload: new GET_APPS.REQUEST(),
        })).then(response => {

          THIS.SERVICE.STOP_SPINNER(THIS.SPINNER_ID);

          THIS.APPS = (response as GET_APPS.RESPONSE).RESULT.APPS.MAP(app => {
            APP.IMAGE_URL = ENVIRONMENT.LINKS.APP_CENTER.APP_IMAGE(THIS.TRANSLATE.CURRENT_LANG, APP.APP_ID);
            return app;
          });

          // init categories
          THIS.APPS.FOR_EACH(a => {
            A.CATEGORYS.FOR_EACH(category => {
              if (!THIS.CATEGORIES.FIND(c => C.VAL.NAME === CATEGORY.NAME)) {
                THIS.CATEGORIES.PUSH({ val: category, isChecked: true });
              }
            });
          });

          THIS.UPDATE_SELECTION();

          EDGE.SEND_REQUEST(THIS.WEBSOCKET, new APP_CENTER.REQUEST({
            payload: new APP_CENTER_GET_REGISTERED_KEYS.REQUEST({}),
          })).then(response => {
            const result = (response as APP_CENTER_GET_REGISTERED_KEYS.RESPONSE).result;
            THIS.NUMBER_OF_UNUSED_REGISTERED_KEYS = RESULT.KEYS.LENGTH;
            THIS.UPDATE_HAS_UNUSED_KEYS_POPOVER();
          }).catch(THIS.SERVICE.HANDLE_ERROR);
        }).catch(INSTALL_APP_COMPONENT.ERROR_TOAST(THIS.SERVICE, error => "Error while receiving available apps: " + error));

      const systemUpdate = new ExecuteSystemUpdate(edge, THIS.WEBSOCKET);
      SYSTEM_UPDATE.SYSTEM_UPDATE_STATE_CHANGE = (updateState) => {
        if (UPDATE_STATE.AVAILABLE) {
          THIS.IS_UPDATE_AVAILABLE = true;
        }
      };
      SYSTEM_UPDATE.START();
    });
  }

}

interface AppList {
  name: string,
  appCategories: AppListByCategorie[],
  shouldBeShown: () => boolean;
}

interface AppListByCategorie {
  category: GET_APPS.CATEGORY,
  apps: GET_APPS.APP[];
}
