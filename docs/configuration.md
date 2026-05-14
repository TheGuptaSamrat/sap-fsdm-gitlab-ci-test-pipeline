# Configuration Reference

## `config/entities.csv`

| Column | Required | Description |
| --- | --- | --- |
| `entity_name` | Yes | Logical entity name, for example `financial_contract`. |
| `table_name` | Yes | Target HANA table, for example `/FSDM/D_...`. |
| `input_csv` | Yes | File name under `data/input`. |
| `key_columns` | Yes | Pipe-separated business key columns. Include bitemporal keys when needed. |
| `compare_columns` | No | Pipe-separated validation columns. Defaults to all CSV headers. |
| `insert_mode` | No | `APPEND` or `DELETE_INSERT`. Use `APPEND` for shared SAP tables. |
| `where_clause` | No | SQL predicate used for validation reads. Supports `${RUN_ID}` substitution. |

## `config/jobs.csv`

| Column | Required | Description |
| --- | --- | --- |
| `sequence` | Yes | Numeric execution order. |
| `stage_name` | Yes | Stable name used by `validations.csv`. |
| `job_name` | Yes | SAP job/RFC action name. |
| `endpoint_path` | Yes | API path below `RFC_HTTP_BASE_URL`. |
| `PP_01` | Yes | First mandatory job parameter. |
| `PP_02` to `PP_10` | No | Optional job parameters. |

## `config/validations.csv`

| Column | Required | Description |
| --- | --- | --- |
| `stage_name` | Yes | Stage to validate after, for example `activate` or `z30_rspc`. |
| `entity_name` | Yes | Logical entity name. |
| `table_name` | Yes | HANA table to read. |
| `expected_csv` | Yes | File under `data/expected`. |
| `key_columns` | Yes | Pipe-separated comparison key. |
| `compare_columns` | No | Pipe-separated fields to compare. Defaults to expected CSV headers. |
| `where_clause` | No | SQL predicate used to read actual rows. Supports `${RUN_ID}` substitution. |
