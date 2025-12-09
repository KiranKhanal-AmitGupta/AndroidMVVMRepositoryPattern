package com.newitventure.ntentertainment.livetv;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.ads.AdView;
import com.mancj.slideup.SlideUp;
import com.newitventure.ntentertainment.MainActivity;
import com.newitventure.ntentertainment.MainApplication;
import com.newitventure.ntentertainment.R;
import com.newitventure.ntentertainment.adapter.EpgDaysRecyclerViewAdapter;
import com.newitventure.ntentertainment.adapter.EpgDvrRecyclerViewAdapter;
import com.newitventure.ntentertainment.adapter.EpgLandDaysListAdapter;
import com.newitventure.ntentertainment.adapter.EpgLandRecyclerViewAdapter;
import com.newitventure.ntentertainment.adapter.SimpleListAdapter;
import com.newitventure.ntentertainment.adapter.TwoWayViewAdapter;
import com.newitventure.ntentertainment.audio.audionotification.AudioNotification;
import com.newitventure.ntentertainment.audio.musichomefragment.audioplayeractivity.AudioPlayerActivity;
import com.newitventure.ntentertainment.entitynew.common.TimeStamp;
import com.newitventure.ntentertainment.entitynew.livetv.Category;
import com.newitventure.ntentertainment.entitynew.livetv.Channel;
import com.newitventure.ntentertainment.entitynew.livetv.ChannelsData;
import com.newitventure.ntentertainment.entitynew.livetv.CurrentChannels;
import com.newitventure.ntentertainment.entitynew.livetv.EPG;
import com.newitventure.ntentertainment.entitynew.livetv.EpgTokenList;
import com.newitventure.ntentertainment.entitynew.livetv.Streaming;
import com.newitventure.ntentertainment.entitynew.login.User;
import com.newitventure.ntentertainment.fmradio.BackgroundAudioService;
import com.newitventure.ntentertainment.movie.MovieApiInterface;
import com.newitventure.ntentertainment.realmOperations.RealmRead;
import com.newitventure.ntentertainment.utils.AnimationUtil;
import com.newitventure.ntentertainment.utils.CheckServiceRunning;
import com.newitventure.ntentertainment.utils.InternetConnectionDetector;
import com.newitventure.ntentertainment.utils.LinkConfig;
import com.newitventure.ntentertainment.utils.OnSwipeTouchListener;
import com.newitventure.ntentertainment.utils.SetupRetrofit;
import com.newitventure.ntentertainment.utils.Utils;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.lucasr.twowayview.TwoWayView;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import retrofit2.HttpException;
import retrofit2.Response;
import retrofit2.Retrofit;
import timber.log.Timber;

/**
 * Created by nitv on 3/15/17.
 */

public class ChannelPlayingActivity extends AppCompatActivity {

    private static final String TAG = "ChannelPlayingActivity";
    //Retaining data from fragment on orientation change
    private static final String TAG_RETAINED_FRAGMENT = "RetainFragment";
    public static String timeStampUrl, adCodeNitvBanner, adCodeAdMobBanner, adCodeAdMobFull, adCodeNitvFull;
    public static int nitvPriority, admobPriority;
    public static boolean nitvBannerStatus, nitvFullStatus, admobBannerStatus, admobFullStatus;
    public static HashMap<String, List<EpgTokenList>> epgHash;
    public static ArrayList<Channel> channelList, talakoChannelList;
    public static int position = 0;
    public static List<String> epgKey;
    static ViewPagerAdapter viewPagerAdapter;
    private final Handler mHandler = new Handler();
    public String STREAM_TYPE = "sd";
    @BindView(R.id.tabLayout)
    TabLayout tabLayout;
    @BindView(R.id.relatedContentPager)
    ViewPager viewPager;
    @BindView(R.id.videoView1)
    VideoView mVideoView;
    @BindView(R.id.imageView2)
    ImageView mPlayPause;
    @BindView(R.id.progressBar1)
    ProgressBar mLoading;
    @BindView(R.id.controllers)
    View mControllers;
    @BindView(R.id.imageView3)
    ImageView mFullScreen;
    @BindView(R.id.gad_bottom)
    LinearLayout adLinlay;
    @BindView(R.id.nitvAdImageView)
    ImageView nitvAdImageView;
    @BindView(R.id.play_circle)
    ImageButton mPlayCircle;
    @BindView(R.id.coverArtView)
    ImageView mCoverArt;
    //    @BindView(R.id.recyclerViewChannels)
//    RecyclerView recyclerViewChannels;
    @BindView(R.id.recyclerViewDays)
    RecyclerView recyclerViewDays;
    @BindView(R.id.recyclerViewEpg)
    RecyclerView recyclerViewEpg;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.imageViewDVR)
    ImageView ivDvr;
    @BindView(R.id.ivNext)
    ImageView ivNext;
    @BindView(R.id.ivPrev)
    ImageView ivPrev;
    @BindView(R.id.ivEpg)
    ImageView ivEpg;
    @BindView(R.id.slideView)
    View slideView;
    @BindView(R.id.twoWayView)
    TwoWayView twoWayView;
    @BindView(R.id.dim)
    View dim;
    @BindView(R.id.container)
    View mContainer;
    @BindView(R.id.ivCurrentChannel)
    ImageView ivCurrentImage;
    @BindView(R.id.tvCurrentProg)
    TextView tvCurrentProgram;
    @BindView(R.id.bListSelector)
    TextView bListSelector;
    @BindView(R.id.lvCategory)
    ListView lvCategory;
    @BindView(R.id.bEpgDaysListSelector)
    TextView bListEpgDaysSelector;
    @BindView(R.id.tvUpDownArrow)
    ImageView ivUpDownArrow;
    @BindView(R.id.epgUpDownArrow)
    ImageView epgUpDownArrow;
    @BindView(R.id.epgInLandscape)
    View epgInLandscapeView;
    @BindView(R.id.lvDays)
    ListView lvDays;
    @BindView(R.id.rvEpgGuide)
    RecyclerView rvEpgGuide;
    @BindView(R.id.llEpgSelector)
    LinearLayout llEpgSelector;
    @BindView(R.id.mtunes_img)
    ImageView ivCurrentChannelImageEpgLand;
    @BindView(R.id.mtunes_txt)
    TextView tvCurrentChannelEpgLand;
    @BindView(R.id.tvDate)
    TextView tvCurrentDateEpgLand;
    @BindView(R.id.onair_txt_time)
    TextView tvOnAirTime;
    @BindView(R.id.on_air_txt_name)
    TextView tvOnAirName;
    @BindView(R.id.timetxtUpNext)
    TextView tvUpNextTime;
    @BindView(R.id.nametxtUpNext)
    TextView tvUpNextName;
    //    @BindView(R.id.ivShare)
