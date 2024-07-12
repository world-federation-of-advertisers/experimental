import numpy as np

from src.noiseninja.noised_measurements import Measurement
from src.report.report import MetricReport, Report


def __ci_union(a, b):
    return a + b - a * b


def __simple_reach_from_impressions(impressions, population):
    return impressions / (impressions + population)


PERIODS = 12


def create_report(target_impressions_a, target_impressions_b):
    population = 1000
    # A = random.gauss(mu=0.4, sigma=0.05)
    # B_custom = random.gauss(mu=0.05, sigma=0.005)
    impressions_a = 0
    impressions_b = 0

    zero_inflation = .5

    reach_by_edp_ts_by_metric = [[[], []], [[], []], [[], []]]
    total_reach_by_metric = [[], [], []]

    for i in range(0, PERIODS):
        if (np.random.uniform(0, 1) > zero_inflation):
            impressions_a += np.random.exponential(target_impressions_a / (PERIODS * (1 - zero_inflation)))
        if (np.random.uniform(0, 1) > zero_inflation):
            impressions_b += np.random.exponential(target_impressions_b / PERIODS * (1 - zero_inflation))

        A = impressions_a / (impressions_a + 1)
        b_custom = impressions_b / (impressions_b + 1)
        B_mrc = b_custom * 1.05
        B_ami = B_mrc * 1.05
        AUB_custom = __ci_union(A, b_custom)
        AUB_mrc = __ci_union(A, B_mrc)
        AUB_ami = __ci_union(A, B_ami)
        A *= population
        b_custom *= population
        B_mrc *= population
        B_ami *= population
        AUB_custom *= population
        AUB_mrc *= population
        AUB_ami *= population

        total_reach_by_metric[0].append(Measurement(AUB_ami, AUB_ami / 100 + 1e-5))
        reach_by_edp_ts_by_metric[0][0].append(Measurement(A, 0))
        reach_by_edp_ts_by_metric[0][1].append(Measurement(B_ami, B_ami / 100 + 1e-5))

        total_reach_by_metric[1].append(Measurement(AUB_mrc, AUB_mrc / 100 + 1e-5))
        reach_by_edp_ts_by_metric[1][0].append(Measurement(A, 0))
        reach_by_edp_ts_by_metric[1][1].append(Measurement(B_mrc, B_mrc / 100 + 1e-5))

        total_reach_by_metric[2].append(Measurement(AUB_custom, AUB_custom / 100 + 1e-5))
        reach_by_edp_ts_by_metric[2][0].append(Measurement(A, 0))
        reach_by_edp_ts_by_metric[2][1].append(Measurement(b_custom, b_custom / 100 + 1e-5))

    return Report(list(MetricReport(total_reach_by_metric[i], reach_by_edp_ts_by_metric[i]) for i in range(0, 3)))


def main():
    noised_error = np.zeros(PERIODS * 3 * 3)
    corrected_error = np.zeros(PERIODS * 3 * 3)
    for i in range(0, 100):
        report = create_report(.1, .2)
        noised_report = report.sample_with_noise()
        corrected_report = noised_report.get_corrected_report()
        noised_error += np.square(report.to_array() - noised_report.to_array())
        corrected_error += np.square(report.to_array() - corrected_report.to_array())

        print(np.divide(corrected_error - noised_error, noised_error))

    print(noised_error)


if __name__ == "__main__":
    main()
