# Keycloak-reactive-with-postgres

## Stack
- Kotlin
- Spring Boot
  - Spring Webflux with Coroutines
  - Spring Security with OAuth Resource Server
- DB
  - Postgres R2DBC
  - FlyWay for migration

## Description
Small reactive application which shows how to implement
REST API secured by JWT tokens. Tokens and user management
is outsourced to Keycloak which needs to be running.
Application contains endpoints to add/update and delete patients.
