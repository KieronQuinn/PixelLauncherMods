package com.kieronquinn.app.pixellaunchermods.ui.screens.backuprestore.restore.issues

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentBackupRestoreIssuesBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import com.kieronquinn.app.pixellaunchermods.utils.extensions.applyBottomNavigationInset

class BackupRestoreIssuesFragment: BoundFragment<FragmentBackupRestoreIssuesBinding>(FragmentBackupRestoreIssuesBinding::inflate), BackAvailable {

    private val args by navArgs<BackupRestoreIssuesFragmentArgs>()

    private val adapter by lazy {
        BackupRestoreIssuesAdapter(requireContext(), args.issues.toList())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() = with(binding.restoreIssuesRecyclerview) {
        adapter = this@BackupRestoreIssuesFragment.adapter
        layoutManager = LinearLayoutManager(context)
        applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
    }

}