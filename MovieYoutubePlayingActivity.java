package com.newitventure.ntentertainment.movie.playing;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.newitventure.ntentertainment.MainActivity;
import com.newitventure.ntentertainment.R;
import com.newitventure.ntentertainment.adapter.MovieHorizontalRecyclerViewAdapter;
import com.newitventure.ntentertainment.audio.audionotification.AudioNotification;
import com.newitventure.ntentertainment.audio.musichomefragment.audioplayeractivity.AudioPlayerActivity;
import com.newitventure.ntentertainment.entitynew.common.Streaming;
import com.newitventure.ntentertainment.entitynew.common.TimeStamp;
import com.newitventure.ntentertainment.entitynew.login.User;
import com.newitventure.ntentertainment.entitynew.movies.Movie;
import com.newitventure.ntentertainment.fmradio.BackgroundAudioService;
import com.newitventure.ntentertainment.movie.MovieApiInterface;
import com.newitventure.ntentertainment.realmOperations.RealmRead;
import com.newitventure.ntentertainment.utils.CheckServiceRunning;
import com.newitventure.ntentertainment.utils.InternetConnectionDetector;
import com.newitventure.ntentertainment.utils.LinkConfig;
import com.newitventure.ntentertainment.utils.SetupRetrofit;
import com.squareup.picasso.Picasso;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

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

public class MovieYoutubePlayingActivity extends AppCompatActivity {
    public static int position;
    final String TAG = getClass().getSimpleName();
    //    @BindView(R.id.youtube_movie)
    YouTubePlayerFragment youtube_movie;
    //    @BindView(R.id.movie_youtube_trailer)
    YouTubePlayerFragment movie_youtube_trailer;
    @BindView(R.id.youtube_relative)
    RelativeLayout youtube_relative;
    @BindView(R.id.watchLinear)
    LinearLayout watchLinear;
    @BindView(R.id.watch_movie)
    Button watch_movie;
    @BindView(R.id.watch_trailer)
    Button watch_trailer;
    @BindView(R.id.errorText)
    TextView errorText;
    @BindView(R.id.skip_trailer)
    Button skip_trailer;
    @BindView(R.id.coverArtView)
    ImageView mCoverArt;
    @BindView(R.id.coverArtBackground)
    ImageView mCoverArtBackground;
    @BindView(R.id.youtube_progress)
    ProgressBar youtube_progress;
    @BindView(R.id.movie_image)
    ImageView movie_image;
    @BindView(R.id.movie_genre)
    TextView movie_genre;
    @BindView(R.id.movie_title)
    TextView movie_title;
    @BindView(R.id.movie_year)
    TextView movie_year;
    @BindView(R.id.movie_rate)
    RatingBar movie_rate;
    @BindView(R.id.movie_duration)
    TextView movie_duration;
    @BindView(R.id.movie_vendor)
    TextView movie_vendor;
    @BindView(R.id.movie_watch)
    TextView movie_watch;
    @BindView(R.id.movie_synopsis_content)
    TextView movie_synopsis_content;
    //    @BindView(R.id.movie_cast_recycler)
//    RecyclerView recycler_cast_view_list;
    @BindView(R.id.morelikelist)
    RecyclerView recycler_view_list;
    @BindView(R.id.fragmentLinear)
    LinearLayout fragmentLinear;
    YouTubePlayer youTubePlayerTrailer;
    String videoId;
    ArrayList<Movie> movieList;
    Movie currentMovie;
    InternetConnectionDetector detector;
    Boolean isInternet;
    //    MovieHorizontalCastRecyclerViewAdapter horizontalCastRecyclerViewAdapter;
    MovieHorizontalRecyclerViewAdapter horizontalRecyclerViewAdapter;
    int id;
    String AUTHORIZATION;
    Realm realm;
    CheckServiceRunning checkServiceRunning;
    //    MovieData movieData;
    private SetupRetrofit setupRetrofit;
    private Bundle bundle;
    private String secret_key;

