

from pyomo.environ import *

# This function estimates enter and exit times of parts flowing into an aggregation. The output of the function is
# a log with the estimation of enter and exit times of the parts for all the stations/buffers in the aggregation
def estimateFlowTimes(partID,enterTime,exitTime,sequence,resources):
    np = len(partID)
    nm = len(sequence)
    log = ""

    # Initial the pyomo model
    m = ConcreteModel()

    # add variables
    m.parts = range(np)
    m.machs = range(nm)
    m.trans = range(nm+1)

    m.itime = Var(m.parts, domain=NonNegativeReals)
    m.otime = Var(m.parts, domain=NonNegativeReals)
    m.ttime = Var(m.parts, m.trans, domain=NonNegativeReals)
    m.Ctot = Var(domain=NonNegativeReals)

    # Set the objective, i.e., the minimization of total completion time
    m.objective = Objective(expr=m.Ctot, sense=minimize )

    # Add constraints
    # enter times
    m.itConstr = ConstraintList()
    for i in m.parts:
        m.itConstr.add(m.ttime[i,m.machs[0]]== enterTime[i])
        for j in m.machs:
                m.itConstr.add(m.ttime[i, j] >= enterTime[i])
    #exit times
    m.otConstr = ConstraintList()
    for i in m.parts:
        m.otConstr.add(m.ttime[i,m.trans[-1]] == exitTime[i])
        for j in m.machs:
            m.otConstr.add(m.ttime[i, j] <= exitTime[i])

    #processing times
    m.procConstr = ConstraintList()
    for i in m.parts:
        for j in m.machs:
            #print(i,j)
            m.procConstr.add(m.ttime[i,j+1]-m.ttime[i,j] >= resources[sequence[j]]['time'])

    #capacity
    m.capConstr = ConstraintList()
    for j in m.machs:#(3,4):
        for i in range(np-resources[sequence[j]]['cap']):
            m.capConstr.add(m.ttime[i+resources[sequence[j]]['cap'],j] >= m.ttime[i,j+1])

    #total completion time
    m.objConstr = ConstraintList()
    m.objConstr.add(sum(m.ttime[i,j] for i in m.parts for j in m.machs)==m.Ctot)

    solver = SolverFactory('glpk')
    results = solver.solve(m)

    if (results.solver.status == SolverStatus.ok) and (
            results.solver.termination_condition == TerminationCondition.optimal):
        for i in m.parts:
            for j in range(1,nm):
                log += (partID[i] + "\t" + str(float("{:.2f}".format(m.ttime[i,j].value))) + "\t" + "place" + "\t" + "exit" + "\t" + sequence[j-1] + "\n")
                log += (partID[i] + "\t" + str(float("{:.2f}".format(m.ttime[i,j].value))) + "\t" + "place" + "\t" + "enter" + "\t" + sequence[j] + "\n")

    elif (results.solver.termination_condition == TerminationCondition.infeasible):
        log += "INFEASIBLE"
    else:
        # Something else is wrong
        log += "Status: ”"
        log += results.solver.status

    return log


# Input data are structured according to the following:
# partID: list of the names of the parts entering and exiting the aggregation
# enterTime: list of the instants when a part is entering the aggregation
# exitTime: list of the instants when a part is exiting the aggregation
# sequence: ordered list of the stations/buffers in the aggregation
# resources: characterization of stations/buffers in the aggregation in terms of capacity 'cap' and 'processing time 'time'

partID=['partA.1','partA.2','partA.3','partA.4','partA.5',
        'partA.6','partA.7','partA.8','partA.9','partA.10']
enterTime=[0.00, 9.45, 14.45, 19.45, 24.45,
           29.45, 34.45, 39.45, 44.45, 49.45]
exitTime=[7.8, 17.25, 22.25, 27.25, 32.25,
          63.27, 73.27, 83.27, 93.27, 103.27]
sequence=['M1','B1']
resources={'M1': {'cap': 1, 'time': 5}, 'B1': {'cap': 5, 'time': 2}}

log = estimateFlowTimes(partID, enterTime, exitTime, sequence, resources)
print(log)