//    ImageView ivShare;
    @BindView(R.id.epg_close)
    ImageView epgCloseImageView;
    @BindView(R.id.imageViewHq)
    ImageView imageViewHq;
    @BindView(R.id.land_epg_close)
    ImageView landEPGCloseImageView;
    MainApplication mainApplication;
    AdView mAdView;
    CheckServiceRunning checkServiceRunning;
    Channel currentChannel;
    ChannelsData channelsData;
    SlideUp slideUp, slideUpDetails, slideUpInLandscape;
    int count;
    TextView tvToolbarTitle;
    int onAirEpgPosition, tabPos;
    String category;
    int userID;
    int id;
    String AUTHORIZATION;
    Realm realm;
    Boolean isEpg;
    InternetConnectionDetector detector;
    Boolean isInternet;
    private boolean ACTIVITY_IN_LANDSCAPE = false;
    private boolean ACTIVITY_IN_PORTRAIT = true;
    //    @BindView(R.id.videoPlayerPager)
//    ViewPager videoPlayerPager;
    private PlaybackState mPlaybackState;
    private PlaybackLocation mLocation;
    private MenuItem mediaRouteMenuItem;
    private Timer mControllersTimer;
    private boolean mControllersVisible;
    private String channelLink;
    private Bundle bundle;
    //    public static List<EPG> epgList;
//    public static HashMap<String, List<EpgTokenList>> epgHash;
    private TwoWayViewAdapter twoWayAdapter;
    private List<Category> genreWiseChannelList;
    private SetupRetrofit setupRetrofit;
    private RetainFragment mRetainedFragment;
    private String secret_key;

    public static void start(Context context, boolean shouldStart, List<Channel> channelList, ChannelsData channelsData, int tabPos) {
        Intent starter = new Intent(context, ChannelPlayingActivity.class);
//        starter.putExtra("position", position);
        starter.putExtra("shouldStart", shouldStart);
        starter.putExtra("channel-datas", channelsData);
        starter.putExtra("tab-pos", tabPos);
        starter.putParcelableArrayListExtra("channel-list", (ArrayList<? extends Parcelable>) channelList);
        context.startActivity(starter);
    }

    private static final String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest
                    .getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_playing);

        int colors = R.color.movies_color;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, colors));
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, colors));
        }
        setupRetrofit = new SetupRetrofit();
        ButterKnife.bind(this);

        detector = new InternetConnectionDetector(getApplicationContext());
        isInternet = detector.isConnectingToInternet();

        checkServiceRunning = new CheckServiceRunning(getApplicationContext());
        if (checkServiceRunning.isMyServiceRunning(BackgroundAudioService.class)) {
            Log.d(TAG, "onCreateView: audio service runnning");
            Intent backgroundAudioIntent = new Intent(this, BackgroundAudioService.class);
            stopService(backgroundAudioIntent);
        }
        if (checkServiceRunning.isMyServiceRunning(AudioNotification.class)) {
            Intent backgroundAudioIntent = new Intent(this, AudioNotification.class);
            stopService(backgroundAudioIntent);
            AudioPlayerActivity.exoPlayer.setPlayWhenReady(false);
        }

        realm = Realm.getDefaultInstance();

        User user = RealmRead.findUser(realm);

        id = user.getId();
        AUTHORIZATION = "Bearer " + user.getToken();

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        slideUp = new SlideUp.Builder(slideView)
                .withListeners(new SlideUp.Listener() {
                    @Override
                    public void onSlide(float percent) {
                        dim.setAlpha(1 - (percent / 100));
                    }

                    @Override
                    public void onVisibilityChanged(int visibility) {
                        if (visibility == View.GONE) {

                        }
                    }
                })
                .withStartGravity(Gravity.END)
                .withLoggingEnabled(true)
                .withStartState(SlideUp.State.HIDDEN)
                .build();

        slideUpInLandscape = new SlideUp.Builder(epgInLandscapeView)
                .withListeners(new SlideUp.Listener() {
                    @Override
                    public void onSlide(float percent) {
                        dim.setAlpha(1 - (percent / 100));
                    }

                    @Override
                    public void onVisibilityChanged(int visibility) {
                        if (visibility == View.GONE) {

                        }
                    }
                })
                .withStartGravity(Gravity.END)
                .withLoggingEnabled(true)
                .withStartState(SlideUp.State.HIDDEN)
                .build();

        bundle = getIntent().getExtras();
        getIntent().setExtrasClassLoader(Channel.class.getClassLoader());
//        position = bundle.getInt("position");
        tabPos = getIntent().getIntExtra("tab-pos", 0);
        count = position;
        channelsData = getIntent().getParcelableExtra("channel-datas");
        channelList = bundle.getParcelableArrayList("channel-list");
//        getStreamingUrl(AUTHORIZATION, channelList.get(position).getStreamUrl());


        secret_key = MainActivity.secretkey;


        /*twoWayAdapter = new TwoWayViewAdapter(this, R.layout.grid_item, channelList);
        twoWayView.setAdapter(twoWayAdapter);
        */
        twoWayView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Timber.d("position %s", i);
                int h1 = twoWayView.getWidth();
                int h2 = view.getWidth();
                twoWayView.smoothScrollToPositionFromOffset(i, h1 / 2 - h2 / 2, 2500);
//                twoWayView.smoothScrollToPositionFromOffset(i, twoWayView.getWidth() / 2, 2000);
                twoWayAdapter.setItemPosition(i);
                twoWayAdapter.notifyDataSetChanged();
                getChannelUrl(channelList.get(i), i);
//                mRetainedFragment.setViewPagerPos(i);
//                Toast.makeText(ChannelPlayingActivity.this, "land"+i, Toast.LENGTH_SHORT).show();

//                setImmediateChannelInfos(i);
                count = i;
            }
        });
//        videoPlayerPager.setAdapter(new VideoPlayerPagerAdapter(getSupportFragmentManager(),channelList));

        currentChannel = channelList.get(position);
        tvToolbarTitle = (TextView) toolbar.findViewById(R.id.toolbarTitle);
        setCurrentChannelInfos(currentChannel);

        mainApplication = (MainApplication) getApplicationContext();
        Calendar calendar = Calendar.getInstance();
        int index = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int nextIndex = 3;

        try {
            category = getIntent().getStringExtra("category");

        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        // find the retained fragment on activity restarts
        FragmentManager fm = getSupportFragmentManager();
        mRetainedFragment = (RetainFragment) fm.findFragmentByTag(TAG_RETAINED_FRAGMENT);

        twoWayAdapter = new TwoWayViewAdapter(ChannelPlayingActivity.this, R.layout.grid_item, channelList);
        twoWayView.setAdapter(twoWayAdapter);
        for (int i = 0; i < channelList.size(); i++) {
            if (channelList.get(i).getId() == currentChannel.getId()) {
                twoWayAdapter.setItemPosition(i);
                count = i;
            }
        }

        // create the fragment and data the first time
        if (mRetainedFragment == null) {
            // add the fragment
            mRetainedFragment = new RetainFragment();
            fm.beginTransaction().add(mRetainedFragment, TAG_RETAINED_FRAGMENT).commit();
            // load data from a data source or perform any calculation
//            mRetainedFragment.setCurrentChannel(currentChannel);
//            getChannelUrl(currentChannel);
//            getGenreAndChannels(userID);
//            mRetainedFragment.setData(loadMyData());
            mRetainedFragment.setChannelList(channelList);
            mRetainedFragment.setChannelCategories(channelsData.getCategories());

//            getChannelUrl(currentChannel, position);
//            Toast.makeText(this, "reta"+position, Toast.LENGTH_SHORT).show();

            viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), mRetainedFragment.getChannelCategories());
            viewPager.setAdapter(viewPagerAdapter);
            viewPager.setCurrentItem(tabPos);
            viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
            tabLayout.setupWithViewPager(viewPager);
        } else {
            Timber.d("motherfucker link %s", mRetainedFragment.getChannelLink());
            currentChannel = mRetainedFragment.getCurrentChannel();
            Timber.d("retained current channel %s", currentChannel.getName());
//            loadViewUrl(mRetainedFragment.getChannelLink(), 2500);
            position = mRetainedFragment.getViewPagerPos();
//            getChannelUrl(currentChannel, mRetainedFragment.getViewPagerPos());

            viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), mRetainedFragment.getChannelCategories());
            viewPager.setAdapter(viewPagerAdapter);
            viewPager.setCurrentItem(tabPos);
            viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
            tabLayout.setupWithViewPager(viewPager);

