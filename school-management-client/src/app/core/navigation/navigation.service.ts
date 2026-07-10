import { Injectable } from '@angular/core';

import { MockAuthRole } from '../auth/models/auth.models';
import { APP_NAVIGATION } from './app-navigation.config';
import { NavItem } from './models/nav-item.model';

@Injectable({ providedIn: 'root' })
export class NavigationService {
  getMenuForRole(role: MockAuthRole | null): NavItem[] {
    if (!role) {
      return [];
    }

    return this.filterByRole(APP_NAVIGATION, role);
  }

  private filterByRole(items: NavItem[], role: MockAuthRole): NavItem[] {
    return items
      .filter((item) => !item.allowedRoles || item.allowedRoles.includes(role))
      .map((item) => ({
        ...item,
        children: item.children ? this.filterByRole(item.children, role) : undefined
      }));
  }
}

