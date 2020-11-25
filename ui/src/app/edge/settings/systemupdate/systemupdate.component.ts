import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { parse } from 'date-fns';
import { Subject, timer } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { cmp } from 'semver-compare-multi';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { ExecuteSystemCommandRequest } from 'src/app/shared/jsonrpc/request/executeCommandRequest';
import { ExecuteSystemCommandResponse } from 'src/app/shared/jsonrpc/response/executeSystemCommandResponse';
import { Edge, Service, Utils, Websocket } from '../../../shared/shared';

class Package {

  public currentVersion = null;
  public latestVersion = null;
  public isUpdateAvailable = null;

  constructor(
    public readonly name: string,
    public readonly description: string,
  ) {
  }

  public getNameUnderscore() {
    return this.name.replace(/-/g, "_");
  }

  public setVersions(current: string, latest: string) {
    this.currentVersion = current;
    this.latestVersion = latest;
    if (current == null || latest == null) {
      this.isUpdateAvailable = null;
      return;
    }
    this.isUpdateAvailable = cmp(this.latestVersion, this.currentVersion) > 0
  }

  public resetVersions() {
    this.setVersions(null, null);
  }
};

@Component({
  selector: SystemUpdateComponent.SELECTOR,
  templateUrl: './systemupdate.component.html'
})
export class SystemUpdateComponent implements OnInit, OnDestroy {

  private static readonly SELECTOR = "systemUpdate";
  private static readonly USERNAME = "root";
  private static readonly PASSWORD = "hidden";
  private static readonly MAX_LOG_ENTRIES = 200;

  public edge: Edge = null;
  private ngUnsubscribe = new Subject<void>();
  public isDpkgRunning: Boolean = null;

  public logLines: {
    time: string,
    level: string,
    color: string,
    message: string,
    source: string
  }[] = [];

  public packages = [
    new Package("openems-core", "OpenEMS"),
    new Package("openems-core-fems", "FEMS"),
  ]

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent("", this.route).then(edge => {
      this.edge = edge;

      // Update version information now and every minute
      const source = timer(0, 30000);
      source.pipe(
        takeUntil(this.ngUnsubscribe)
      ).subscribe(ignore => {
        this.updateVersions();
      });

      // Subscribe to system log
      this.subscribeSystemLog();
    });
  }

  ngOnDestroy() {
    this.unsubscribeSystemLog();
  }

  private updateVersions() {
    this.log("INFO", "Updating versions");

    let command = "";
    for (let pkg of this.packages) {
      pkg.resetVersions();
      // Read currently installed version
      command += "dpkg-query --showformat='" + pkg.name + " current:${Version}\\n' --show " + pkg.name + "; ";
      // Read latest version
      command += "echo -n \"" + pkg.name + " latest:\"; "
        + "wget -qO- http://fenecon.de/debian-test/" + pkg.getNameUnderscore() + "-latest.version; ";
    }
    command += "lsof /var/lib/dpkg/lock >/dev/null 2>&1; [ $? = 0 ] && echo \"dpkg is running\"";

    this.edge.sendRequest(this.websocket,
      new ComponentJsonApiRequest({
        componentId: "_host",
        payload: new ExecuteSystemCommandRequest({
          username: SystemUpdateComponent.USERNAME,
          password: SystemUpdateComponent.PASSWORD,
          timeoutSeconds: 60,
          runInBackground: false,
          command: command
        })
      })).then(response => {
        let result = (response as ExecuteSystemCommandResponse).result;
        if (result.stdout.length = this.packages.length * 2 + 1) {
          for (let i = 0; i < this.packages.length; i++) {
            let pkg = this.packages[i];
            pkg.setVersions(result.stdout[i * 2].split(":")[1], result.stdout[i * 2 + 1].split(":")[1]);
          }
          if (result.stdout[result.stdout.length - 1] == "dpkg is running") {
            this.isDpkgRunning = true;
          } else {
            this.isDpkgRunning = false;
          }
        }

      }).catch(reason => {
        this.log("ERROR", reason.error.message);
      });
  }

  public updatePackage(pkg: Package) {
    let filename = pkg.getNameUnderscore() + "-" + pkg.latestVersion + ".deb";

    // Start Download
    this.edge.sendRequest(this.websocket,
      new ComponentJsonApiRequest({
        componentId: "_host",
        payload: new ExecuteSystemCommandRequest({
          username: SystemUpdateComponent.USERNAME,
          password: SystemUpdateComponent.PASSWORD,
          timeoutSeconds: 300,
          runInBackground: false,
          command: "which at || DEBIAN_FRONTEND=noninteractive apt-get -y install at; "
            + "wget http://fenecon.de/debian-test/" + filename + " -q --show-progress --progress=dot:mega -O /tmp/" + filename + " "
            + "&& echo 'DEBIAN_FRONTEND=noninteractive nohup dpkg -i /tmp/" + filename + "' | at now"
        })
      })).then(response => {
        this.log("INFO", "Downloading [" + filename + "]");

      }).catch(reason => {
        this.log("ERROR", reason.error.message);
      });
  }

  private subscribeSystemLog() {
    this.service.getCurrentEdge().then(edge => {
      // send request to Edge
      edge.subscribeSystemLog(this.websocket);

      // subscribe to notifications
      edge.systemLog.pipe(
        takeUntil(this.ngUnsubscribe),
        filter(line => line.source.startsWith("io.openems.edge.core.host."))
      ).subscribe(line => {
        // add line
        this.logLines.unshift({
          time: parse(line.time, "yyyy-MM-dd'T'HH:mm:ss.SSSxxx", new Date()).toLocaleString(),
          color: this.getColor(line.level),
          level: line.level,
          source: line.source,
          message: line.message,
        })

        // remove old lines
        if (this.logLines.length > SystemUpdateComponent.MAX_LOG_ENTRIES) {
          this.logLines.length = SystemUpdateComponent.MAX_LOG_ENTRIES;
        }
      });
    })
  };

  private unsubscribeSystemLog() {
    this.service.getCurrentEdge().then(edge => {
      edge.unsubscribeSystemLog(this.websocket);
    });
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
    this.ngUnsubscribe = new Subject<void>();
  }

  private getColor(level): string {
    switch (level) {
      case 'INFO':
        return 'green';
      case 'WARN':
        return 'orange';
      case 'DEBUG':
        return 'gray';
      case 'ERROR':
        return 'red';
    };
    return 'black';
  }

  private log(level: 'INFO' | 'ERROR', message: string) {
    this.logLines.unshift({
      time: new Date().toLocaleString(),
      color: this.getColor(level),
      level: level,
      source: "",
      message: message,
    })
  }
}