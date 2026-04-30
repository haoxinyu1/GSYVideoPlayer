package com.shuyu.gsyvideoplayer.preview;

import org.junit.Assert;
import org.junit.Test;

public class GSYVideoPreviewVttParserTest {

    @Test
    public void parseSpriteVttAndFindFrame() {
        String vtt = "WEBVTT\n"
                + "\n"
                + "1\n"
                + "00:00:00.000 --> 00:00:01.000\n"
                + "160p-00001.jpg#xywh=0,0,284,160\n"
                + "\n"
                + "2\n"
                + "00:00:01.000 --> 00:00:02.000\n"
                + "160p-00001.jpg#xywh=284,0,284,160\n";

        GSYVideoPreviewProvider provider = GSYVideoPreviewVttParser.parse(
                vtt,
                "https://stdlwcdn.lwcdn.com/i/asset/160p.vtt");

        Assert.assertEquals(2, provider.getFrames().size());
        GSYVideoPreviewFrame frame = provider.getPreviewFrame(1500);
        Assert.assertNotNull(frame);
        Assert.assertEquals("https://stdlwcdn.lwcdn.com/i/asset/160p-00001.jpg", frame.getImageUrl());
        Assert.assertTrue(frame.hasCrop());
        Assert.assertEquals(284, frame.getCropX());
        Assert.assertEquals(0, frame.getCropY());
        Assert.assertEquals(284, frame.getCropWidth());
        Assert.assertEquals(160, frame.getCropHeight());
    }

    @Test
    public void parseSeparateImageVtt() {
        String vtt = "WEBVTT\n"
                + "\n"
                + "00:01.000 --> 00:02.500\n"
                + "thumb.jpg\n";

        GSYVideoPreviewProvider provider = GSYVideoPreviewVttParser.parse(
                vtt,
                "https://example.com/thumbs/index.vtt");

        GSYVideoPreviewFrame frame = provider.getPreviewFrame(1000);
        Assert.assertNotNull(frame);
        Assert.assertEquals(1000, frame.getStartTimeMs());
        Assert.assertEquals(2500, frame.getEndTimeMs());
        Assert.assertEquals("https://example.com/thumbs/thumb.jpg", frame.getImageUrl());
        Assert.assertFalse(frame.hasCrop());
    }
}
