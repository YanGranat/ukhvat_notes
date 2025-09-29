package com.ukhvat.notes.data.datasource

import android.content.Context
import com.ukhvat.notes.domain.datasource.PromptDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.nio.charset.Charset

// Loads prompts from assets/prompts/{name}.md
class AssetPromptDataSource(
    private val context: Context,
    private val fileIoDispatcher: CoroutineDispatcher
) : PromptDataSource {

    override suspend fun getPrompt(name: String): String = withContext(fileIoDispatcher) {
        val path = "prompts/$name.md"
        val am = context.assets
        am.open(path).use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        }
    }
}


