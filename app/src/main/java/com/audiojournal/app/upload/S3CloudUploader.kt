package com.audiojournal.app.upload

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.fromFile
import java.io.File

/**
 * Immutable S3 configuration, populated from BuildConfig (which in turn reads
 * local.properties / environment variables at build time).
 */
data class S3Config(
    val bucket: String,
    val region: String,
    val accessKeyId: String,
    val secretAccessKey: String,
) {
    val isConfigured: Boolean
        get() = bucket.isNotBlank() &&
            region.isNotBlank() &&
            accessKeyId.isNotBlank() &&
            secretAccessKey.isNotBlank()
}

/** Uploads recordings to `s3://<bucket>/recordings/[<folder>/]<file name>`. */
class S3CloudUploader(private val config: S3Config) : CloudUploader {

    override val isConfigured: Boolean
        get() = config.isConfigured

    override val backendLabel: String = "Amazon S3"

    override suspend fun upload(file: File, folderName: String?): UploadResult {
        if (!config.isConfigured) return UploadResult.NotConfigured
        val prefix = folderName?.takeIf { it.isNotBlank() }?.let { "$it/" } ?: ""
        return try {
            S3Client {
                region = config.region
                credentialsProvider = StaticCredentialsProvider(
                    Credentials(
                        accessKeyId = config.accessKeyId,
                        secretAccessKey = config.secretAccessKey,
                    ),
                )
            }.use { s3 ->
                s3.putObject(
                    PutObjectRequest {
                        bucket = config.bucket
                        key = "recordings/$prefix${file.name}"
                        body = ByteStream.fromFile(file)
                        contentType = "audio/mp4"
                    },
                )
            }
            UploadResult.Success
        } catch (e: Exception) {
            UploadResult.Error(e)
        }
    }
}
