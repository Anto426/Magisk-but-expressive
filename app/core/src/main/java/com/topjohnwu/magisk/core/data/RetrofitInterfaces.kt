package com.topjohnwu.magisk.core.data

import com.topjohnwu.magisk.core.model.ModuleJson
import com.topjohnwu.magisk.core.model.UpdateJson
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface RawUrl {

    @GET
    @Streaming
    suspend fun fetchFile(@Url url: String): ResponseBody

    @GET
    @Streaming
    suspend fun fetchString(@Url url: String): ResponseBody

    @GET
    suspend fun fetchModuleJson(@Url url: String): ModuleJson

    @GET
    suspend fun fetchUpdateJson(@Url url: String): UpdateJson
}
