# Requirements Mapping

| Requirement | Repository Support |
| --- | --- |
| Upload CSV files for financial contract, settlement, accrual, business partner, effective limit, and future entities | `config/entities.csv` defines an open-ended entity list, target table, input file, key columns, compare columns, and validation predicate. |
| Insert into `/FSDM/D_*` tables using JDBC | `DbClient` inserts CSV rows with JDBC prepared statements and quoted HANA identifiers. |
| Configurable JDBC URL, user, password | `.gitlab-ci.yml` expects `HANA_JDBC_URL`, `HANA_USER`, and `HANA_PASSWORD` from GitLab CI/CD variables. |
| Validate inserted data against CSV | `TableLoader` reads the actual HANA rows after insert and compares them with the input CSV. |
| Call SAP RFC endpoints with job name and parameters | `RfcJobClient` posts `jobName` and `PP_01` to `PP_10` parameters to the configured RFC endpoint API. |
| Job parameters configurable in CSV | `config/jobs.csv` controls sequence, job, endpoint path, and `PP_01` through `PP_10`. |
| `PP_01` mandatory and remaining parameters optional | `JobConfig` rejects any job row with blank `PP_01`; `PP_02` through `PP_10` are optional. |
| Flow from D load to Activate, Z30, Z40, balance snapshot, GAI, netting | `config/jobs.csv` contains the default sequence and `FlowRunner` executes jobs in sequence order. |
| Validate data at each point | `config/validations.csv` maps each stage to expected files under `data/expected/<stage>/`. |
| Record detailed failure reason in HANA | `AuditWriter` writes mismatch details to the configurable `AUDIT_TABLE`; DDL is in `docs/hana-audit-table.sql`. |
| Pipeline configurable through GitLab CI YAML | `.gitlab-ci.yml` exposes config-file paths, data directories, run id, driver path/URL, and integration gating through variables. |
