# 近期播放能力说明

本文档汇总近期 Demo 和基础能力调整，方便回归和二次开发时快速确认入口、API 和注意事项。

## 入口总览

| 能力 | Demo 入口 | 主要类 | 说明 |
| --- | --- | --- | --- |
| WebVTT 进度条预览 | `打开VIDEO` | `PreViewGSYVideoPlayer` | 通过 WebVTT 缩略图轨道展示预览图，支持独立图片和 sprite 坐标裁剪。 |
| 通用外挂字幕 | `自定义EXO支持字幕`、`通用字幕非EXO` | `GSYSubtitleController`、`GSYSubtitleView` | SRT/WebVTT 由播放器 UI 层统一解析和渲染，可跨 IJK、System、Media3 使用。 |
| 完成后保留最后一帧 | `完成保留最后一帧` | `KeepLastFrameVideo` | Demo 级实现，通过 flag 控制自然播放完成时保留当前渲染画面或回到默认封面态。 |
| 截图语义增强 | 滤镜 Demo、MediaCodec Demo 等 | `StandardGSYVideoPlayer`、`GSYRenderView` | 保留视频帧截图，同时新增包含播放器 UI 的组合截图 API。 |
| GLSurfaceView 效果和生命周期 | `滤镜` | `DetailFilterActivity`、`GSYVideoGLView*Render` | 整理 GL render 生命周期，补充滤镜、纹理、多窗口、遮罩等 Demo 场景。 |
| 多 URL 清晰度切换稳态优化 | `无缝切换` | `SmartPickVideo` | 保留双 manager 方案，强化切换过程中的位置同步、超时、失败回退和临时 manager 释放。 |
| Exo 自适应清晰度 | `EXO自适应清晰度` | `ExoAdaptiveTrackActivity`、`GSYExo2MediaPlayer` | 使用单个 HLS master playlist 或 DASH MPD，由 Media3 TrackSelector 在同一时间线内自适应或固定 video track。 |

## WebVTT 进度条预览

预览不再依赖客户端对原视频批量抽帧。业务侧可以预生成 WebVTT 缩略图轨道，cue 可以指向独立图片，也可以指向 sprite 坐标：

```text
WEBVTT

00:00:00.000 --> 00:00:05.000
thumbs.jpg#xywh=0,0,160,90
```

主要 API：

```java
player.setOpenPreView(true);
player.setPreviewVttUrl("https://example.com/thumbs.vtt");
```

库层提供 `GSYVideoPreviewVttParser`、`GSYVideoPreviewProvider`、`GSYVideoPreviewFrame`，业务层只需要按拖动进度获取对应帧。

## 通用外挂字幕

外挂字幕能力已从 Media3 专属实现调整为 UI overlay 能力。SRT/WebVTT 由 `GSYSubtitleController` 加载、解析和根据播放进度刷新，异常时只清空字幕，不影响主视频播放。

基本用法：

```java
GSYSubtitleSource source = new GSYSubtitleSource.Builder("https://example.com/subtitle.srt")
    .setMimeType(GSYSubtitleMime.APPLICATION_SUBRIP)
    .setLanguage("zh")
    .setLabel("中文")
    .setDefault(true)
    .build();

videoPlayer.setSubtitleSource(source);
videoPlayer.setSubtitleEnabled(true);
```

详细说明见 [SUBTITLE_CN.md](SUBTITLE_CN.md)。

## 完成后保留最后一帧

`KeepLastFrameVideo` 是 Demo 级实现，核心是覆写自然播放完成后的 UI 状态切换，在不主动释放 render view 的情况下保留画面。

```java
keepLastFrameVideo.setKeepLastFrameWhenComplete(true);
```

注意：这是给业务验证交互语义的 Demo，不是全局默认行为。需要根据业务播放器封面、Surface 释放策略、重播逻辑决定是否下沉到基础库。

## 截图能力

现有视频帧截图语义保持不变：

```java
player.taskShotPic(listener);
player.saveFrame(file, listener);
```

新增组合截图语义，用于需要把视频画面和播放器 UI 一起截下来的场景：

```java
player.taskShotPicWithView(listener);
player.saveFrameWithView(file, listener);
```

当前修复点：

- SurfaceView 通过 PixelCopy 截图时所有失败路径都会回调 `null` 或 `success=false`。
- TextureView、SurfaceView、GLSurfaceView 保存文件时会根据真实写入结果回调。
- 组合截图会先拿视频帧，再叠加播放器 UI，适合需要包含字幕 overlay、控制栏等视图层的场景。

## GLSurfaceView 效果

GL 相关整理集中在 `DetailFilterActivity` 和 `GSYVideoGLView*Render`：

- Demo 内统一切换到 `GSYVideoType.GLSURFACE`，退出时恢复进入前的 render type。
- `GSYVideoGLViewBaseRender` / `GSYVideoGLViewSimpleRender` 增强 release 和截图回调，降低 GL 生命周期异常导致的卡住风险。
- `滤镜` Demo 可以切换普通滤镜、纹理水印、多窗口播放、图片穿孔、背景高斯等场景。

## 多 URL 清晰度切换

`SmartPickVideo` 仍然适用于多个独立 URL 的清晰度切换或下一集切换。这个方案不是 HLS/DASH 标准 ABR，核心是使用临时 manager 预加载新 URL，准备好后同步到当前播放位置并提交切换。

当前优化点：

- 切换时记录最新播放位置，避免直接回到 0。
- 增加 seek 误差阈值、重试和超时保护。
- 切换失败会回退原播放，不让临时 manager 长期占用资源。

如果服务端可以提供 HLS master playlist 或 DASH MPD，优先使用下面的 Exo 自适应清晰度 Demo。

## Exo 自适应清晰度

`EXO自适应清晰度` Demo 使用一个 HLS master URL 或一个 DASH MPD URL。Exo/Media3 会把同一个 track group 里的多码率 video track 用于 adaptive playback。

Demo 内置测试源：

- HLS master：`https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8`
- DASH MPD：`https://dash.akamaized.net/envivio/EnvivioDash3/manifest.mpd`

新增能力：

```java
GSYExoVideoManager.instance().getVideoTrackInfoList();
GSYExoVideoManager.instance().clearVideoTrackOverride();
GSYExoVideoManager.instance().setVideoTrackOverride(groupIndex, trackIndex);
```

说明：

- 默认选择“自动”，由 TrackSelector 根据带宽、buffer、设备能力选择 track。
- 选择固定清晰度时，会使用 `TrackSelectionOverride` 固定到某个 video track。
- 清除 override 后恢复自适应。

## 回归建议

每次修改这些能力后，至少执行：

```bash
./gradlew :gsyVideoPlayer-java:testDebugUnitTest :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

真机建议检查：

- `打开VIDEO`：拖动进度条，确认 WebVTT 预览图能出现。
- `自定义EXO支持字幕`、`通用字幕非EXO`：本地 SRT、本地 VTT、网络 SRT 都能切换，失败不影响播放。
- `完成保留最后一帧`：打开/关闭 flag，确认完成后分别停留最后一帧和回到封面。
- `滤镜`：切换滤镜和 GL 场景，确认播放、截图、GIF 不崩。
- `无缝切换`：切换多个 URL，确认不回 0，失败可回退。
- `EXO自适应清晰度`：HLS 和 DASH 都能播放，轨道列表能显示，自动/固定清晰度能切换。
