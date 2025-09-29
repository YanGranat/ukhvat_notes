package com.ukhvat.notes.domain.datasource

/**
 * DataSource for loading AI prompts stored as Markdown files.
 * Prompts are plain text notes located under assets/prompts/{name}.md and should
 * contain only the final prompt text without any front matter.
 */
interface PromptDataSource {
    /**
     * Load prompt text by name from assets/prompts/{name}.md using UTF-8.
     * @throws Exception if the prompt cannot be loaded
     */
    suspend fun getPrompt(name: String): String
}


