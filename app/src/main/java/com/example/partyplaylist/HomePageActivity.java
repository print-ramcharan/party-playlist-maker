package com.example.partyplaylist;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.partyplaylist.services.MediaPlayerService;
import com.example.partyplaylist.services.SpotifySyncService;
import com.google.android.material.bottomnavigation.BottomNavigationView;


public class HomePageActivity extends AppCompatActivity implements HomePageFragment.OnArtistSelectedListener, LibraryFragment.FragmentTransactionListener{

    private static final String TAG = "HomePageActivity";
    private ProgressBar songProgressBar;
    private BottomNavigationView bottomNavigationView;
    private ViewPager2 viewPager;
    private MediaPlayerService mediaPlayerService;
    private boolean isBound = false;
    private boolean isPlaying = false;
    private  SwipeableTextView songTitleTextView;

    private final ServiceConnection mediaPlayerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            mediaPlayerService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    private BroadcastReceiver songTitleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("SONG_TITLE_UPDATE".equals(intent.getAction())) {
                String title = intent.getStringExtra("title");
                Log.d("title",title);
                updateSongTitle(title);
            }
            if ("UPDATE_PROGRESS_BAR".equals(intent.getAction())) {
                int progress = intent.getIntExtra("progress", 0);
                songProgressBar.setProgress(progress);
            }
        }
    };

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        songProgressBar = findViewById(R.id.songProgressBar);
        initializeViews();
        setupViewPagerAndBottomNavigation();
        setupServiceConnections();
        setupListeners();
//        setupSeekBar();
         songTitleTextView = findViewById(R.id.song_title);

        songTitleTextView.setOnSwipeListener(new SwipeableTextView.OnSwipeListener() {
            @Override
            public void onSwipeLeft() {
                Log.d("Swipeable2", "onSwipeLeft() called");

                mediaPlayerService.next(); // Implement this method to change to the next song
            }

            @Override
            public void onSwipeRight() {
                Log.d("Swipeable2", "onSwipeRight() called");

                mediaPlayerService.prev(); // Implement this method to change to the previous song
            }
        });
        // Register the BroadcastReceiver
        LocalBroadcastManager.getInstance(this).registerReceiver(songTitleReceiver,
                new IntentFilter("SONG_TITLE_UPDATE"));

        Log.d(TAG, "onCreate: HomePageActivity created");
    }

    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottomnavigationview);
        viewPager = findViewById(R.id.view_page);

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            // Check if the back stack is empty
            boolean isBackStackEmpty = getSupportFragmentManager().getBackStackEntryCount() == 0;

            // Restore BottomNavigationView visibility
            if (isBackStackEmpty) {
                if (bottomNavigationView != null) {
                    bottomNavigationView.setVisibility(View.VISIBLE);
                }

                View homepage = findViewById(R.id.homepage);
                View playerbottom = findViewById(R.id.play_pause_button);
                TextView songTitle = findViewById(R.id.song_title);

                if (homepage != null) {
                    homepage.setBackgroundColor(getResources().getColor(android.R.color.white));
                }

                if (playerbottom != null) {
                    playerbottom.setBackgroundColor(getResources().getColor(android.R.color.black));
                    playerbottom.setBackground(getDrawable(R.drawable.button_background));
                }

                if (songTitle != null) {
                    songTitle.setBackgroundColor(getResources().getColor(android.R.color.white));
                    songTitle.setTextColor(getResources().getColor(android.R.color.black));
                }
            }
        });
    }

    private void setupViewPagerAndBottomNavigation() {
        // Setup ViewPager2 with the adapter
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(viewPagerAdapter);

        // Link BottomNavigationView with ViewPager2
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_home) {
                Log.d(TAG, "onNavigationItemSelected: Home selected");
                viewPager.setCurrentItem(0);
                return true;
            } else if (item.getItemId() == R.id.navigation_search) {
                Log.d(TAG, "onNavigationItemSelected: Search selected");
                viewPager.setCurrentItem(1);
                return true;
            } else if (item.getItemId() == R.id.navigation_library) {
                Log.d(TAG, "onNavigationItemSelected: Library selected");
                viewPager.setCurrentItem(2);
                return true;
            } else {
                Log.d(TAG, "onNavigationItemSelected: Unknown item selected");
                return false;
            }
        });

        // Sync ViewPager2 with BottomNavigationView
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                Log.d(TAG, "onPageSelected: Page " + position + " selected");
                bottomNavigationView.getMenu().getItem(position).setChecked(true);
            }
        });
    }
    private void updateUIForCurrentPage(int position) {
        // Update UI based on current page
        View homepage = findViewById(R.id.homepage);
        View playerBottom = findViewById(R.id.progress_bar);

        if (homepage != null) {
            if (position == 0) {
                homepage.setBackgroundColor(getResources().getColor(android.R.color.white));
            } else {
                homepage.setBackgroundColor(getResources().getColor(android.R.color.black));
            }
        }

        if (playerBottom != null) {
            if (position == 0) {
                playerBottom.setVisibility(View.VISIBLE);
                playerBottom.setBackgroundColor(getResources().getColor(android.R.color.black));
            } else {
                playerBottom.setVisibility(View.GONE);
            }
        }

        if (songTitleTextView != null) {
            if (position == 0) {
                songTitleTextView.setBackgroundColor(getResources().getColor(android.R.color.white));
                songTitleTextView.setTextColor(getResources().getColor(android.R.color.black));
            } else {
                songTitleTextView.setBackgroundColor(getResources().getColor(android.R.color.black));
                songTitleTextView.setTextColor(getResources().getColor(android.R.color.white));
            }
        }
    }

    private void setupServiceConnections() {
        startSpotifySyncService();
        startMusicPlayerService();
        bindMediaPlayerService();
        Intent intent = new Intent(this, MediaPlayerService.class);
        bindService(intent, mediaPlayerServiceConnection, Context.BIND_AUTO_CREATE);
    }

