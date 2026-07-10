import { Routes } from '@angular/router';

export const routes: Routes = [
  {
	path: '',
	loadComponent: () =>
	  import('./features/marketing/pages/home/home.page').then((module) => module.HomePage)
  },
  {
	path: 'login',
	loadComponent: () =>
	  import('./features/auth/pages/login/login.page').then((module) => module.LoginPage)
  },
  {
	path: '**',
	redirectTo: ''
  }
];
