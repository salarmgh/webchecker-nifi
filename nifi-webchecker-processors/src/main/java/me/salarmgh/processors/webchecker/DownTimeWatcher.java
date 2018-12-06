/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.salarmgh.processors.webchecker;


import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Tags({"WebChecker Watch"})
@CapabilityDescription("Send Request to Website on an interval and watch for expected response code!")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute = "", description = "")})
@WritesAttributes({@WritesAttribute(attribute = "", description = "")})
public class WebCheckerWatcher extends AbstractProcessor {
    public static final PropertyDescriptor SITE_URL = new PropertyDescriptor
            .Builder().name("siteUrl")
            .displayName("Site URL")
            .description("Site URL status check")
            .required(true)
            .defaultValue("https://www.example.com")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor RESPONSE_CODE_TO_COUNT = new PropertyDescriptor
            .Builder().name("responseCodeToCount")
            .displayName("Specify Response Code to Count")
            .description("503")
            .required(true)
            .defaultValue("503")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor PROGRAM = new PropertyDescriptor
            .Builder().name("program")
            .displayName("Program Name")
            .description("e.g: Desktop")
            .required(true)
            .defaultValue("desktop")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship SUCCESS = new Relationship.Builder()
            .name("SUCCESS")
            .description("Succes relationship")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(SITE_URL);
        descriptors.add(PROGRAM);
        descriptors.add(RESPONSE_CODE_TO_COUNT);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(SUCCESS);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
    }


    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        double value = 1;

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(context.getProperty("siteUrl").getValue())
                .build();
        Response response = null;
        String responseCode = context.getProperty("responseCodeToCount").getValue();

        try {
            response = client.newCall(request).execute();
            responseCode = Integer.toString(response.code());
        } catch (IOException e) {
            responseCode = context.getProperty("responseCodeToCount").getValue();
        }

        if (responseCode.equals(context.getProperty("responseCodeToCount").getValue())) {
            value = 0;
        } else {
            value = 1;
        }

        WebCheckerCounter p = new WebCheckerCounter(context.getProperty("program").getValue(), context.getProperty("program").getValue(), value);
        FlowFile flowFile = null;
        try {
            flowFile = p.toMetric(session);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }


        flowFile = session.write(flowFile, new OutputStreamCallback() {
            @Override
            public void process(OutputStream out) throws IOException {}
        });
        session.transfer(flowFile, SUCCESS);

        response.close();
    }
}
