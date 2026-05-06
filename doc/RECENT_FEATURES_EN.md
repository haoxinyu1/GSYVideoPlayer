# Recent Playback Features

This document summarizes recent demo and playback changes so maintainers can quickly find the entry points, APIs, and regression scope.

## Entry Points

| Feature | Demo entry | Main classes | Notes |
| --- | --- | --- | --- |
| WebVTT seek preview | `Open VIDEO` | `PreViewGSYVideoPlayer` | Uses a WebVTT thumbnail track, supporting standalone images and sprite crop coordinates. |
| Unified external subtitles | `Custom EXO subtitles`, `Common subtitles non-EXO` | `GSYSubtitleController`, `GSYSubtitleView` | SRT/WebVTT are parsed and rendered by the UI layer and can work across IJK, System, and Media3. |
| Keep last frame on completion | `Keep last frame` | `KeepLastFrameVideo` | Demo-level flag to keep the current render frame after natural playback completion. |
| Screenshot semantics | Filter and MediaCodec demos | `StandardGSYVideoPlayer`, `GSYRenderView` | Keeps video-only screenshots and adds composed player screenshots including UI. |
| GLSurfaceView effects and lifecycle | `Filter` | `DetailFilterActivity`, `GSYVideoGLView*Render` | Cleans up GL renderer lifecycle and adds filter, texture, multi-window, mask, and blur scenes. |
| Multi-URL quality switching | `Seamless switch` | `SmartPickVideo` | Keeps the two-manager approach and improves position sync, timeout, fallback, and temporary manager release. |
| Exo adaptive quality | `EXO adaptive quality` | `ExoAdaptiveTrackActivity`, `GSYExo2MediaPlayer` | Uses one HLS master playlist or DASH MPD and lets Media3 TrackSelector switch video tracks in one media timeline. |

## WebVTT Seek Preview

Seek preview no longer extracts many frames from the original video on the client. The app can use a generated WebVTT thumbnail track. Each cue can point to a separate image or a sprite region:

```text
WEBVTT

00:00:00.000 --> 00:00:05.000
thumbs.jpg#xywh=0,0,160,90
```

Main API:

```java
player.setOpenPreView(true);
player.setPreviewVttUrl("https://example.com/thumbs.vtt");
```

Library classes include `GSYVideoPreviewVttParser`, `GSYVideoPreviewProvider`, and `GSYVideoPreviewFrame`.

## Unified External Subtitles

External subtitles are now a UI overlay capability instead of a Media3-only media source merge. `GSYSubtitleController` loads, parses, and refreshes SRT/WebVTT subtitles by playback position. Failures clear the subtitle view but do not interrupt video playback.

Basic usage:

```java
GSYSubtitleSource source = new GSYSubtitleSource.Builder("https://example.com/subtitle.srt")
    .setMimeType(GSYSubtitleMime.APPLICATION_SUBRIP)
    .setLanguage("zh")
    .setLabel("Chinese")
    .setDefault(true)
    .build();

videoPlayer.setSubtitleSource(source);
videoPlayer.setSubtitleEnabled(true);
```

See [SUBTITLE_CN.md](SUBTITLE_CN.md) for the detailed Chinese subtitle guide.

## Keep Last Frame

`KeepLastFrameVideo` is a demo-level implementation. It overrides the natural completion UI transition and keeps the render view instead of returning to the cover immediately.

```java
keepLastFrameVideo.setKeepLastFrameWhenComplete(true);
```

This is not a global default. Before moving it into base components, confirm the app's cover policy, surface release policy, and replay behavior.

## Screenshots

Existing video-only screenshot APIs stay unchanged:

```java
player.taskShotPic(listener);
player.saveFrame(file, listener);
```

Composed player screenshots include the video frame and player UI:

```java
player.taskShotPicWithView(listener);
player.saveFrameWithView(file, listener);
```

Current fixes:

- SurfaceView PixelCopy now reports every failure path through `null` or `success=false`.
- TextureView, SurfaceView, and GLSurfaceView save callbacks now reflect the actual file write result.
- Composed screenshots capture the video frame first and then draw player UI over it, which is useful for subtitle overlays and controls.

## GLSurfaceView Effects

GL changes are mainly in `DetailFilterActivity` and `GSYVideoGLView*Render`:

- The demo switches to `GSYVideoType.GLSURFACE` while active and restores the previous render type on exit.
- `GSYVideoGLViewBaseRender` and `GSYVideoGLViewSimpleRender` have safer release and screenshot callbacks.
- The `Filter` demo covers regular filters, texture watermark, multi-window playback, image mask, and blurred background scenes.

## Multi-URL Quality Switching

`SmartPickVideo` still targets multiple standalone URLs, such as separate quality URLs or next-episode URLs. It is not standard HLS/DASH ABR. It preloads the target URL with a temporary manager, seeks to the current playback position, and commits when ready.

Current hardening:

- Records the latest playback position to avoid jumping back to 0.
- Adds seek tolerance, retry, and timeout protection.
- Falls back to the original playback and releases the temporary manager on failure.

If the server can provide an HLS master playlist or DASH MPD, prefer the Exo adaptive quality demo below.

## Exo Adaptive Quality

The `EXO adaptive quality` demo uses one HLS master URL or one DASH MPD URL. Exo/Media3 can use multiple bitrate tracks in the same track group for adaptive playback.

Built-in test streams:

- HLS master: `https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8`
- DASH MPD: `https://dash.akamaized.net/envivio/EnvivioDash3/manifest.mpd`

New APIs:

```java
GSYExoVideoManager.instance().getVideoTrackInfoList();
GSYExoVideoManager.instance().clearVideoTrackOverride();
GSYExoVideoManager.instance().setVideoTrackOverride(groupIndex, trackIndex);
```

Notes:

- `Auto` lets TrackSelector pick tracks based on bandwidth, buffer, and device capability.
- Fixed quality uses `TrackSelectionOverride` for a specific video track.
- Clearing the override restores adaptive playback.

## Regression Checklist

Run at least:

```bash
./gradlew :gsyVideoPlayer-java:testDebugUnitTest :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Manual checks:

- `Open VIDEO`: drag the seek bar and confirm WebVTT preview frames appear.
- `Custom EXO subtitles`, `Common subtitles non-EXO`: local SRT, local VTT, and network SRT can switch; failures do not stop playback.
- `Keep last frame`: toggle the flag and confirm completion keeps the last frame or returns to cover.
- `Filter`: switch filters and GL scenes; playback, screenshots, and GIF creation should not crash.
- `Seamless switch`: switch multiple URLs and confirm it does not jump back to 0.
- `EXO adaptive quality`: HLS and DASH play, tracks are listed, and auto/fixed quality switching works.
