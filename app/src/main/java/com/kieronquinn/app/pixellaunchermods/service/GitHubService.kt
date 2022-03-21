package com.kieronquinn.app.pixellaunchermods.service

import com.kieronquinn.app.pixellaunchermods.model.github.GitHubRelease
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface GitHubService {

    @GET("releases")
    fun getReleases(): Call<Array<GitHubRelease>>

}

fun createGitHubService(): GitHubService =
    Retrofit.Builder()
        .baseUrl("https://api.github.com/repos/KieronQuinn/PixelLauncherMods/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .run {
            create(GitHubService::class.java)
        }