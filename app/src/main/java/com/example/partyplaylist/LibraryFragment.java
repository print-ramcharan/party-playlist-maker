package com.example.partyplaylist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.partyplaylist.adapters.PlaylistAdapter;
import com.example.partyplaylist.models.Playlist;
import com.example.partyplaylist.repositories.FirebaseRepository;

import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends Fragment {

    private SearchView searchView;
    private RecyclerView playlistsRecyclerView;
    private PlaylistAdapter playlistAdapter;
    private FragmentTransactionListener fragmentTransactionListener;
    private FirebaseRepository firebaseRepository;
    private List<Playlist> allPlaylists = new ArrayList<>();

    public interface FragmentTransactionListener {
        void replaceFragment(Fragment fragment, String tag);
    }

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);
        firebaseRepository = new FirebaseRepository();
        searchView = view.findViewById(R.id.search_view);
        playlistsRecyclerView = view.findViewById(R.id.playlists_recycler_view);

        playlistsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        playlistAdapter = new PlaylistAdapter(new ArrayList<>());
        playlistsRecyclerView.setAdapter(playlistAdapter);

        // Fetch playlists when the view is created
        fetchPlaylists();

        // Setup SearchView listener
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

        // Setup FloatingActionButton listener
        view.findViewById(R.id.create_playlist_fab).setOnClickListener(v -> {
            if (fragmentTransactionListener != null) {
                Fragment collaborativePlaylistFragment = new CollaborativePlaylistFragment();
                fragmentTransactionListener.replaceFragment(collaborativePlaylistFragment, "CollaborativePlaylistFragment");
                // You might want to fetch the playlists again if needed
            }
        });

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
}
