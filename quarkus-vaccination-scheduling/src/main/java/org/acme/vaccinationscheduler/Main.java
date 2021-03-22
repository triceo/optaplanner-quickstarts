/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.acme.vaccinationscheduler;

import java.time.Duration;
import java.time.Instant;
import org.acme.vaccinationscheduler.bootstrap.DemoDataGenerator;
import org.acme.vaccinationscheduler.domain.VaccinationSchedule;
import org.optaplanner.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import org.optaplanner.core.api.score.stream.ConstraintStreamImplType;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.phase.PhaseConfig;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;

import static org.optaplanner.core.api.score.stream.ConstraintStreamImplType.DROOLS;

public class Main {

    private static Solver<VaccinationSchedule> getSolver(ConstraintStreamImplType constraintStreamImplType,
                                                         HardMediumSoftLongScore expectedScore) {
        SolverConfig solverConfig = SolverConfig.createFromXmlResource("org/acme/vaccinationscheduler/solverConfig.xml");
        solverConfig.getScoreDirectorFactoryConfig().setConstraintStreamImplType(constraintStreamImplType);
        TerminationConfig terminationConfig = new TerminationConfig();
        if (constraintStreamImplType == DROOLS) { // If DROOLS, run until we reach the expected score.
            terminationConfig.setBestScoreLimit(expectedScore.toString());
        } else { // If BAVET, run local search for 10 minutes after construction heuristics terminates.
            terminationConfig.setMinutesSpentLimit(10L);
        }
        PhaseConfig<?> localSeachConfig = solverConfig.getPhaseConfigList().get(1);
        localSeachConfig.setTerminationConfig(terminationConfig);
        return SolverFactory.<VaccinationSchedule>create(solverConfig)
                .buildSolver();
    }

    private static VaccinationSchedule generateData(int centerCount, int lineCount) {
        DemoDataGenerator generator = new DemoDataGenerator();
        generator.setVaccinationCenterCount(centerCount);
        generator.setTotalLineCount(lineCount);
        return generator.createVaccinationSchedule();
    }

    public static void main(String[] args) {
        //                            vvvvv Change to 20, 100 for medium data set.
        //                            vvvvv Change to 40, 1200 for production data set.
        VaccinationSchedule problem = generateData(40, 1200);

        // First run Bavet and store the score it reaches within the preconfigured interval.
        Solver<VaccinationSchedule> solver = getSolver(ConstraintStreamImplType.BAVET, null);
        Instant bavetStart = Instant.now();
        VaccinationSchedule bestSolution = solver.solve(problem);
        Duration bavetDuration = Duration.between(Instant.now(), bavetStart).abs();

        // Then run Drools in the same JVM to get comparable performance, and ask it to reach the same score as BAVET.
        Solver<VaccinationSchedule> solver2 = getSolver(DROOLS, bestSolution.getScore());
        Instant droolsStart = Instant.now();
        solver2.solve(problem);
        Duration droolsDuration = Duration.between(Instant.now(), droolsStart).abs();

        // Now tabulate the results so that they do not get lost in possibly a lengthy log.
        System.out.println("==========");
        System.out.println("Target score: " + bestSolution.getScore());
        System.out.println("  BAVET took: " + bavetDuration);
        System.out.println(" DROOLS took: " + droolsDuration);
    }

}
