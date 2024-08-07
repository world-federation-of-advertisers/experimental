from src.noiseninja.noised_measurements import Measurement
from src.report.report import Report, MetricReport
import sys
import argparse
import pandas as pd
from functools import partial
import math

# This is a demo script that has the following assumptions :
#   1. There are 2 EDPs one with Name Google, the other Linear TV.
#   2. CUSTOM filters are not yet supported in this tool.
#   3. AMI is a parent of MRC and there are no other relationships between metrics.
#   4. The standard deviation for all Measurements are assumed to be 1
#   5. Frequency results are not corrected.
#   6. Impression results are not corrected.


SIGMA = 1

AMI_FILTER = "AMI"
MRC_FILTER = "MRC"


# TODO(uakyol) : Read the EDP names dynamically from the excel sheet
# TODO(uakyol) : Make this work for 3 EDPs
EDP_ONE = "Google"
EDP_TWO = "Linear TV"
TOTAL_CAMPAIGN = "Total Campaign"

edp_names = [EDP_ONE, EDP_TWO]

CUML_REACH_PREFIX = "Cuml. Reach"

EDP_MAP = {
    edp_name: {"sheet": f"{CUML_REACH_PREFIX} ({edp_name})", "ind": ind}
    for ind, edp_name in enumerate(edp_names + [TOTAL_CAMPAIGN])
}

CUML_REACH_COL_NAME = "Cumulative Reach 1+"
TOTAL_REACH_COL_NAME = "Total Reach (1+)"
FILTER_COL_NAME = "Impression Filter"

ami = "ami"
mrc = "mrc"


def createMeasurements(rows, reach_col_name, sigma):
    # These rows are already sorted by timestamp.
    return [
        Measurement(measured_value, sigma)
        for measured_value in list(rows[reach_col_name])
    ]


def getMeasurements(df, reach_col_name, sigma):
    ami_rows = df[df[FILTER_COL_NAME] == AMI_FILTER]
    mrc_rows = df[df[FILTER_COL_NAME] == MRC_FILTER]

    ami_measurements = createMeasurements(ami_rows, reach_col_name, sigma)
    mrc_measurements = createMeasurements(mrc_rows, reach_col_name, sigma)

    return (ami_measurements, mrc_measurements)


def readExcel(excel_file_path, unnoised_edps):
    measurements = {}
    dfs = pd.read_excel(excel_file_path, sheet_name=None)
    for edp in EDP_MAP:
        sigma = 0 if edp in unnoised_edps else SIGMA

        cumilative_sheet_name = EDP_MAP[edp]["sheet"]
        (cumilative_ami_measurements, cumilative_mrc_measurements) = getMeasurements(
            dfs[cumilative_sheet_name], CUML_REACH_COL_NAME, sigma
        )

        (total_ami_measurements, total_mrc_measurements) = getMeasurements(
            dfs[edp], TOTAL_REACH_COL_NAME, sigma
        )

        # There has to be 1 row for AMI and MRC metrics in the total reach sheet.
        assert len(total_mrc_measurements) == 1 and len(total_ami_measurements) == 1

        measurements[edp] = {
            AMI_FILTER: cumilative_ami_measurements + total_ami_measurements,
            MRC_FILTER: cumilative_mrc_measurements + total_mrc_measurements,
        }

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
        df.at[index, CUML_REACH_COL_NAME] = math.ceil(func(period).value)


def correctCumSheet(df, ami_func, mrc_func):
    ami_rows = df[df[FILTER_COL_NAME] == AMI_FILTER]
    mrc_rows = df[df[FILTER_COL_NAME] == MRC_FILTER]
    correctSheetMetric(df, ami_rows, ami_func)
    correctSheetMetric(df, mrc_rows, mrc_func)
    return df


def correctTotSheet(df, ami_val, mrc_val):
    ami_rows = df[df[FILTER_COL_NAME] == AMI_FILTER]
    mrc_rows = df[df[FILTER_COL_NAME] == MRC_FILTER]

    # There has to be 1 row for AMI and MRC metrics in the total reach sheet.
    assert ami_rows.shape[0] == 1 and mrc_rows.shape[0] == 1
    df.at[ami_rows.index[0], TOTAL_REACH_COL_NAME] = math.ceil(ami_val)
    df.at[mrc_rows.index[0], TOTAL_REACH_COL_NAME] = math.ceil(mrc_val)
    return df


def buildCorrectedExcel(correctedReport, excel):
    ami_metric_report = correctedReport.get_metric_report(ami)
    mrc_metric_report = correctedReport.get_metric_report(mrc)
    for edp in EDP_MAP:
        edp_index = EDP_MAP[edp]["ind"]
        amiFunc = (
            ami_metric_report.get_total_reach_measurement
            if (edp == TOTAL_CAMPAIGN)
            else partial(ami_metric_report.get_edp_measurement, edp_index)
        )
        mrcFunc = (
            mrc_metric_report.get_total_reach_measurement
            if (edp == TOTAL_CAMPAIGN)
            else partial(mrc_metric_report.get_edp_measurement, edp_index)
        )

        cumilative_sheet_name = EDP_MAP[edp]["sheet"]
        excel[cumilative_sheet_name] = correctCumSheet(
            excel[cumilative_sheet_name], amiFunc, mrcFunc
        )

        # The last value of the corrected measurement series is the total reach.
        totAmiVal = (
            ami_metric_report.get_total_reach_measurement(-1).value
            if (edp == TOTAL_CAMPAIGN)
            else ami_metric_report.get_edp_measurement(edp_index, -1).value
        )
        totMrcVal = (
            mrc_metric_report.get_total_reach_measurement(-1).value
            if (edp == TOTAL_CAMPAIGN)
            else mrc_metric_report.get_edp_measurement(edp_index, -1).value
        )
        total_sheet_name = edp
        excel[total_sheet_name] = correctTotSheet(
            excel[total_sheet_name], totAmiVal, totMrcVal
        )
    return excel


def writeCorrectedExcel(path, corrected_excel):
    with pd.ExcelWriter(path) as writer:
        # Write each dataframe to a different sheet
        for sheet_name in corrected_excel:
            corrected_excel[sheet_name].to_excel(
                writer, sheet_name=sheet_name, index=False
            )


def correctExcelFile(path_to_report, unnoised_edps):
    (measurements, excel) = readExcel(path_to_report, unnoised_edps)
    correctedReport = getCorrectedReport(measurements)
    return buildCorrectedExcel(correctedReport, excel)


def main():
    parser = argparse.ArgumentParser(description="Read an Excel file.")
    parser.add_argument(
        "--path_to_report", required=True, help="Path to the Excel file"
    )
    parser.add_argument(
        "--unnoised_edps",
        nargs="+",
        type=str,
        help="List of EDPs that didnt add noise to their measurements",
    )
    args = parser.parse_args()

    # Check if any of the unnoised EDPs are not recognized
    for unnoised_edp in args.unnoised_edps:
        if unnoised_edp not in edp_names:
            raise ValueError(f"Unnoised Edp '{unnoised_edp}' is not a known Edp")

    corrected_excel = correctExcelFile(args.path_to_report, args.unnoised_edps)

    writeCorrectedExcel(
        f"{args.path_to_report.split('/')[-1].split('.')[0]}_corrected.xlsx",
        corrected_excel,
    )


if __name__ == "__main__":
    main()
