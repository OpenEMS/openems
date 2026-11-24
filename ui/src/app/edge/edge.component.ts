// @ts-strict-ignore
import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, OnDestroy, OnInit } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { ActivatedRoute, Router } from "@angular/router";
import { distinctUntilChanged, map, skip } from "rxjs";
import { Edge, Service, Websocket } from "src/app/shared/shared";
import { States } from "src/app/shared/states/states";
import { Pagination } from "../shared/service/pagination";
import { RouteService } from "../shared/service/route/route.service";

/** This component is needed as a routing parent and acts as a transit station without being displayed. */
@Component({
    selector: "edge",
    template: `
        <ng-content></ng-content>
        <router-outlet id="content"></router-outlet>
    `,
    standalone: false,
    changeDetection: ChangeDetectionStrategy.Eager,
})
export class EdgeComponent implements OnDestroy, OnInit {
    private latestIncident: { message: string | null; id: string } | null = null;
    private edge: Edge | null = null;
    private readonly destroyRef = inject(DestroyRef);

    constructor(
        private routeService: RouteService,
        private service: Service,
        private websocket: Websocket,
        private pagination: Pagination,
        private router: Router,
        private route: ActivatedRoute,
    ) {
        effect(async () => {
            const edge = this.service.currentEdge();
            const edgeId = this.routeService.getRouteParam<string>("edgeId");
            if (!edgeId || !edge || edge.id == this.edge?.id) {
                return;
            }
            this.edge = edge;

            pagination.getAndSubscribeEdge(edge.id);
        });
    }

    public async ngOnInit(): Promise<void> {
        this.route.paramMap
            .pipe(
                map((params) => params.get("edgeId")),
                distinctUntilChanged(),
                skip(1),
                takeUntilDestroyed(this.destroyRef),
            )
            .subscribe((edgeId) => {
                if (edgeId != null) {
                    void this.selectEdge(edgeId);
                }
            });
        const edgeId = this.route.snapshot.paramMap.get("edgeId");
        if (edgeId != null) {
            await this.selectEdge(edgeId);
        }
    }

    public ngOnDestroy(): void {
        this.service.currentEdge.set(null);
    }

    private async selectEdge(edgeId: string): Promise<void> {
        try {
            const edge = await this.service.updateCurrentEdge(edgeId);
            if (this.route.snapshot.paramMap.get("edgeId") !== edgeId) {
                return;
            }
            this.websocket.state.set(States.EDGE_SELECTED);
            await this.pagination.subscribeEdge(edge, this.websocket);
        } catch {
            if (this.route.snapshot.paramMap.get("edgeId") === edgeId) {
                await this.router.navigate(["index"]);
            }
        }
    }
}
