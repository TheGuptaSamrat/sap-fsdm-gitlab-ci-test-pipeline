# GitLab Setup

This repository can stay public in GitHub while GitLab runs the pipeline.

## Project Setup

1. Create or import a GitLab project from the GitHub repository.
2. Keep repository mirroring enabled if GitHub remains the source of truth.
3. Use a GitLab runner that can reach:
   - SAP HANA JDBC endpoint
   - the internal RFC/job API endpoint
   - the internal location of the SAP HANA JDBC driver JAR, if `HANA_JDBC_DRIVER_URL` is used
4. Keep the integration job manual until the target SAP client, test data isolation, and audit table are confirmed.

## Required CI/CD Variables

Set these in GitLab under `Settings > CI/CD > Variables`.

| Variable | Recommended Type |
| --- | --- |
| `HANA_JDBC_URL` | Masked, protected |
| `HANA_USER` | Masked, protected |
| `HANA_PASSWORD` | Masked, protected |
| `HANA_SCHEMA` | Protected if system-specific |
| `HANA_JDBC_DRIVER_JAR` | Runner-local path |
| `HANA_JDBC_DRIVER_URL` | Internal URL, optional |
| `RFC_HTTP_BASE_URL` | Protected |
| `RFC_HTTP_TOKEN` | Masked, protected |
| `RUN_SAP_INTEGRATION` | Plain variable, set to `true` only when ready |
| `BUSINESS_DATE` | Plain variable or schedule variable |

## First Real Run

1. Create the HANA audit table from `docs/hana-audit-table.sql`.
2. Replace placeholder `/FSDM/D_*` and `/FSDM/A_*` table names in `config/`.
3. Confirm that the SAP HANA user has insert/select access only for the intended test scope.
4. Add a unique `RUN_ID` column or equivalent test partition to all fixture rows.
5. Start with one entity and the `Activate` job before enabling the full chain.
