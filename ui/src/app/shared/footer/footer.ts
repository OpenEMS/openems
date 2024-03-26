import { Component, HostBinding, OnInit } from "@angular/core";
import { Title } from "@angular/platform-browser";
import { filter } from "rxjs/operators";

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
      ion-row {
        text-align: center;
      }

      ion-item {
        --min-height: initial !important;
        font-size: inherit;
      }
    }
  `],
  templateUrl: 'footer.html',
})
export class FooterComponent implements OnInit {

  protected readonly VERSION_PREFIX = "Version ";

  protected user: User | null = null;
  protected edge: Edge | null = null;
  protected displayValues: { version: string, id: string, comment: string } | null = null;
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

        if (edge) {
          this.displayValues = this.getDisplayValues(edge);

          if (this.user.hasMultipleEdges) {
            this.title.setTitle(edge.id);
          }
        }

        this.isAtLeastOwner = Role.isAtLeast(this.user.globalRole, Role.OWNER);
      });
    });
  }

  private getDisplayValues(edge: Edge): { version: string; id: string; comment: string; } {
    return {
      comment: edge?.comment,
      id: edge.id,
      version: edge.version,
    };
  }
}
