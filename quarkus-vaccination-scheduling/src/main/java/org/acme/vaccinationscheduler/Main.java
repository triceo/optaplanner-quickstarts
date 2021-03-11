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

import java.util.Collections;
import org.acme.vaccinationscheduler.bootstrap.DemoDataGenerator;
import org.acme.vaccinationscheduler.domain.Person;
import org.acme.vaccinationscheduler.domain.VaccinationSchedule;
import org.optaplanner.core.api.score.stream.ConstraintStreamImplType;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;

public class Main {

    private static Solver<VaccinationSchedule> getSolver(ConstraintStreamImplType constraintStreamImplType) {
        SolverConfig solverConfig = SolverConfig.createFromXmlResource("solverConfig.xml");
        solverConfig.setSolutionClass(VaccinationSchedule.class);
        solverConfig.setEntityClassList(Collections.singletonList(Person.class));
        solverConfig.getScoreDirectorFactoryConfig().setConstraintStreamImplType(constraintStreamImplType);
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
        //                            Production data set will take several minutes to run out of 8G heap with DROOLS.
        VaccinationSchedule problem = generateData(3, 5);
        //                                                                   vvvvvvvvvvvvvv Change to BAVET.
        Solver<VaccinationSchedule> solver = getSolver(ConstraintStreamImplType.DROOLS);
        // Just to show that something is running.
        solver.addEventListener(event -> System.out.println("New best solution:" + event.getNewBestScore()));
        // This runs the solver and it will run forever.
        solver.solve(problem);
    }

}
