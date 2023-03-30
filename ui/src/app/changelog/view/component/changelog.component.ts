import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { environment } from 'src/environments';
import { Service } from '../../../shared/shared';
import { Role } from '../../../shared/type/role';
import { Changelog, Library, OpenemsComponent, Product } from './changelog.constants';
import { HttpClient } from '@angular/common/http';

@component({
selector: 'changelog',
templateUrl: './changelog.component.html'
})
export class ChangelogComponent implements OnInit {

public environment = environment;

protected slice: number = 10;
protected showAll: boolean = false;
public changelogs: {
    version: string,
    changes: Array<string | { roleIsAtLeast: Role, change: string }>
}[] = [];

constructor(
    public translate: TranslateService,
    public service: Service,
    private route: ActivatedRoute,
    private http: HttpClient,
) { }

ngOnInit() {
    this.service.setCurrentComponent({ languageKey: 'Menu.changelog' }, this.route);
    this.loadChangelog();
}

public readonly roleIsAtLeast = Role.isAtLeast;
public numberToRole(role: number): string {
    return Role[role].toLowerCase();
}

private loadChangelog(): void {
    this.http.get('assets/changelog.json').subscribe(
        (data: any[]) => {
            this.changelogs = data.map(item => ({
                version: item.version,
                changes: item.changes.map(change => {
                    if (typeof change === 'string') {
                        return change;
                    } else {
                        return { roleIsAtLeast: Role[change.roleIsAtLeast], change: change.change };
                    }
                }),
            }));
        },
        (error) => {
            console.error('Failed to load changelog.json:', error);
        }
    );
}
}
