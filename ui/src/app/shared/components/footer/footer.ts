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
  templateUrl: "footer.html",
  standalone: false,
})
export class FooterComponent {

  @HostBinding("attr.data-isSmartPhone")
  public isSmartPhone: boolean = this.service.isSmartphoneResolution;

  protected user: User | null = null;
  protected edge: Edge | null = null;
  protected displayValues: { comment: string, id: string, version: string } | null = null;
  protected isAtLeastOwner: boolean | null = null;

  constructor(
    protected service: Service,
    private title: Title,
  ) {

    effect(() => {
      const edge = this.service.currentEdge();

      if (!edge) {
        this.edge = null;
        return;
      }
      this.edge = edge;

      this.setDisplayValues(edge);
    });
  }

  private static getDisplayValues(user: User, edge: Edge): { comment: string, id: string, version: string } {
    const result = {
      comment: "",
      id: "",
      version: edge.version,
    };

    switch (environment.backend) {
      case "OpenEMS Backend":
        if (Role.isAtLeast(user.globalRole, Role.OWNER) && user.hasMultipleEdges) {
          result.comment = edge?.comment;
        }
        result.id = edge.id;
        break;

      case "OpenEMS Edge":
        result.id = environment.edgeShortName;
        break;
    }

    return result;
  }

  private setDisplayValues(edge: Edge) {

    this.service.metadata.pipe(filter(metadata => !!metadata)).subscribe((metadata) => {
      this.user = metadata.user;

      let title = environment.edgeShortName;
      if (edge) {
        this.displayValues = FooterComponent.getDisplayValues(this.user, edge);

        if (this.user.hasMultipleEdges) {
          title += " | " + edge.id;
        }
      }

      this.title.setTitle(title);
    });
  }
}
