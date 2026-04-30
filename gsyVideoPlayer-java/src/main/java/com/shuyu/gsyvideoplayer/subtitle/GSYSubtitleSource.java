package com.shuyu.gsyvideoplayer.subtitle;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GSYSubtitleSource {

    private final String url;
    private final String id;
    private final String mimeType;
    private final String language;
    private final String label;
    private final String charsetName;
    private final long offsetMs;
    private final boolean isDefault;
    private final Map<String, String> headers;

    private GSYSubtitleSource(Builder builder) {
        this.url = builder.url;
        this.id = builder.id;
        this.mimeType = GSYSubtitleMime.infer(builder.url, builder.mimeType);
        this.language = builder.language;
        this.label = builder.label;
        this.charsetName = builder.charsetName;
        this.offsetMs = builder.offsetMs;
        this.isDefault = builder.isDefault;
        this.headers = Collections.unmodifiableMap(new HashMap<>(builder.headers));
    }

    public String getUrl() {
        return url;
    }

    public String getId() {
        if (id != null && id.length() > 0) {
            return id;
        }
        if (language != null && language.length() > 0) {
            return language;
        }
        if (label != null && label.length() > 0) {
            return label;
        }
        return url;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getLanguage() {
        return language;
    }

    public String getLabel() {
        return label;
    }

    public String getCharsetName() {
        return charsetName;
    }

    public long getOffsetMs() {
        return offsetMs;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public static class Builder {
        private final String url;
        private String id;
        private String mimeType;
        private String language;
        private String label;
        private String charsetName = "UTF-8";
        private long offsetMs;
        private boolean isDefault;
        private Map<String, String> headers = new HashMap<>();

        public Builder(String url) {
            this.url = url;
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder setLanguage(String language) {
            this.language = language;
            return this;
        }

        public Builder setLabel(String label) {
            this.label = label;
            return this;
        }

        public Builder setCharsetName(String charsetName) {
            this.charsetName = charsetName;
            return this;
        }

        public Builder setOffsetMs(long offsetMs) {
            this.offsetMs = offsetMs;
            return this;
        }

        public Builder setDefault(boolean aDefault) {
            isDefault = aDefault;
            return this;
        }

        public Builder setHeaders(Map<String, String> headers) {
            if (headers != null) {
                this.headers = new HashMap<>(headers);
            }
            return this;
        }

        public GSYSubtitleSource build() {
            return new GSYSubtitleSource(this);
        }
    }
}