//    private void setupSeekBar() {
//        SeekBar progressBar = findViewById(R.id.progress_bar);
//        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                if (isBound && fromUser) {
//                    long duration = mediaPlayerService.getDuration();
//                    long newPosition = (duration * progress) / 100;
//                    mediaPlayerService.seekTo(newPosition);
//                }
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {}
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {}
//        });
//
//        BroadcastReceiver seekBarReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                if ("UPDATE_SEEKBAR".equals(intent.getAction())) {
//                    int progress = intent.getIntExtra("progress", 0);
//                    progressBar.setProgress(progress);
//                }
//            }
//        };
//
//        IntentFilter filter = new IntentFilter("UPDATE_SEEKBAR");
//        registerReceiver(seekBarReceiver, filter);
//    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupListeners() {
        ImageButton playPauseButton = findViewById(R.id.play_pause_button);

        playPauseButton.setOnClickListener(v -> {
            Log.d("clicking", "Play/Pause button clicked");
            if (isBound) {
                if (isPlaying) {
                    mediaPlayerService.pause();
                    playPauseButton.setImageResource(R.drawable.ic_play_arrow); // Change to play icon
                } else {
                    mediaPlayerService.play("https://p.scdn.co/mp3-preview/939c9e6c6835a0610e02f80510f8d577c6d5c1f4?cid=9e55757a811a432c88d740c04711f5a0", "Hiss");
                    playPauseButton.setImageResource(R.drawable.pause); // Change to pause icon
                }
                isPlaying = !isPlaying;
            }
        });

        viewPager.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Start swipe detection
                    break;
                case MotionEvent.ACTION_UP:
                    // Detect swipe direction
                    // Implement swipe-based song navigation here
                    break;
            }
            return false;
        });
    }


    private void updateSongTitle(String title) {
        TextView songTitleTextView = findViewById(R.id.song_title);
        if (songTitleTextView != null) {
            songTitleTextView.setText(title);
        }else songTitleTextView.setText("title got null");
    }

    @Override
    public void onArtistSelected(String artistName) {
        Log.d(TAG, "onArtistSelected: Artist selected - " + artistName);

        // Handle the artist selection, for example, open ArtistDetailsFragment
        Fragment artistDetailsFragment = ArtistDetailsFragment.newInstance(artistName);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        // Make sure the container is visible
        FrameLayout artistDetailsContainer = findViewById(R.id.artist_details_container);
        artistDetailsContainer.setVisibility(View.VISIBLE);
        bottomNavigationView.setVisibility(viewPager.GONE);

        Log.d(TAG, "onArtistSelected: Replacing fragment with ArtistDetailsFragment");
        fragmentTransaction.replace(R.id.homepage, artistDetailsFragment);
//        var homepage = findViewById(R.id.homepage);
        TextView songTitle = findViewById(R.id.song_title);
        var playerbottom = findViewById(R.id.player_bottom);
        playerbottom.setBackgroundColor(getResources().getColor(android.R.color.black));
//        homepage.setBackgroundColor(getResources().getColor(android.R.color.black));
        songTitle.setBackgroundColor(getResources().getColor(android.R.color.black));
        songTitle.setTextColor(getResources().getColor(android.R.color.white));
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();

        Log.d(TAG, "onArtistSelected: Fragment transaction committed");
    }

    @Override
    public void onAlbumSelected(@Nullable String albumId) {
        Fragment albumTracksFragment = AlbumTracksFragment.newInstance(albumId);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        FrameLayout albumTracksContainer = findViewById(R.id.artist_details_container);
        albumTracksContainer.setVisibility(View.VISIBLE);
        bottomNavigationView.setVisibility(viewPager.GONE);
        fragmentTransaction.replace(R.id.homepage, albumTracksFragment);
//        var homepage = findViewById(R.id.homepage);
        TextView songTitle = findViewById(R.id.song_title);
        var playerbottom = findViewById(R.id.player_bottom);
        playerbottom.setBackgroundColor(getResources().getColor(android.R.color.black));
//        homepage.setBackgroundColor(getResources().getColor(android.R.color.black));
        songTitle.setBackgroundColor(getResources().getColor(android.R.color.black));
        songTitle.setTextColor(getResources().getColor(android.R.color.white));
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public void replaceFragment(Fragment fragment, String tag) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.GONE);
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_library, fragment, tag);
        fragmentTransaction.addToBackStack(tag);
        fragmentTransaction.commit();
    }

    private static class ViewPagerAdapter extends FragmentStateAdapter {

        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Log.d(TAG, "ViewPagerAdapter: Creating fragment for position " + position);
            if (position == 0) {
                return new HomePageFragment();
            } else if (position == 1) {
                return new SearchFragment();
            } else if (position == 2) {
                return new LibraryFragment();
            } else {
                return new HomePageFragment(); // Default to HomePageFragment
            }
        }

        @Override
        public int getItemCount() {
            return 3; // Number of pages
        }
    }

    private void startSpotifySyncService() {
        Intent intent = new Intent(this, SpotifySyncService.class);
        startService(intent);
    }

    private void startMusicPlayerService() {
        Intent intent = new Intent(this, MediaPlayerService.class);
        startService(intent);
    }

    private void bindMediaPlayerService() {
        Intent intent = new Intent(this, MediaPlayerService.class);
        bindService(intent, mediaPlayerServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindMediaPlayerService() {
        if (isBound) {
            unbindService(mediaPlayerServiceConnection);
            isBound = false;
        }
    }

    private void stopSpotifySyncService() {
        Intent intent = new Intent(this, SpotifySyncService.class);
        stopService(intent);
    }

    private void stopMusicPlayerService() {
        Intent intent = new Intent(this, MediaPlayerService.class);
        stopService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the BroadcastReceiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(songTitleReceiver);
        stopSpotifySyncService();
        stopMusicPlayerService();
        unbindMediaPlayerService();
    }
}