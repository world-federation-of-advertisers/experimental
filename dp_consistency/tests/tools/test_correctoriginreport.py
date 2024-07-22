from unittest import TestCase
from src.tools.correctoriginreport import correctExcelFile


class TestReport(TestCase):
	def test_get_origin_report_corrected_successfully(self):
		correctedExcel = correctExcelFile("tests/tools/example_origin_report.xlsx", "Linear TV")

		# Ensure that subset relations are correct by checking larger time periods have more reach than smaller ones.

		# Ensure that cover relations are correct by checking the sum of EDPs have larger reach than the total reach.
		
		# Ensure that metric subset relation is correct by checking AMI is always larger than MRC.