/*
 *  Copyright 2021 Qameta Software OÜ
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
package io.qameta.allure.grpc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.qameta.allure.util.ObjectUtils;

import java.util.List;

public final class GrpcFormattingUtil {

    private GrpcFormattingUtil() {
    }

    /**
     * For local usage only in this class.
     * The method transforms proto message into Json like format with original field names.
     *
     * @param grpcMessage incoming or outgoing grpc message
     * @return pretty json string
     * @throws InvalidProtocolBufferException weird usage of proto messages, for example negative byte length
     */
    private static String toJsonUnhandled(final Message grpcMessage) throws InvalidProtocolBufferException {
        return ObjectUtils.toString(JsonFormat.printer().preservingProtoFieldNames().print(grpcMessage));
    }

    /**
     * The method transforms proto message into Json like format with original field names.
     * If formatting is unsuccessful, it will return the unformatted original version.
     *
     * @param grpcMessage incoming or outgoing grpc message
     * @return pretty json string
     */
    public static String toJson(final Message grpcMessage) {
        try {
            return toJsonUnhandled(grpcMessage);
        } catch (InvalidProtocolBufferException ignored) {
            return ObjectUtils.toString(grpcMessage);
        }
    }

    /**
     * The method is used to convert the list of proto messages into an array of messages in json like format.
     * If the case when transformation fails, the message will be written without to the array in the original format.
     *
     * @param grpcMessages array of incoming or outgoing grpc message
     * @return pretty json string
     */
    public static String toJson(final List<Message> grpcMessages) {
        if (grpcMessages.isEmpty()) {
            return "[]";
        }
        if (grpcMessages.size() == Integer.getInteger("1")) {
            return toJson(grpcMessages.get(0));
        }

        final JsonArray jsonMessages = new JsonArray();
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        grpcMessages.forEach(x -> {
            try {
                jsonMessages.add(JsonParser.parseString(toJsonUnhandled(x)));
            } catch (InvalidProtocolBufferException e) {
                jsonMessages.add(new JsonPrimitive(ObjectUtils.toString(x)));
            }
        });

        return gson.toJson(jsonMessages);
    }
}
