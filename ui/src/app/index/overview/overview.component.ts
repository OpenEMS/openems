import { Component, OnDestroy, OnInit } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { InfiniteScrollCustomEvent } from "@ionic/angular";
import { TranslateService } from '@ngx-translate/core';
import { Subject } from "rxjs";
import { filter, take } from 'rxjs/operators';
import { Pagination } from "src/app/shared/service/pagination";
import { Edge, Service, Utils, Websocket } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { environment } from "src/environments";

import { ChosenFilter } from "../filter/filter.component";

@Component({
    selector: 'overview',
    templateUrl: './overview.component.html',
})
export class OverViewComponent implements OnInit, OnDestroy {
    public environment = environment;
    /** True, if there is no access to any Edge. */
    public noEdges: boolean = false;

    /**  True, if the logged in user is allowed to install new edges. */
    public loggedInUserCanInstall: boolean = false;

    public form: FormGroup;
    public filteredEdges: Edge[] = [];

    private stopOnDestroy: Subject<void> = new Subject<void>();
    private page = 0;
    private query: string | null = null;

    /** Limits edges in pagination response */
    private readonly limit: number = 20;
    /** True, if all available edges for this user had been retrieved */
    private limitReached: boolean = false;
    protected loading: boolean = false;
    protected searchParams: Map<string, ChosenFilter['value']> = new Map();

    constructor(
        public service: Service,
        public websocket: Websocket,
        public utils: Utils,
        private router: Router,
        private route: ActivatedRoute,
        public translate: TranslateService,
        public pagination: Pagination,
    ) { }

    ngOnInit() {
        this.page = 0;
        this.filteredEdges = [];
        this.limitReached = false;
        this.service.metadata.pipe(filter(metadata => !!metadata), take(1)).subscribe(() => {
            this.init();
        });
    }

    ionViewWillEnter() {
        this.service.setCurrentComponent('', this.route);
    }

    protected getAndSubscribeEdge(edge: Edge) {
        this.pagination.getAndSubscribeEdge(edge);
    }

    /**
     * Search on change, triggered by searchbar input-event.
     *
     * @param event from template passed event
     */
    protected searchOnChange(searchParams?: Map<string, ChosenFilter['value']>) {

        if (searchParams) {
            this.searchParams = searchParams;
        }

        this.filteredEdges = [];
        this.page = 0;
        this.limitReached = false;

        this.loadNextPage().then((edges) => {
            this.filteredEdges = edges;
            this.page++;
        });
    }

    private init() {
        this.loadNextPage().then((edges) => {
            this.service.metadata
                .pipe(
                    filter(metadata => !!metadata),
                    take(1),
                )
                .subscribe(metadata => {

                    const edgeIds = Object.keys(metadata.edges);
                    this.noEdges = edgeIds.length === 0;
                    this.loggedInUserCanInstall = Role.isAtLeast(metadata.user.globalRole, "installer");

                    // Forward directly to device page, if
                    // - Direct local access to Edge
                    // - No installer (i.e. guest or owner) and access to only one Edge
                    if (environment.backend == 'OpenEMS Edge' || (!this.loggedInUserCanInstall && edgeIds.length == 1)) {
                        const edge = metadata.edges[edgeIds[0]];
                        setTimeout(() => {
                            this.router.navigate(['/device', edge.id]);
                        }, 100);
                        return;
                    }
                    this.filteredEdges = edges;
                });
        });
    }

    /**
     * Updates available edges on scroll-event
     *
     * @param infiniteScroll the InfiniteScrollCustomEvent
     */
    doInfinite(infiniteScroll: InfiniteScrollCustomEvent) {
        setTimeout(() => {
            this.page++;
            this.loadNextPage().then((edges) => {
                this.filteredEdges.push(...edges);
                infiniteScroll.target.complete();
            }).catch(() => {
                infiniteScroll.target.complete();
            });
        }, 200);
    }

    ngOnDestroy() {
        this.stopOnDestroy.next();
        this.stopOnDestroy.complete();
    }

    loadNextPage(): Promise<Edge[]> {

        this.loading = true;
        return new Promise<Edge[]>((resolve, reject) => {
            if (this.limitReached) {
                resolve([]);
                return;
            }

            const searchParamsObj = {};
            if (this.searchParams && this.searchParams.size > 0) {
                for (const [key, value] of this.searchParams) {
                    searchParamsObj[key] = value;
                }
            }
            this.service.getEdges(this.page, this.query, this.limit, searchParamsObj)
                .then((edges) => {
                    this.limitReached = edges.length < this.limit;
                    resolve(edges);
                }).catch((err) => {
                    reject(err);
                });
        }).finally(() =>
            this.loading = false);
    }
}
