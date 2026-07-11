# SchoolSaaS Backend

Backend robuste et scalable pour la plateforme **SchoolSaaS**, une solution de gestion scolaire complète (ERP) conçue pour les écoles privées en Guinée et en Afrique de l'Ouest.

## 🚀 Vue d'ensemble

SchoolSaaS permet aux établissements scolaires de gérer l'intégralité de leurs processus : de l'onboarding des écoles (multi-tenant) à la gestion des inscriptions, des frais de scolarité, des notes, des présences et de la communication par SMS.

## 🛠 Stack Technique

- **Java 17**
- **Spring Boot 3.3.4**
- **PostgreSQL 16** (Architecture Multi-tenant schema-per-tenant)
- **Spring Security & JWT** (Stateless authentication)
- **Hibernate 6** (Gestion de la persistence et du multi-tenancy)
- **Flyway** (Migrations de base de données public & tenant)
- **MapStruct 1.6.2** (Mappage Entité ↔ DTO)
- **Lombok** (Réduction du boilerplate)
- **SpringDoc OpenAPI 2.6.0** (Documentation Swagger)
- **Caffeine Cache** (Performance du dashboard)
- **Thymeleaf + OpenHTMLtoPDF** (Génération de bulletins et reçus PDF)

## 🏗 Architecture

Le projet suit une architecture **modulaire par domaine**, garantissant une séparation claire des responsabilités :

- `platform` : Gestion des écoles (tenants) et abonnements (Schéma `public`).
- `identity` : Authentification, rôles et gestion des utilisateurs.
- `academic` : Années scolaires, cycles, classes et matières.
- `enrollment` : Inscriptions des élèves et promotions.
- `finance` : Structures de frais, paiements et allocations.
- `communication` : Envoi de SMS (rappels de frais, notifications de notes).
- `grading` / `attendance` / `timetable` : Gestion pédagogique.
- `infrastructure` : Services transversaux (SMS providers, Storage, PDF).

### Multi-tenancy
Le système utilise une isolation par **schéma PostgreSQL**. Chaque école possède son propre schéma de base de données, créé et initialisé lors de l'onboarding. Le `tenantId` est extrait du jeton JWT pour router chaque requête vers le bon schéma.

## 🔐 Sécurité

- Authentification via **JWT**.
- Autorisations basées sur les rôles (`DIRECTOR`, `TEACHER`, `ACCOUNTANT`).
- Isolation stricte des données entre les écoles.

## 📖 Documentation API

La documentation Swagger est disponible une fois l'application lancée à l'adresse suivante :
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

Le fichier de spécification JSON peut être généré via les tests d'intégration dans `target/openapi-spec.json`.

## ⚙️ Installation et Lancement

### Prérequis
- Java 17+
- Maven 3.9+
- PostgreSQL 16+

### Configuration
1. Cloner le dépôt.
2. Configurer les variables d'environnement (ou modifier `application.yml`) :
   - `DATABASE_URL`
   - `DATABASE_USERNAME`
   - `DATABASE_PASSWORD`
   - `JWT_SECRET`

### Lancement
```bash
# Compilation et packaging
./mvnw clean package -DskipTests

# Lancement de l'application
./mvnw spring-boot:run
```

## 🧪 Tests
L'application utilise **Testcontainers** pour les tests d'intégration nécessitant une base de données réelle.
```bash
./mvnw test
```

## 📝 Licence
Ce projet est la propriété de SchoolSaaS.
