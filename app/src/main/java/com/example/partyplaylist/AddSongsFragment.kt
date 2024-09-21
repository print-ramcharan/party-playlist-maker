package com.example.partyplaylist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class AddSongsFragment : Fragment() {

    companion object {
        private const val ARG_PLAYLIST_ID = "playlist_id"

        fun newInstance(playlistId: String): AddSongsFragment {
            val fragment = AddSongsFragment()
            val args = Bundle()
            args.putString(ARG_PLAYLIST_ID, playlistId)
            fragment.arguments = args
            return fragment
        }
    }

    private var playlistId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playlistId = arguments?.getString(ARG_PLAYLIST_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_songs, container, false)
    }

    // Implement the logic to display songs and add them to the playlist here
}
