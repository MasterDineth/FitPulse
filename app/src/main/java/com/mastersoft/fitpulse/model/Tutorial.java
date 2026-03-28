package com.mastersoft.fitpulse.model;

public class Tutorial {
    private String title;
    private String level;
    private String duration;
    private String emoji;
    private String videoUrl;

    public Tutorial(String title, String level, String duration, String emoji, String videoUrl) {
        this.title = title;
        this.level = level;
        this.duration = duration;
        this.emoji = emoji;
        this.videoUrl = videoUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getLevel() {
        return level;
    }

    public String getDuration() {
        return duration;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getVideoUrl() {
        return videoUrl;
    }
}