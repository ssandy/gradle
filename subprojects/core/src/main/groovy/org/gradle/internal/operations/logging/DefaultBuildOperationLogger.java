/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.internal.operations.logging;

import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.logging.ConsoleRenderer;

import java.io.PrintWriter;

class DefaultBuildOperationLogger implements BuildOperationLogger {
    private final BuildOperationLogInfo configuration;
    private final Logger logger;
    private final PrintWriter logWriter;

    private boolean started;
    private int numberOfFailedOperationsSeen;

    DefaultBuildOperationLogger(BuildOperationLogInfo configuration, Logger logger, PrintWriter logWriter) {
        this.configuration = configuration;
        this.logger = logger;
        this.logWriter = logWriter;

        this.numberOfFailedOperationsSeen = 0;
        this.started = false;
    }

    @Override
    public void start() {
        assert !started;
        logInBoth(LogLevel.INFO, String.format("See %s for all output for %s.", formatOutputFileAsUrl(), configuration.getTaskName()));
        started = true;
    }

    @Override
    public synchronized void operationSuccess(String description, String output) {
        assert started;
        logInBoth(LogLevel.DEBUG, description.concat(" successful."));
        maybeShowSuccess(output);
    }

    @Override
    public synchronized void operationFailed(String description, String output) {
        assert started;
        logInBoth(LogLevel.DEBUG, description.concat(" failed."));
        maybeShowFailure(output);
    }

    @Override
    public void done() {
        assert started;
        int suppressedCount = numberOfFailedOperationsSeen - configuration.getMaximumFailedOperationsShown();
        if (suppressedCount > 0) {
            logger.log(LogLevel.ERROR, String.format("...output for %d more failed operation(s) continued in %s.", suppressedCount, formatOutputFileAsUrl()));
        }
        logInBoth(LogLevel.INFO, String.format("Finished %s, see full log %s.", configuration.getTaskName(), formatOutputFileAsUrl()));
        logWriter.close();
        started = false;
    }

    private void maybeShowSuccess(String output) {
        logger.log(LogLevel.INFO, output);
        logWriter.println(output);
    }

    private void maybeShowFailure(String output) {
        if (numberOfFailedOperationsSeen < configuration.getMaximumFailedOperationsShown()) {
            logger.log(LogLevel.ERROR, output);
        }
        logWriter.println(output);
        numberOfFailedOperationsSeen++;
    }

    private void logInBoth(LogLevel logLevel, String message) {
        logger.log(logLevel, message);
        logWriter.println(message);
    }

    private String formatOutputFileAsUrl() {
        return new ConsoleRenderer().asClickableFileUrl(configuration.getOutputFile());
    }
}