//            genreWiseChannelList = mRetainedFragment.getGenreWiseList();
           /* final List<Genre> genreList = new ArrayList<Genre>();
            for (int i = 0; i < mRetainedFragment.getGenreWiseChannelList().size(); i++) {
                genreList.add(mRetainedFragment.getGenreWiseChannelList().get(i).getGenre());
            }*/
            channelList = (ArrayList<Channel>) mRetainedFragment.getChannelList();
            Timber.e("1st channel retain %s", channelList.get(position).getName());
//            setCurrentChannelInfos(currentChannel);
//            Toast.makeText(this, "poss"+mRetainedFragment.getViewPagerPos(), Toast.LENGTH_SHORT).show();

            setImmediateChannelInfos(mRetainedFragment.getViewPagerPos());
            twoWayAdapter = new TwoWayViewAdapter(ChannelPlayingActivity.this, R.layout.grid_item, channelList);
            twoWayView.setAdapter(twoWayAdapter);
            for (int i = 0; i < channelList.size(); i++) {
                if (channelList.get(i).getId() == currentChannel.getId()) {
                    twoWayAdapter.setItemPosition(i);
                    count = i;
                }
            }

            twoWayAdapter.notifyDataSetChanged();
            bListSelector.setText(mRetainedFragment.getChannelCategories().get(tabPos).getName());
            lvCategory.setVisibility(View.INVISIBLE);
            lvCategory.setAdapter(new SimpleListAdapter(ChannelPlayingActivity.this, R.layout.simple_list, channelsData.getCategories()));
            lvCategory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                    channelList = (ArrayList<Channel>) mRetainedFragment.getChannelCategories().get(0).getChannels();
                    bListSelector.setText(channelsData.getCategories().get(i).getName());
                    lvCategory.setVisibility(View.INVISIBLE);
                    ivUpDownArrow.setImageDrawable(ContextCompat.getDrawable(ChannelPlayingActivity.this, R.drawable.ic_keyboard_arrow_up_white_24px));
                    channelList = (ArrayList<Channel>) mRetainedFragment.getChannelCategories().get(i).getChannels();
                    mRetainedFragment.setChannelList(channelList);
                    twoWayAdapter = new TwoWayViewAdapter(ChannelPlayingActivity.this, R.layout.grid_item, channelList);
                    twoWayView.setAdapter(twoWayAdapter);
                    count = 0;
                }
            });
        }

//        getChannelUrl(channelList.get(position),"channel");
//        Toast.makeText(this, "tala"+position+"==========="+mRetainedFragment.getViewPagerPos(), Toast.LENGTH_SHORT).show();
//        setImmediateChannelInfos(position);

        ivPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swipePrevious();

            }
        });

        ivNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swipeNext();

            }
        });

        mLoading.setVisibility(View.VISIBLE);

        ivEpg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isEpg) {
                    if (ACTIVITY_IN_PORTRAIT) {
                        slideUp.show();
                    } else {
                        slideUpInLandscape.show();
                    }
                } else {
                    Toast.makeText(ChannelPlayingActivity.this, "EPG not available.", Toast.LENGTH_SHORT).show();
                }
            }
        });


        bListSelector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (lvCategory.getVisibility() == View.INVISIBLE) {
                    lvCategory.setVisibility(View.VISIBLE);
                    ivUpDownArrow.setImageDrawable(ContextCompat.getDrawable(ChannelPlayingActivity.this, R.drawable.ic_keyboard_arrow_down_white_24px));
                } else {
                    lvCategory.setVisibility(View.INVISIBLE);
                    ivUpDownArrow.setImageDrawable(ContextCompat.getDrawable(ChannelPlayingActivity.this, R.drawable.ic_keyboard_arrow_up_white_24px));
                }
            }
        });


        epgCloseImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ACTIVITY_IN_PORTRAIT) {
                    slideUp.hide();
                } else {
                    slideUpInLandscape.hide();
                }
            }
        });

        imageViewHq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInternet) {
                    if (STREAM_TYPE.equalsIgnoreCase("sd")) {
                        STREAM_TYPE = "hq";
                        getTimeStamp(currentChannel.getHqStreamUrl());
                    } else {
                        STREAM_TYPE = "sd";
                        getTimeStamp(currentChannel.getStreamUrl());
                    }
                    mLoading.setVisibility(View.VISIBLE);
                } else {
                    detector.showNoInternetAlertDialog(ChannelPlayingActivity.this);
                }
            }
        });


        mFullScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {                    //To fullscreen
                    ACTIVITY_IN_PORTRAIT = true;
                    ACTIVITY_IN_LANDSCAPE = false;
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    mFullScreen.setImageResource(R.drawable.minimize);
                    getSupportActionBar().show();
                    adLinlay.setVisibility(View.VISIBLE);
                    nitvAdImageView.setVisibility(View.VISIBLE);
                    bListSelector.setVisibility(View.VISIBLE);
                } else {
                    ACTIVITY_IN_PORTRAIT = false;
                    ACTIVITY_IN_LANDSCAPE = true;
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    getSupportActionBar().hide();
                    mFullScreen.setImageResource(R.drawable.maximize);
                    adLinlay.setVisibility(View.INVISIBLE);
                    nitvAdImageView.setVisibility(View.INVISIBLE);
                    bListSelector.setVisibility(View.INVISIBLE);
                }
            }
        });

