import { Component, Input, Output, EventEmitter, OnInit, OnChanges, ElementRef, NgZone, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { BaseChartComponent } from 'ngx-charts';
import { ColorHelper } from 'ngx-charts';
import * as d3 from 'd3';
//import { StorageSection, ProductionSection, ConsumptionSection, GridSection } from './section/section';


export class Section {
  public outlinePath;
  public Path;

  constructor(
    public startAngle: number,
    public endAngle: number,
    public percentageAngle: number,
  ) { }
}
@Component({
  selector: 'chart-current',
  templateUrl: './current.component.html'
})
export class ChartCurrentComponent extends BaseChartComponent implements OnInit {

  private sections: Section[];

  ngOnInit() {
    var width = 800;
    var height = 800;
    var radius = Math.min(width, height) / 2;
    var outerRadius = radius;
    var innerRadius = radius - 30;
    var arc = <any>d3.arc()
      .innerRadius(innerRadius)
      .outerRadius(outerRadius);

    var grid = new Section(this.deg2rad(226), this.deg2rad(314), this.deg2rad(300));
    grid.outlinePath = arc
      .startAngle(grid.startAngle)
      .endAngle(grid.endAngle)();

    /* var prod = new Section(this.deg2rad(316), this.deg2rad(404), this.deg2rad(390));
    prod.outlinePath = arc
      .startAngle(prod.startAngle)
      .endAngle(prod.endAngle)();

    var cons = new Section(this.deg2rad(46), this.deg2rad(134), this.deg2rad(90));
    cons.outlinePath = arc
      .startAngle(cons.startAngle)
      .endAngle(cons.endAngle)();

    var battery = new Section(this.deg2rad(136), this.deg2rad(224), this.deg2rad(180));
    battery.outlinePath = arc
      .startAngle(battery.startAngle)
      .endAngle(battery.endAngle)(); */

    var grid = new Section(this.deg2rad(226), this.deg2rad(314), this.deg2rad(300));
    grid.Path = arc
      .startAngle(grid.startAngle)
      .endAngle(grid.percentageAngle)();

    /* var prod = new Section(this.deg2rad(316), this.deg2rad(404), this.deg2rad(390));
    prod.outlinePath = arc
      .startAngle(prod.startAngle)
      .endAngle(prod.percentageAngle)();

    var cons = new Section(this.deg2rad(46), this.deg2rad(134), this.deg2rad(90));
    cons.outlinePath = arc
      .startAngle(cons.startAngle)
      .endAngle(cons.percentageAngle)();

    var battery = new Section(this.deg2rad(136), this.deg2rad(224), this.deg2rad(180));
    battery.outlinePath = arc
      .startAngle(battery.startAngle)
      .endAngle(battery.percentageAngle)()*/


    this.sections = [
      grid, // prod, cons, battery
    ];
  }

  private deg2rad(value: number): number {
    return value * (Math.PI / 180)
  }
}
