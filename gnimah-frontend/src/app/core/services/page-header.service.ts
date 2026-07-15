import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface PageHeader {
  title: string;
  subtitle: string;
}

@Injectable({ providedIn: 'root' })
export class PageHeaderService {
  private headerSubject = new BehaviorSubject<PageHeader>({ title: '', subtitle: '' });
  header$ = this.headerSubject.asObservable();

  set(title: string, subtitle = ''): void {
    // Deferred: components call this from ngOnInit, which runs during the same
    // change-detection pass as the layout's template — emitting synchronously
    // here would mutate a binding the layout already checked this tick.
    setTimeout(() => this.headerSubject.next({ title, subtitle }));
  }
}
