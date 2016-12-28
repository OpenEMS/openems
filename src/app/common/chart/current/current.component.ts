import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  OnChanges,
  ElementRef,
  NgZone,
  ChangeDetectionStrategy,
  ChangeDetectorRef
} from '@angular/core';
import { BaseChartComponent } from 'ngx-charts';
import { ColorHelper } from 'ngx-charts';
import * as d3 from 'd3';

class Section {
  private outlinePath: number = null;
  private path: number = null;
  private value: number = 10;
  private innerRadius: number = 0;
  private outerRadius: number = 0;

  constructor(
    private startAngle: number,
    private endAngle: number,
    private color: string) { }

  public update(outerRadius: number, innerRadius: number) {
    this.outerRadius = outerRadius;
    this.innerRadius = innerRadius;
    let arc = this.getArc();
    this.outlinePath = arc.endAngle(this.deg2rad(this.endAngle))();
    let valueEndAngle = ((this.endAngle - this.startAngle) * this.value) / 100 + this.startAngle;
    this.path = arc.endAngle(this.deg2rad(valueEndAngle))();
  }

  public setValue(value: number) {
    if (value > 100) {
      value = 100;
    } else if (value < 0) {
      value = 0;
    }
    this.value = value;
    this.update(this.innerRadius, this.outerRadius);
  }

  public getValue(): number {
    return this.value;
  }

  private getArc(): any {
    return d3.arc()
      .innerRadius(this.innerRadius)
      .outerRadius(this.outerRadius)
      .startAngle(this.deg2rad(this.startAngle));
  }

  private deg2rad(value: number): number {
    return value * (Math.PI / 180)
  }
}

@Component({
  selector: 'chart-current',
  templateUrl: './current.component.html'
})
export class ChartCurrentComponent extends BaseChartComponent implements OnChanges, OnInit {

  private translation: string;

  private sections = {
    grid: new Section(226, 314, "blue"),
    production: new Section(316, 404, "green"),
    consumption: new Section(46, 134, "yellow"),
    storage: new Section(136, 224, "red")
  }

  ngOnInit() {
    this.update();
  }

  ngOnChanges() {
    this.update();
  }

  update() {
    super.update();
    let xOffset = this.width / 2;
    let yOffset = this.height / 2;
    this.translation = `translate(${xOffset}, ${yOffset})`;

    let radius = Math.min(this.width, this.height) / 2;
    let outerRadius = radius;
    let innerRadius = radius - 30;

    for (let section in this.sections) {
      this.sections[section].update(outerRadius, innerRadius);
    }

    console.log("VIEW", this.width, this.height);

    setInterval(() => {
      this.sections.grid.setValue(this.sections.grid.getValue() + 10);
    }, 500);
  }
}
