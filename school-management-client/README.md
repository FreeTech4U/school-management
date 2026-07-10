# School Management Client

Frontend Angular pour une application de gestion d'ecole.

## Lancer le projet

```bash
npm install
npm start
```

Le serveur de dev demarre sur `http://localhost:4200/`.

## Compiler le projet

```bash
npm run build
```

## Structure recommandee

```text
src/
  app/
	core/
	  layout/
		header/
		footer/
	features/
	  auth/
		models/
		pages/
		  login/
	  marketing/
		pages/
		  home/
	app.config.ts
	app.routes.ts
	app.ts
  styles.css
```

- `core/`: layout global, services singleton, guards.
- `features/`: modules metier (auth, students, teachers, finance, attendance, communication).
- `shared/` (a ajouter ensuite): composants reutilisables, pipes, directives, UI kit.

## Librairies recommandees pour ce calibre

- `@angular/cdk`: composants et patterns UI robustes.
- `@ngx-translate/core` + `@ngx-translate/http-loader`: internationalisation runtime (fr/en).
- `rxjs`: flux reactifs (deja present).

## Charte de couleur (globale)

La charte est centralisee dans `src/styles.css` via variables CSS:

- `--color-primary`: vert foret guineen (`#1E6F3F`)
- `--color-secondary`: orange soleil (`#F5A623`)
- `--color-surface`: gris clair profond (`#F5F7FA`)
- `--color-success`: vert clair paiement complet (`#2ECC71`)
- `--color-warning`: orange tranche restante (`#F39C12`)
- `--color-danger`: rouge guinee impaye/retard (`#E74C3C`)

Ces couleurs et les classes globales (`.btn`, `.btn-primary`, `.btn-ghost`, `.container`) sont reutilisees dans toute l'application.

## Internationalisation (fr/en)

- Fichiers de traduction: `public/i18n/fr.json` et `public/i18n/en.json`
- Configuration provider: `src/app/app.config.ts`
- Service langue: `src/app/core/i18n/language.service.ts`
- Selection langue dans le header (`FR` / `EN`)

## Mode mock pour tester la connexion

Le projet inclut maintenant un faux service d'authentification pour tester le parcours sans backend.

- Service mock: `src/app/core/auth/auth.service.ts`
- Guardes: `src/app/core/auth/auth.guard.ts` et `src/app/core/auth/guest.guard.ts`
- Garde role-based: `src/app/core/auth/role.guard.ts`
- Navigation role-based: `src/app/core/navigation/navigation.service.ts`
- Route protegee: `/dashboard`
- Session de test stockee dans `sessionStorage`

Comptes de demo disponibles:

- `admin@school.com` / `School@123`
- `compta@school.com` / `School@123`
- `+2250102030405` / `School@123`

## Structure RBAC (roles et sous-menus)

Le projet est maintenant profile pour permettre des fonctionnalites et sous-menus selon le role stocke dans le token.

- Le role est recupere depuis le token mock et expose par `AuthService`
- Le menu authentifie est genere dynamiquement via `NavigationService`
- Les routes sensibles utilisent `roleGuard([...roles])`
- Les modules metier sont prepares via des routes protegees (`/students`, `/attendance`, `/billing`, `/payments`, `/reports`, `/communication`)

## Scripts utiles

```bash
npm start
npm run build
npm run test
```

