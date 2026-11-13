# PlantOps

Industrial asset and maintenance management demo built with Vue 3, TypeScript, Spring Boot, and PostgreSQL.

## Run locally

Requirements: Docker Desktop with Compose enabled. From this directory run:

```sh
docker compose up --build
```

Open <http://localhost:5173>. The API is available at <http://localhost:8080/api> and Swagger UI at <http://localhost:8080/swagger-ui.html>.

Demo accounts (password for all: `PlantOps123!`):

| Role | Email |
| --- | --- |
| Admin | `admin@plantops.local` |
| Maintenance manager | `manager@plantops.local` |
| Technician | `tech@plantops.local` |

The first startup seeds a sample factory, locations, assets, schedules, work orders, readings, and accounts. The simulated health feed runs every 20 seconds. Stop with `docker compose down`; use `docker compose down -v` to also clear the demo database.

## Stack and layout

- `frontend/`: Vue 3 + TypeScript + Vite dashboard.
- `backend/`: Java 21 + Spring Boot REST API, Spring Security JWT, Spring Data JPA, PostgreSQL.
- `openshift/`: OpenShift-compatible Deployment, Service, Route, and PostgreSQL resources. Set secrets and image references for your cluster before deploying.

## Development

The Compose setup is the supported local run path. To run the API outside Docker, use Java 21 and Maven with `backend/pom.xml`; set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `JWT_SECRET`. To run the UI outside Docker, use Node.js 20.19+ or 22.12+ in `frontend/`, run `npm install`, then `npm run dev` (Vite proxies `/api` to `localhost:8080`).

For OpenShift, build and push the `backend/` and `frontend/` images to the image references in `openshift/plantops.yaml`, update the secret values, then apply the manifest with `oc apply -f openshift/plantops.yaml`. The UI's Nginx config proxies `/api` to the in-cluster `api` service.

## API

JWT bearer authentication is provided by `POST /api/auth/login`. The other APIs are under `/api`: `/assets`, `/work-orders`, `/schedules`, `/alerts`, `/readings`, `/audit`, `/dashboard`, and `/users`. OpenAPI documentation is exposed at `/swagger-ui.html`.
