# Implementation Plan

## 1. Repository and CI Foundation

1. Keep the project source in GitHub and configure a GitLab project to mirror/import this repository.
2. Use `.gitlab-ci.yml` as the single pipeline entrypoint.
3. Store all secrets in GitLab CI/CD variables, never in Git.
4. Use a GitLab runner with network access to SAP HANA and the RFC endpoint API.
5. Keep the SAP HANA JDBC driver outside source control. Provide it to CI by runner path or internal artifact URL.

## 2. Configurable Entity Load

1. Add every loadable entity to `config/entities.csv`.
2. Map each entity to its target `/FSDM/D_*` table and input CSV file.
3. Treat the entity list as open-ended; adding a new business partner, accrual, settlement, effective limit, or future object is a config-only change when the table columns match the CSV headers.
4. Insert CSV rows using JDBC prepared statements and quoted HANA identifiers so `/FSDM/...` table names are handled safely.
5. Validate the inserted data immediately by reading actual rows through JDBC and comparing keys plus configured compare columns.

## 3. SAP RFC/Job Execution

1. Maintain the execution sequence in `config/jobs.csv`.
2. Keep `PP_01` mandatory for every job row.
3. Allow `PP_02` through `PP_10` to be optional.
4. Send the configured job name and parameters to the RFC endpoint API as JSON.
5. Model the expected order as:
   - load `/FSDM/D_*`
   - Activate D-to-A flow
   - validate activated bitemporal A-layer data
   - Z30 RSPC chain
   - Z40 RSPC chain
   - balance snapshot
   - GAI
   - netting
   - final reconciliation

## 4. Bitemporal Validation

1. Keep bitemporal columns in `key_columns` and/or `compare_columns` per entity.
2. Avoid hardcoding column names like valid-from/valid-to because customer FSDM table naming may vary.
3. Use `where_clause` with `${RUN_ID}` to isolate the test dataset.
4. Validate record counts, key uniqueness, field-level values, and stage-specific expected files.
5. Write every mismatch to a configurable HANA audit table with run id, stage, entity, key, reason, expected JSON, and actual JSON.

## 5. Expected Data

1. Store expected files under `data/expected/<stage>/`.
2. Define each validation point in `config/validations.csv`.
3. Keep expected CSV headers aligned with the compare columns.
4. Prefer narrow expected datasets for CI speed, but include enough data to prove bitemporal validity, amount/currency correctness, and flow completeness.

## 6. Hardening Backlog

1. Add job polling/status handling if the RFC endpoint returns asynchronous job ids.
2. Add typed comparators for dates, decimals, timestamps, and tolerances.
3. Add optional pre-load cleanup by `RUN_ID` rather than broad table deletion.
4. Add row-count and amount-total reconciliation assertions.
5. Add GitLab environments per SAP system: DEV, QA, pre-prod.
6. Add retry/backoff policy for RFC endpoint calls.
7. Add per-stage HTML/JSON reports as CI artifacts.
