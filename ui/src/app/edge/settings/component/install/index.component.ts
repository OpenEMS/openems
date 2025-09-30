// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { CategorizedComponents, CategorizedFactories } from "src/app/shared/components/edge/edgeconfig";
import { JsonrpcRequest, JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { Edge, EdgeConfig, EdgePermission, Service, Utils, Websocket } from "../../../../shared/shared";

interface MyCategorizedFactories extends CategorizedFactories {
  isClicked?: boolean,
  filteredFactories?: EDGE_CONFIG.FACTORY[],
}

@Component({
  selector: INDEX_COMPONENT.SELECTOR,
  templateUrl: "./INDEX.COMPONENT.HTML",
  standalone: false,
})
export class IndexComponent implements OnInit {

  private static readonly SELECTOR = "indexComponentInstall";

  public components: CategorizedComponents[] | null = null;
  public list: MyCategorizedFactories[];
  public showAllFactories = false;

  private edge: Edge;

  constructor(
    private translate: TranslateService,
    private service: Service,
    private websocket: Websocket,
  ) {
  }

  async ngOnInit() {
    THIS.EDGE = await THIS.SERVICE.GET_CURRENT_EDGE();
    THIS.LIST = await THIS.GET_CATEGORIZED_FACTORIES();
    for (const entry of THIS.LIST) {
      ENTRY.IS_CLICKED = false;
      ENTRY.FILTERED_FACTORIES = ENTRY.FACTORIES;
    }
    THIS.UPDATE_FILTER("");
  }

  updateFilter(completeFilter: string) {
    // take each space-separated string as an individual and-combined filter
    const filters = COMPLETE_FILTER.TO_LOWER_CASE().split(" ");
    let countFilteredEntries = 0;
    for (const entry of THIS.LIST) {
      ENTRY.FILTERED_FACTORIES = ENTRY.FACTORIES.FILTER(entry =>
        // Search for filter strings in Factory-ID, -Name and Description
        UTILS.MATCH_ALL(filters, [
          ENTRY.ID.TO_LOWER_CASE(),
          ENTRY.NAME.TO_LOWER_CASE(),
          ENTRY.DESCRIPTION.TO_LOWER_CASE(),
        ]),
      );
      countFilteredEntries += ENTRY.FILTERED_FACTORIES.LENGTH;
    }
    // If not more than 10 Factories survived filtering -> show all of them immediately
    if (countFilteredEntries > 10) {
      THIS.SHOW_ALL_FACTORIES = false;
    } else {
      THIS.SHOW_ALL_FACTORIES = true;
    }
  }

  private async getCategorizedFactories(): Promise<MyCategorizedFactories[]> {
    if (EDGE_PERMISSION.HAS_REDUCED_FACTORIES(THIS.EDGE)) {
      const response = await THIS.EDGE.SEND_REQUEST<GetAllComponentFactoriesResponse>(THIS.WEBSOCKET, new ComponentJsonApiRequest({
        componentId: "_componentManager",
        payload: new GetAllComponentFactoriesRequest(),
      }));
      for (const [factoryId, factory] of OBJECT.ENTRIES(RESPONSE.RESULT.FACTORIES)) {
        FACTORY.ID = factoryId;
      }

      return EDGE_CONFIG.LIST_AVAILABLE_FACTORIES(RESPONSE.RESULT.FACTORIES, THIS.TRANSLATE);
    }

    const config = await THIS.SERVICE.GET_CONFIG();
    return CONFIG.LIST_AVAILABLE_FACTORIES(THIS.TRANSLATE);
  }
}


class GetAllComponentFactoriesRequest extends JsonrpcRequest {

  private static METHOD: string = "getAllComponentFactories";

  public constructor() {
    super(GET_ALL_COMPONENT_FACTORIES_REQUEST.METHOD, {});
  }

}

class GetAllComponentFactoriesResponse extends JsonrpcResponseSuccess {

  public constructor(
    public override readonly id: string,
    public override readonly result: {
      factories: { [factoryId: string]: EDGE_CONFIG.FACTORY },
    },
  ) {
    super(id, result);
  }

}

