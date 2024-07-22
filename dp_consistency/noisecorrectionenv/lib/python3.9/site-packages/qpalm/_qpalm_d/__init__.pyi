"""C and C++ implementation of QPALM"""
from __future__ import annotations
import _qpalm_d
import typing

__all__ = [
    "Data",
    "Info",
    "Settings",
    "Solution",
    "Solver",
    "build_time",
    "debug"
]


class Data():
    def __init__(self, n: int, m: int) -> None: ...
    def _get_c_data_ptr(self) -> QPALMData: 
        """
        Return a pointer to the C data struct (of type ::QPALMData).
        """
    @property
    def A(self) -> scipy.sparse.csc_matrix[numpy.float64]:
        """
        :type: scipy.sparse.csc_matrix[numpy.float64]
        """
    @A.setter
    def A(self, arg1: scipy.sparse.csc_matrix[numpy.float64]) -> None:
        pass
    @property
    def Q(self) -> scipy.sparse.csc_matrix[numpy.float64]:
        """
        :type: scipy.sparse.csc_matrix[numpy.float64]
        """
    @Q.setter
    def Q(self, arg1: scipy.sparse.csc_matrix[numpy.float64]) -> None:
        pass
    @property
    def bmax(self) -> numpy.ndarray:
        """
        :type: numpy.ndarray
        """
    @bmax.setter
    def bmax(self, arg1: numpy.ndarray) -> None:
        pass
    @property
    def bmin(self) -> numpy.ndarray:
        """
        :type: numpy.ndarray
        """
    @bmin.setter
    def bmin(self, arg1: numpy.ndarray) -> None:
        pass
    @property
    def c(self) -> float:
        """
        :type: float
        """
    @c.setter
    def c(self, arg0: float) -> None:
        pass
    @property
    def q(self) -> numpy.ndarray:
        """
        :type: numpy.ndarray
        """
    @q.setter
    def q(self, arg1: numpy.ndarray) -> None:
        pass
    pass
class Info():
    @property
    def dua2_res_norm(self) -> float:
        """
        :type: float
        """
    @dua2_res_norm.setter
    def dua2_res_norm(self, arg0: float) -> None:
        pass
    @property
    def dua_res_norm(self) -> float:
        """
        :type: float
        """
    @dua_res_norm.setter
    def dua_res_norm(self, arg0: float) -> None:
        pass
    @property
    def dual_objective(self) -> float:
        """
        :type: float
        """
    @dual_objective.setter
    def dual_objective(self, arg0: float) -> None:
        pass
    @property
    def iter(self) -> int:
        """
        :type: int
        """
    @iter.setter
    def iter(self, arg0: int) -> None:
        pass
    @property
    def iter_out(self) -> int:
        """
        :type: int
        """
    @iter_out.setter
    def iter_out(self, arg0: int) -> None:
        pass
    @property
    def objective(self) -> float:
        """
        :type: float
        """
    @objective.setter
    def objective(self, arg0: float) -> None:
        pass
    @property
    def pri_res_norm(self) -> float:
        """
        :type: float
        """
    @pri_res_norm.setter
    def pri_res_norm(self, arg0: float) -> None:
        pass
    @property
    def run_time(self) -> float:
        """
        :type: float
        """
    @run_time.setter
    def run_time(self, arg0: float) -> None:
        pass
    @property
    def setup_time(self) -> float:
        """
        :type: float
        """
    @setup_time.setter
    def setup_time(self, arg0: float) -> None:
        pass
    @property
    def solve_time(self) -> float:
        """
        :type: float
        """
    @solve_time.setter
    def solve_time(self, arg0: float) -> None:
        pass
    @property
    def status(self) -> str:
        """
        :type: str
        """
    @status.setter
    def status(self, arg1: str) -> None:
        pass
    @property
    def status_val(self) -> int:
        """
        :type: int
        """
    @status_val.setter
    def status_val(self, arg0: int) -> None:
        pass
    DUAL_INFEASIBLE = -4
    DUAL_TERMINATED = 2
    ERROR = 0
    MAX_ITER_REACHED = -2
    PRIMAL_INFEASIBLE = -3
    SOLVED = 1
    TIME_LIMIT_REACHED = -5
    UNSOLVED = -10
    USER_CANCELLATION = -6
    pass
