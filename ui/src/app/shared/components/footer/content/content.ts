import { Component, effect, ViewEncapsulation, ChangeDetectionStrategy } from "@angular/core";
import { Title } from "@angular/platform-browser";
import { RouterModule } from "@angular/router";
import { filter } from "rxjs";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { User } from "src/app/shared/jsonrpc/shared";
import { PipeComponentsModule } from "src/app/shared/pipe/pipe.module";
import { Edge, Service } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { environment } from "src/environments";

@Component({
    selector: "oe-footer-content",
    templateUrl: "./content.html",
    imports: [CommonUiModule, PipeComponentsModule, RouterModule],
    changeDetection: ChangeDetectionStrategy.Eager,
    encapsulation: ViewEncapsulation.ShadowDom,
})
export class FooterContentComponent {
    protected user: User | null = null;
    protected edge: Edge | null = null;
    protected displayValues: { comment: string; id: string; version: string } | null = null;
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

    private static getDisplayValues(user: User, edge: Edge): { comment: string; id: string; version: string } {
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
        this.service.metadata.pipe(filter((metadata) => !!metadata)).subscribe((metadata) => {
            this.user = metadata.user;

            let title = environment.edgeShortName;
            if (edge) {
                this.displayValues = FooterContentComponent.getDisplayValues(this.user, edge);

                if (this.user.hasMultipleEdges) {
                    title += " | " + edge.id;
                }
            }

            this.title.setTitle(title);
        });
    }
}
