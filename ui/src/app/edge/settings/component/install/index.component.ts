// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { CategorizedFactories } from "src/app/shared/components/edge/edgeconfig";
import { JsonrpcRequest, JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { Edge, EdgeConfig, EdgePermission, Service, Utils, Websocket } from "../../../../shared/shared";

interface MyCategorizedFactories extends CategorizedFactories {
  isClicked?: boolean,
  filteredFactories?: EdgeConfig.Factory[]
}

@Component({
  selector: IndexComponent.SELECTOR,
  templateUrl: "./index.component.html",
})
export class IndexComponent implements OnInit {

  private static readonly SELECTOR = "indexComponentInstall";

  public list: MyCategorizedFactories[];
  public showAllFactories = false;

  private edge: Edge;

  constructor(
    private service: Service,
    private websocket: Websocket,
  ) {
  }

  async ngOnInit() {
    this.edge = await this.service.getCurrentEdge();
    this.list = await this.getCategorizedFactories();
    for (const entry of this.list) {
      entry.isClicked = false;
      entry.filteredFactories = entry.factories;
    }
    this.updateFilter("");
  }

  updateFilter(completeFilter: string) {
    // take each space-separated string as an individual and-combined filter
    const filters = completeFilter.toLowerCase().split(" ");
    let countFilteredEntries = 0;
    for (const entry of this.list) {
      entry.filteredFactories = entry.factories.filter(entry =>
        // Search for filter strings in Factory-ID, -Name and Description
        Utils.matchAll(filters, [
          entry.id.toLowerCase(),
          entry.name.toLowerCase(),
          entry.description.toLowerCase(),
        ]),
      );
      countFilteredEntries += entry.filteredFactories.length;
    }
    // If not more than 10 Factories survived filtering -> show all of them immediately
    if (countFilteredEntries > 10) {
      this.showAllFactories = false;
    } else {
      this.showAllFactories = true;
    }
  }

  private async getCategorizedFactories(): Promise<MyCategorizedFactories[]> {
    if (EdgePermission.hasReducedFactories(this.edge)) {
      const response = await this.edge.sendRequest<GetAllComponentFactoriesResponse>(this.websocket, new ComponentJsonApiRequest({
        componentId: "_componentManager",
        payload: new GetAllComponentFactoriesRequest(),
      }));
      for (const [factoryId, factory] of Object.entries(response.result.factories)) {
        factory.id = factoryId;
      }

      return EdgeConfig.listAvailableFactories(response.result.factories);
    }

    const config = await this.service.getConfig();
    return config.listAvailableFactories();
  }
}


class GetAllComponentFactoriesRequest extends JsonrpcRequest {

  private static METHOD: string = "getAllComponentFactories";

  public constructor() {
    super(GetAllComponentFactoriesRequest.METHOD, {});
  }

}

class GetAllComponentFactoriesResponse extends JsonrpcResponseSuccess {

  public constructor(
    public override readonly id: string,
    public override readonly result: {
      factories: { [factoryId: string]: EdgeConfig.Factory },
    },
  ) {
    super(id, result);
  }

}

