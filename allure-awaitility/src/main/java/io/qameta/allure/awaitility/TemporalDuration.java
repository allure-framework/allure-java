/*
 *  Copyright 2022 Qameta Software OÜ
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.qameta.allure.awaitility;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.UnsupportedTemporalTypeException;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;

/**
 * Class helper for Duration printing purposes.
 */
public class TemporalDuration implements TemporalAccessor {
    private static final Temporal BASE = LocalDateTime.of(0, 1, 1, 0, 0, 0, 0);

    private static final DateTimeFormatter DTF = new DateTimeFormatterBuilder().optionalStart()
            .appendValue(YEAR)
            .appendLiteral(" years ").optionalEnd()
            .optionalStart().appendLiteral(' ').appendValue(MONTH_OF_YEAR).appendLiteral(" months ").optionalEnd()
            .optionalStart().appendLiteral(' ').appendValue(DAY_OF_MONTH).appendLiteral(" days ").optionalEnd()
            .optionalStart().appendLiteral(' ').appendValue(HOUR_OF_DAY).appendLiteral(" hours ").optionalEnd()
            .optionalStart().appendLiteral(' ').appendValue(MINUTE_OF_HOUR).appendLiteral(" minutes ").optionalEnd()
            .optionalStart().appendLiteral(' ').appendValue(SECOND_OF_MINUTE).appendLiteral(" seconds").optionalEnd()
            .optionalStart().appendLiteral(' ').appendValue(MILLI_OF_SECOND).appendLiteral(" milliseconds")
            .optionalEnd()
            .toFormatter();

    private final Duration duration;
    private final Temporal temporal;

    TemporalDuration(final Duration duration) {
        this.duration = duration;
        this.temporal = duration.addTo(BASE);
    }

    @Override
    public boolean isSupported(final TemporalField field) {
        if (!temporal.isSupported(field)) {
            return false;
        }
        return temporal.getLong(field) - BASE.getLong(field) != 0L;
    }

    @Override
    public long getLong(final TemporalField temporalField) {
        if (!isSupported(temporalField)) {
            throw new UnsupportedTemporalTypeException(temporalField.toString());
        }
        return temporal.getLong(temporalField) - BASE.getLong(temporalField);
    }

    @Override
    public String toString() {
        if (duration.compareTo(Duration.ofMillis(1)) < 0) {
            return duration.toNanos() + " nanoseconds";
        } else {
            return DTF.format(this).trim();
        }
    }

}
