/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */









package org.gololang.gradle.test.integration.framework

import org.gradle.BuildResult
import org.gradle.GradleLauncher
import org.gradle.StartParameter
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState
import org.gradle.initialization.DefaultGradleLauncher
import org.gradle.logging.internal.StreamBackedStandardOutputListener
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.util.mop.Use

/**
 * @author Marcin Erdmann
 */
@Use(UpToDateCategory)
abstract class IntegrationSpec extends Specification {
    @Rule final TemporaryFolder dir = new TemporaryFolder()

	private final StringBuilder errorOutputBuilder = new StringBuilder()
    protected List<ExecutedTask> executedTasks = []

    protected GradleLauncher launcher(String... args) {
        StartParameter startParameter = GradleLauncher.createStartParameter(args)
        startParameter.setProjectDir(dir.root)
        DefaultGradleLauncher launcher = GradleLauncher.newInstance(startParameter)
        launcher.gradle.scriptClassLoader.addParent(getClass().classLoader)
		launcher.addStandardErrorListener(new StreamBackedStandardOutputListener(errorOutputBuilder))
        executedTasks.clear()
        launcher.addListener(new TaskExecutionListener() {
            void beforeExecute(Task task) {
                executedTasks << new ExecutedTask(task: task)
            }

            void afterExecute(Task task, TaskState taskState) {
                executedTasks.last().state = taskState
            }
        })
        launcher
    }

	protected String getStandardErrorOutput() {
		errorOutputBuilder.toString()
	}

    protected File getBuildFile() {
        file('build.gradle')
    }

    protected File directory(String path) {
        new File(dir.root, path).with {
            mkdirs()
            it
        }
    }

    protected File file(String path) {
        def splitted = path.split('/')
        def directory = splitted.size() > 1 ? directory(splitted[0..-2].join('/')) : dir.root
        def file = new File(directory, splitted[-1])
        file.createNewFile()
        file
    }

    protected boolean fileExists(String path) {
        new File(dir.root, path).exists()
    }

    protected ExecutedTask task(String name) {
        executedTasks.find { it.task.name == name }
    }

    protected Collection<ExecutedTask> tasks(String... names) {
        def tasks = executedTasks.findAll { it.task.name in names }
        assert tasks.size() == names.size()
        tasks
    }

    protected BuildResult runTasksSuccessfully(String... tasks) {
        BuildResult result = runTasks(tasks)
        if (result.failure) {
            throw result.failure
        }
        result
    }

	protected BuildResult runTasksWithFailure(String... tasks) {
		BuildResult result = runTasks(tasks)
		assert result.failure
		result
	}

	protected BuildResult runTasks(String... tasks) {
		launcher(tasks).run()
	}
}