class Settings():
    def __init__(self) -> None: ...
    @property
    def delta(self) -> float:
        """
        :type: float
        """
    @delta.setter
    def delta(self, arg0: float) -> None:
        pass
    @property
    def dual_objective_limit(self) -> float:
        """
        :type: float
        """
    @dual_objective_limit.setter
    def dual_objective_limit(self, arg0: float) -> None:
        pass
    @property
    def enable_dual_termination(self) -> int:
        """
        :type: int
        """
    @enable_dual_termination.setter
    def enable_dual_termination(self, arg0: int) -> None:
        pass
    @property
    def eps_abs(self) -> float:
        """
        :type: float
        """
    @eps_abs.setter
    def eps_abs(self, arg0: float) -> None:
        pass
    @property
    def eps_abs_in(self) -> float:
        """
        :type: float
        """
    @eps_abs_in.setter
    def eps_abs_in(self, arg0: float) -> None:
        pass
    @property
    def eps_dual_inf(self) -> float:
        """
        :type: float
        """
    @eps_dual_inf.setter
    def eps_dual_inf(self, arg0: float) -> None:
        pass
    @property
    def eps_prim_inf(self) -> float:
        """
        :type: float
        """
    @eps_prim_inf.setter
    def eps_prim_inf(self, arg0: float) -> None:
        pass
    @property
    def eps_rel(self) -> float:
        """
        :type: float
        """
    @eps_rel.setter
    def eps_rel(self, arg0: float) -> None:
        pass
    @property
    def eps_rel_in(self) -> float:
        """
        :type: float
        """
    @eps_rel_in.setter
    def eps_rel_in(self, arg0: float) -> None:
        pass
    @property
    def factorization_method(self) -> int:
        """
        :type: int
        """
    @factorization_method.setter
    def factorization_method(self, arg0: int) -> None:
        pass
    @property
    def gamma_init(self) -> float:
        """
        :type: float
        """
    @gamma_init.setter
    def gamma_init(self, arg0: float) -> None:
        pass
    @property
    def gamma_max(self) -> float:
        """
        :type: float
        """
    @gamma_max.setter
    def gamma_max(self, arg0: float) -> None:
        pass
    @property
    def gamma_upd(self) -> float:
        """
        :type: float
        """
    @gamma_upd.setter
    def gamma_upd(self, arg0: float) -> None:
        pass
    @property
    def inner_max_iter(self) -> int:
        """
        :type: int
        """
    @inner_max_iter.setter
    def inner_max_iter(self, arg0: int) -> None:
        pass
    @property
    def max_iter(self) -> int:
        """
        :type: int
        """
    @max_iter.setter
    def max_iter(self, arg0: int) -> None:
        pass
    @property
    def max_rank_update(self) -> int:
        """
        :type: int
        """
    @max_rank_update.setter
    def max_rank_update(self, arg0: int) -> None:
        pass
    @property
    def max_rank_update_fraction(self) -> float:
        """
        :type: float
        """
    @max_rank_update_fraction.setter
    def max_rank_update_fraction(self, arg0: float) -> None:
        pass
    @property
    def nonconvex(self) -> int:
        """
        :type: int
        """
    @nonconvex.setter
    def nonconvex(self, arg0: int) -> None:
        pass
    @property
    def ordering(self) -> int:
        """
        :type: int
        """
    @ordering.setter
    def ordering(self, arg0: int) -> None:
        pass
    @property
    def print_iter(self) -> int:
        """
        :type: int
        """
    @print_iter.setter
    def print_iter(self, arg0: int) -> None:
        pass
    @property
    def proximal(self) -> int:
        """
        :type: int
        """
    @proximal.setter
    def proximal(self, arg0: int) -> None:
        pass
    @property
    def reset_newton_iter(self) -> int:
        """
        :type: int
        """
    @reset_newton_iter.setter
    def reset_newton_iter(self, arg0: int) -> None:
        pass
    @property
    def rho(self) -> float:
        """
        :type: float
        """
    @rho.setter
    def rho(self, arg0: float) -> None:
        pass
    @property
    def scaling(self) -> int:
        """
        :type: int
        """
    @scaling.setter
    def scaling(self, arg0: int) -> None:
        pass
    @property
    def sigma_init(self) -> float:
        """
        :type: float
        """
    @sigma_init.setter
    def sigma_init(self, arg0: float) -> None:
        pass
    @property
    def sigma_max(self) -> float:
        """
        :type: float
        """
    @sigma_max.setter
    def sigma_max(self, arg0: float) -> None:
        pass
    @property
    def theta(self) -> float:
        """
        :type: float
        """
    @theta.setter
    def theta(self, arg0: float) -> None:
        pass
    @property
    def time_limit(self) -> float:
        """
        :type: float
        """
    @time_limit.setter
    def time_limit(self, arg0: float) -> None:
        pass
    @property
    def verbose(self) -> int:
        """
        :type: int
        """
    @verbose.setter
    def verbose(self, arg0: int) -> None:
        pass
    @property
    def warm_start(self) -> int:
        """
        :type: int
        """
    @warm_start.setter
    def warm_start(self, arg0: int) -> None:
        pass
    pass
class Solution():
    @property
    def x(self) -> numpy.ndarray:
        """
        :type: numpy.ndarray
        """
    @property
    def y(self) -> numpy.ndarray:
        """
        :type: numpy.ndarray
        """
    pass
class Solver():
    def __init__(self, data: Data, settings: Settings) -> None: ...
    def _get_c_work_ptr(self) -> capsule: 
        """
        Return a pointer to the C workspace struct (of type ::QPALMWorkspace).
        """
    def cancel(self) -> None: ...
    def solve(self, asynchronous: bool = True, suppress_interrupt: bool = False) -> None: ...
    def update_Q_A(self, Q_vals: numpy.ndarray, A_vals: numpy.ndarray) -> None: ...
    def update_bounds(self, bmin: typing.Optional[numpy.ndarray] = None, bmax: typing.Optional[numpy.ndarray] = None) -> None: ...
    def update_q(self, q: numpy.ndarray) -> None: ...
    def update_settings(self, settings: Settings) -> None: ...
    def warm_start(self, x: typing.Optional[numpy.ndarray] = None, y: typing.Optional[numpy.ndarray] = None) -> None: ...
    @property
    def dual_inf_certificate(self) -> numpy.ndarray:
        """
        :type: numpy.ndarray
        """
    @property
    def info(self) -> Info:
        """
        :type: Info
        """
    @property
    def prim_inf_certificate(self) -> numpy.ndarray:
        """
        :type: numpy.ndarray
        """
    @property
    def solution(self) -> Solution:
        """
        :type: Solution
        """
    pass
__version__ = '1.2.3'
build_time = 'Mar 28 2024 - 17:13:27'
debug = True
