package com.kieronquinn.app.pixellaunchermods.ui.screens.backuprestore.restore.issues

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kieronquinn.app.pixellaunchermods.databinding.ItemBackupRestoreIssueBinding
import com.kieronquinn.app.pixellaunchermods.repositories.BackupRestoreRepository.RestoreIssue

class BackupRestoreIssuesAdapter(
    context: Context,
    private val items: List<RestoreIssue>
): RecyclerView.Adapter<BackupRestoreIssuesAdapter.ViewHolder>() {

    private val layoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemBackupRestoreIssueBinding.inflate(layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding){
            val context = root.context
            itemBackupRestoreIssueTitle.text = item.getTitle(context)
            itemBackupRestoreIssueContent.text = item.getContent(context)
        }
    }

    class ViewHolder(val binding: ItemBackupRestoreIssueBinding): RecyclerView.ViewHolder(binding.root)

}