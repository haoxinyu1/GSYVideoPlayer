package com.shuyu.gsyvideoplayer.subtitle;

import android.content.Context;
import android.text.TextUtils;

import com.shuyu.gsyvideoplayer.utils.Debuger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GSYSubtitleController {

    private final Context context;
    private final GSYSubtitleView subtitleView;
    private GSYSubtitleLoader loader = new GSYSubtitleLoader();
    private final List<GSYSubtitleSource> sources = new ArrayList<>();

    private GSYSubtitleStyle style = GSYSubtitleStyle.defaultStyle();
    private GSYSubtitleSource selectedSource;
    private GSYSubtitleProvider provider;
    private boolean loadingExternalSubtitle;
    private boolean enabled = true;
    private boolean reloadAfterLoaderRelease;
    private long offsetMs;
    private int loadVersion;
    private String embeddedText = "";

    public GSYSubtitleController(Context context, GSYSubtitleView subtitleView) {
        this.context = context.getApplicationContext();
        this.subtitleView = subtitleView;
        if (this.subtitleView != null) {
            this.subtitleView.applyStyle(style);
        }
    }

    public void setSources(List<GSYSubtitleSource> sourceList) {
        setSources(sourceList, true);
    }

    private void setSources(List<GSYSubtitleSource> sourceList, boolean autoSelect) {
        cancelLoad();
        sources.clear();
        provider = null;
        selectedSource = null;
        loadingExternalSubtitle = false;
        embeddedText = "";
        if (sourceList != null) {
            sources.addAll(sourceList);
        }
        if (autoSelect) {
            selectInitialSource();
        } else {
            clear();
        }
    }

    public void setSource(GSYSubtitleSource source) {
        if (source == null) {
            setSources(null);
            return;
        }
        List<GSYSubtitleSource> list = new ArrayList<>();
        list.add(source);
        setSources(list);
    }

    public List<GSYSubtitleSource> getSources() {
        return Collections.unmodifiableList(sources);
    }

    public boolean selectSubtitle(String id) {
        if (TextUtils.isEmpty(id)) {
            cancelLoad();
            selectedSource = null;
            provider = null;
            loadingExternalSubtitle = false;
            clear();
            return false;
        }
        for (GSYSubtitleSource source : sources) {
            if (id.equals(source.getId()) || id.equals(source.getUrl())
                || id.equals(source.getLanguage()) || id.equals(source.getLabel())) {
                loadSource(source);
                return true;
            }
        }
        return false;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            clear();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean hasExternalSubtitle() {
        return selectedSource != null && (loadingExternalSubtitle || provider != null);
    }

    public void setOffsetMs(long offsetMs) {
        this.offsetMs = offsetMs;
    }

    public long getOffsetMs() {
        return offsetMs;
    }

    public void setStyle(GSYSubtitleStyle style) {
        if (style == null) {
            return;
        }
        this.style = style;
        if (subtitleView != null) {
            subtitleView.applyStyle(style);
        }
    }

    public GSYSubtitleStyle getStyle() {
        return style;
    }

    public void update(long positionMs) {
        if (!enabled || subtitleView == null) {
            return;
        }
        if (provider != null && selectedSource != null) {
            long sourcePosition = positionMs + offsetMs + selectedSource.getOffsetMs();
            subtitleView.showText(provider.findText(sourcePosition));
        } else if (!hasExternalSubtitle() && !TextUtils.isEmpty(embeddedText)) {
            subtitleView.showText(embeddedText);
        }
    }

    public void setEmbeddedText(String text) {
        embeddedText = text == null ? "" : text;
        if (!hasExternalSubtitle() && enabled && subtitleView != null) {
            subtitleView.showText(embeddedText);
        }
    }

    public void clearEmbeddedText() {
        embeddedText = "";
        if (!hasExternalSubtitle()) {
            clear();
        }
    }

    public void clear() {
        if (subtitleView != null) {
            subtitleView.showText("");
        }
    }

    private void showEmbeddedTextOrClear() {
        if (!TextUtils.isEmpty(embeddedText) && enabled && subtitleView != null) {
            subtitleView.showText(embeddedText);
        } else {
            clear();
        }
    }

    public void cancelLoad() {
        loadVersion++;
        loadingExternalSubtitle = false;
        reloadAfterLoaderRelease = false;
        if (loader != null) {
            loader.cancelCurrent();
        }
    }

    public void release() {
        cancelLoad();
        clear();
        releaseLoader();
    }

    public void releaseLoader() {
        if (loader != null) {
            reloadAfterLoaderRelease = loadingExternalSubtitle && selectedSource != null && provider == null;
            loadVersion++;
            loadingExternalSubtitle = false;
            loader.release();
            loader = null;
        }
    }

    public void resumeReleasedLoader() {
        if (reloadAfterLoaderRelease && selectedSource != null && provider == null) {
            GSYSubtitleSource source = selectedSource;
            reloadAfterLoaderRelease = false;
            loadSource(source);
        }
    }

    public GSYSubtitleSnapshot snapshot() {
        return new GSYSubtitleSnapshot(new ArrayList<>(sources), selectedSource == null ? null : selectedSource.getId(),
            enabled, offsetMs, style, embeddedText);
    }

    public void restore(GSYSubtitleSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        setStyle(snapshot.getStyle());
        setEnabled(snapshot.isEnabled());
        setOffsetMs(snapshot.getOffsetMs());
        setSources(snapshot.getSources(), false);
        embeddedText = snapshot.getEmbeddedText();
        if (!TextUtils.isEmpty(snapshot.getSelectedId())) {
            selectSubtitle(snapshot.getSelectedId());
        } else if (!sources.isEmpty()) {
            selectInitialSource();
        } else if (!TextUtils.isEmpty(embeddedText) && enabled && subtitleView != null) {
            subtitleView.showText(embeddedText);
        }
    }

    private void selectInitialSource() {
        if (sources.isEmpty()) {
            clear();
            return;
        }
        GSYSubtitleSource first = sources.get(0);
        for (GSYSubtitleSource source : sources) {
            if (source.isDefault()) {
                first = source;
                break;
            }
        }
        loadSource(first);
    }

    private void loadSource(final GSYSubtitleSource source) {
        final int version = ++loadVersion;
        selectedSource = source;
        provider = null;
        loadingExternalSubtitle = true;
        reloadAfterLoaderRelease = false;
        clear();
        ensureLoader().load(context, source, new GSYSubtitleLoader.Callback() {
            @Override
            public void onLoaded(GSYSubtitleSource loadedSource, GSYSubtitleProvider loadedProvider) {
                if (version != loadVersion || selectedSource != source) {
                    return;
                }
                loadingExternalSubtitle = false;
                if (loadedProvider == null || loadedProvider.size() == 0) {
                    Debuger.printfWarning("GSY subtitle load returned empty cues: " + loadedSource.getUrl());
                    provider = null;
                    showEmbeddedTextOrClear();
                    return;
                }
                provider = loadedProvider;
            }

            @Override
            public void onFailed(GSYSubtitleSource failedSource, Exception error) {
                if (version != loadVersion || selectedSource != source) {
                    return;
                }
                loadingExternalSubtitle = false;
                provider = null;
                showEmbeddedTextOrClear();
            }
        });
    }

    private GSYSubtitleLoader ensureLoader() {
        if (loader == null) {
            loader = new GSYSubtitleLoader();
        }
        return loader;
    }

    public static class GSYSubtitleSnapshot {
        private final List<GSYSubtitleSource> sources;
        private final String selectedId;
        private final boolean enabled;
        private final long offsetMs;
        private final GSYSubtitleStyle style;
        private final String embeddedText;

        GSYSubtitleSnapshot(List<GSYSubtitleSource> sources, String selectedId, boolean enabled,
                            long offsetMs, GSYSubtitleStyle style, String embeddedText) {
            this.sources = sources;
            this.selectedId = selectedId;
            this.enabled = enabled;
            this.offsetMs = offsetMs;
            this.style = style;
            this.embeddedText = embeddedText;
        }

        public List<GSYSubtitleSource> getSources() {
            return sources;
        }

        public String getSelectedId() {
            return selectedId;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public long getOffsetMs() {
            return offsetMs;
        }

        public GSYSubtitleStyle getStyle() {
            return style;
        }

        public String getEmbeddedText() {
            return embeddedText;
        }
    }
}
