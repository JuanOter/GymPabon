# GymPabon – Backend (API REST)

API REST del sistema de gestión del gimnasio GymPabon. Proyecto de Aula de Programación III.

- Java 21 · Spring Boot 3.3.5 · Maven
- MySQL 8 con JDBC y patrón DAO
- Arquitectura en 3 capas: `controller` (presentación) → `service` (lógica de negocio) → `dao` (acceso a datos)

## Estructura

```
backend/src/main/java/com/gympabon
├── controller   Endpoints REST y manejo global de errores
├── service      Reglas de negocio
├── dao          Interfaces DAO
│   └── impl     Implementaciones JDBC
├── model        Clases del dominio
├── exception    Excepciones propias
└── util         Conexión a la base de datos y cifrado de contraseñas
database/
├── schema.sql   Crea la base de datos y las tablas
└── seed.sql     Datos de prueba
```

## Cómo ejecutarlo

1. **Base de datos.** En MySQL Workbench, abre y ejecuta `database/schema.sql` y después `database/seed.sql`.
2. **Credenciales.** Copia `backend/src/main/resources/application-local.properties.example` en la misma carpeta con el nombre `application-local.properties` y escribe tu usuario y contraseña de MySQL. Git ignora ese archivo.
3. **IntelliJ IDEA.** Ve a *File → Open*, selecciona la carpeta `backend` y ábrela como proyecto Maven. Usa el JDK 21.
4. **Ejecutar.** Corre la clase `GymPabonApplication`.
5. **Probar.** Abre <http://localhost:8080/api/health>. Si todo está bien, responde:

```json
{"api": "ok", "database": "connected"}
```

## Usuarios de prueba

Todos usan la contraseña `GymPabon2026`.

| Usuario | Rol | Estado |
|---|---|---|
| admin | Administrador | Activo |
| lmejia | Recepcionista | Activo |
| kortiz | Recepcionista | Activo |
| mruiz | Recepcionista | Inactivo |

## Ramas y commits

- Cada integrante trabaja en una rama `feature/...` creada desde `develop`.
- Los commits se escriben en inglés: `feat: ...`, `fix: ...`, `docs: ...`, `test: ...`.
- Cuando el módulo está listo, se abre un Pull Request hacia `develop`.
