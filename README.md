# SIGAE API

API Spring Boot para SIGAE.

## Configuracion

La configuracion sensible vive en variables de entorno. Los defaults locales estan en
`application-dev.yml`; produccion debe recibir valores reales por entorno y no debe depender
de defaults versionados.

Variables principales:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET`, `JWT_ISSUER`
- `JWT_ACCESS_TOKEN_TTL`, `JWT_REFRESH_TOKEN_TTL`, `JWT_PASSWORD_RESET_TOKEN_TTL`
- `CORS_ALLOWED_ORIGINS`
- `BOOTSTRAP_ADMIN_*` solo para el perfil `dev`

Usa `.env.example` como referencia para desarrollo local. No versionar archivos `.env`.
