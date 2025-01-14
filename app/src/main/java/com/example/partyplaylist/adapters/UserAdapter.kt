package com.example.partyplaylist.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.partyplaylist.R
import com.example.partyplaylist.data.User

class UserAdapter(private val userList: List<User>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private val selectedUsers = mutableSetOf<User>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.userName.text = user.displayName

        val imageUrl = user.images?.firstOrNull()?.url ?: ""
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_person)
            .into(holder.userImage)


        holder.itemView.setOnClickListener {
            toggleSelection(user, holder)
        }


        updateSelectionUI(holder, user in selectedUsers)
    }

    override fun getItemCount(): Int = userList.size

    private fun toggleSelection(user: User, holder: UserViewHolder) {
        if (selectedUsers.contains(user)) {
            selectedUsers.remove(user)
        } else {
            selectedUsers.add(user)
        }
        updateSelectionUI(holder, user in selectedUsers)
    }

    private fun updateSelectionUI(holder: UserViewHolder, isSelected: Boolean) {
        val context = holder.itemView.context
        holder.itemView.setBackgroundColor(
            if (isSelected) ContextCompat.getColor(context, R.color.selected_item)
            else ContextCompat.getColor(context, R.color.default_item)
        )
    }

    fun getSelectedUsers(): List<User> = selectedUsers.toList()

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userImage: ImageView = itemView.findViewById(R.id.user_image)
        val userName: TextView = itemView.findViewById(R.id.user_name)
    }
}