//        getChannelEPG(AUTHORIZATION, LinkConfig.BASE_URL + "api/channels/epg/" + currentChannel.getId());
        if (isInternet) {
            if (currentChannel.getEpg().equalsIgnoreCase("")) {
                isEpg = false;
            } else {
                getChannelEPG(AUTHORIZATION, currentChannel.getEpg());

            }
        } else {
            detector.showNoInternetAlertDialog(ChannelPlayingActivity.this);
        }


        ivDvr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!currentChannel.getDrvUrl().equalsIgnoreCase("")) {
                    Intent intent = new Intent(ChannelPlayingActivity.this, DvrActivity.class);
                    intent.putExtra("channel", currentChannel);
                    startActivity(intent);
                } else {
                    Toast.makeText(ChannelPlayingActivity.this, "DVR not Available", Toast.LENGTH_SHORT).show();
                }
            }
        });

        landEPGCloseImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ACTIVITY_IN_LANDSCAPE) {
                    slideUpInLandscape.hide();
                }
            }
        });
        /*if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {//To fullscreen
            ACTIVITY_IN_PORTRAIT = false;
            ACTIVITY_IN_LANDSCAPE = true;
            setActivityToFullScreen();
            mFullScreen.setImageResource(R.drawable.normal_screen);
            mRetainedFragment.setVideoPosition(mVideoView.getCurrentPosition());
            channelLink = mRetainedFragment.getChannelLink();
            if (channelLink == null) {
                getChannelUrl(currentChannel);
            }
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            ACTIVITY_IN_PORTRAIT = true;
            ACTIVITY_IN_LANDSCAPE = false;
            Timber.d("ooo");
            exitActivityFromFullScreen();
            mFullScreen.setImageResource(R.drawable.maximize);
            channelLink = mRetainedFragment.getChannelLink();
            if (channelLink == null) {
                getChannelUrl(currentChannel);
            }
        }*/


    }

    public String getTimeStamp(final String streamUrl) {
        /*mLoading.setVisibility(View.VISIBLE);
        mLoading.bringToFront();
        final String streamingUrl = streamUrl;*/

        Retrofit retrofit = setupRetrofit.getRetrofit(LinkConfig.BASE_URL);
        MovieApiInterface movieApiInterface = retrofit.create(MovieApiInterface.class);

        Observable<Response<TimeStamp>> observable = movieApiInterface.getTimeStamp(AUTHORIZATION);
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).unsubscribeOn(Schedulers.io())
                .subscribe(new Observer<Response<TimeStamp>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Response<TimeStamp> value) {
                        int responseCode = value.code();
                        if (responseCode == 200) {
                            String hash = md5(secret_key + value.body().getTimestamp());
                            Log.d(TAG, "onNext: md5 hash " + hash);
                            String main_url = streamUrl + "?hash=" + hash + "&utc=" + value.body().getTimestamp();
                            Log.d(TAG, "onNext: main_url " + main_url);

                            //Toast.makeText(getContext(), "onNext: main_url " + main_url, Toast.LENGTH_LONG).show();
                            getChannelMainUrl(main_url);
                        } else {

                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();

                    }

                    @Override
                    public void onComplete() {

                    }
                });
        return streamUrl;
    }

    public void getChannelMainUrl(String streamUrl) {
        Retrofit retrofit = setupRetrofit.getRetrofit(LinkConfig.BASE_URL);
        ChannelApiInterface channelApiInterface = retrofit.create(ChannelApiInterface.class);

        Observable<Response<Streaming>> observable = channelApiInterface.getStreamUrl(AUTHORIZATION, streamUrl);
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).unsubscribeOn(Schedulers.io())
                .subscribe(new Observer<Response<Streaming>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Response<Streaming> value) {
                        int responseCode = value.code();
                        if (responseCode == 200) {
//                            channelLink = value.body().getHref0();
                            mRetainedFragment.setChannelLink(value.body().getStream());
                            loadViewUrl(value.body().getStream());
//                            setCurrentChannelInfos(mRetainedFragment.getCurrentChannel());
//                            setImmediateChannelInfos(mRetainedFragment.getViewPagerPos());
                            if (STREAM_TYPE.equalsIgnoreCase("sd")) {
                                imageViewHq.setImageDrawable(getResources().getDrawable(R.drawable.hq));
                            } else {
                                imageViewHq.setImageDrawable(getResources().getDrawable(R.drawable.ic_sd));
                            }
                        } else {
                            mLoading.setVisibility(View.INVISIBLE);
                            if (STREAM_TYPE.equalsIgnoreCase("sd")) {
                                STREAM_TYPE = "hq";
                            } else {
                                STREAM_TYPE = "sd";
                            }
                            Snackbar.make(findViewById(android.R.id.content), value.headers().get("message"), Snackbar.LENGTH_SHORT).show();


                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        if (e instanceof HttpException) {
                            Snackbar.make(findViewById(android.R.id.content), "No Internet Connection", Snackbar.LENGTH_SHORT).show();
                        } else if (e instanceof UnknownHostException) {
                            Snackbar.make(findViewById(android.R.id.content), "Couldn't connect to server", Snackbar.LENGTH_SHORT).show();
                        } else if (e instanceof SocketTimeoutException) {
                            Snackbar.make(findViewById(android.R.id.content), "Connection timed out", Snackbar.LENGTH_SHORT).show();
                        } else {
                            Snackbar.make(findViewById(android.R.id.content), "Error Occured", Snackbar.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void setImmediateChannelInfos(int position) {
        int leftPosition = position - 1;
        int rightPosition = position + 1;
        if (leftPosition < 0) {
            leftPosition = 0;
            ivPrev.setImageResource(android.R.color.transparent);
        } else {
            try {
                Picasso.with(this).load(channelList.get(leftPosition).getLogo()).placeholder(R.drawable.placeholder_nt_square).into(ivPrev);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (rightPosition >= channelList.size()) {
            rightPosition = channelList.size();
            ivNext.setImageResource(android.R.color.transparent);
        } else {
            try {
                Picasso.with(this).load(channelList.get(rightPosition).getLogo()).placeholder(R.drawable.placeholder_nt_square).into(ivNext);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isInternet) {
            detector.showNoInternetAlertDialog(ChannelPlayingActivity.this);
        }
        mVideoView.start();

        if (!mVideoView.isPlaying()) {
            mLoading.setVisibility(View.VISIBLE);
        } else {
            mLoading.setVisibility(View.INVISIBLE);
        }
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {//To fullscreen
            ACTIVITY_IN_PORTRAIT = false;
            ACTIVITY_IN_LANDSCAPE = true;
            setActivityToFullScreen();
            mFullScreen.setImageResource(R.drawable.minimize);
//            mRetainedFragment.setVideoPosition(mVideoView.getCurrentPosition());
//            channelLink = mRetainedFragment.getChannelLink();
            getChannelUrl(currentChannel, position);
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            ACTIVITY_IN_PORTRAIT = true;
            ACTIVITY_IN_LANDSCAPE = false;
            Timber.d("ooo");
            exitActivityFromFullScreen();
            mFullScreen.setImageResource(R.drawable.maximize);
//            channelLink = mRetainedFragment.getChannelLink();
            getChannelUrl(currentChannel, position);
        }
    }

    public void setCurrentChannelInfos(final Channel currentChannel) {

        ivCurrentImage.setVisibility(View.VISIBLE);// yo ra talako remove garni bhaniyeko le, invisible banako ho
        tvCurrentProgram.setVisibility(View.VISIBLE);//
        if (!currentChannel.getLogo().equalsIgnoreCase("")) {
            Picasso.with(this).load(currentChannel.getLogo()).placeholder(R.drawable.placeholder_nt_square).into(ivCurrentImage);
        } else {
            Picasso.with(this).load(R.drawable.placeholder_nt_square).placeholder(R.drawable.placeholder_nt_square).into(ivCurrentImage);
        }
        tvToolbarTitle.setText(currentChannel.getName());

        tvCurrentProgram.setText(currentChannel.getName());

        //for epg in landscape mode
        if (!currentChannel.getLogo().equalsIgnoreCase("")) {
            Picasso.with(this).load(currentChannel.getLogo()).placeholder(R.drawable.placeholder_nt_square).into(ivCurrentChannelImageEpgLand);
        } else {
            Picasso.with(this).load(R.drawable.placeholder_nt_square).placeholder(R.drawable.placeholder_nt_square).into(ivCurrentChannelImageEpgLand);
        }
        tvCurrentChannelEpgLand.setText(currentChannel.getName());
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = df.format(c.getTime());
        Timber.d("value of date %s", formattedDate);
        tvCurrentDateEpgLand.setText(formattedDate);

        /*ivShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_SUBJECT, "World Tv Go");
                    String sAux = "\nI am watching " + currentChannel.getChannelName() + " on World Tv Go \n\n";
                    sAux = sAux + "https://play.google.com/store/apps/details?id=" + getPackageName() + " \n\n";
                    i.putExtra(Intent.EXTRA_TEXT, sAux);
                    startActivity(Intent.createChooser(i, "choose one"));
                } catch (Exception e) {
                    //e.toString();
                }
            }
        });*/
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Timber.d("onconfigurationchanged");

    }

    public void setChannelFromBundle(List<Channel> channelList) {
        int scrollPos = 0;
        for (int i = 0; i < channelList.size(); i++) {
            if (currentChannel.getId() == (channelList.get(i).getId())) {
                scrollPos = i;
                break;
            }
        }
        Timber.d("scrool to post %s", scrollPos);
//        LinearLayoutManager llm3 = new LinearLayoutManager(ChannelPlayingActivity.this, LinearLayoutManager.HORIZONTAL, false);
//        recyclerViewChannels.setLayoutManager(llm3);
//        recyclerViewChannels.setAdapter(new ChannelEpgAdapter(ChannelPlayingActivity.this, channelList, userID));
//        recyclerViewChannels.scrollToPosition(scrollPos);

        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = df.format(c.getTime());
        Timber.d("value of date %s", formattedDate);
        TimeZone tz = TimeZone.getDefault();
        Timber.d("time zone from id %s", TimeZone.getTimeZone(tz.getID()));
        System.out.println("TimeZone   " + tz.getDisplayName(false, TimeZone.SHORT) + " Timezon id :: " + tz.getID());

        if (currentChannel.getEpg().equalsIgnoreCase("")) {
            isEpg = false;
        } else {
            getChannelEPG(AUTHORIZATION, currentChannel.getEpg());

        }

    }

    public void getChannelUrl(final Channel currentChannel, int position) {
//        if (from.equalsIgnoreCase("grid")) {
//            this.currentChannel = currentChannel;
//            setChannelFromBundle(channelList);
//            mRetainedFragment.setChannelList(channelList);
//        }
        setCurrentChannelInfos(currentChannel);
        setImmediateChannelInfos(position);
        this.currentChannel = currentChannel;
        Timber.d("current channel %s", currentChannel.getName());
        mRetainedFragment.setCurrentChannel(currentChannel);
//        mRetainedFragment.setChannelList(channelList);
        mRetainedFragment.setViewPagerPos(position);
        updatePlayButton(PlaybackState.BUFFERING);
        mPlayPause.setImageDrawable(
                getResources().getDrawable(R.drawable.ic_av_pause_dark));

        mLoading.bringToFront();
        getTimeStamp(currentChannel.getStreamUrl());


//        String as = "{" + "channel" + "-" + "id" + "}";
//        String newUrl = url.replace(as, currentChannel.getId() + "");
//        Timber.d("new url %s", newUrl);


        /*Observable<Response<Links>> observable = channelApiInterface.getChannelUrl(url,STREAM_TYPE);
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).unsubscribeOn(Schedulers.io())
                .subscribe(new Observer<Response<Links>>() {
                    @Override
                    public void onNext(Response<Links> value) {
                        int responseCode = value.code();
                        if (responseCode == 200) {
                            channelLink = value.body().getHref();
//                            channelLink = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";
                            mRetainedFragment.setChannelLink(channelLink);
                            loadViewUrl(channelLink, 0);
                            setCurrentChannelInfos(currentChannel);
                        } else {

                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();

                    }

                    @Override
                    public void onComplete() {

                    }
                });*/
    }

    public void getStreamingUrl(String AUTHORIZATION, String url) {
        Log.d(TAG, "getStreamingUrl: ");
        Retrofit retrofit = setupRetrofit.getRetrofit(LinkConfig.BASE_URL);
        ChannelApiInterface channelApiInterface = retrofit.create(ChannelApiInterface.class);

        Observable<Response<Streaming>> observable = channelApiInterface.getStreamUrl(AUTHORIZATION, url);
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).unsubscribeOn(Schedulers.io())
                .subscribe(new Observer<Response<Streaming>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Response<Streaming> value) {
                        int responseCode = value.code();
                        if (responseCode == 200) {
                            mRetainedFragment.setChannelLink(value.body().getStream());
                            loadViewUrl(value.body().getStream());
//                        setCurrentChannelInfos(currentChannel);
                        } else {
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        if (e instanceof HttpException) {
                            Snackbar.make(findViewById(android.R.id.content), "No Internet Connection", Snackbar.LENGTH_SHORT).show();
                        } else if (e instanceof UnknownHostException) {
                            Snackbar.make(findViewById(android.R.id.content), "Couldn't connect to server", Snackbar.LENGTH_SHORT).show();
                        } else if (e instanceof SocketTimeoutException) {
                            Snackbar.make(findViewById(android.R.id.content), "Connection timed out", Snackbar.LENGTH_SHORT).show();
                        } else {
                            Snackbar.make(findViewById(android.R.id.content), "Error Occured", Snackbar.LENGTH_SHORT).show();
                        }

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void getChannelEPG(String authorization, String url) {
        Retrofit retrofit = setupRetrofit.getRetrofit(LinkConfig.BASE_URL);
        ChannelApiInterface channelApiInterface = retrofit.create(ChannelApiInterface.class);
        Observable<Response<List<EPG>>> observable = channelApiInterface.getChannelEPG(authorization, url);
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).unsubscribeOn(Schedulers.io())
                .subscribe(new Observer<Response<List<EPG>>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(Response<List<EPG>> value) {
                        int responseCode = value.code();
                        if (responseCode == 200) {
                            final List<EPG> epgs = value.body();
                            Timber.d("value of time %s", epgs.get(0));

                            epgKey = new ArrayList<String>();
                            epgHash = new HashMap<>();
                            for (EPG epg : epgs) {
                                epgKey.add(epg.getDay());
                                epgHash.put(epg.getDay(), epg.getEpgTokenList());
                            }
                            Calendar calendar = Calendar.getInstance();
                            Date date = calendar.getTime();
                            String newdate = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(date);
                            int onEpgDaysPosition = calendar.get(Calendar.DAY_OF_WEEK) - 1;
                            if (epgKey.contains(newdate)) {
                                onAirEpgPosition = getpositionofOnAir(epgHash.get(newdate));
//                            Timber.d("current program %s", epgs.get(onEpgDaysPosition).getEpgTokenList().get(onAirEpgPosition).getProgramName());
//                            tvCurrentProgram.setText("Now Playing : " + epgs.get(onEpgDaysPosition).getEpgTokenList().get(onAirEpgPosition).getProgramName());
                                LinearLayoutManager llm1 = new LinearLayoutManager(ChannelPlayingActivity.this, LinearLayoutManager.VERTICAL, false);
                                LinearLayoutManager llm2 = new LinearLayoutManager(ChannelPlayingActivity.this, LinearLayoutManager.VERTICAL, false);
                                recyclerViewDays.setLayoutManager(llm1);
                                recyclerViewEpg.setLayoutManager(llm2);
                                recyclerViewDays.setAdapter(new EpgDaysRecyclerViewAdapter(ChannelPlayingActivity.this, epgHash, epgKey, recyclerViewEpg, onEpgDaysPosition, onAirEpgPosition));

                                EpgDvrRecyclerViewAdapter epgDvrRecyclerViewAdapter = new EpgDvrRecyclerViewAdapter(ChannelPlayingActivity.this, epgHash, newdate, onAirEpgPosition);
                                recyclerViewEpg.setAdapter(epgDvrRecyclerViewAdapter);
                                recyclerViewEpg.smoothScrollToPosition(onAirEpgPosition);
                                recyclerViewEpg.setSelected(true);
                                epgDvrRecyclerViewAdapter.notifyDataSetChanged();
//                            menuList.setAdapter(new EpgListAdapter(ChannelPlayingActivity.this,value.body(),value.body()));
                                lvDays.setAdapter(new EpgLandDaysListAdapter(ChannelPlayingActivity.this, R.layout.simple_list, epgs));
                                LinearLayoutManager llm3 = new LinearLayoutManager(ChannelPlayingActivity.this, LinearLayoutManager.VERTICAL, false);
                                rvEpgGuide.setLayoutManager(llm3);
                                rvEpgGuide.setAdapter(new EpgLandRecyclerViewAdapter(ChannelPlayingActivity.this, epgHash, newdate, onAirEpgPosition));
                                bListEpgDaysSelector.setText(newdate);
                                llEpgSelector.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (lvDays.getVisibility() == View.INVISIBLE) {
                                            lvDays.setVisibility(View.VISIBLE);
                                            epgUpDownArrow.setImageDrawable(ContextCompat.getDrawable(ChannelPlayingActivity.this, R.drawable.ic_keyboard_arrow_down_white_24px));
                                        } else {
                                            lvDays.setVisibility(View.INVISIBLE);
                                            epgUpDownArrow.setImageDrawable(ContextCompat.getDrawable(ChannelPlayingActivity.this, R.drawable.ic_keyboard_arrow_up_white_24px));
                                        }
                                    }
                                });
                                lvDays.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                        bListEpgDaysSelector.setText(epgs.get(i).getDay());
                                        if (lvDays.getVisibility() == View.INVISIBLE) {
                                            lvDays.setVisibility(View.VISIBLE);
                                            epgUpDownArrow.setImageDrawable(ContextCompat.getDrawable(ChannelPlayingActivity.this, R.drawable.ic_keyboard_arrow_down_white_24px));
                                        } else {
                                            lvDays.setVisibility(View.INVISIBLE);
                                            epgUpDownArrow.setImageDrawable(ContextCompat.getDrawable(ChannelPlayingActivity.this, R.drawable.ic_keyboard_arrow_up_white_24px));
                                        }
                                        rvEpgGuide.setAdapter(new EpgLandRecyclerViewAdapter(ChannelPlayingActivity.this, epgHash, epgs.get(i).getDay(), onAirEpgPosition));
                                    }
                                });
                                tvOnAirTime.setText(epgs.get(0).getEpgTokenList().get(onAirEpgPosition).getTimeFrom() + " - " + epgs.get(0).getEpgTokenList().get(onAirEpgPosition).getTimeTo());
                                tvOnAirName.setText(epgs.get(0).getEpgTokenList().get(onAirEpgPosition).getProgramName());
                                if (onAirEpgPosition++ > epgs.get(0).getEpgTokenList().size()) {
                                    tvUpNextTime.setText("-");
                                    tvUpNextName.setText("-");
                                } else {
                                    tvUpNextTime.setText(epgs.get(0).getEpgTokenList().get(onAirEpgPosition).getTimeFrom() + " - " + epgs.get(0).getEpgTokenList().get(onAirEpgPosition).getTimeTo());
                                    tvUpNextName.setText(epgs.get(0).getEpgTokenList().get(onAirEpgPosition).getProgramName());
                                }

                                isEpg = true;
                            } else {
                                isEpg = false;
                            }
                        } else {
                            isEpg = false;

                            Toast.makeText(ChannelPlayingActivity.this, "No EPG Found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        if (e instanceof HttpException) {
                            Snackbar.make(findViewById(android.R.id.content), "No Internet Connection", Snackbar.LENGTH_SHORT).show();
                        } else if (e instanceof UnknownHostException) {
                            Snackbar.make(findViewById(android.R.id.content), "Couldn't connect to server", Snackbar.LENGTH_SHORT).show();
                        } else if (e instanceof SocketTimeoutException) {
                            Snackbar.make(findViewById(android.R.id.content), "Connection timed out", Snackbar.LENGTH_SHORT).show();
                        } else {
                            Snackbar.make(findViewById(android.R.id.content), "Error Occured", Snackbar.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(CurrentChannels currentChannels) {
        ArrayList<Channel> channelList = currentChannels.getChannelArrayList();
        position = currentChannels.getPosition();
//        Toast.makeText(this, "grid"+position, Toast.LENGTH_SHORT).show();
        setImmediateChannelInfos(position);

        mRetainedFragment.setCurrentChannel(channelList.get(position));
        mRetainedFragment.setChannelList(channelList);
//        mRetainedFragment.setViewPagerPos(position);
        mRetainedFragment.setCurrentChannels(currentChannels);
        Timber.d("1st channel %s", channelList.get(0).getName());
    }

    private int getpositionofOnAir(List<EpgTokenList> epgs) {
        Log.e(TAG, "getpositionofOnAir: " + epgs);
        SimpleDateFormat sdf = new SimpleDateFormat("HHmm");
        int s = Integer.parseInt(sdf.format(new Date()));
        for (int i = 0; i < epgs.size(); i++) {

            EpgTokenList epg = epgs.get(i);
            if (Integer.parseInt(epg.getTimeFrom().replace(":", "")) <= s && s <= Integer.parseInt(epg.getTimeTo().replace(":", ""))) {
                return i;
            }
        }
        return 0;
    }

    /*public void getAds(int channelId) {
        Retrofit retrofit = ApiManager.getAdapter();
        final ChannelApiInterface channelApiInterface = retrofit.create(ChannelApiInterface.class);


        Observable<Response<AdUnits>> observable = channelApiInterface.getAdUnits(channelId, 1);
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).unsubscribeOn(Schedulers.io())
                .subscribe(new Observer<Response<AdUnits>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Response<AdUnits> value) {
                        int responseCode = value.code();
                        if (responseCode == 200) {
                            try {

                                mainApplication = (MainApplication) getApplicationContext();

                                nitvPriority = value.body().getNitvAd().getPriority();
                                admobPriority = value.body().getAdmob().getPriority();
                                nitvBannerStatus = value.body().getNitvAd().getBanner().getStatus();
                                nitvFullStatus = value.body().getNitvAd().getInterstitial().getStatus();
                                admobBannerStatus = value.body().getAdmob().getBanner().getStatus();
                                admobFullStatus = value.body().getAdmob().getInterstitial().getStatus();
                                adCodeAdMobBanner = value.body().getAdmob().getBanner().getName();
                                adCodeAdMobFull = value.body().getAdmob().getInterstitial().getName();
                                adCodeNitvBanner = value.body().getNitvAd().getBanner().getName();
                                adCodeNitvFull = value.body().getNitvAd().getInterstitial().getName();

                                try {

                                    if (nitvBannerStatus && admobBannerStatus) {
                                        if (nitvPriority < admobPriority) {
                                            mainApplication.loadNitvBannerAd(ChannelPlayingActivity.this, adLinlay, adCodeNitvBanner, adCodeAdMobBanner, nitvAdImageView);
                                            mainApplication.setNitvBannerAd(true);
                                            mainApplication.setAdMobBannerAd(false);
                                        } else {
                                            mAdView = mainApplication.loadAdMobBannerAd(ChannelPlayingActivity.this, adLinlay, adCodeAdMobBanner);
                                            mainApplication.setAdMobBannerAd(true);
                                            mainApplication.setNitvBannerAd(false);
                                        }
                                    } else if (nitvBannerStatus && !admobBannerStatus) {
                                        mainApplication.loadNitvBannerAd(ChannelPlayingActivity.this, adLinlay, adCodeNitvBanner, adCodeAdMobBanner, nitvAdImageView);
                                        mainApplication.setNitvBannerAd(true);
                                        mainApplication.setAdMobBannerAd(false);
                                    } else if (!nitvBannerStatus && admobBannerStatus) {
                                        mAdView = mainApplication.loadAdMobBannerAd(ChannelPlayingActivity.this, adLinlay, adCodeAdMobBanner);
                                        mainApplication.setAdMobBannerAd(true);
                                        mainApplication.setNitvBannerAd(false);
                                    }

                                    if (nitvFullStatus && admobFullStatus) {
                                        if (nitvPriority < admobPriority) {
                                            mainApplication.setNitvFullAd(true);
                                            mainApplication.setAdMobFullAd(false);
                                        } else {
                                            mainApplication.setAdMobFullAd(true);
                                            mainApplication.setNitvFullAd(false);
                                        }
                                    } else if (nitvBannerStatus && !admobBannerStatus) {
                                        mainApplication.setNitvFullAd(true);
                                        mainApplication.setAdMobFullAd(false);
                                    } else if (!nitvBannerStatus && admobBannerStatus) {
                                        mainApplication.setAdMobFullAd(true);
                                        mainApplication.setNitvFullAd(false);
                                    }


                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                if (mainApplication.isNitvBannerAd()) {
                                    mainApplication.loadNitvBannerAd(ChannelPlayingActivity.this, adLinlay, value.body().getNitvAd().getBanner().getName(), value.body().getAdmob().getBanner().getName(), nitvAdImageView);
                                } else {
                                    mainApplication.loadAdMobBannerAd(ChannelPlayingActivity.this, adLinlay, value.body().getAdmob().getBanner().getName());
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {

                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }*/

    private void loadViewUrl(String channelLink) {
        if (bundle != null) {

//            channelLink = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";

            boolean shouldStartPlayback = bundle.getBoolean("shouldStart");
            int startPosition = bundle.getInt("startPosition", 0);
            try {
                mVideoView.setVideoURI(Uri.parse(channelLink));
            } catch (Exception e) {
                e.printStackTrace();
            }
            mPlaybackState = PlaybackState.PLAYING;
            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                        @Override
                        public void onVideoSizeChanged(MediaPlayer mediaPlayer, int i, int i1) {
                            mLoading.setVisibility(View.GONE);
                        }
                    });
                    mVideoView.start();
                    mPlayPause.setVisibility(View.VISIBLE);

                }
            });

            mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                    mLoading.setVisibility(View.INVISIBLE);
//                    errorTextView.setText(getResources().getString(R.string.error_msg_video));
                    return true;
                }
            });

            mVideoView.setOnTouchListener(new OnSwipeTouchListener(this) {
                @Override
                public void onSwipeRight() {
//                Toast.makeText(MainActivity.this, "Right", Toast.LENGTH_SHORT).show();
                    Timber.d("value of position right before %s", position);
                    swipePrevious();
                }

                @Override
                public void onSwipeLeft() {
//                Toast.makeText(MainActivity.this, "Left", Toast.LENGTH_SHORT).show();
                    swipeNext();
                }

                @Override
                public void onCenterTouch() {
                    /*if(mControllers.getVisibility()==View.VISIBLE){
                        mControllers.setVisibility(View.INVISIBLE);
                    }else {
                        mControllers.setVisibility(View.VISIBLE);
                    }*/
//                    lvCategory.setVisibility(View.VISIBLE);
                    if (!mControllersVisible) {
                        updateControllersVisibility(true);
                    }
                    startControllersTimer();
                }



                /*@Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(mControllers.getVisibility()==View.VISIBLE){
                        mControllers.setVisibility(View.INVISIBLE);
                    }else {
                        mControllers.setVisibility(View.VISIBLE);
                    }
                    return false;
                }*/


            });
            mPlayPause.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    togglePlayback();
                }
            });
        }

    }

    public void swipeNext() {
        count++;
        if (count > channelList.size() - 1) {
            count = channelList.size() - 1;
        } else {
            Timber.d("value of count left %s", count);

            currentChannel = channelList.get(count);
            AnimationUtil animationUtil = new AnimationUtil();
            animationUtil.SlideRight(mVideoView, ChannelPlayingActivity.this);
            getChannelUrl(currentChannel, count);
            tvToolbarTitle.setText(currentChannel.getName());
            setCurrentChannelInfos(currentChannel);
            setImmediateChannelInfos(count);
//                Timber.d("position in next %s",position);
            if (ACTIVITY_IN_LANDSCAPE) {
                twoWayAdapter.setItemPosition(count);
//                Timber.d("position in next after %s",position);
                twoWayAdapter.notifyDataSetChanged();
            }
            mPlayPause.setVisibility(View.INVISIBLE);
            mRetainedFragment.setCurrentChannel(currentChannel);
            TimeZone timeZone = TimeZone.getDefault();
            Timber.d("time zone %s", timeZone.getDisplayName(false, TimeZone.SHORT));

        }
    }

    public void swipePrevious() {
        count--;
        if (count < 0) {
            count = 0;
        } else {
            Timber.d("value of count right %s", count);

            currentChannel = channelList.get(count);
            AnimationUtil animationUtil = new AnimationUtil();
            animationUtil.SlideLeft(mVideoView, ChannelPlayingActivity.this);
            getChannelUrl(currentChannel, count);
            tvToolbarTitle.setText(currentChannel.getName());
            setCurrentChannelInfos(currentChannel);
            setImmediateChannelInfos(count);
//                    Timber.d("position in next %s", position);
            if (ACTIVITY_IN_LANDSCAPE) {
                twoWayAdapter.setItemPosition(count);
//                    Timber.d("position in next after %s", position);
                twoWayAdapter.notifyDataSetChanged();
            }
            mPlayPause.setVisibility(View.INVISIBLE);
            mRetainedFragment.setCurrentChannel(currentChannel);
        }
    }

    private void togglePlayback() {
        stopControllersTimer();
        switch (mPlaybackState) {
            case PAUSED:
                mVideoView.start();
                Timber.d(TAG, "Playing locally...");
                mPlaybackState = PlaybackState.PLAYING;
                startControllersTimer();
                updatePlaybackLocation(PlaybackLocation.LOCAL);
                break;
            case PLAYING:
                mPlaybackState = PlaybackState.PAUSED;
                mVideoView.pause();
                break;
            case IDLE:
                mVideoView.setVideoURI(Uri.parse(channelLink));
                mVideoView.seekTo(0);
                mVideoView.start();
                mPlaybackState = PlaybackState.PLAYING;
                updatePlaybackLocation(PlaybackLocation.LOCAL);
                break;
            default:
                break;
        }
        updatePlayButton(mPlaybackState);
    }

    private void updatePlaybackLocation(PlaybackLocation location) {
        mLocation = location;
        if (mPlaybackState == PlaybackState.PLAYING
                || mPlaybackState == PlaybackState.BUFFERING) {
            startControllersTimer();
        } else {
            stopControllersTimer();
        }
    }

    private void updatePlayButton(PlaybackState state) {
        Timber.d(TAG, "Controls: PlayBackState: " + state);
//        boolean isConnected = (mCastSession != null) && (mCastSession.isConnected() || mCastSession.isConnecting());
        boolean isConnected = false;

        mControllers.setVisibility(isConnected ? View.GONE : View.VISIBLE);
        mPlayCircle.setVisibility(isConnected ? View.GONE : View.VISIBLE);
        switch (state) {
            case PLAYING:
                //mLoading.setVisibility(View.INVISIBLE);
                mControllers.setVisibility(View.GONE);
                mPlayPause.setVisibility(View.VISIBLE);
                mPlayPause.setImageDrawable(
                        getResources().getDrawable(R.drawable.ic_av_pause_dark));
                mPlayCircle.setVisibility(isConnected ? View.VISIBLE : View.GONE);
                break;
            case IDLE:
                mPlayCircle.setVisibility(View.VISIBLE);
                mControllers.setVisibility(View.GONE);
                mCoverArt.setVisibility(View.VISIBLE);
                mVideoView.setVisibility(View.INVISIBLE);
                break;
            case PAUSED:
                mLoading.setVisibility(View.INVISIBLE);
                mPlayPause.setVisibility(View.VISIBLE);
                mPlayPause.setImageDrawable(
                        getResources().getDrawable(R.drawable.ic_av_play_dark));
                mPlayCircle.setVisibility(isConnected ? View.VISIBLE : View.GONE);
                break;
            case BUFFERING:
                mPlayCircle.setVisibility(View.INVISIBLE);
                mPlayPause.setVisibility(View.INVISIBLE);
                mControllers.setVisibility(View.GONE);
                mLoading.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void stopControllersTimer() {
        if (mControllersTimer != null) {
            mControllersTimer.cancel();
        }
    }

    private void startControllersTimer() {
        if (mControllersTimer != null) {
            mControllersTimer.cancel();
        }
        if (mLocation == PlaybackLocation.REMOTE) {
            return;
        }
        mControllersTimer = new Timer();
        mControllersTimer.schedule(new HideControllersTask(), 5000);
    }

    private void updateControllersVisibility(boolean show) {
        if (show) {
//            getSupportActionBar().show();
//            mControllers.setVisibility(View.VISIBLE);
//            if(mLoading.getVisibility()==View.VISIBLE) {
//                mControllers.setVisibility(View.INVISIBLE);
//            }else {
//                mControllers.setVisibility(View.VISIBLE);
//            }
            if (mControllers.isShown() || mLoading.getVisibility() == View.VISIBLE) {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    getSupportActionBar().hide();
                }
                mControllers.setVisibility(View.INVISIBLE);
            } else {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    getSupportActionBar().hide();
                } else {
                    getSupportActionBar().show();
                }
                mControllers.setVisibility(View.VISIBLE);
                mControllers.requestLayout();
            }
        } else {
            if (!Utils.isOrientationPortrait(this)) {
                getSupportActionBar().hide();
            }
            mControllers.setVisibility(View.INVISIBLE);
        }
    }

    public void setActivityToFullScreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    public void exitActivityFromFullScreen() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    public enum PlaybackLocation {
        LOCAL,
        REMOTE
    }

    /**
     * List of various states that we can be in
     */
    public enum PlaybackState {
        PLAYING, PAUSED, BUFFERING, IDLE
    }

    private class HideControllersTask extends TimerTask {
        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateControllersVisibility(false);
                    mControllersVisible = false;
                }
            });
        }
    }

    public class ViewPagerAdapter extends FragmentStatePagerAdapter {
        List<Category> genreWiseChannels;

        public ViewPagerAdapter(FragmentManager fm, List<Category> genreWiseChannels) {
            super(fm);
            this.genreWiseChannels = genreWiseChannels;
        }

        @Override
        public Fragment getItem(int position) {
            Fragment newsTabFragment = new ChannelViewPagerFragment();
            Bundle basket = new Bundle();
            basket.putParcelableArrayList("channels_list", (ArrayList<? extends Parcelable>) genreWiseChannels.get(position).getChannels());
            newsTabFragment.setArguments(basket);
            return newsTabFragment;
        }

        @Override
        public int getCount() {
            return genreWiseChannels.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return genreWiseChannels.get(position).getName();

        }
    }

}
