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
import java.util.concurrent.atomic.AtomicInteger;

import kotlin.Unit;

public class LibraryFragment extends Fragment {

    private SearchView searchView;
    private RecyclerView playlistsRecyclerView;
    private PlaylistAdapter playlistAdapter;
    private FragmentTransactionListener fragmentTransactionListener;
    private FirebaseRepository firebaseRepository;
    private List<Playlist> allPlaylists = new ArrayList<>();
    private ImageView userPhoto; // Add this for user photo
    //    private TextView playlistName ;
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
//        playlistName = view.findViewById(R.id.playlist_title);
        playlistsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize the adapter with a click listener
        playlistAdapter = new PlaylistAdapter(new ArrayList<>(), playlist -> openPlaylistDetail(playlist));
        playlistsRecyclerView.setAdapter(playlistAdapter);
        loadUserProfilePicture();
        fetchPlaylists();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterPlaylists(query); // Filter playlists when the user submits the query
                return true; // Return true to prevent the default search action
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    // If the query is empty, show all playlists
                    playlistAdapter.clearPlaylists();
                    playlistAdapter.addPlaylists(allPlaylists); // Reset to original playlists
                } else {
                    filterPlaylists(newText); // Otherwise, filter based on the query
                }
                return true;
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
    // Get the current user ID from shared preferences
    String userId = SharedPreferencesManager.getUserId(requireContext());

    // Handle the case when the user ID is not found
    if (userId == null) {
        Toast.makeText(getContext(), "Please log in to view your playlists.", Toast.LENGTH_SHORT).show();
        return;
    }

    // Show loading indicator (if applicable)
    firebaseRepository.getAllPlaylists(playlists -> {
        if (playlists != null) {
            List<Playlist> filteredPlaylists = new ArrayList<>();
            int playlistsCount = playlists.size();
            AtomicInteger playlistsFetchedCount = new AtomicInteger();

            // Filter playlists where the user is the owner or a collaborator
            for (Playlist playlist : playlists) {
                boolean isOwner = playlist.getOwner() != null && userId.equals(playlist.getOwner().getId());
                Log.d("owner value", playlist.getName() + playlist.getOwner());
                boolean isCollaborator = playlist.getCollaborators() != null &&
                        playlist.getCollaborators().stream().anyMatch(collaborator -> userId.equals(collaborator.getId()));

                if (isCollaborator) {
                    // Fetch collaborative playlist
                    firebaseRepository.getCollaborativePlaylist(playlist, collaboratorPlaylist -> {
                        if (collaboratorPlaylist != null) {
                            filteredPlaylists.add(collaboratorPlaylist);
                            Log.d("collaborative ", String.valueOf(collaboratorPlaylist));
                        } else {
                            Log.e("FirebaseRepository", "Collaborative playlist not found.");
                        }

                        // Increment the counter when the collaborative playlist is fetched
                        playlistsFetchedCount.getAndIncrement();

                        // When all playlists have been processed, update the UI
                        if (playlistsFetchedCount.get() == playlistsCount) {
                            updateUI(filteredPlaylists);
                        }

                        return null;
                    });
                }

                if (isOwner) {
                    filteredPlaylists.add(playlist);
                    playlistsFetchedCount.getAndIncrement();

                    // Update UI when all playlists are fetched
                    if (playlistsFetchedCount.get() == playlistsCount) {
                        updateUI(filteredPlaylists);
                    }
                }
            }
        } else {
            Log.e("LibraryFragment", "Failed to fetch playlists.");
            Toast.makeText(getContext(), "Failed to fetch playlists.", Toast.LENGTH_SHORT).show();
        }
    });
}

    private void updateUI(List<Playlist> filteredPlaylists) {
        if (!filteredPlaylists.isEmpty()) {
            Log.d("LibraryFragment", "Fetched playlists for user: " + filteredPlaylists.size());
            allPlaylists.addAll(filteredPlaylists);
            playlistAdapter.clearPlaylists();
            playlistAdapter.addPlaylists(filteredPlaylists);
        } else {
            Log.d("LibraryFragment", "No playlists found for this user.");
            Toast.makeText(getContext(), "You don't have any playlists.", Toast.LENGTH_SHORT).show();
        }
    }


    private void filterPlaylists(String query) {
        // Log the query to check what value it's receiving
        Log.d("FilterPlaylists", "Query received: " + query);

        // Trim and convert the query to lowercase
        String queryLower = query.trim().toLowerCase();

        // Log the trimmed and lowercased query
        Log.d("FilterPlaylists", "Trimmed and lowercased query: " + queryLower);

        List<Playlist> filteredPlaylists = new ArrayList<>();

        // Check if the allPlaylists list is not empty
        if (allPlaylists.isEmpty()) {
            Log.d("FilterPlaylists", "No playlists available in allPlaylists.");
        }

        // Filter playlists based on the query
        for (Playlist playlist : allPlaylists) {
            String playlistName = playlist.getName().toLowerCase();
            Log.d("FilterPlaylists", "Checking playlist: " + playlistName);

            if (playlistName.contains(queryLower)) {
                Log.d("FilterPlaylists", "Playlist matched: " + playlistName);
                filteredPlaylists.add(playlist);
            } else {
                Log.d("FilterPlaylists", "Playlist did not match: " + playlistName);
            }
        }

        // Log the number of filtered playlists
        Log.d("FilterPlaylists", "Filtered playlists count: " + filteredPlaylists.size());

        // Clear the existing playlists and add filtered ones
        playlistAdapter.clearPlaylists();
        playlistAdapter.addPlaylists(filteredPlaylists);
    }


    private Unit openPlaylistDetail(Playlist playlist) {
        // Use a Bundle to pass the playlist ID to the detail fragment
        Bundle bundle = new Bundle();
        bundle.putString("playlistId", playlist.getId());
        bundle.putString("playlistName", playlist.getName());
        bundle.putString("owner", playlist.getOwner().getId());
        bundle.putString("collaborators", playlist.getCollaborators().toString());
//        Log.d("playlistname is:",playlist.getName()+playlistName.toString());
//        if(playlist.getName()!=null) {
//            playlistName.setText(playlist.getName());
//        }
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
