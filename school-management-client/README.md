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

- `--color-primary`: bleu (`#2563eb`)
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

## Scripts utiles

```bash
npm start
npm run build
npm run test
```