    public static void start(Context context, int position, boolean shouldStart, List<Movie> movieList) {
        Intent starter = new Intent(context, MovieYoutubePlayingActivity.class);
        starter.putExtra("position", position);
        starter.putExtra("shouldStart", shouldStart);
//        starter.putExtra("movies", movieList.get(position));
//        starter.putExtra("moviesData", movieData);
        starter.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        starter.putParcelableArrayListExtra("movie-list", (ArrayList<? extends Parcelable>) movieList);
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

    /*@Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
        if (!b) {
            this.youTubePlayer = youTubePlayer;
            youTubePlayer.loadVideo(videoId);
            youTubePlayer.setPlayerStyle(YouTubePlayer.PlayerStyle.DEFAULT);
            youTubePlayer.setShowFullscreenButton(true);
            youTubePlayer.setFullscreenControlFlags(YouTubePlayer.FULLSCREEN_FLAG_CONTROL_ORIENTATION | YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE);

            *//*youTubePlayerView.setOnTouchListener(new OnSwipeTouchListener(this) {
                @Override
                public void onSwipeRight() {
                    Toast.makeText(MovieYoutubePlayingActivity.this, "Right", Toast.LENGTH_SHORT).show();
                    Timber.d("Swipe Right", position);
                }

                @Override
                public void onSwipeLeft() {
                    Toast.makeText(MovieYoutubePlayingActivity.this, "Left", Toast.LENGTH_SHORT).show();
                    Timber.d("Swipe Left", position);
                }

                @Override
                public void onCenterTouch() {
                    Toast.makeText(MovieYoutubePlayingActivity.this, "Center", Toast.LENGTH_SHORT).show();
                    Timber.d("Swipe Center", position);
                }



                *//**//*@Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(mControllers.getVisibility()==View.VISIBLE){
                        mControllers.setVisibility(View.INVISIBLE);
                    }else {
                        mControllers.setVisibility(View.VISIBLE);
                    }
                    return false;
                }*//**//*


            });*//*

        }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
        if (youTubeInitializationResult.isUserRecoverableError()) {
            youTubeInitializationResult.getErrorDialog(this, 1).show();
        } else {
            Toast.makeText(this, "errorMessage", Toast.LENGTH_LONG).show();
        }
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_youtube_playing);
        ButterKnife.bind(this);

        checkServiceRunning = new CheckServiceRunning(getApplicationContext());
        if (checkServiceRunning.isMyServiceRunning(BackgroundAudioService.class)) {
            Log.d(TAG, "onCreateView: audio service runnning");
            Intent backgroundAudioIntent = new Intent(this, BackgroundAudioService.class);
            stopService(backgroundAudioIntent);
        }
        if (checkServiceRunning.isMyServiceRunning(AudioNotification.class)) {
            Intent intent = new Intent(this, AudioNotification.class);
            stopService(intent);
            AudioPlayerActivity.exoPlayer.setPlayWhenReady(false);
        }

        detector = new InternetConnectionDetector(getApplicationContext());
        isInternet = detector.isConnectingToInternet();
        secret_key = MainActivity.secretkey;

        realm = Realm.getDefaultInstance();

        User user = RealmRead.findUser(realm);

        id = user.getId();
        AUTHORIZATION = "Bearer " + user.getToken();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.movies_color));
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.movies_color));
        }

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {//To fullscreen
            setFullscreenLayout();
        } else {
            setPortraitLayout();
        }

        setupRetrofit = new SetupRetrofit();

        bundle = getIntent().getExtras();
        getIntent().setExtrasClassLoader(Movie.class.getClassLoader());
//        position = bundle.getInt("position");

        movieList = bundle.getParcelableArrayList("movie-list");

        currentMovie = movieList.get(position);
//        movieData = movieList.get(position);

//        videoId = "OAOSUjMFwsk";
//        Collections.shuffle(movieList);

        if (!movieList.get(position).getLogo().equalsIgnoreCase("")) {
            Picasso.with(MovieYoutubePlayingActivity.this).load(movieList.get(position).getLogo()).placeholder(R.drawable.placeholder_nt).into(movie_image);
        } else {
            Picasso.with(MovieYoutubePlayingActivity.this).load(R.drawable.placeholder_nt).placeholder(R.drawable.placeholder_nt).into(movie_image);
        }

        movie_genre.setText(movieList.get(position).getGenre());
        movie_title.setText(movieList.get(position).getName());
        movie_year.setText(movieList.get(position).getReleasedYear());
        movie_rate.setRating(movieList.get(position).getRating().floatValue());
        movie_duration.setText(movieList.get(position).getDuration().toString() + " mins");
        movie_vendor.setText(movieList.get(position).getVendor());
        movie_watch.setText("");

        movie_synopsis_content.setText(movieList.get(position).getDescription());

        if (!movieList.get(position).getLogo().equalsIgnoreCase("")) {
            Picasso.with(MovieYoutubePlayingActivity.this).load(movieList.get(position).getCoverImage()).placeholder(R.drawable.placeholder_nt_square).into(mCoverArt);
        } else {
            Picasso.with(MovieYoutubePlayingActivity.this).load(R.drawable.placeholder_nt_square).placeholder(R.drawable.placeholder_nt_square).into(mCoverArt);
        }

        /*horizontalCastRecyclerViewAdapter = new MovieHorizontalCastRecyclerViewAdapter(MovieYoutubePlayingActivity.this, movieList.get(position).getCastCrew());

        recycler_cast_view_list.setHasFixedSize(true);
        recycler_cast_view_list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recycler_cast_view_list.setAdapter(horizontalCastRecyclerViewAdapter);

        recycler_cast_view_list.setNestedScrollingEnabled(false);*/

        horizontalRecyclerViewAdapter = new MovieHorizontalRecyclerViewAdapter(MovieYoutubePlayingActivity.this, movieList);

        recycler_view_list.setHasFixedSize(true);
        recycler_view_list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recycler_view_list.setAdapter(horizontalRecyclerViewAdapter);

        recycler_view_list.setNestedScrollingEnabled(false);

        watch_movie.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                watchLinear.setVisibility(View.INVISIBLE);
                mCoverArt.setVisibility(View.INVISIBLE);
                mCoverArtBackground.setVisibility(View.INVISIBLE);
                fragmentLinear.setVisibility(View.INVISIBLE);

                youtube_progress.setVisibility(View.VISIBLE);
                if (isInternet) {
                    getStreamUrl(currentMovie);
                } else {
                    detector.showNoInternetAlertDialog(MovieYoutubePlayingActivity.this);
                }
//                moviePlayer(currentMovie.getStreamUrl());
            }
        });

        watch_trailer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInternet) {
                    watchLinear.setVisibility(View.INVISIBLE);
                    mCoverArt.setVisibility(View.INVISIBLE);
                    mCoverArtBackground.setVisibility(View.INVISIBLE);
                    skip_trailer.setVisibility(View.VISIBLE);

                    movie_youtube_trailer = (YouTubePlayerFragment) getFragmentManager().findFragmentById(R.id.movie_youtube_trailer);
//9851201614
                    if (!movieList.get(position).getTrailer().equalsIgnoreCase("")) {
                        movie_youtube_trailer.initialize(LinkConfig.API_KEY_ANDROID, new YouTubePlayer.OnInitializedListener() {
                            @Override
                            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
                                youTubePlayerTrailer = youTubePlayer;
                                youTubePlayerTrailer.loadVideo(movieList.get(position).getTrailer());
                                youTubePlayerTrailer.setPlayerStateChangeListener(new YouTubePlayer.PlayerStateChangeListener() {
                                    @Override
                                    public void onLoading() {

                                    }

                                    @Override
                                    public void onLoaded(String s) {

                                    }

                                    @Override
                                    public void onAdStarted() {

                                    }

                                    @Override
                                    public void onVideoStarted() {

                                    }

                                    @Override
                                    public void onVideoEnded() {
                                        watchLinear.setVisibility(View.VISIBLE);
                                        mCoverArt.setVisibility(View.VISIBLE);
                                        mCoverArtBackground.setVisibility(View.VISIBLE);
                                        skip_trailer.setVisibility(View.GONE);
                                        youTubePlayerTrailer.release();
                                    }

                                    @Override
                                    public void onError(YouTubePlayer.ErrorReason errorReason) {
//                                errorText.setVisibility(View.VISIBLE);
//                                errorText.setText(errorReason.toString());

                                        watchLinear.setVisibility(View.VISIBLE);
                                        mCoverArt.setVisibility(View.VISIBLE);
                                        mCoverArtBackground.setVisibility(View.VISIBLE);
                                        skip_trailer.setVisibility(View.GONE);
                                        youTubePlayerTrailer.release();
                                    }
                                });
                            }

                            @Override
                            public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
                                if (youTubeInitializationResult.isUserRecoverableError()) {
//                    youTubeInitializationResult.getErrorDialog(this, 1).show();
                                } else {
//                    Toast.makeText(this, "errorMessage", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    } else {
                        watchLinear.setVisibility(View.VISIBLE);
                        mCoverArt.setVisibility(View.VISIBLE);
                        mCoverArtBackground.setVisibility(View.VISIBLE);
                        skip_trailer.setVisibility(View.GONE);

                        Toast.makeText(MovieYoutubePlayingActivity.this, "We haven't found any trailers. You could watch movie instead.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    detector.showNoInternetAlertDialog(MovieYoutubePlayingActivity.this);
                }
            }
        });

        skip_trailer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                youTubePlayerTrailer.release();
                watchLinear.setVisibility(View.VISIBLE);
                mCoverArt.setVisibility(View.VISIBLE);
                mCoverArtBackground.setVisibility(View.VISIBLE);
                skip_trailer.setVisibility(View.GONE);
            }
        });
    }

    public void getStreamUrl(final Movie currentMovie) {
        youtube_progress.setVisibility(View.VISIBLE);
        youtube_progress.bringToFront();

        watchLinear.setVisibility(View.INVISIBLE);
        mCoverArt.setVisibility(View.INVISIBLE);
        mCoverArtBackground.setVisibility(View.INVISIBLE);
        fragmentLinear.setVisibility(View.INVISIBLE);

        String streamUrl = currentMovie.getStreamUrl();
        Timber.d("stream url %s", streamUrl);
        getTimeStamp(streamUrl);
    }

    public void getTimeStamp(final String streamUrl) {
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
                            getStreamingUrl(main_url);
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

    public void getStreamingUrl(String url) {
        Retrofit retrofit = setupRetrofit.getRetrofit(LinkConfig.BASE_URL);
        MovieApiInterface movieApiInterface = retrofit.create(MovieApiInterface.class);

        Observable<Response<Streaming>> observable = movieApiInterface.getStreamUrl(AUTHORIZATION, url);
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).unsubscribeOn(Schedulers.io())
                .subscribe(new Observer<Response<Streaming>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(Response<Streaming> value) {
                        int responseCode = value.code();
                        if (responseCode == 200) {
                            videoId = value.body().getStream();

                            moviePlayer(videoId);
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

    public void moviePlayer(final String videoId) {
        youtube_progress.setVisibility(View.GONE);

        youtube_movie = (YouTubePlayerFragment) getFragmentManager().findFragmentById(R.id.youtube_movie);
        youtube_movie.initialize(LinkConfig.API_KEY_ANDROID, new YouTubePlayer.OnInitializedListener() {
            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
                youTubePlayer.loadVideo(videoId);
            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
                if (youTubeInitializationResult.isUserRecoverableError()) {
//                    youTubeInitializationResult.getErrorDialog(this, 1).show();
                } else {
//                    Toast.makeText(this, "errorMessage", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setFullscreenLayout();
            if (youTubePlayerTrailer != null)
                youTubePlayerTrailer.setFullscreen(true);
        } else {
            setPortraitLayout();
            if (youTubePlayerTrailer != null)
                youTubePlayerTrailer.setFullscreen(false);
        }

    }

    private void setFullscreenLayout() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) youtube_relative.getLayoutParams();

        params.height = metrics.heightPixels;
        params.width = metrics.widthPixels;// + getTitlebarHeight();
        params.rightMargin = 0;
        params.bottomMargin = 0;
        params.setMargins(0, 0, 0, 0);

        youtube_relative.setLayoutParams(params);
    }

    private void setPortraitLayout() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) youtube_relative.getLayoutParams();

        double height = 3.2;
        params.width = metrics.widthPixels;
        params.height = (int) (metrics.heightPixels / height);
        params.setMargins(0, 0, 0, 0);

        youtube_relative.setLayoutParams(params);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isInternet) {
            detector.showNoInternetAlertDialog(MovieYoutubePlayingActivity.this);
        }
    }
}
