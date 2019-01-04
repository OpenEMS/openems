import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, filter } from 'rxjs/operators';

import { Utils } from '../service/utils';
import { Edge } from '../edge/edge';
import { Service } from '../service/service';

@Component({
  selector: 'abstractconfig',
  templateUrl: 'abstractconfig.component.html'
})
export class AbstractConfigComponent implements OnInit {

  public showSubThings: boolean = false;
  public edge: Edge = null;
  // public config: ConfigImpl_2018_7 = null;
  public things: string[] = [];
  private stopOnDestroy: Subject<void> = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private service: Service,
    public utils: Utils
  ) { }

  ngOnInit() {
    this.service.setCurrentEdge(this.route)
    // .pipe(takeUntil(this.stopOnDestroy),
    //   filter(edge => edge != null))
    // .subscribe(edge => {
    //   this.edge = edge;
    //   edge.config
    //     .pipe(filter(edge => edge != null),
    //       takeUntil(this.stopOnDestroy)).subscribe(config => {
    //         if (edge.isVersionAtLeast('2018.8')) {
    //           console.error("AbstractConfigComponent is not compatible with version > 2018.8");
    //           this.config = null;
    //           this.things = [];
    //         } else {
    //           this.config = <ConfigImpl_2018_7>config;
    //           this.things = this.filterThings(config);
    //         }
    //       });
    // });
  }

  ngOnDestroy() {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }

  // TODO
  // protected filterThings(config: ConfigImpl): string[] {
  //   return this.utils.keys(this.config.things);
  // }
}