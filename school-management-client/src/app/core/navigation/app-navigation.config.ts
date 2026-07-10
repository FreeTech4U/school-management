import { NavItem } from './models/nav-item.model';

export const APP_NAVIGATION: NavItem[] = [
  {
    key: 'dashboard',
    labelKey: 'navigation.dashboard',
    route: '/dashboard'
  },
  {
    key: 'academic',
    labelKey: 'navigation.academic.root',
    route: '/students',
    allowedRoles: ['administrator', 'director'],
    children: [
      {
        key: 'students',
        labelKey: 'navigation.academic.students',
        route: '/students',
        allowedRoles: ['administrator', 'director']
      },
      {
        key: 'attendance',
        labelKey: 'navigation.academic.attendance',
        route: '/attendance',
        allowedRoles: ['administrator', 'director']
      }
    ]
  },
  {
    key: 'finance',
    labelKey: 'navigation.finance.root',
    route: '/billing',
    children: [
      {
        key: 'billing',
        labelKey: 'navigation.finance.billing',
        route: '/billing'
      },
      {
        key: 'payments',
        labelKey: 'navigation.finance.payments',
        route: '/payments'
      },
      {
        key: 'reports',
        labelKey: 'navigation.finance.reports',
        route: '/reports',
        allowedRoles: ['administrator', 'director']
      }
    ]
  },
  {
    key: 'communication',
    labelKey: 'navigation.communication',
    route: '/communication'
  }
];

