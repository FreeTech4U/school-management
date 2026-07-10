import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';
import { guestGuard } from './core/auth/guest.guard';
import { roleGuard } from './core/auth/role.guard';

export const routes: Routes = [
  {
	path: '',
	loadComponent: () =>
	  import('./features/marketing/pages/home/home.page').then((module) => module.HomePage)
  },
  {
	path: 'login',
	canActivate: [guestGuard],
	loadComponent: () =>
	  import('./features/auth/pages/login/login.page').then((module) => module.LoginPage)
  },
  {
	path: 'dashboard',
	canActivate: [authGuard],
	loadComponent: () =>
	  import('./features/dashboard/pages/dashboard.page').then((module) => module.DashboardPage)
  },
  {
	path: 'students',
	canActivate: [roleGuard(['administrator', 'director'])],
	data: { moduleKey: 'students' },
	loadComponent: () =>
	  import('./features/workspace/pages/module-placeholder.page').then((module) => module.ModulePlaceholderPage)
  },
  {
	path: 'attendance',
	canActivate: [roleGuard(['administrator', 'director'])],
	data: { moduleKey: 'attendance' },
	loadComponent: () =>
	  import('./features/workspace/pages/module-placeholder.page').then((module) => module.ModulePlaceholderPage)
  },
  {
	path: 'billing',
	canActivate: [authGuard],
	data: { moduleKey: 'billing' },
	loadComponent: () =>
	  import('./features/workspace/pages/module-placeholder.page').then((module) => module.ModulePlaceholderPage)
  },
  {
	path: 'payments',
	canActivate: [authGuard],
	data: { moduleKey: 'payments' },
	loadComponent: () =>
	  import('./features/workspace/pages/module-placeholder.page').then((module) => module.ModulePlaceholderPage)
  },
  {
	path: 'reports',
	canActivate: [roleGuard(['administrator', 'director'])],
	data: { moduleKey: 'reports' },
	loadComponent: () =>
	  import('./features/workspace/pages/module-placeholder.page').then((module) => module.ModulePlaceholderPage)
  },
  {
	path: 'communication',
	canActivate: [authGuard],
	data: { moduleKey: 'communication' },
	loadComponent: () =>
	  import('./features/workspace/pages/module-placeholder.page').then((module) => module.ModulePlaceholderPage)
  },
  {
	path: '**',
	redirectTo: ''
  }
];
