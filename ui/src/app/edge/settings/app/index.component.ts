import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { Environment, environment } from 'src/environments';
import { Edge, Service, Websocket } from '../../../shared/shared';
import { ExecuteSystemUpdate } from '../systemupdate/executeSystemUpdate';
import { GetApps } from './jsonrpc/getApps';

@Component({
  selector: IndexComponent.SELECTOR,
  templateUrl: './index.component.html'
})
export class IndexComponent {

  private static readonly SELECTOR = "appIndex";
  public readonly spinnerId: string = IndexComponent.SELECTOR;

  protected readonly environment: Environment = environment;
  protected edge: Edge | null = null;

  /**
   * e. g. if more than 4 apps are in a list the apps are displayed in their categories
   */
  private static readonly MAX_APPS_IN_LIST: number = 4;

  public apps: GetApps.App[] = [];

  public installedApps: AppList = { name: 'Edge.Config.App.installed', appCategories: [] };
  public availableApps: AppList = { name: 'Edge.Config.App.available', appCategories: [] };
  // TODO incompatible apps should not be shown in the future
  public incompatibleApps: AppList = { name: 'Edge.Config.App.incompatible', appCategories: [] };

  public appLists: AppList[] = [this.installedApps, this.availableApps, this.incompatibleApps];

  public categories: { val: GetApps.Category, isChecked: boolean }[] = [];

  // check if update is available
  protected isUpdateAvailable: boolean = false;

  public constructor(
    private route: ActivatedRoute,
    private service: Service,
    private websocket: Websocket,
    private translate: TranslateService,
  ) {
  }

  private ionViewWillEnter() {
    // gets always called when entering the page
    this.init()
  }

  private init() {
    this.service.startSpinner(this.spinnerId);

    this.appLists.forEach(element => {
      element.appCategories = []
    });

    this.service.setCurrentComponent(environment.edgeShortName + " Apps", this.route).then(edge => {
      this.edge = edge;
      edge.sendRequest(this.websocket,
        new ComponentJsonApiRequest({
          componentId: "_appManager",
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
          })

          this.updateSelection(null)

        }).catch(reason => {
          console.error(reason.error);
          this.service.toast("Error while receiving available apps: " + reason.error.message, 'danger');
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

  /**
   * Updates the selected categories.
   * @param event the event of a click on a 'ion-fab-list' to stop it from closing
   */
  protected updateSelection(event: PointerEvent) {
    if (event != null) {
      event.stopPropagation()
    }
    this.installedApps.appCategories = []
    this.availableApps.appCategories = []

    var sortedApps = []
    this.apps.forEach(a => {
      a.categorys.every(category => {
        var cat = this.categories.find(c => c.val.name === category.name)
        if (cat.isChecked) {
          sortedApps.push(a)
          return false
        }
        return true
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

  private pushIntoCategory(app: GetApps.App, list: AppList) {
    app.categorys.forEach(category => {
      var catList = list.appCategories.find(l => l.category.name === category.name)
      if (catList == undefined) {
        catList = { category: category, apps: [] }
        list.appCategories.push(catList)
      }
      catList.apps.push(app)
    })
  }

  protected showCategories(app: AppList) {
    return this.sum(app) > IndexComponent.MAX_APPS_IN_LIST
  }

  protected isEmpty(app: AppList) {
    return this.sum(app) === 0
  }

  private sum(app: AppList) {
    let sum = 0
    app.appCategories.forEach(element => {
      sum += element.apps.length
    });
    return sum
  }

}

interface AppList {
  name: string,
  appCategories: AppListByCategorie[];
}

interface AppListByCategorie {
  category: GetApps.Category,
  apps: GetApps.App[];
}
