package com.example.planify.utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import com.example.planify.R;

public class SoundManager {
    private static SoundManager instance;
    private SoundPool soundPool;
    private int clickSoundId;
    private int pomodoroStartSoundId;
    private int successAuthSoundId;
    private int invalidSoundId;
    private int levelUpSoundId;
    private int taskCompletedSoundId;
    private boolean loaded = false;

    private SoundManager(Context context) {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        
        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(attributes)
                .build();

        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
            if (status == 0) loaded = true;
        });

        clickSoundId = soundPool.load(context, R.raw.click_evr, 1);
        pomodoroStartSoundId = soundPool.load(context, R.raw.pomodoro_start, 1);
        successAuthSoundId = soundPool.load(context, R.raw.suksesloginsignup, 1);
        invalidSoundId = soundPool.load(context, R.raw.invalid, 1);
        levelUpSoundId = soundPool.load(context, R.raw.levelup, 1);
        taskCompletedSoundId = soundPool.load(context, R.raw.taskcompleted, 1);
    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new SoundManager(context.getApplicationContext());
        }
    }

    public static SoundManager getInstance() {
        return instance;
    }

    public void playClick() {
        if (soundPool != null && loaded) {
            soundPool.play(clickSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }

    public void playPomodoroStart() {
        if (soundPool != null && loaded) {
            soundPool.play(pomodoroStartSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }

    public void playSuccessAuth() {
        if (soundPool != null && loaded) {
            soundPool.play(successAuthSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }

    public void playInvalid() {
        if (soundPool != null && loaded) {
            soundPool.play(invalidSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }

    public void playLevelUp() {
        if (soundPool != null && loaded) {
            soundPool.play(levelUpSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }

    public void playSuccess() {
        if (soundPool != null && loaded) {
            soundPool.play(successAuthSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }

    public void playTaskComplete() {
        if (soundPool != null && loaded) {
            soundPool.play(taskCompletedSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }
}
