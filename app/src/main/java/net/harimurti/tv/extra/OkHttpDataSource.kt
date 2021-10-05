@file:Suppress("unused")
package net.harimurti.tv.extra

import android.net.Uri
import androidx.annotation.Nullable
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayerLibraryInfo
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.HttpDataSource.*
import com.google.android.exoplayer2.upstream.HttpUtil.buildRangeRequestHeader
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.util.Util.castNonNull
import com.google.common.base.Predicate
import com.google.common.net.HttpHeaders
import okhttp3.*
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.util.*


class OkHttpDataSource private constructor(
    callFactory: Call.Factory,
    @Nullable userAgent: String?,
    @Nullable cacheControl: CacheControl?,
    @Nullable defaultRequestProperties: RequestProperties?,
    @Nullable contentTypePredicate: Predicate<String?>?
) : BaseDataSource( /* isNetwork= */true), HttpDataSource {
    companion object {
        init {
            ExoPlayerLibraryInfo.registerModule("goog.exo.okhttp")
        }
    }

    /** [DataSource.Factory] for [OkHttpDataSource] instances.  */
    class Factory(private val callFactory: Call.Factory) : HttpDataSource.Factory {
        private val defaultRequestProperties: RequestProperties = RequestProperties()

        @Nullable
        private var userAgent: String? = null

        @Nullable
        private var transferListener: TransferListener? = null

        @Nullable
        private var cacheControl: CacheControl? = null

        @Nullable
        private var contentTypePredicate: Predicate<String?>? = null

        private var allowCrossProtocolRedirects = false

        @Deprecated("Use {@link #setDefaultRequestProperties(Map)} instead. ")
        override fun getDefaultRequestProperties(): RequestProperties {
            return defaultRequestProperties
        }

        override fun setDefaultRequestProperties(defaultRequestProperties: Map<String, String>): Factory {
            this.defaultRequestProperties.clearAndSet(defaultRequestProperties)
            return this
        }

        /**
         * Sets the user agent that will be used.
         *
         *
         * The default is `null`, which causes the default user agent of the underlying [ ] to be used.
         *
         * @param userAgent The user agent that will be used, or `null` to use the default user
         * agent of the underlying [OkHttpClient].
         * @return This factory.
         */
        fun setUserAgent(@Nullable userAgent: String?): Factory {
            this.userAgent = userAgent
            return this
        }

        /**
         * Sets the [CacheControl] that will be used.
         *
         *
         * The default is `null`.
         *
         * @param cacheControl The cache control that will be used.
         * @return This factory.
         */
        fun setCacheControl(@Nullable cacheControl: CacheControl?): Factory {
            this.cacheControl = cacheControl
            return this
        }

        /**
         * Sets whether to allow cross protocol redirects.
         *
         *
         * The default is `false`.
         *
         * @param allowCrossProtocolRedirects Whether to allow cross protocol redirects.
         * @return This factory.
         */
        fun setAllowCrossProtocolRedirects(allowCrossProtocolRedirects: Boolean): Factory {
            this.allowCrossProtocolRedirects = allowCrossProtocolRedirects
            return this
        }

        /**
         * Sets a content type [Predicate]. If a content type is rejected by the predicate then a
         * [HttpDataSource.InvalidContentTypeException] is thrown from [ ][OkHttpDataSource.open].
         *
         *
         * The default is `null`.
         *
         * @param contentTypePredicate The content type [Predicate], or `null` to clear a
         * predicate that was previously set.
         * @return This factory.
         */
        fun setContentTypePredicate(@Nullable contentTypePredicate: Predicate<String?>): Factory {
            this.contentTypePredicate = contentTypePredicate
            return this
        }

        /**
         * Sets the [TransferListener] that will be used.
         *
         *
         * The default is `null`.
         *
         *
         * See [DataSource.addTransferListener].
         *
         * @param transferListener The listener that will be used.
         * @return This factory.
         */
        fun setTransferListener(@Nullable transferListener: TransferListener?): Factory {
            this.transferListener = transferListener
            return this
        }

        override fun createDataSource(): OkHttpDataSource {
            val dataSource = OkHttpDataSource(
                callFactory, userAgent, cacheControl, defaultRequestProperties, contentTypePredicate
            )
            if (transferListener != null) {
                dataSource.addTransferListener(transferListener!!)
            }
            return dataSource
        }

    }

    private val callFactory: Call.Factory = Assertions.checkNotNull(callFactory)
    private val requestProperties: RequestProperties

    @Nullable
    private val userAgent: String?

    @Nullable
    private val cacheControl: CacheControl?

    @Nullable
    private val defaultRequestProperties: RequestProperties?

    @Nullable
    private var contentTypePredicate: Predicate<String?>?

    @Nullable
    private var dataSpec: DataSpec? = null

    @Nullable
    private var response: Response? = null

    @Nullable
    private var responseByteStream: InputStream? = null
    private var opened = false
    private var bytesToRead: Long = 0
    private var bytesRead: Long = 0

    override fun getUri(): Uri? {
        return if (response == null) null else Uri.parse(response!!.request().url().toString())
    }

    override fun getResponseCode(): Int {
        return if (response == null) -1 else response!!.code()
    }

    override fun getResponseHeaders(): Map<String, List<String>> {
        return if (response == null) Collections.emptyMap() else response!!.headers().toMultimap()
    }

    override fun setRequestProperty(name: String, value: String) {
        Assertions.checkNotNull(name)
        Assertions.checkNotNull(value)
        requestProperties[name] = value
    }

    override fun clearRequestProperty(name: String) {
        Assertions.checkNotNull(name)
        requestProperties.remove(name)
    }

    override fun clearAllRequestProperties() {
        requestProperties.clear()
    }

    @Throws(HttpDataSourceException::class)
    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        bytesRead = 0
        bytesToRead = 0
        transferInitializing(dataSpec)
        val request = makeRequest(dataSpec)
        val response: Response?
        val responseBody: ResponseBody
        try {
            this.response = callFactory.newCall(request).execute()
            response = this.response
            responseBody = Assertions.checkNotNull(response!!.body())
            responseByteStream = responseBody.byteStream()
        } catch (e: IOException) {
            throw HttpDataSourceException.createForIOException(
                e, dataSpec, HttpDataSourceException.TYPE_OPEN
            )
        }
        val responseCode = response.code()

        // Check for a valid response code.
        if (!response.isSuccessful) {
            if (responseCode == 416) {
                val documentSize =
                    HttpUtil.getDocumentSize(response.headers()[HttpHeaders.CONTENT_RANGE])
                if (dataSpec.position == documentSize) {
                    opened = true
                    transferStarted(dataSpec)
                    return if (dataSpec.length != C.LENGTH_UNSET.toLong()) dataSpec.length else 0
                }
            }
            val errorResponseBody: ByteArray = try {
                Util.toByteArray(Assertions.checkNotNull(responseByteStream))
            } catch (e: IOException) {
                Util.EMPTY_BYTE_ARRAY
            }
            val headers = response.headers().toMultimap()
            closeConnectionQuietly()
            val cause: IOException? =
                if (responseCode == 416) DataSourceException(PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE) else null
            throw InvalidResponseCodeException(
                responseCode, response.message(), cause, headers, dataSpec, errorResponseBody
            )
        }

        // Check for a valid content type.
        val mediaType = responseBody.contentType()
        val contentType = mediaType?.toString() ?: ""
        if (contentTypePredicate != null && !contentTypePredicate!!.apply(contentType)) {
            closeConnectionQuietly()
            throw InvalidContentTypeException(contentType, dataSpec)
        }

        // If we requested a range starting from a non-zero position and received a 200 rather than a
        // 206, then the server does not support partial requests. We'll need to manually skip to the
        // requested position.
        val bytesToSkip =
            if (responseCode == 200 && dataSpec.position != 0L) dataSpec.position else 0

        // Determine the length of the data to be read, after skipping.
        bytesToRead = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            val contentLength = responseBody.contentLength()
            if (contentLength != -1L) contentLength - bytesToSkip else C.LENGTH_UNSET.toLong()
        }
        opened = true
        transferStarted(dataSpec)
        try {
            skipFully(bytesToSkip, dataSpec)
        } catch (e: HttpDataSourceException) {
            closeConnectionQuietly()
            throw e
        }
        return bytesToRead
    }

    @Throws(HttpDataSourceException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return try {
            readInternal(buffer, offset, length)
        } catch (e: IOException) {
            throw HttpDataSourceException.createForIOException(
                e, castNonNull(dataSpec), HttpDataSourceException.TYPE_READ
            )
        }
    }

    override fun close() {
        if (opened) {
            opened = false
            transferEnded()
            closeConnectionQuietly()
        }
    }

    /** Establishes a connection.  */
    @Throws(HttpDataSourceException::class)
    private fun makeRequest(dataSpec: DataSpec): Request {
        val position = dataSpec.position
        val length = dataSpec.length
        val url = HttpUrl.parse(dataSpec.uri.toString())
            ?: throw HttpDataSourceException(
                "Malformed URL",
                dataSpec,
                PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK,
                HttpDataSourceException.TYPE_OPEN
            )
        val builder: Request.Builder = Request.Builder().url(url)
        if (cacheControl != null) {
            builder.cacheControl(cacheControl)
        }
        val headers: MutableMap<String, String> = HashMap()
        if (defaultRequestProperties != null) {
            headers.putAll(defaultRequestProperties.snapshot)
        }
        headers.putAll(requestProperties.snapshot)
        headers.putAll(dataSpec.httpRequestHeaders)
        for ((key, value) in headers) {
            builder.header(key, value)
        }
        val rangeHeader = buildRangeRequestHeader(position, length)
        if (rangeHeader != null) {
            builder.addHeader(HttpHeaders.RANGE, rangeHeader)
        }
        if (userAgent != null) {
            builder.addHeader(HttpHeaders.USER_AGENT, userAgent)
        }
        if (!dataSpec.isFlagSet(DataSpec.FLAG_ALLOW_GZIP)) {
            builder.addHeader(HttpHeaders.ACCEPT_ENCODING, "identity")
        }
        var requestBody: RequestBody? = null
        if (dataSpec.httpBody != null) {
            requestBody = RequestBody.create(null, dataSpec.httpBody!!)
        } else if (dataSpec.httpMethod == DataSpec.HTTP_METHOD_POST) {
            // OkHttp requires a non-null body for POST requests.
            requestBody = RequestBody.create(null, Util.EMPTY_BYTE_ARRAY)
        }
        builder.method(dataSpec.httpMethodString, requestBody)
        return builder.build()
    }

    /**
     * Attempts to skip the specified number of bytes in full.
     *
     * @param bytesToSkip The number of bytes to skip.
     * @param dataSpec The [DataSpec].
     * @throws HttpDataSourceException If the thread is interrupted during the operation, or an error
     * occurs while reading from the source, or if the data ended before skipping the specified
     * number of bytes.
     */
    @Throws(HttpDataSourceException::class)
    private fun skipFully(bytesToSkip: Long, dataSpec: DataSpec) {
        var toSkip = bytesToSkip
        if (toSkip == 0L) {
            return
        }
        val skipBuffer = ByteArray(4096)
        try {
            while (toSkip > 0) {
                val readLength = toSkip.coerceAtMost(skipBuffer.size.toLong()).toInt()
                val read = castNonNull(responseByteStream).read(skipBuffer, 0, readLength)
                if (Thread.currentThread().isInterrupted) {
                    throw InterruptedIOException()
                }
                if (read == -1) {
                    throw HttpDataSourceException(
                        dataSpec,
                        PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
                        HttpDataSourceException.TYPE_OPEN
                    )
                }
                toSkip -= read.toLong()
                bytesTransferred(read)
            }
            return
        } catch (e: IOException) {
            if (e is HttpDataSourceException) {
                throw e
            } else {
                throw HttpDataSourceException(
                    dataSpec,
                    PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                    HttpDataSourceException.TYPE_OPEN
                )
            }
        }
    }

    /**
     * Reads up to `length` bytes of data and stores them into `buffer`, starting at index
     * `offset`.
     *
     *
     * This method blocks until at least one byte of data can be read, the end of the opened range
     * is detected, or an exception is thrown.
     *
     * @param buffer The buffer into which the read data should be stored.
     * @param offset The start offset into `buffer` at which data should be written.
     * @param readLength The maximum number of bytes to read.
     * @return The number of bytes read, or [C.RESULT_END_OF_INPUT] if the end of the opened
     * range is reached.
     * @throws IOException If an error occurs reading from the source.
     */
    @Throws(IOException::class)
    private fun readInternal(buffer: ByteArray, offset: Int, readLength: Int): Int {
        var length = readLength
        if (length == 0) {
            return 0
        }
        if (bytesToRead != C.LENGTH_UNSET.toLong()) {
            val bytesRemaining = bytesToRead - bytesRead
            if (bytesRemaining == 0L) {
                return C.RESULT_END_OF_INPUT
            }
            length = length.toLong().coerceAtMost(bytesRemaining).toInt()
        }
        val read = castNonNull(responseByteStream).read(buffer, offset, length)
        if (read == -1) {
            return C.RESULT_END_OF_INPUT
        }
        bytesRead += read.toLong()
        bytesTransferred(read)
        return read
    }


    /** Closes the current connection quietly, if there is one.  */
    private fun closeConnectionQuietly() {
        if (response != null) {
            Assertions.checkNotNull(response!!.body()).close()
            response = null
        }
        responseByteStream = null
    }

    init {
        this.userAgent = userAgent
        this.cacheControl = cacheControl
        this.defaultRequestProperties = defaultRequestProperties
        this.contentTypePredicate = contentTypePredicate
        requestProperties = RequestProperties()
    }
}