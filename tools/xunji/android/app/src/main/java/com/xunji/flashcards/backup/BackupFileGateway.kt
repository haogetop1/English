package com.xunji.flashcards.backup

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.net.Uri
import com.xunji.core.backup.*
import java.io.InputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface FileGateway {
    suspend fun write(uri:Uri,document:BackupDocument)
    suspend fun read(uri:Uri):InputStream
}
class BackupFileGateway(private val resolver:ContentResolver):FileGateway {
    override suspend fun write(uri:Uri,document:BackupDocument)=withContext(Dispatchers.IO) {
        val bytes=BackupCodec.encode(document).toByteArray(Charsets.UTF_8)
        (resolver.openOutputStream(uri,"wt") ?: throw IOException("无法打开文件")).use { it.write(bytes);it.flush() }
    }
    // Ownership is transferred to BackupService.prepare, which closes it with use.
    @SuppressLint("Recycle")
    override suspend fun read(uri:Uri):InputStream=withContext(Dispatchers.IO) { resolver.openInputStream(uri) ?: throw IOException("无法读取文件") }
}
