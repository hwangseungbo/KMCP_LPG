package com.example.kmcplpg;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import java.util.HashMap;

public class MySoundPlayer {
    public static final int Pager_Beeps = R.raw.pagerbeeps;
    public static final int Alarm_Clock = R.raw.alarmclock;
    public static int StreamId;

    private static SoundPool soundPool;
    private static HashMap<Integer, Integer> soundPoolMap;

    // sound media initialize
    public static void initSounds(Context context) {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .setMaxStreams(1)       // 동시재생 가능한 사운드 숫자
                .build();

        soundPoolMap = new HashMap<>(2);
        soundPoolMap.put(Pager_Beeps, soundPool.load(context, Pager_Beeps, 1));
        soundPoolMap.put(Alarm_Clock, soundPool.load(context, Alarm_Clock, 2));

    }

    public static void play(int raw_id) {
        if(soundPoolMap.containsKey(raw_id))
        {
            StreamId = soundPool.play(soundPoolMap.get(raw_id), 1, 1, 1, 9999, 1f);
        }
    }

    public static void stop() {
        soundPool.stop(StreamId);
    }


}

