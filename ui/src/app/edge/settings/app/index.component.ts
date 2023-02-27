import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { Role } from 'src/app/shared/type/role';
import { Environment, environment } from 'src/environments';
import { Edge, Service, Websocket } from '../../../shared/shared';
import { ExecuteSystemUpdate } from '../systemupdate/executeSystemUpdate';
import { GetApps } from './jsonrpc/getApps';
import { AppCenter } from './keypopup/appCenter';
import { AppCenterGetPossibleApps } from './keypopup/appCenterGetPossibleApps';
import { Key } from './keypopup/key';
import { KeyModalComponent, KeyValidationBehaviour } from './keypopup/modal.component';
import { canEnterKey } from './permissions';

@Component({
  selector: IndexComponent.SELECTOR,
  templateUrl: './index.component.html'
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
    , shouldBeShown: () => this.key === null // only show installed apps when the user is not currently selecting an app from a key
  };
  public availableApps: AppList = {
    name: 'Edge.Config.App.available', appCategories: []
    , shouldBeShown: () => true // always show available apps
  };
  public incompatibleApps: AppList = {
    name: 'Edge.Config.App.incompatible', appCategories: []
    , shouldBeShown: () => this.edge.roleIsAtLeast(Role.ADMIN) // only show incompatible apps for admins
  };

  public appLists: AppList[] = [this.installedApps, this.availableApps, this.incompatibleApps];

  public categories: { val: GetApps.Category, isChecked: boolean }[] = [];

  protected key: Key | null = null
  protected selectedBundle: number | null = null

  // check if update is available
  protected isUpdateAvailable: boolean = false;

  protected canEnterKey: boolean | undefined

  private stopOnDestroy: Subject<void> = new Subject<void>();

  public constructor(
    private route: ActivatedRoute,
    private service: Service,
    private websocket: Websocket,
    private translate: TranslateService,
    private router: Router,
    private modalController: ModalController
  ) {
  }

  public ngOnInit() {
    this.init()
  }

  private init() {
    this.service.startSpinner(this.spinnerId);
    this.key = null;
    this.selectedBundle = null;

    this.appLists.forEach(element => {
      element.appCategories = []
    });

    this.service.setCurrentComponent(environment.edgeShortName + ' Apps', this.route).then(edge => {
      this.edge = edge;

      this.service.metadata
        .pipe(takeUntil(this.stopOnDestroy))
        .subscribe(entry => {
          this.canEnterKey = canEnterKey(edge, entry.user);
        });
      edge.sendRequest(this.websocket,
        new ComponentJsonApiRequest({
          componentId: '_appManager',
          payload: new GetApps.Request()
        })).then(response => {

          this.service.stopSpinner(this.spinnerId);
          this.apps = (response as GetApps.Response).result.apps;

          // init categories
          this.apps.forEach(a => {
            a.categorys.forEach(category => {
              if (!this.categories.find(c => c.val.name === category.name)) {
                this.categories.push({ val: category, isChecked: true })
              }
            });
          });

          this.updateSelection(null);

        }).catch(reason => {
          console.error(reason.error);
          this.service.toast('Error while receiving available apps: ' + reason.error.message, 'danger');
        });

      const systemUpdate = new ExecuteSystemUpdate(edge, this.websocket);
      systemUpdate.systemUpdateStateChange = (updateState) => {
        if (updateState.available) {
          this.isUpdateAvailable = true;
        }
      }
      systemUpdate.start();
    });
  }

  public ngOnDestroy(): void {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }

  /**
   * Updates the selected categories.
   * @param event the event of a click on a 'ion-fab-list' to stop it from closing
   */
  protected updateSelection(event: PointerEvent) {
    if (event != null) {
      event.stopPropagation();
    }
    this.installedApps.appCategories = [];
    this.availableApps.appCategories = [];

    var sortedApps = []
    this.apps.forEach(a => {
      a.categorys.forEach(category => {
        if (this.selectedBundle >= 0 && this.key) {
          if (!this.key.bundles[this.selectedBundle].some((app) => app.appId === a.appId)) {
            return false;
          }
        }
        var cat = this.categories.find(c => c.val.name === category.name)
        if (!cat.isChecked) {
          return false;
        }
        sortedApps.push(a);
        return true;
      })
    })

    sortedApps.forEach(a => {
      if (a.instanceIds.length > 0) {
        this.pushIntoCategory(a, this.installedApps)
        if (a.cardinality === 'MULTIPLE' && a.status.name !== 'INCOMPATIBLE') {
          this.pushIntoCategory(a, this.availableApps)
        }
      } else {
        if (a.status.name === 'INCOMPATIBLE') {
          this.pushIntoCategory(a, this.incompatibleApps)
        } else {
          this.pushIntoCategory(a, this.availableApps)
        }
      }
    })
  }

  private pushIntoCategory(app: GetApps.App, list: AppList): void {
    app.categorys.forEach(category => {
      var catList = list.appCategories.find(l => l.category.name === category.name)
      if (catList === undefined) {
        catList = { category: category, apps: [] };
        list.appCategories.push(catList);
      }
      catList.apps.push(app);
    })
  }

  protected showCategories(app: AppList): boolean {
    return this.sum(app) > IndexComponent.MAX_APPS_IN_LIST
  }

  protected isEmpty(app: AppList): boolean {
    return this.sum(app) === 0
  }

  private sum(app: AppList): number {
    return app.appCategories.reduce((p, c) => p + c.apps.length, 0)
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
        knownApps: this.apps
      },
      cssClass: 'auto-height'
    });
    modal.onDidDismiss().then(data => {
      if (!data.data) {
        this.key = null;
        this.updateSelection(null);
        return; // no key selected
      }
      this.key = data.data.key
      if (!this.key.bundles) {
        // load bundles
        this.edge.sendRequest(this.websocket, new AppCenter.Request({
          payload: new AppCenterGetPossibleApps.Request({
            key: this.key.keyId
          })
        })).then(response => {
          const result = (response as AppCenterGetPossibleApps.Response).result;
          this.key.bundles = result.bundles;
          this.selectedBundle = 0;
          this.updateSelection(null);
        })
      } else {
        this.selectedBundle = 0;
        this.updateSelection(null);
      }
    })
    return await modal.present();
  }

  protected onAppClicked(app: GetApps.App): void {
    if (this.key != null) {
      this.router.navigate(['device/' + (this.edge.id) + '/settings/app/single/' + app.appId]
        , { queryParams: { name: app.name }, state: { app: app, appKey: this.key.keyId } });
    } else {
      this.router.navigate(['device/' + (this.edge.id) + '/settings/app/single/' + app.appId], { queryParams: { name: app.name }, state: app });
    }
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
      cssClass: 'auto-height'
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
