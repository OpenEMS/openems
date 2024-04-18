import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, NavigationEnd, NavigationExtras, Router } from '@angular/router';
import { IonPopover, ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { filter, switchMap, takeUntil } from 'rxjs/operators';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { Role } from 'src/app/shared/type/role';
import { Environment, environment } from 'src/environments';
import { Edge, Service, Websocket } from '../../../shared/shared';
import { ExecuteSystemUpdate } from '../system/executeSystemUpdate';
import { GetApps } from './jsonrpc/getApps';
import { AppCenter } from './keypopup/appCenter';
import { AppCenterGetPossibleApps } from './keypopup/appCenterGetPossibleApps';
import { Key } from './keypopup/key';
import { KeyModalComponent, KeyValidationBehaviour } from './keypopup/modal.component';
import { canEnterKey } from './permissions';
import { Flags } from './jsonrpc/flag/flags';
import { App } from './keypopup/app';
import { InstallAppComponent } from './install.component';
import { AppCenterGetRegisteredKeys } from './keypopup/appCenterGetRegisteredKeys';

@Component({
  selector: IndexComponent.SELECTOR,
  templateUrl: './index.component.html',
})
export class IndexComponent implements OnInit, OnDestroy {

  private static readonly SELECTOR = 'app-index';
  public readonly spinnerId: string = IndexComponent.SELECTOR;

  protected readonly environment: Environment = environment;
  protected edge: Edge | null = null;

  /**
   * e. g. if more than 4 apps are in a list the apps are displayed in their categories
   */
  private static readonly MAX_APPS_IN_LIST: number = 4;

  public apps: GetApps.App[] = [];

  public installedApps: AppList = {
    name: 'Edge.Config.App.installed', appCategories: []
    , shouldBeShown: () => this.key === null, // only show installed apps when the user is not currently selecting an app from a key
  };
  public availableApps: AppList = {
    name: 'Edge.Config.App.available', appCategories: []
    , shouldBeShown: () => true, // always show available apps
  };
  public incompatibleApps: AppList = {
    name: 'Edge.Config.App.incompatible', appCategories: []
    , shouldBeShown: () => this.edge.roleIsAtLeast(Role.ADMIN), // only show incompatible apps for admins
  };

  public appLists: AppList[] = [this.installedApps, this.availableApps, this.incompatibleApps];

  public categories: { val: GetApps.Category, isChecked: boolean }[] = [];

  protected key: Key | null = null;
  private useMasterKey: boolean = false;
  protected selectedBundle: number | null = null;

  protected isUpdateAvailable: boolean = false;
  protected canEnterKey: boolean = false;
  protected numberOfUnusedRegisteredKeys: number = 0;
  protected showPopover: boolean = false;
  private hasSeenPopover: boolean = false;

  @ViewChild('hasKeyPopover') private hasKeyPopover: IonPopover;

  private stopOnDestroy: Subject<void> = new Subject<void>();

  public constructor(
    private route: ActivatedRoute,
    private service: Service,
    private websocket: Websocket,
    private translate: TranslateService,
    private router: Router,
    private modalController: ModalController,
  ) {
  }

  public ngOnInit() {
    this.init();
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      switchMap(() => this.route.url),
    ).subscribe(() => {
      const navigationExtras = this.router.getCurrentNavigation()?.extras as NavigationExtras;
      const installedAnApp = navigationExtras?.state?.installedAnApp;
      if (installedAnApp != null && installedAnApp) {
        this.init();
      }
    });
  }

  private init() {
    this.service.startSpinner(this.spinnerId);
    this.key = null;
    this.selectedBundle = null;

    this.appLists.forEach(element => {
      element.appCategories = [];
    });

    this.service.setCurrentComponent({
      languageKey: 'Edge.Config.App.NAME_WITH_EDGE_NAME',
      interpolateParams: { edgeShortName: environment.edgeShortName },
    }, this.route).then(edge => {
      this.edge = edge;

      this.service.metadata
        .pipe(takeUntil(this.stopOnDestroy))
        .subscribe(entry => {
          this.canEnterKey = canEnterKey(edge, entry.user);
          this.updateHasUnusedKeysPopover();
        });
      edge.sendRequest(this.websocket,
        new ComponentJsonApiRequest({
          componentId: '_appManager',
          payload: new GetApps.Request(),
        })).then(response => {

          this.service.stopSpinner(this.spinnerId);

          this.apps = (response as GetApps.Response).result.apps.map(app => {
            app.imageUrl = environment.links.APP_CENTER.APP_IMAGE(this.translate.currentLang, app.appId);
            return app;
          });

          // init categories
          this.apps.forEach(a => {
            a.categorys.forEach(category => {
              if (!this.categories.find(c => c.val.name === category.name)) {
                this.categories.push({ val: category, isChecked: true });
              }
            });
          });

          this.updateSelection();

          edge.sendRequest(this.websocket, new AppCenter.Request({
            payload: new AppCenterGetRegisteredKeys.Request({}),
          })).then(response => {
            const result = (response as AppCenterGetRegisteredKeys.Response).result;
            this.numberOfUnusedRegisteredKeys = result.keys.length;
            this.updateHasUnusedKeysPopover();
          }).catch(this.service.handleError);
        }).catch(InstallAppComponent.errorToast(this.service, error => 'Error while receiving available apps: ' + error));

      const systemUpdate = new ExecuteSystemUpdate(edge, this.websocket);
      systemUpdate.systemUpdateStateChange = (updateState) => {
        if (updateState.available) {
          this.isUpdateAvailable = true;
        }
      };
      systemUpdate.start();
    });
  }

  public ngOnDestroy(): void {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }

  private updateHasUnusedKeysPopover() {
    if (this.hasSeenPopover) {
      return;
    }
    if (!this.canEnterKey) {
      return;
    }
    if (this.numberOfUnusedRegisteredKeys === 0) {
      return;
    }

    this.hasSeenPopover = true;

    this.hasKeyPopover.event = {
      type: 'willPresent',
      target: document.querySelector('#redeemKeyCard'),
    };
    this.showPopover = true;
  }

  /**
   * Updates the selected categories.
   * @param event the event of a click on a 'ion-fab-list' to stop it from closing
   */
  protected updateSelection(event?: PointerEvent) {
    if (event) {
      event.stopPropagation();
    }
    this.installedApps.appCategories = [];
    this.availableApps.appCategories = [];

    const sortedApps = [];
    this.apps.forEach(app => {
      app.categorys.forEach(category => {
        if (this.selectedBundle >= 0 && this.key) {
          if (!this.key.bundles[this.selectedBundle].some((a) => app.appId === a.appId)) {
            return false;
          }
        } else {
          if (Flags.getByType(app.flags, Flags.SHOW_AFTER_KEY_REDEEM)
            && environment.production
            && app.instanceIds.length === 0) {
            return false;
          }
        }
        const cat = this.categories.find(c => c.val.name === category.name);
        if (!cat.isChecked) {
          return false;
        }
        sortedApps.push(app);
        return true;
      });
    });

    sortedApps.forEach(a => {
      if (a.instanceIds.length > 0) {
        this.pushIntoCategory(a, this.installedApps);
        if (a.cardinality === 'MULTIPLE' && a.status.name !== 'INCOMPATIBLE') {
          this.pushIntoCategory(a, this.availableApps);
        }
      } else {
        if (a.status.name === 'INCOMPATIBLE') {
          this.pushIntoCategory(a, this.incompatibleApps);
        } else {
          this.pushIntoCategory(a, this.availableApps);
        }
      }
    });
  }

  private pushIntoCategory(app: GetApps.App, list: AppList): void {
    app.categorys.forEach(category => {
      let catList = list.appCategories.find(l => l.category.name === category.name);
      if (catList === undefined) {
        catList = { category: category, apps: [] };
        list.appCategories.push(catList);
      }
      catList.apps.push(app);
    });
  }

  protected showCategories(app: AppList): boolean {
    return this.sum(app) > IndexComponent.MAX_APPS_IN_LIST;
  }

  protected isEmpty(app: AppList): boolean {
    return this.sum(app) === 0;
  }

  private sum(app: AppList): number {
    return app.appCategories.reduce((p, c) => p + c.apps.length, 0);
  }

  /**
   * Opens a popup to select a key.
   */
  protected async redeemKey(): Promise<void> {
    const modal = await this.modalController.create({
      component: KeyModalComponent,
      componentProps: {
        edge: this.edge,
        behaviour: KeyValidationBehaviour.SELECT,
        knownApps: this.apps,
      },
      cssClass: 'auto-height',
    });
    modal.onDidDismiss().then(data => {
      if (!data.data) {
        this.key = null;
        this.useMasterKey = false;
        this.updateSelection();
        return; // no key selected
      }
      if (data.data?.useMasterKey) {
        this.selectedBundle = 0;
        // set dummy key for available apps to install
        this.key = {
          keyId: null, bundles: [this.apps
            .filter(e => !Flags.getByType(e.flags, Flags.SHOW_AFTER_KEY_REDEEM))
            .map<App>(d => {
              return { id: 0, appId: d.appId };
            })],
        };
        this.useMasterKey = true;
        this.updateSelection();
        return;
      }
      this.useMasterKey = false;
      this.key = data.data.key;
      if (!this.key.bundles) {
        // load bundles
        this.edge.sendRequest(this.websocket, new AppCenter.Request({
          payload: new AppCenterGetPossibleApps.Request({
            key: this.key.keyId,
          }),
        })).then(response => {
          const result = (response as AppCenterGetPossibleApps.Response).result;
          this.key.bundles = result.bundles;
          this.selectedBundle = 0;
          this.updateSelection();
        });
      } else {
        this.selectedBundle = 0;
        this.updateSelection();
      }
    });
    return await modal.present();
  }

  protected onAppClicked(app: GetApps.App): void {
    // navigate
    if (this.key != null || this.useMasterKey) {
      this.router.navigate(['device/' + (this.edge.id) + '/settings/app/single/' + app.appId]
        , { queryParams: { name: app.name }, state: { app: app, appKey: this.key.keyId, useMasterKey: this.useMasterKey } });
    } else {
      this.router.navigate(['device/' + (this.edge.id) + '/settings/app/single/' + app.appId], { queryParams: { name: app.name }, state: app });
    }
    // reset keys
    this.key = null;
    this.useMasterKey = false;
  }

  /**
   * Opens a popup to register a key.
   */
  protected async registerKey(): Promise<void> {
    const modal = await this.modalController.create({
      component: KeyModalComponent,
      componentProps: {
        edge: this.edge,
        behaviour: KeyValidationBehaviour.REGISTER,
      },
      cssClass: 'auto-height',
    });

    return await modal.present();
  }

}

interface AppList {
  name: string,
  appCategories: AppListByCategorie[],
  shouldBeShown: () => boolean;
}

interface AppListByCategorie {
  category: GetApps.Category,
  apps: GetApps.App[];
}
