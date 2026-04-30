package com.shuyu.gsyvideoplayer.subtitle;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class GSYSubtitleParserTest {

    @Test
    public void parseSrtWithCommaTime() {
        String srt = "1\n"
            + "00:00:01,000 --> 00:00:02,500\n"
            + "hello\n"
            + "world\n\n"
            + "2\n"
            + "00:00:03,000 --> 00:00:04,000\n"
            + "next";

        List<GSYSubtitleCue> cues = new GSYSrtSubtitleParser().parse(srt);

        Assert.assertEquals(2, cues.size());
        Assert.assertEquals("hello\nworld", cues.get(0).getText());
        Assert.assertEquals("hello\nworld", new GSYSubtitleProvider(cues).findText(1500));
        Assert.assertEquals("", new GSYSubtitleProvider(cues).findText(2500));
        Assert.assertEquals("next", new GSYSubtitleProvider(cues).findText(3000));
    }

    @Test
    public void parseWebVttWithIdentifierAndSettings() {
        String vtt = "WEBVTT\n\n"
            + "intro\n"
            + "00:00:01.000 --> 00:00:02.000 align:middle\n"
            + "hello vtt\n\n"
            + "00:00:02.500 --> 00:00:04.000\n"
            + "second";

        List<GSYSubtitleCue> cues = new GSYWebVttSubtitleParser().parse(vtt);

        Assert.assertEquals(2, cues.size());
        GSYSubtitleProvider provider = new GSYSubtitleProvider(cues);
        Assert.assertEquals("hello vtt", provider.findText(1000));
        Assert.assertEquals("", provider.findText(2000));
        Assert.assertEquals("second", provider.findText(3999));
    }

    @Test
    public void inferMimeFromUrl() {
        Assert.assertEquals(GSYSubtitleMime.TEXT_VTT, GSYSubtitleMime.infer("https://a.b/sub.webvtt?x=1", null));
        Assert.assertEquals(GSYSubtitleMime.APPLICATION_SUBRIP, GSYSubtitleMime.infer("https://a.b/sub.srt", null));
        Assert.assertEquals("text/custom", GSYSubtitleMime.infer("https://a.b/sub.srt", "text/custom"));
    }

    @Test
    public void sourceIdIsIndependentFromLanguageAndLabel() {
        GSYSubtitleSource source = new GSYSubtitleSource.Builder("https://a.b/zh.srt")
            .setId("network-zh-srt")
            .setLanguage("zh")
            .setLabel("中文")
            .build();

        Assert.assertEquals("network-zh-srt", source.getId());
        Assert.assertEquals("zh", source.getLanguage());
        Assert.assertEquals("中文", source.getLabel());
    }

    @Test
    public void parseSrtWithBomBlankSpacesInvalidAndOverlap() {
        String srt = "\uFEFF1\n"
            + "00:00:03,000 --> 00:00:04,000\n"
            + "later\n"
            + "   \n"
            + "2\n"
            + "00:00:01,000 --> 00:00:03,000\n"
            + "first\n\n"
            + "3\n"
            + "00:00:02,000 --> 00:00:02,500\n"
            + "overlap\n\n"
            + "4\n"
            + "00:00:05,000 --> 00:00:04,000\n"
            + "invalid";

        GSYSubtitleProvider provider = new GSYSubtitleProvider(new GSYSrtSubtitleParser().parse(srt));

        Assert.assertEquals("first", provider.findText(1000));
        Assert.assertEquals("first\noverlap", provider.findText(2200));
        Assert.assertEquals("later", provider.findText(3000));
        Assert.assertEquals("", provider.findText(5000));
    }

    @Test
    public void parseWebVttSkipsNoteStyleRegionAndNoHourTime() {
        String vtt = "\uFEFFWEBVTT\n\n"
            + "STYLE\n"
            + "::cue { color: lime }\n\n"
            + "NOTE this is ignored\n"
            + "00:00.000 --> 00:01.000\n"
            + "not a cue\n\n"
            + "REGION\n"
            + "id:foo\n\n"
            + "00:01.000 --> 00:02.000\n"
            + "valid";

        GSYSubtitleProvider provider = new GSYSubtitleProvider(new GSYWebVttSubtitleParser().parse(vtt));

        Assert.assertEquals("", provider.findText(500));
        Assert.assertEquals("valid", provider.findText(1000));
    }

    @Test
    public void parseRealisticSrtDialogueFile() {
        String srt = "1\r\n"
            + "00:00:00,000 --> 00:00:03,200\r\n"
            + "<font color=\"#ffff00\">Previously on GSYVideoPlayer...</font>\r\n\r\n"
            + "2\r\n"
            + "00:00:03,500 --> 00:00:08,100\r\n"
            + "- First speaker line.\r\n"
            + "- Second speaker line.\r\n\r\n"
            + "3\r\n"
            + "00:00:08,500 --> 00:00:11,000\r\n"
            + "{\\an8}Top positioned text marker should stay as text.";

        GSYSubtitleProvider provider = new GSYSubtitleProvider(new GSYSrtSubtitleParser().parse(srt));

        Assert.assertEquals("<font color=\"#ffff00\">Previously on GSYVideoPlayer...</font>", provider.findText(0));
        Assert.assertEquals("- First speaker line.\n- Second speaker line.", provider.findText(4000));
        Assert.assertEquals("{\\an8}Top positioned text marker should stay as text.", provider.findText(9000));
        Assert.assertEquals("", provider.findText(11000));
    }

    @Test
    public void parseRealisticWebVttFileWithCommentsAndSettings() {
        String vtt = "WEBVTT - GSY sample\n\n"
            + "NOTE fetched from a CDN or bundled local raw resource\n\n"
            + "STYLE\n"
            + "::cue { background: rgba(0, 0, 0, .6); }\n\n"
            + "chapter-1\n"
            + "00:00:00.000 --> 00:00:03.000 line:90% align:center position:50%\n"
            + "WebVTT local file is active.\n\n"
            + "00:03.500 --> 00:08.000 size:80%\n"
            + "<v Roger>Voice-tagged cue text</v>\n\n"
            + "NOTE this block must not become a cue\n"
            + "00:09.000 --> 00:10.000\n"
            + "ignored";

        GSYSubtitleProvider provider = new GSYSubtitleProvider(new GSYWebVttSubtitleParser().parse(vtt));

        Assert.assertEquals("WebVTT local file is active.", provider.findText(1000));
        Assert.assertEquals("<v Roger>Voice-tagged cue text</v>", provider.findText(5000));
        Assert.assertEquals("", provider.findText(9000));
    }
}
