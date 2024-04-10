package com.metricalp.android;

import android.os.Build;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public final class Metricalp {

    private static Metricalp INSTANCE;

    public final String API_ENDPOINT = "https://event.metricalp.com";
    private HashMap<String, String> attributes = null;

    public OkHttpClient client = new OkHttpClient();

    private Metricalp() {
    }

    public static Metricalp getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Metricalp();
        }

        return INSTANCE;
    }

    public static boolean init(HashMap<String, String> attributes, String initialScreen) {
        Metricalp instance = Metricalp.getInstance();
        if (instance.getAttributes() == null) {
            attributes.put("metr_collected_via", "android");
            attributes.put("metr_os_detail", "Android " + Build.VERSION.SDK_INT + "_" + Build.VERSION.RELEASE);
            attributes.put("metr_app_detail", attributes.getOrDefault("app", "(not-set)"));
            instance.setAttributes(attributes);
        }
        if (initialScreen == null) {
            return true;
        }

        return Metricalp.screenViewEvent(initialScreen, null, null);
    }

    public HashMap<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(HashMap<String, String> attributes) {
        this.attributes = attributes;
    }


    public static void resetAttributes(HashMap<String, String> attributes) {
        Metricalp instance = Metricalp.getInstance();
        instance.setAttributes(attributes);
    }

    public static void updateAttributes(HashMap<String, String> attributes) {
        Metricalp instance = Metricalp.getInstance();
        HashMap<String, String> currentAttributes = instance.getAttributes();
        currentAttributes.putAll(attributes);
        instance.setAttributes(currentAttributes);
    }

    public static HashMap<String, String> getAllAttributes() {
        Metricalp instance = Metricalp.getInstance();
        return instance.getAttributes();
    }

    public static boolean sendEvent(String type, HashMap<String, String> eventAttributes, HashMap<String, String> overrideAttributes) {
        Metricalp instance = Metricalp.getInstance();
        HashMap<String, String> attributes = instance.getAttributes();
        HashMap<String, String> body = new HashMap<>(attributes);

        body.putAll(attributes);

        if (overrideAttributes != null) {
            body.putAll(overrideAttributes);
        }

        if (!body.containsKey("tid")) {
            throw new RuntimeException("Metricalp: tid is missing in attributes");
        }

        if (body.containsKey("metr_bypass_ip") && !body.containsKey("metr_unique_identifier")) {
            throw new RuntimeException("Metricalp: when metr_bypass_ip is true, metr_unique_identifier must be set.");
        }


        if (eventAttributes != null) {
            body.putAll(eventAttributes);
            body.put("path", eventAttributes.getOrDefault("path", "(not-set)"));
        }

        body.put("type", type);


        if (!body.containsKey("metr_user_language")) {
            body.put("metr_user_language", "unknown-unknown");
        }

        if (!body.containsKey("metr_unique_identifier")) {
            body.put("metr_unique_identifier", "");
        }


        String apiUrl = attributes.getOrDefault("endpoint", instance.API_ENDPOINT);
        Gson gson = new Gson();
        Type typeObject = new TypeToken<HashMap>() {
        }.getType();
        String gsonData = gson.toJson(body, typeObject);

        instance.asynchronousPostRequest(apiUrl, gsonData);

        return true;
    }

    public static boolean screenViewEvent(String path, HashMap<String, String> eventAttributes, HashMap<String, String> overrideAttributes) {
        HashMap<String, String> attrs = new HashMap<>();
        attrs.put("path", path);
        if (eventAttributes != null) {
            attrs.putAll(eventAttributes);
        }
        return Metricalp.sendEvent(
                "screen_view",
                attrs,
                overrideAttributes
        );
    }

    public static boolean sessionExitEvent(String path, HashMap<String, String> eventAttributes, HashMap<String, String> overrideAttributes) {
        HashMap<String, String> attrs = new HashMap<>();
        attrs.put("path", path);
        if (eventAttributes != null) {
            attrs.putAll(eventAttributes);
        }
        return Metricalp.sendEvent(
                "session_exit",
                attrs,
                overrideAttributes
        );
    }

    public static boolean customEvent(String type, HashMap<String, String> eventAttributes, HashMap<String, String> overrideAttributes) {
        return Metricalp.sendEvent(
                type,
                eventAttributes,
                overrideAttributes
        );
    }

    public void asynchronousPostRequest(String url, String requestBody) {
        RequestBody body = RequestBody.create(requestBody, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        this.client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) {}
        });
    }
}