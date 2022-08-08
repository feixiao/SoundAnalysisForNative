package cn.okfuture.soundanalysfornative;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    /**
     * 20分贝以下的声音，一般来说，我们认为它是安静的，当然，一般来说15分贝以下的我们就可以认为它属于"死寂"的了。
     * 、20-40分贝大约是情侣耳边的喃喃细语。40-60分贝属于我们正常的交谈声音。60分贝以上就属于吵闹范围了，70分贝
     * 我们就可以认为它是很吵的，而且开始损害听力神经，90分贝以上就会使听力受损，
     *
     * 暂取正常音乐范围为 30-80
     */

    /**
     * 选取音乐
     */
    private static final int SELECT_MUSIC = 0x00;
    /**
     * 求情读文件权限
     */
    private static final int REQUEST_READ_STORAGE = 0x01;
    /**
     * 请求读取文件
     */
    private static final int REQUEST_RECORD_AUDIO = 0X01;
    private static final int FREQUENCY_LINE = 0;
    private Button btn_select_music;
    private TextView tv_select_music;
    private TextView tv_currentFrequency;
    private Button btn_start;
    private MediaPlayer mMediaPlayer;
    private Visualizer visualizer;
    private LinearLayout ll_main;

    private long lastChangeTime;


    /**
     * 当前音量
     */
    private int currentVolume;
    /**
     * 当前频率
     */
    private int currentFrequency;
    private String Tags = "test";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {


            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_STORAGE);
            }
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            }
        }
        mMediaPlayer = new MediaPlayer();
        visualizer = new Visualizer(mMediaPlayer.getAudioSessionId());

        // 设置可视化数据的数据大小
        visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {

            /**
             * onWaveFormDataCapture返回的是声音的波形数据。
             * waveform 是波形采样的字节数组，它包含一系列的 8 位（无符号）的 PCM 单声道样本
             * */
            @SuppressLint("LongLogTag")
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {

                // intensity 强度
                float intensity = ((float) waveform[0] + 128f) / 256;


                // volume 音量
                long v = 0;
                for (int i = 0; i < waveform.length; i++) {
                    v += Math.pow(waveform[i], 2);
                }

                double volume = 10 * Math.log10(v / (double) waveform.length);

                currentVolume = (int) volume;

                Log.d("onWaveFormDataCapture intensity : ", String.valueOf(intensity) + " volume : " + String.valueOf(volume));

            }

            /**
             * Convert square of magnitude to decibels
             * @param squareMag square of magnitude
             * @return decibels
             */
            public  float magnitudeToDb(float squareMag) {
                if (squareMag == 0)
                    return 0;
                return (float) (20 * Math.log10(squareMag));
            }

            /**
             * onFftDataCapture返回的是经过傅里叶变换处理后的音频数据
             * fft 是经过 FFT 转换后频率采样的字节数组，频率范围为 0（直流）到采样值的一半！返回的数据如上图所示：n 为采样值Rf 和 lf 分别对应第 k 个频率的实部和虚部；
             * 如果 Fs 为采样频率，那么第 k 个频率为(k*Fs)/(n/2)；换句话说：频率横坐标的取值范围为[0, Fs]
             *
             * 数据排列: https://developer.android.com/reference/android/media/audiofx/Visualizer#getFft(byte[])
             * */
            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                if (mMediaPlayer == null || !mMediaPlayer.isPlaying()) {
                    return;
                }

                // magnitudes 振幅
                // 获取最多的振幅
                int n = fft.length;
                float[] magnitudes = new float[n / 2 + 1];
                float[] phases = new float[n / 2 + 1];
                magnitudes[0] = (float)Math.abs(fft[0]);      // DC
                magnitudes[n / 2] = (float)Math.abs(fft[1]);  // Nyquist
                phases[0] = phases[n / 2] = 0;
                int max = 0;
                for (int k = 1; k < n / 2; k++) {
                    int i = k * 2;
                    magnitudes[k] = (float)Math.hypot(fft[i], fft[i + 1]);
                    if (magnitudes[max] < magnitudes[k]) {
                        max = k;
                    }
                    phases[k] = (float)Math.atan2(fft[i + 1], fft[i]);
                }

                Log.i(Tags,"phases  0 : " + String.valueOf(phases[0]));
                Log.i(Tags,"phases  1 : " + String.valueOf(phases[1]));
                Log.i(Tags,"phases  2 : " + String.valueOf(phases[2]));
                Log.i(Tags,"phases  3 : " + String.valueOf(phases[3]));


                // 获取当前的Frequency
                currentFrequency = max * samplingRate / fft.length;

                Log.i("xiaozhu", "[onFftDataCapture] currentFrequency=" + currentFrequency);
                tv_currentFrequency.setText( getString(R.string.frequency)+" : "+currentFrequency);
                if (currentFrequency<0){
                    return;
                }

                float db = magnitudeToDb(magnitudes[max]);
                Log.i(Tags,"magnitudes : " + String.valueOf(magnitudes[max]) + " db : " + String.valueOf(db));


                ll_main.setBackgroundColor(ColorUtils.argb((currentVolume - 30) * 0.02f, Color.red(ColorUtils.COLOR_LIST_140[currentFrequency % 140]), Color.green(ColorUtils.COLOR_LIST_140[currentFrequency % 140]), Color.blue(ColorUtils.COLOR_LIST_140[currentFrequency % 140])));


            }
        }, Visualizer.getMaxCaptureRate() / 2  , true, true);

        visualizer.setEnabled(true);

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {

            }
        });


    }


    private void initView() {
        btn_select_music = (Button) findViewById(R.id.btn_select_music);
        tv_select_music = (TextView) findViewById(R.id.tv_select_music);
        tv_currentFrequency = (TextView) findViewById(R.id.tv_currentFrequency);
        btn_start = (Button) findViewById(R.id.btn_start);

        btn_select_music.setOnClickListener(this);
        btn_start.setOnClickListener(this);
        ll_main = (LinearLayout) findViewById(R.id.ll_main);
        ll_main.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_select_music:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, SELECT_MUSIC);
                break;
            case R.id.btn_start:

                if (mMediaPlayer != null) {
                    if (mMediaPlayer.isPlaying()) {
                        mMediaPlayer.pause();
                    } else {
                        mMediaPlayer.start();
                    }
                }
                break;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();


            tv_select_music.setText(uri.getPath());
            try {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.reset();
                }
                mMediaPlayer.setDataSource(this, uri);
                mMediaPlayer.prepare();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                finish();
            }
        }
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                finish();
            }
        }

    }

    @Override
    protected void onPause() {
        super.onPause();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        visualizer.setEnabled(false);
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }

        if (visualizer != null) {
            visualizer.release();
        }


    }
}
