from src.noiseninja.noised_measurements import Measurement
from src.report.report import Report, MetricReport
import sys
import argparse
import pandas as pd
from functools import partial

# This is a demo script that has the following assumptions :
#   1. There are 2 EDPs one with Name Google, the other Linear TV.
#   2. CUSTOM filter is ignored since it won't be used for the forseeable future.
#   3. AMI is a parent of MRC and there are no other relationships between metrics.
#   4. The variance is assumed to be 1??
#   5. Frequency results are not corrected.
#   6. Impression results are not corrected.
#   7. The issue with the Linear TV shouldn't be corrected can be addressed by setting the variance of its measurements to 0.


# Important Note: If sigma is set to a low number like 1, the corrected report returns numbers very close to 0. 
SIGMA = 100

AMI_FILTER = "AMI"
MRC_FILTER = "MRC"

EDP_ONE = "Google"
EDP_TWO = "Linear TV"
TOTAL_CAMPAIGN = "Total Campaign"

edp_names = [EDP_ONE, EDP_TWO]

CUML_REACH_PREFIX = "Cuml. Reach"

CUML_REACH_MAP = {
    edp_name: {"sheet": f"{CUML_REACH_PREFIX} ({edp_name})", "ind": ind}
    for ind, edp_name in enumerate(edp_names + [TOTAL_CAMPAIGN])
}

CUML_REACH_COL_NAME = "Cumulative Reach 1+"
FILTER_COL_NAME = "Impression Filter"

ami = "ami"
mrc = "mrc"


def readExcel(excel_file_path):
    try:
        measurements = {}
        dfs = pd.read_excel(excel_file_path, sheet_name=None)
        for edp in CUML_REACH_MAP:
            sheet_name = CUML_REACH_MAP[edp]["sheet"]
            df = dfs[sheet_name]
            ami_rows = df[df[FILTER_COL_NAME] == AMI_FILTER]
            mrc_rows = df[df[FILTER_COL_NAME] == MRC_FILTER]
            # These rows are already sorted by timestamp.
            ami_measurements = [
                Measurement(measured_value, SIGMA)
                for measured_value in list(ami_rows[CUML_REACH_COL_NAME])
            ]
            mrc_measurements = [
                Measurement(measured_value, SIGMA)
                for measured_value in list(mrc_rows[CUML_REACH_COL_NAME])
            ]
            measurements[edp] = {
                AMI_FILTER: ami_measurements,
                MRC_FILTER: mrc_measurements,
            }

    except FileNotFoundError:
        print(f"Error: File not found at {excel_file_path}")
    except Exception as e:
        print(f"An error occurred: {e}")

    return (measurements, dfs)


def getCorrectedReport(measurements):
    report = Report(
        {
            ami: MetricReport(
                total_campaign_reach_time_series=measurements[TOTAL_CAMPAIGN][
                    AMI_FILTER
                ],
                reach_time_series_by_edp=[
                    measurements[EDP_ONE][AMI_FILTER],
                    measurements[EDP_TWO][AMI_FILTER],
                ],
            ),
            mrc: MetricReport(
                total_campaign_reach_time_series=measurements[TOTAL_CAMPAIGN][
                    MRC_FILTER
                ],
                reach_time_series_by_edp=[
                    measurements[EDP_ONE][MRC_FILTER],
                    measurements[EDP_TWO][MRC_FILTER],
                ],
            ),
        },
        # AMI is a parent of MRC
        metric_subsets_by_parent={ami: [mrc]},
    )
    return report.get_corrected_report()

def correctSheetMetric(df, rows, func):
    for period, (index, row) in enumerate(rows.iterrows()):
        df.at[index, CUML_REACH_COL_NAME] = int(func(period).value)

def correctSheet(df, ami_func, mrc_func):
    ami_rows = df[df[FILTER_COL_NAME] == AMI_FILTER]
    mrc_rows = df[df[FILTER_COL_NAME] == MRC_FILTER]
    correctSheetMetric(df, ami_rows, ami_func)
    correctSheetMetric(df, mrc_rows, mrc_func)
    return df

def buildCorrectedExcel(correctedReport, excel):
    ami_metric_report = correctedReport.get_metric_report(ami)
    mrc_metric_report = correctedReport.get_metric_report(mrc)
    for edp in CUML_REACH_MAP:
        amiFunc = (
            ami_metric_report.get_total_reach_measurement
            if (edp == TOTAL_CAMPAIGN)
            else partial(
                ami_metric_report.get_edp_measurement, CUML_REACH_MAP[edp]["ind"]
            )
        )
        mrcFunc = (
            mrc_metric_report.get_total_reach_measurement
            if (edp == TOTAL_CAMPAIGN)
            else partial(
                mrc_metric_report.get_edp_measurement, CUML_REACH_MAP[edp]["ind"]
            )
        )

        sheet_name = CUML_REACH_MAP[edp]['sheet']
        excel[sheet_name] = correctSheet(
            excel[sheet_name], amiFunc, mrcFunc
        )
    return excel


def writeCorrectedExcel(path, corrected_excel):
    with pd.ExcelWriter(path) as writer:
        # Write each dataframe to a different sheet
        for sheet_name in corrected_excel:
            corrected_excel[sheet_name].to_excel(
                writer, sheet_name=sheet_name, index=False
            )


def main():
    parser = argparse.ArgumentParser(description="Read an Excel file.")
    parser.add_argument(
        "--path_to_report", required=True, help="Path to the Excel file"
    )
    args = parser.parse_args()

    (measurements, excel) = readExcel(args.path_to_report)
    correctedReport = getCorrectedReport(measurements)
    correctedExcel = buildCorrectedExcel(correctedReport, excel)
    writeCorrectedExcel(
        f"{args.path_to_report.split('.')[0]}_corrected.xlsx", correctedExcel
    )


if __name__ == "__main__":
    main()
