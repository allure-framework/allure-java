/*
 *  Copyright 2019 Qameta Software OÜ
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
package io.qameta.allure.assertj.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringsUtils {

    /**
     * Method converts camel-case string to human-readable one.
     * It doesn't work correctly with names which contain abbreviations (DBConnection, helloASMFramework, etc.)
     *
     * @param camelCaseName standard Java style method or class name.
     * @return human-readable name interpretation.
     * @author Achitheus (Yury Yurchenko).
     */
    public static String humanReadableMethodOrClassName(String camelCaseName) {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = Pattern.compile("([A-Za-z][^A-Z]*)").matcher(camelCaseName);
        for (int i = 0; matcher.find(); i++) {
            String group = matcher.group(1);
            if (i == 0) {
                sb.append(Character.toUpperCase(group.charAt(0)));
                if (group.length() > 1) {
                    sb.append(group, 1, group.length());
                }
            } else {
                sb.append(" ").append(group.toLowerCase());
            }
        }
        return sb.toString();
    }
}
