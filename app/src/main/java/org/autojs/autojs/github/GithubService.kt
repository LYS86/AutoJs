package org.autojs.autojs.github

import com.google.gson.Gson
import okhttp3.Headers
import okhttp3.OkHttpClient
import org.autojs.autojs.github.model.ApiError
import org.autojs.autojs.github.model.ApiResult
import org.autojs.autojs.github.model.Contents
import org.autojs.autojs.github.model.Contributor
import org.autojs.autojs.github.model.RateLimit
import org.autojs.autojs.github.model.Release
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import timber.log.Timber

open class GithubService(private val owner: String, private val repo: String) {

    private interface GithubApi {
        @GET("repos/{owner}/{repo}/releases/latest")
        suspend fun latest(
            @Path("owner") owner: String,
            @Path("repo") repo: String,
            @Header("Accept") accept: String = "application/vnd.github+json"
        ): Response<Release>

        @GET("repos/{owner}/{repo}/contents/{path}")
        suspend fun getFile(
            @Path("owner") owner: String,
            @Path("repo") repo: String,
            @Path("path") path: String,
            @Header("Accept") accept: String = "application/vnd.github+json"
        ): Response<Contents>

        @GET("repos/{owner}/{repo}/contents/{path}")
        suspend fun getDirectory(
            @Path("owner") owner: String,
            @Path("repo") repo: String,
            @Path("path") path: String,
            @Header("Accept") accept: String = "application/vnd.github+json"
        ): Response<List<Contents>>

        @GET("repos/{owner}/{repo}/contributors")
        suspend fun contributors(
            @Path("owner") owner: String,
            @Path("repo") repo: String,
            @Header("Accept") accept: String = "application/vnd.github+json"
        ): Response<List<Contributor>>
    }


    private val retrofit: Retrofit by lazy {
        Retrofit.Builder().baseUrl("https://api.github.com/").client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(Gson())).build()
    }

    private val api: GithubApi by lazy {
        retrofit.create(GithubApi::class.java)
    }

    /**
     * 解析请求头获取速率限制信息
     */
    private fun parseHeaders(headers: Headers): RateLimit {
        return RateLimit(
            limit = headers["x-ratelimit-limit"]?.toIntOrNull() ?: 0,
            remaining = headers["x-ratelimit-remaining"]?.toIntOrNull() ?: 0,
            reset = headers["x-ratelimit-reset"]?.toLongOrNull() ?: 0,
            used = headers["x-ratelimit-used"]?.toIntOrNull() ?: 0
        )
    }

    /**
     * 通用 API 调用处理
     */
    private suspend fun <T> executeApiCall(apiCall: suspend () -> Response<T>): ApiResult<T> {
        return try {
            val response = apiCall().also {
                Timber.d("Response: $it")
            }

            val rateLimit = parseHeaders(response.headers()).also {
                Timber.d("RateLimit: $it")
            }

            when {
                response.isSuccessful -> {
                    response.body()?.let { body ->
                        ApiResult.Success(body, rateLimit)
                    } ?: createEmptyBodyError(response, rateLimit)
                }

                else -> createHttpError(response, rateLimit)
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    private fun <T> createEmptyBodyError(
        response: Response<T>, rateLimit: RateLimit
    ): ApiResult.Error {
        val error = ApiError(
            code = response.code(), message = "响应体为空", rateLimit = rateLimit
        )
        Timber.w("空响应体错误: $error")
        return ApiResult.Error(error)
    }

    private fun <T> createHttpError(response: Response<T>, rateLimit: RateLimit): ApiResult.Error {
        val error = ApiError(
            code = response.code(),
            message = response.message(),
            rateLimit = rateLimit
        ).apply {
            if (code == 404) {
                message = "$code : Not Found"
            }
        }


        Timber.w("HTTP错误: $error")
        Timber.w("boby: ${response.body()}")
        Timber.w("errorBody: ${response.errorBody()?.string()}")
        return ApiResult.Error(error)
    }

    private fun handleException(e: Exception): ApiResult.Error {
        val code = when (e) {
            is java.net.UnknownHostException -> -100 // 网络不可用
            is java.net.SocketTimeoutException -> -101 // 连接超时
            is retrofit2.HttpException -> e.code() // HTTP 异常
            else -> -1 // 其他未知错误
        }

        val error = ApiError(
            code = code, message = e.message ?: "未知错误: ${e.javaClass.simpleName}"

        )

        Timber.e(e, "API调用异常: $error")
        return ApiResult.Error(error)
    }


    /**
     * 获取最新发布信息
     */
    suspend fun getLatest(): ApiResult<Release> = executeApiCall {
        api.latest(owner, repo)
    }

    /**
     * 获取文件内容
     * @param path 文件路径，仓库相对路径
     */
    suspend fun getFile(path: String): ApiResult<Contents> = executeApiCall {
        api.getFile(owner, repo, path)
    }

    /**
     * 获取目录内容
     * @param path 目录路径，仓库相对路径
     */
    suspend fun getDirectoryContent(path: String): ApiResult<List<Contents>> = executeApiCall {
        api.getDirectory(owner, repo, path)
    }

    /**
     * 获取贡献者列表
     */
    suspend fun getContributors(): ApiResult<List<Contributor>> = executeApiCall {
        api.contributors(owner, repo)
    }
}