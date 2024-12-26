package com.example.partyplaylist;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.appcompat.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.partyplaylist.adapters.PlaylistAdapter;
import com.example.partyplaylist.data.User;
import com.example.partyplaylist.models.Playlist;
import com.example.partyplaylist.repositories.FirebaseRepository;
import com.example.partyplaylist.utils.SharedPreferencesManager;

import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;

public class LibraryFragment extends Fragment {

    private SearchView searchView;
    private RecyclerView playlistsRecyclerView;
    private PlaylistAdapter playlistAdapter;
    private FragmentTransactionListener fragmentTransactionListener;
    private FirebaseRepository firebaseRepository;
    private List<Playlist> allPlaylists = new ArrayList<>();
    private ImageView userPhoto; // Add this for user photo

    public interface FragmentTransactionListener {
        void replaceFragment(Fragment fragment, String tag);
    }

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);
        firebaseRepository = new FirebaseRepository(requireContext());
        searchView = view.findViewById(R.id.search_view);
        playlistsRecyclerView = view.findViewById(R.id.playlists_recycler_view);
        userPhoto = view.findViewById(R.id.user_photo); // Initialize user photo

        playlistsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize the adapter with a click listener
        playlistAdapter = new PlaylistAdapter(new ArrayList<>(), playlist -> openPlaylistDetail(playlist));
        playlistsRecyclerView.setAdapter(playlistAdapter);
        loadUserProfilePicture();
        fetchPlaylists();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterPlaylists(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterPlaylists(newText);
                return false;
            }
        });

        view.findViewById(R.id.create_playlist_fab).setOnClickListener(v -> {
            if (fragmentTransactionListener != null) {
                Fragment collaborativePlaylistFragment = new CollaborativePlaylistFragment();
                fragmentTransactionListener.replaceFragment(collaborativePlaylistFragment, "CollaborativePlaylistFragment");
            }
        });

        // Add click listener for the user photo
        userPhoto.setOnClickListener(v -> showUserOptions());

        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof FragmentTransactionListener) {
            fragmentTransactionListener = (FragmentTransactionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement FragmentTransactionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        fragmentTransactionListener = null;
    }

    private void fetchPlaylists() {
        firebaseRepository.getAllPlaylists(playlists -> {
            if (playlists != null) {
                Log.d("LibraryFragment", "Fetched playlists: " + playlists);
                allPlaylists.clear();
                allPlaylists.addAll(playlists);
                playlistAdapter.clearPlaylists();
                playlistAdapter.addPlaylists(playlists);
            } else {
                Log.e("LibraryFragment", "Failed to fetch playlists");
                Toast.makeText(getContext(), "Failed to fetch playlists", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterPlaylists(String query) {
        List<Playlist> filteredPlaylists = new ArrayList<>();
        for (Playlist playlist : allPlaylists) {
            if (playlist.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredPlaylists.add(playlist);
            }
        }
        playlistAdapter.clearPlaylists();
        playlistAdapter.addPlaylists(filteredPlaylists);
    }

    private Unit openPlaylistDetail(Playlist playlist) {
        // Use a Bundle to pass the playlist ID to the detail fragment
        Bundle bundle = new Bundle();
        bundle.putString("playlistId", playlist.getId());

        PlaylistDetailFragment playlistDetailFragment = new PlaylistDetailFragment();
        playlistDetailFragment.setArguments(bundle);

        if (fragmentTransactionListener != null) {
            fragmentTransactionListener.replaceFragment(playlistDetailFragment, "PlaylistDetailFragment");
        }
        return null;
    }

    private void showUserOptions() {
        // Create a dialog to show user options
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_user_options);

        // Get references to the TextViews
        TextView viewProfile = dialog.findViewById(R.id.view_profile);
        TextView logout = dialog.findViewById(R.id.logout);

        // Set click listeners for the options
        viewProfile.setOnClickListener(v -> {
            viewUserData();
            dialog.dismiss();
        });

        logout.setOnClickListener(v -> {
            logoutUser();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void viewUserData() {
        String userId = SharedPreferencesManager.getUserId(requireContext());
        String userName = SharedPreferencesManager.getUserName(requireContext());

        if (userId != null && userName != null) {
            Toast.makeText(getContext(), "User ID: " + userId + "\nUser Name: " + userName, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), "User data not found.", Toast.LENGTH_SHORT).show();
        }
    }

    private void logoutUser() {
        SharedPreferencesManager.clearUserData(requireContext());
        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Create an Intent to start the HomeActivity (or whatever your homepage activity is named)
        Intent intent = new Intent(requireContext(), MainActivity.class); // Replace HomeActivity with your actual homepage activity class name
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Clear the back stack and start a new task
        startActivity(intent);

        // Optionally, you can also call finish() if you want to close the current fragment/activity
        requireActivity().finish();
    }

    private void loadUserProfilePicture() {
        // Fetch the User object from SharedPreferences

        // Get the User profile from SharedPreferencesManager
        User user = SharedPreferencesManager.getUserProfile(requireContext());

        // Check if the user and imageUrl exist
        if (user != null && user.getImages() != null && !user.getImages().isEmpty()) {
            // Get the first image URL from the images list
            String imageUrl = user.getImages().get(0).getUrl();

            // Use Glide to load the image URL into the ImageView
            Glide.with(this)
                    .load(imageUrl) // Load image from URL
                    .placeholder(R.drawable.spotify_green_icon) // Default placeholder image
                    .into(userPhoto); // Set image to the ImageView
        } else {
            // If no image URL exists, set a default image
            Glide.with(this)
                    .load(R.drawable.spotify_green_icon) // Default image
                    .into(userPhoto); // Set default image to the ImageView
        }
    }
}
