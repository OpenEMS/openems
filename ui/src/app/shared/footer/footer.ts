import { Component, HostBinding, OnInit } from "@angular/core";
import { Title } from "@angular/platform-browser";
import { filter } from "rxjs/operators";

import { environment } from '../../../environments';
import { User } from "../jsonrpc/shared";
import { Edge, Service } from "../shared";
import { Role } from "../type/role";

@Component({
  selector: 'oe-footer',
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

      :is(ion-item) { /* Update the selector here */
        --min-height: initial !important;
        font-size: inherit;
      }
    }
  `],
  templateUrl: 'footer.html',
})
export class FooterComponent implements OnInit {

  protected user: User | null = null;
  protected edge: Edge | null = null;
  protected displayValues: { comment: string, id: string, version: string } | null = null;
  protected isAtLeastOwner: boolean | null = null;

  @HostBinding('attr.data-isSmartPhone')
  public isSmartPhone: boolean = this.service.isSmartphoneResolution;

  constructor(
    protected service: Service,
    private title: Title,
  ) { }

  ngOnInit() {
    this.service.currentEdge.subscribe((edge) => {
      this.edge = edge;

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
}
