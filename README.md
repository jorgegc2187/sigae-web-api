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
- `MAIL_PROVIDER` (`smtp` o `resend`)
- `BOOTSTRAP_ADMIN_*` solo para el perfil `dev`
- `MAIL_*`, `RESEND_API_KEY` y `APP_FRONTEND_URL` para recuperacion/invitacion por correo

Usa `.env.example` como referencia para desarrollo local. No versionar archivos `.env`.

## Correo local

La API carga `.env` automaticamente solo cuando el perfil `dev` fue solicitado explicitamente.
Las variables reales del entorno siguen teniendo prioridad sobre `.env`.

## Arranque por entorno

Desarrollo local:

1. Crear `.env` a partir de `.env.example`.
2. Arrancar con `SPRING_PROFILES_ACTIVE=dev`.
3. La API usara `application-dev.yml` y tomara valores faltantes desde `.env`.

Produccion:

1. Arrancar con `SPRING_PROFILES_ACTIVE=prod`.
2. Inyectar variables reales desde el entorno o la plataforma de despliegue.
3. No depender de `.env` para el arranque del servicio.

Si no se define un perfil explicito, la API ya no asume `dev` por omision.

Para Gmail:

- `MAIL_HOST=smtp.gmail.com`
- `MAIL_PORT=587`
- `MAIL_SMTP_AUTH=true`
- `MAIL_SMTP_STARTTLS_ENABLE=true`
- `MAIL_FROM` debe corresponder a la cuenta autenticada
- `MAIL_PASSWORD` debe ser una App Password valida

Para Resend:

- `MAIL_PROVIDER=resend`
- `RESEND_API_KEY` debe ser una clave valida
- `MAIL_FROM` debe ser un remitente verificado en Resend

Verificacion rapida del flujo:

1. Levantar la API con un `.env` valido.
2. Crear un usuario con `Enviar invitacion por correo`.
3. Confirmar recepcion del correo.
4. Abrir el enlace `/auth/reset-password?token=...` y definir la contraseña.
