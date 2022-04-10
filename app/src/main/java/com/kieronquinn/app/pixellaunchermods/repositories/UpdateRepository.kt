package com.kieronquinn.app.pixellaunchermods.repositories

import com.kieronquinn.app.pixellaunchermods.BuildConfig
import com.kieronquinn.app.pixellaunchermods.model.update.Release
import com.kieronquinn.app.pixellaunchermods.service.createGitHubService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface UpdateRepository {

    suspend fun getUpdate(currentTag: String = BuildConfig.TAG_NAME): Release?

}

class UpdateRepositoryImpl: UpdateRepository {

    companion object {
        private const val CONTENT_TYPE_APK = "application/vnd.android.package-archive"
    }

    private val gitHubService = createGitHubService()

    override suspend fun getUpdate(currentTag: String): Release? = withContext(Dispatchers.IO) {
        val releasesResponse = try {
            gitHubService.getReleases().execute()
        }catch (e: Exception) {
            return@withContext null
        }
        if(!releasesResponse.isSuccessful) return@withContext null
        val newestRelease = releasesResponse.body()?.firstOrNull() ?: return@withContext null
        if(newestRelease.tag == null || newestRelease.tag == currentTag) return@withContext null
        //Found a new release
        val versionName = newestRelease.versionName ?: return@withContext null
        val asset = newestRelease.assets?.firstOrNull { it.contentType == CONTENT_TYPE_APK } ?: return@withContext null
        val downloadUrl = asset.downloadUrl ?: return@withContext null
        val fileName = asset.fileName ?: return@withContext null
        val gitHubUrl = newestRelease.gitHubUrl ?: return@withContext null
        val body = newestRelease.body ?: return@withContext null
        return@withContext Release(newestRelease.tag, versionName, downloadUrl, fileName, gitHubUrl, body)
    }

}