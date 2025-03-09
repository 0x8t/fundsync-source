package com.zero.fundsync.adapter

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zero.fundsync.R
import com.zero.fundsync.databinding.NotificationCardBinding
import com.zero.fundsync.model.UpiNotification

class NotificationsAdapter : ListAdapter<UpiNotification, NotificationsAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = NotificationCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: NotificationCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: UpiNotification) {
            binding.amountText.text = "₹ ${notification.amount}"
            binding.senderText.text = notification.sender
            binding.timestampText.text = DateUtils.getRelativeTimeSpanString(
                notification.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            )

            // Show donation ID if available
            if (notification.donationId != null) {
                binding.donationIdText.visibility = View.VISIBLE
                binding.donationIdText.text = "ID: ${notification.donationId}"
            } else {
                binding.donationIdText.visibility = View.GONE
            }

            // Set status text and color
            binding.statusText.text = if (notification.isSuccess) "Success" else "Failed"
            binding.statusText.setTextColor(
                binding.root.context.getColor(
                    if (notification.isSuccess) android.R.color.holo_green_light
                    else android.R.color.holo_red_light
                )
            )

            // Show info button if there's an error message
            if (!notification.isSuccess && notification.errorMessage != null) {
                binding.infoButton.visibility = View.VISIBLE
                binding.infoButton.setOnClickListener {
                    showErrorDialog(notification.errorMessage)
                }
            } else {
                binding.infoButton.visibility = View.GONE
            }
        }

        private fun showErrorDialog(errorMessage: String) {
            val context = binding.root.context
            val dialogView = LayoutInflater.from(context).inflate(R.layout.custom_dialog_layout, null)
            val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
            val dialogMessage = dialogView.findViewById<TextView>(R.id.dialogMessage)
            val positiveButton = dialogView.findViewById<MaterialButton>(R.id.positiveButton)
            val negativeButton = dialogView.findViewById<MaterialButton>(R.id.negativeButton)

            dialogTitle.text = "Error Details"
            dialogMessage.text = errorMessage
            positiveButton.text = "OK"
            negativeButton.visibility = View.GONE
            
            // Set button background color
            positiveButton.setBackgroundColor(context.getColor(R.color.debug_button_background))

            val dialog = AlertDialog.Builder(context, R.style.CustomMaterialDialog)
                .setView(dialogView)
                .create()

            positiveButton.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<UpiNotification>() {
            override fun areItemsTheSame(oldItem: UpiNotification, newItem: UpiNotification): Boolean {
                return oldItem.timestamp == newItem.timestamp
            }

            override fun areContentsTheSame(oldItem: UpiNotification, newItem: UpiNotification): Boolean {
                return oldItem == newItem
            }
        }
    }
} 