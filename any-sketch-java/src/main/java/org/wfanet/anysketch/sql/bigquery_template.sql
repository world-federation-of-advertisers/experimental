-- Copyright 2021 The Cross-Media Measurement Authors
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--      http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

-- Helper functions --
CREATE TEMP FUNCTION IF NOT EXISTS FLOOR_CAST(x FLOAT64) RETURNS NUMERIC AS (
  CAST(FLOOR(x) AS NUMERIC)
);
CREATE TEMP FUNCTION IF NOT EXISTS MAX_UINT_64() RETURNS NUMERIC AS (
  POW(CAST(2 AS NUMERIC), 64) - 1
);
CREATE TEMP FUNCTION IF NOT EXISTS CAST_TO_UNSIGNED(x INT64) RETURNS NUMERIC AS (
  IF(x >= 0, CAST(x AS NUMERIC), MAX_UINT_64() + x + 1)
);
CREATE TEMP FUNCTION IF NOT EXISTS FINGERPRINT(salt STRING, value STRING) RETURNS NUMERIC AS (
  CAST_TO_UNSIGNED(FARM_FINGERPRINT(CONCAT(salt, value)))
);
CREATE TEMP FUNCTION IF NOT EXISTS AS_FRACTION(x NUMERIC) RETURNS FLOAT64 AS (
  CAST(x AS FLOAT64) / CAST(MAX_UINT_64() AS FLOAT64)
);
CREATE TEMP FUNCTION IF NOT EXISTS EXPONENTIAL_DISTRIBUTION(fingerprint NUMERIC, rate FLOAT64, size INT64) RETURNS NUMERIC AS (
  FLOOR_CAST(
    (1 - LOG(EXP(rate) + AS_FRACTION(fingerprint) * (1 - EXP(rate))) / rate) * size
  )
);
CREATE TEMP FUNCTION IF NOT EXISTS UNIFORM_DISTRIBUTION(fingerprint NUMERIC, size INT64) RETURNS NUMERIC AS (
  MOD(fingerprint, size)
);
CREATE TEMP FUNCTION IF NOT EXISTS GEOMETRIC_DISTRIBUTION(fingerprint NUMERIC) RETURNS NUMERIC AS (
  FLOOR_CAST(LOG(fingerprint, 2))
);

-- Sketch Query --
WITH T1 AS (
  SELECT
    $FINGERPRINT_SELECT
  FROM $ORIGINAL_TABLE AS Impressions
), T2 AS (
  SELECT
    $DISTRIBUTION_SELECT
  FROM T1
)
SELECT
  $AGGREGATION_SELECT
FROM T2
GROUP BY $GROUP_BY_COLUMNS