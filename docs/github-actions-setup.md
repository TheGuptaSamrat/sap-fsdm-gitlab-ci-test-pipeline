# GitHub Actions Setup

GitHub Actions can run this project as well as GitLab CI.

## Default CI

On every push to `main` and every pull request, GitHub Actions runs:

1. `validate-config`
2. `unit-test`

These jobs use GitHub-hosted Ubuntu runners and do not need SAP credentials.

## SAP Integration Flow

The real SAP flow is intentionally manual because it needs private SAP HANA and RFC endpoint access.

Use `Actions > SAP FSDM CI > Run workflow` and set `run_sap_integration` to `true`.

The job expects a self-hosted runner with these labels:

- `self-hosted`
- `sap`

Use a self-hosted runner because GitHub-hosted runners usually cannot reach private SAP networks, VPN-only HANA endpoints, or internal RFC wrapper APIs.

## GitHub Secrets and Variables

Create these under `Settings > Secrets and variables > Actions`.

| Name | Type |
| --- | --- |
| `HANA_JDBC_URL` | Secret |
| `HANA_USER` | Secret |
| `HANA_PASSWORD` | Secret |
| `HANA_SCHEMA` | Secret, optional |
| `HANA_JDBC_DRIVER_URL` | Secret, optional |
| `HANA_JDBC_DRIVER_JAR` | Variable, optional runner-local path |
| `RFC_HTTP_BASE_URL` | Secret |
| `RFC_HTTP_TOKEN` | Secret, optional |

