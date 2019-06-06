import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { addDays, format, getDate, getMonth, getYear, isSameDay, subDays } from 'date-fns';
import { IMyDate, IMyDateRange, IMyDateRangeModel, IMyDrpOptions } from 'mydaterangepicker';
import { Edge, Service } from '../../shared/shared';

type PeriodString = "today" | "yesterday" | "lastWeek" | "lastMonth" | "lastYear" | "otherPeriod";

@Component({
  selector: 'history',
  templateUrl: './history.component.html'
})
export class HistoryComponent implements OnInit {

  // sets the height for a chart. This is recalculated on every window resize.
  public socChartHeight: string = "250px";
  public energyChartHeight: string = "250px";

  // holds the Edge dependend Widget names
  public widgetNames: string[] = [];

  // holds the current Edge
  protected edge: Edge = null;

  constructor(
    private route: ActivatedRoute,
    public service: Service,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
    });
    this.service.getWidgets().then(widgets => {
      let result: string[] = [];
      for (let widget of widgets) {
        if (!result.includes(widget.name.toString())) {
          result.push(widget.name.toString());
        }
      }
      this.widgetNames = result;
    });
  }

  updateOnWindowResize() {
    let ref = /* fix proportions */ Math.min(window.innerHeight - 150,
      /* handle grid breakpoints */(window.innerWidth < 768 ? window.innerWidth - 150 : window.innerWidth - 400));
    this.socChartHeight =
      /* minimum size */ Math.max(150,
      /* maximium size */ Math.min(200, ref)
    ) + "px";
    this.energyChartHeight =
      /* minimum size */ Math.max(300,
      /* maximium size */ Math.min(600, ref)
    ) + "px";
  }
}