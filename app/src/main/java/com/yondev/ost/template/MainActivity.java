package com.yondev.ost.template;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.yondev.ost.template.adapter.RecyclerView_Adapter;
import com.yondev.ost.template.api.OSTAPI;
import com.yondev.ost.template.entity.Audio;
import com.yondev.ost.template.service.MediaPlayerService;
import com.yondev.ost.template.utils.AppBarLayoutCustomBehavior;
import com.yondev.ost.template.utils.CustomTouchListener;
import com.yondev.ost.template.utils.Shared;
import com.yondev.ost.template.utils.StorageUtil;
import com.yondev.ost.template.utils.onItemClickListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.os.Build.VERSION.SDK_INT;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String Broadcast_PLAY_NEW_AUDIO = "com.yondev.ost.PlayNewAudio";

    private MediaPlayerService player;
    private boolean serviceBound = false;
    private List<Audio> audioList;
    private ImageView collapsingImageView;
    private TextView textLyrics;
    private ImageButton btnPlay;
    private ImageButton btnNext;
    private ImageButton btnPrev;

    private boolean isPlaying = false;
    private int audioIndex = 0;

    private RecyclerView_Adapter adapter;

    private AdView mAdView;
    private InterstitialAd Interstitial;
    private AdRequest adRequest;
    private boolean isFirstime = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Shared.initialize(getBaseContext());

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        collapsingImageView = (ImageView) findViewById(R.id.collapsingImageView);

        AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);
        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams)appBarLayout.getLayoutParams();
        final AppBarLayoutCustomBehavior mBehavior = new AppBarLayoutCustomBehavior();
        lp.setBehavior(mBehavior);

        toolbar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mBehavior.setInterceptTouchEvent(true);
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        mBehavior.setInterceptTouchEvent(false);
                        return true;
                }
                return false;
            }
        });

        btnPlay = (ImageButton)findViewById(R.id.btnPlay);
        btnNext = (ImageButton)findViewById(R.id.btnNext);
        btnPrev = (ImageButton)findViewById(R.id.btnPrev);

        btnPlay.setOnClickListener(this);
        btnNext.setOnClickListener(this);
        btnPrev.setOnClickListener(this);

        textLyrics = (TextView)findViewById(R.id.textView3);
        textLyrics.setTypeface(Shared.openSansLight);

        textLyrics.setMovementMethod(ScrollingMovementMethod.getInstance());
        final CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) collapsingToolbarLayout.getLayoutParams();

        params.height = getDisplayHeight() - 300;
        collapsingToolbarLayout.setTitle(" ");

        final ScrollView scrollView = (ScrollView)findViewById(R.id.scrollView1);
        final ScrollView.LayoutParams paramView = (ScrollView.LayoutParams) scrollView.getLayoutParams();
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = false;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    collapsingToolbarLayout.setTitle(getString(R.string.app_name));
                    isShow = true;
                } else if(isShow) {
                    collapsingToolbarLayout.setTitle(" ");//carefull there should a space between double quote otherwise it wont work
                    isShow = false;
                }

                if(Math.abs(verticalOffset) > 30)
                    paramView.topMargin = Math.abs(verticalOffset) + 30;
                else
                    paramView.topMargin = 0;

                scrollView.setLayoutParams(paramView);
            }
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Shared.SERVER_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        OSTAPI client = retrofit.create(OSTAPI.class);
        Call<List<Audio>> call = client.getAll(5);

        call.enqueue(new Callback<List<Audio>>() {
            @Override
            public void onResponse(Call<List<Audio>> call, Response<List<Audio>> response) {
                audioList = response.body();
                initRecyclerView();
            }

            @Override
            public void onFailure(Call<List<Audio>> call, Throwable t) {

            }
        });


        mAdView = (AdView)findViewById(R.id.ad_view);
        adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        Interstitial = new InterstitialAd(MainActivity.this);
        Interstitial.setAdUnitId(getString(R.string.admob_interstitial_id));

        Interstitial.loadAd(adRequest);
        Interstitial.setAdListener(new AdListener() {
            public void onAdLoaded() {
                if(isFirstime)
                {
                    displayInterstitial();
                }

            }
        });

    }

    private void initRecyclerView() {
        if (audioList != null && audioList.size() > 0) {
            RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
            adapter = new RecyclerView_Adapter(audioList, getApplication());
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.addOnItemTouchListener(new CustomTouchListener(this, new onItemClickListener() {
                @Override
                public void onClick(View view, int index) {
                    adapter.setSelected(index);
                    playAudio(index);
                    textLyrics.setText(audioList.get(index).getLyric());
                    audioIndex = index;
                    updateImage();
                }
            }));

            textLyrics.setText(audioList.get(0).getLyric());
            updateImage();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
    //    getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putBoolean("serviceStatus", serviceBound);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("serviceStatus");
        if(serviceBound)
        {
            int last = new StorageUtil(getApplicationContext()).loadAudioIndex();
            textLyrics.setText(audioList.get(last).getLyric());
            adapter.setSelected(last);
            updateImage();
        }

    }

    //Binding this Client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;
            isPlaying  = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };


    private void playAudio(int audioIndex) {
        //Check is service is active
        if (!serviceBound) {
            //Store Serializable audioList to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudio((ArrayList<Audio>) audioList);
            storage.storeAudioIndex(audioIndex);

            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            //Store the new audioIndex to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudioIndex(audioIndex);

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }

        btnPlay.setImageResource(R.drawable.btn_play);
        isPlaying = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Interstitial.loadAd(adRequest);
        if (serviceBound) {
            unbindService(serviceConnection);
            //service is active
            player.stopSelf();
            isPlaying = false;
        }

    }

    private int getDisplayHeight()
    {

        WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        final int version = android.os.Build.VERSION.SDK_INT;

        if (version >= 13)
        {
            Point size = new Point();
            display.getSize(size);
            return size.y;
        }
        else
        {

            return  display.getHeight();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.btnPlay:
                   if(isPlaying)
                   {
                       player.pauseMedia();
                       isPlaying = false;
                       btnPlay.setImageResource(R.drawable.btn_play);
                       adapter.setSelected(-1);
                   }
                   else
                   {
                       int last = new StorageUtil(getApplicationContext()).loadAudioIndex();

                       if(!serviceBound)
                            playAudio(audioIndex);
                       else
                       {
                           if(last == player.audioIndex)
                               player.resumeMedia();
                           else
                               playAudio(audioIndex);
                       }


                       btnPlay.setImageResource(R.drawable.btn_pause);
                       adapter.setSelected(audioIndex);
                       isPlaying = true;
                       updateImage();
                   }

                break;
            case R.id.btnNext:
                if(isPlaying) {
                    player.skipToNext();
                    audioIndex = player.audioIndex;
                    adapter.setSelected(audioIndex);
                }
                else
                    setNext();

                textLyrics.setText(audioList.get(audioIndex).getLyric());
                updateImage();

                break;
            case R.id.btnPrev:
                if(isPlaying) {
                    player.skipToPrevious();
                    audioIndex = player.audioIndex;
                    adapter.setSelected(audioIndex);
                }
                else
                    setPrev();

                textLyrics.setText(audioList.get(audioIndex).getLyric());
                updateImage();
                break;
        }
    }

    private void setPrev()
    {
        if (audioIndex == 0) {
            audioIndex = audioList.size() - 1;
        } else {
            --audioIndex;
        }

        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);
    }

    private void setNext()
    {
        if (audioIndex == audioList.size() - 1) {
            //if last in playlist
            audioIndex = 0;
        } else {
            //get next in playlist
            ++audioIndex;
        }

        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);

    }

    private void updateImage()
    {
        Glide.with(MainActivity.this)
                .load(audioList.get(audioIndex).getAlbum())
                .apply(new RequestOptions()
                      //  .placeholder(R.drawable.image1)
                        .error(R.drawable.image1)
                        .fitCenter()
                )
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(collapsingImageView);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void displayInterstitial() {
        // If Ads are loaded, show Interstitial else show nothing.
        if (Interstitial.isLoaded()) {
            Interstitial.show();
        }
    }


}
