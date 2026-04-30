# 通用字幕能力

GSYVideoPlayer 现在提供一套和播放内核解耦的外挂字幕能力。字幕由播放器 UI 基类统一加载、解析和渲染，不再依赖 Media3 的 `SubtitleView` 或 ExoPlayer 的 `MergingMediaSource`。

## 目标

- 普通 IJK、System、Exo/Media3 内核都可以使用外挂字幕。
- 外挂字幕加载失败不会影响视频主播放。
- 全屏、小窗、退出全屏时字幕配置会跟随播放器状态复制。
- Media3 的内嵌字幕 cue 可以桥接到同一套字幕 overlay 显示。
- 播放完成、错误或回到封面状态时会清空字幕显示，避免封面上残留最后一条字幕。

## 支持格式

- SRT：`application/x-subrip`
- WebVTT：`text/vtt`

如果没有显式传 `mimeType`，会按 URL 后缀推断，`.vtt` / `.webvtt` 识别为 WebVTT，其余默认按 SRT 解析。

## 基本用法

```java
GSYSubtitleSource source = new GSYSubtitleSource.Builder("https://example.com/subtitle.srt")
    .setId("zh-main")
    .setMimeType(GSYSubtitleMime.APPLICATION_SUBRIP)
    .setLanguage("zh")
    .setLabel("中文")
    .setCharsetName("UTF-8")
    .setOffsetMs(0)
    .setDefault(true)
    .build();

videoPlayer.setSubtitleSource(source);
videoPlayer.setSubtitleEnabled(true);
```

## 多字幕切换

```java
List<GSYSubtitleSource> subtitles = new ArrayList<>();
subtitles.add(new GSYSubtitleSource.Builder(zhUrl).setLanguage("zh").setDefault(true).build());
subtitles.add(new GSYSubtitleSource.Builder(enUrl).setLanguage("en").build());

videoPlayer.setSubtitleSources(subtitles);
videoPlayer.selectSubtitle("en");
```

建议业务侧给每条字幕设置稳定的 `id`，例如 `zh-main`、`zh-forced`、`en-sdh`。`language` 只表达语言，`label` 只用于展示。

`selectSubtitle()` 可以传 source 的 `id`，也兼容传 `language`、`label` 或 `url`。

## 样式和偏移

```java
GSYSubtitleStyle style = new GSYSubtitleStyle.Builder()
    .setTextColor(Color.WHITE)
    .setTextSizeSp(16)
    .setShadow(Color.BLACK, 3, 1, 1)
    .setBottomMarginDp(56)
    .build();

videoPlayer.setSubtitleStyle(style);
videoPlayer.setSubtitleOffsetMs(500);
```

字幕位置可以通过 `setPosition()` 调整：

```java
GSYSubtitleStyle topStyle = new GSYSubtitleStyle.Builder()
    .setPosition(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 16, 72)
    .build();

GSYSubtitleStyle bottomStyle = new GSYSubtitleStyle.Builder()
    .setPosition(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 16, 56)
    .build();
```

`layoutGravity` 控制字幕在播放器里的上、中、下位置，`horizontalMarginDp` 控制左右边距，`verticalMarginDp` 控制顶部或底部距离。旧的 `setBottomMarginDp()` 仍然可用，等价于底部字幕的垂直边距。

`setSubtitleOffsetMs()` 是全局偏移，`GSYSubtitleSource#setOffsetMs()` 是单条字幕源自己的偏移，两者会相加。

## Media3 内嵌字幕

Media3 内嵌字幕仍由 ExoPlayer 解析，但 demo 中的 `GSYExoSubTitleVideoView` 会把 `CueGroup` 转成通用字幕文本：

- 没有外挂字幕时，内嵌 cue 显示在通用 overlay 上。
- 已选择外挂字幕时，外挂字幕优先，内嵌 cue 不会覆盖外挂字幕。
- 外挂字幕加载、解析失败或解析为空时，会回退允许内嵌 cue 显示，不会因为失败的外挂源压住 Media3 内嵌字幕。

## 降级策略

外挂字幕加载、网络、解析失败或解析为空时，只清空字幕显示并打印 warning，不会抛异常，也不会影响视频 prepare/play。

## Demo

Demo 入口：

- `自定义EXO支持字幕`：Exo/Media3 场景，演示 `setSubtitleSources(...)` 走 GSY 通用外挂字幕，不再把外挂字幕合并进 Exo 的 `MediaSource`；Media3 内嵌字幕仍可以通过 `onCues(CueGroup)` 桥接到同一套 overlay。
- `通用字幕非Exo`：强制切到 IJK 内核，演示非 Exo 内核也可以直接使用 `setSubtitleSources(...)`。

两个页面都提供三种字幕源切换，方便验证不同格式和真实网络字幕：

- `SUBTITLE SRT LOCAL`：本地 `res/raw/demo_subtitle.srt`，离线可用。
- `SUBTITLE VTT LOCAL`：本地 `res/raw/demo_subtitle_vtt.vtt`，覆盖 WebVTT 解析。
- `SUBTITLE SRT NETWORK`：项目历史使用过的真实字幕链接 `http://img.cdn.guoshuyu.cn/subtitle2.srt`，用于验证网络 SRT 加载。

两个页面都提供 `GSY SUBTITLE ON/OFF` 字幕开关、`SUBTITLE SIZE` 字号切换按钮和 `SUBTITLE SRT LOCAL/VTT LOCAL/SRT NETWORK` 字幕源切换按钮。

## 回归建议

每次改字幕相关代码，建议至少跑：

```bash
./gradlew :gsyVideoPlayer-java:testDebugUnitTest :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.gsyvideoplayer/.MainActivity
```

真机手动检查：

- 主页面分别进入 `自定义EXO支持字幕` 和 `通用字幕非Exo`，点击播放，确认视频能进入 `onPrepared`。
- 等到字幕时间点，确认外挂字幕出现、消失和 seek 后刷新正常。
- 点击 `GSY SUBTITLE OFF/ON`，确认字幕只开关显示，不影响视频播放。
- 点击 `SUBTITLE SIZE 22/16`，确认字号可以切换，样式不遮挡播放器控制栏。
- 点击 `SUBTITLE SRT LOCAL/VTT LOCAL/SRT NETWORK`，确认本地 SRT、本地 VTT、真实网络 SRT 都可以切换；网络字幕失败时只清空字幕，不影响视频播放。
- 进入全屏、返回退出全屏，确认没有 crash，字幕状态能跟随。
- 扫 logcat，确认没有 `FATAL EXCEPTION`、`NullPointerException`、`IllegalStateException`、`Player error`。

异常场景：

- 传入 404 或格式错误的字幕地址，预期是无字幕并继续播放。
- 传入 `.vtt` 或 `.srt`，确认 MIME 推断正确。
- 普通非字幕播放器页面仍能正常播放。
