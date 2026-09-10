package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppPreferences
import com.example.data.UserManager
import com.example.data.UserProfile
import com.example.data.api.AiApiClient
import com.example.data.api.AiRepository
import com.example.data.db.AppDatabase
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.data.model.UserFileEntity
import com.example.ui.components.AttachedFilePreview
import com.example.ui.components.FileAttachmentHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val conversationDao = db.conversationDao()
    private val messageDao = db.messageDao()
    private val userFileDao = db.userFileDao()

    private val userManager = UserManager.getInstance(application)
    val userState: StateFlow<UserProfile?> = userManager.userState
    val preferences: StateFlow<AppPreferences> = userManager.preferences

    private val aiRepository = AiRepository(AiApiClient.service)

    private val _activeConversation = MutableStateFlow<ConversationEntity?>(null)
    val activeConversation: StateFlow<ConversationEntity?> = _activeConversation.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    private val _allConversations = conversationDao.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredConversations: StateFlow<List<ConversationEntity>> =
        combine(_allConversations, _searchQuery) { convos, query ->
            if (query.isBlank()) {
                convos
            } else {
                convos.filter { it.title.contains(query, ignoreCase = true) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFiles: StateFlow<List<UserFileEntity>> = userFileDao.getAllFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _currentAttachedFile = MutableStateFlow<AttachedFilePreview?>(null)
    val currentAttachedFile: StateFlow<AttachedFilePreview?> = _currentAttachedFile.asStateFlow()

    private val _apiHealth = MutableStateFlow<Boolean?>(null)
    val apiHealth: StateFlow<Boolean?> = _apiHealth.asStateFlow()

    private var activeJob: Job? = null
    private var messageCollectorJob: Job? = null

    init {
        checkHealth()
        viewModelScope.launch {
            _allConversations.collect { list ->
                if (_activeConversation.value == null && list.isNotEmpty()) {
                    selectConversation(list.first())
                }
            }
        }
    }

    fun checkHealth() {
        viewModelScope.launch {
            val result = aiRepository.checkHealth()
            _apiHealth.value = result.getOrDefault(false)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setAttachedFile(file: AttachedFilePreview?) {
        _currentAttachedFile.value = file
    }

    fun selectConversation(conversation: ConversationEntity) {
        _activeConversation.value = conversation
        messageCollectorJob?.cancel()
        messageCollectorJob = viewModelScope.launch {
            messageDao.getMessagesForConversation(conversation.id).collect {
                _messages.value = it
            }
        }
    }

    fun createNewConversation(title: String = "New Chat"): String {
        val newConvo = ConversationEntity(
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            conversationDao.insert(newConvo)
            selectConversation(newConvo)
            val userId = userState.value?.id ?: "123456"
            aiRepository.createSession(userId)
        }
        return newConvo.id
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversationDao.deleteById(id)
            messageDao.deleteForConversation(id)
            if (_activeConversation.value?.id == id) {
                val remaining = _allConversations.value.filter { it.id != id }
                if (remaining.isNotEmpty()) {
                    selectConversation(remaining.first())
                } else {
                    _activeConversation.value = null
                    _messages.value = emptyList()
                }
            }
        }
    }

    fun updateTitle(id: String, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            conversationDao.updateTitle(id, newTitle)
            if (_activeConversation.value?.id == id) {
                _activeConversation.value = _activeConversation.value?.copy(title = newTitle)
            }
        }
    }

    fun togglePin(id: String, currentPinned: Boolean) {
        viewModelScope.launch {
            conversationDao.updatePin(id, !currentPinned)
        }
    }

    fun toggleFavorite(id: String, currentFavorite: Boolean) {
        viewModelScope.launch {
            conversationDao.updateFavorite(id, !currentFavorite)
        }
    }

    fun deleteMessage(id: String) {
        viewModelScope.launch {
            messageDao.deleteMessage(id)
        }
    }

    fun stopGenerating() {
        activeJob?.cancel()
        _isGenerating.value = false
        _isThinking.value = false
    }

    fun resetMemory(onDone: (Boolean) -> Unit = {}) {
        val userId = userState.value?.id ?: "123456"
        viewModelScope.launch {
            val result = aiRepository.resetMemory(userId)
            onDone(result.isSuccess)
        }
    }

    fun clearAllChats() {
        viewModelScope.launch {
            conversationDao.clearAll()
            messageDao.clearAll()
            userFileDao.clearAll()
            _activeConversation.value = null
            _messages.value = emptyList()
        }
    }

    fun sendMessage(userText: String, attached: AttachedFilePreview? = null) {
        if (userText.isBlank() && attached == null) return

        var convo = _activeConversation.value
        if (convo == null) {
            val title = if (userText.isNotBlank()) {
                if (userText.length > 28) userText.take(28) + "..." else userText
            } else {
                attached?.name ?: "New Chat"
            }
            val newId = createNewConversation(title)
            convo = ConversationEntity(id = newId, title = title)
            _activeConversation.value = convo
        } else if (_messages.value.isEmpty() && userText.isNotBlank()) {
            val autoTitle = if (userText.length > 28) userText.take(28) + "..." else userText
            updateTitle(convo.id, autoTitle)
        }

        val convoId = convo.id
        val fileSnapshot = attached ?: _currentAttachedFile.value

        // If a file was attached, save it into Room file table
        if (fileSnapshot != null) {
            viewModelScope.launch {
                userFileDao.insert(
                    UserFileEntity(
                        conversationId = convoId,
                        name = fileSnapshot.name,
                        size = fileSnapshot.size,
                        mimeType = fileSnapshot.mimeType,
                        contentSnippet = fileSnapshot.content.take(200),
                        fullContent = fileSnapshot.content
                    )
                )
            }
        }

        val displayContent = userText.ifBlank { "Analyze file: ${fileSnapshot?.name}" }
        val userMessage = MessageEntity(
            conversationId = convoId,
            role = "user",
            content = displayContent,
            timestamp = System.currentTimeMillis(),
            attachedFileName = fileSnapshot?.name,
            attachedFileSize = fileSnapshot?.size
        )

        // Reset attached file state
        _currentAttachedFile.value = null

        viewModelScope.launch {
            messageDao.insert(userMessage)
            conversationDao.updateTimestamp(convoId)
        }

        // Build prompt for API
        val promptToSend = if (fileSnapshot != null) {
            FileAttachmentHelper.buildPromptWithFile(userText, fileSnapshot)
        } else {
            userText
        }

        executeAiRequest(convoId, promptToSend)
    }

    fun regenerateLastResponse() {
        val convo = _activeConversation.value ?: return
        val lastUserMessage = _messages.value.lastOrNull { it.role == "user" } ?: return
        executeAiRequest(convo.id, lastUserMessage.content)
    }

    fun retryMessage(failedMessageId: String) {
        val convo = _activeConversation.value ?: return
        viewModelScope.launch {
            messageDao.deleteMessage(failedMessageId)
        }
        val lastUserMessage = _messages.value.lastOrNull { it.role == "user" } ?: return
        executeAiRequest(convo.id, lastUserMessage.content)
    }

    private fun executeAiRequest(conversationId: String, prompt: String) {
        activeJob?.cancel()
        _isGenerating.value = true
        _isThinking.value = true

        activeJob = viewModelScope.launch {
            val userId = userState.value?.id ?: "123456"

            // Let thinking animation run briefly
            delay(400)

            val result = aiRepository.sendMessage(userId = userId, message = prompt)
            _isThinking.value = false

            if (result.isSuccess) {
                val responseText = result.getOrNull()?.ifBlank { "No response received" } ?: "Empty response"
                val assistantMessage = MessageEntity(
                    conversationId = conversationId,
                    role = "assistant",
                    content = responseText,
                    timestamp = System.currentTimeMillis(),
                    isError = false
                )
                messageDao.insert(assistantMessage)
                conversationDao.updateTimestamp(conversationId)
            } else {
                val errMsg = result.exceptionOrNull()?.message ?: "AI service is temporarily unavailable"
                val errorMessage = MessageEntity(
                    conversationId = conversationId,
                    role = "assistant",
                    content = errMsg,
                    timestamp = System.currentTimeMillis(),
                    isError = true
                )
                messageDao.insert(errorMessage)
            }

            _isGenerating.value = false
        }
    }
}
