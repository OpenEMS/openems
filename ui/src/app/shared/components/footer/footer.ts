import { Component, effect, HostBinding } from "@angular/core";
import { Title } from "@angular/platform-browser";
import { filter } from "rxjs/operators";

import { environment } from "../../../../environments";
import { User } from "../../jsonrpc/shared";
import { Edge, Service } from "../../shared";
import { Role } from "../../type/role";

@Component({
  selector: "oe-footer",
  styles: [`

    :host[data-isSmartPhone=true] {
      position: relative;
    }

    :host[data-isSmartPhone=false] {
      position: sticky;
      bottom: 0;
      width: 100%;

      font-size: 14px !important;
      :is(ion-row) {
        text-align: center;
      }

      :is(ion-item) {

        font-size: inherit;
      }
    }
  `],
  templateUrl: "FOOTER.HTML",
  standalone: false,
})
export class FooterComponent {

  @HostBinding("ATTR.DATA-isSmartPhone")
  public isSmartPhone: boolean = THIS.SERVICE.IS_SMARTPHONE_RESOLUTION;

  protected user: User | null = null;
  protected edge: Edge | null = null;
  protected displayValues: { comment: string, id: string, version: string } | null = null;
  protected isAtLeastOwner: boolean | null = null;

  constructor(
    protected service: Service,
    private title: Title,
  ) {

    effect(() => {
      const edge = THIS.SERVICE.CURRENT_EDGE();

      if (!edge) {
        THIS.EDGE = null;
        return;
      }
      THIS.EDGE = edge;

      THIS.SET_DISPLAY_VALUES(edge);
    });
  }

  private static getDisplayValues(user: User, edge: Edge): { comment: string, id: string, version: string } {
    const result = {
      comment: "",
      id: "",
      version: EDGE.VERSION,
    };

    switch (ENVIRONMENT.BACKEND) {
      case "OpenEMS Backend":
        if (ROLE.IS_AT_LEAST(USER.GLOBAL_ROLE, ROLE.OWNER) && USER.HAS_MULTIPLE_EDGES) {
          RESULT.COMMENT = edge?.comment;
        }
        RESULT.ID = EDGE.ID;
        break;

      case "OpenEMS Edge":
        RESULT.ID = ENVIRONMENT.EDGE_SHORT_NAME;
        break;
    }

    return result;
  }

  private setDisplayValues(edge: Edge) {

    THIS.SERVICE.METADATA.PIPE(filter(metadata => !!metadata)).subscribe((metadata) => {
      THIS.USER = METADATA.USER;

      let title = ENVIRONMENT.EDGE_SHORT_NAME;
      if (edge) {
        THIS.DISPLAY_VALUES = FOOTER_COMPONENT.GET_DISPLAY_VALUES(THIS.USER, edge);

        if (THIS.USER.HAS_MULTIPLE_EDGES) {
          title += " | " + EDGE.ID;
        }
      }

      THIS.TITLE.SET_TITLE(title);
    });
  }
}
