package com.ruirui.orbit.fakes

import com.ruirui.orbit.models.InboxMessage
import com.ruirui.orbit.models.SubmitMessageRequest

class FakeLocationApiBackend {
    
    // Maps receiverId -> List of pending InboxMessages
    private val inboxes = mutableMapOf<String, MutableList<InboxMessage>>()
    
    fun submitMessage(senderId: String, request: SubmitMessageRequest) {
        val list = inboxes.getOrPut(request.receiverId) { mutableListOf() }
        list.add(InboxMessage(senderId, request.encryptedBlob))
    }
    
    fun getInbox(receiverId: String): List<InboxMessage> {
        val messages = inboxes[receiverId]?.toList() ?: emptyList()
        inboxes[receiverId]?.clear()
        return messages
    }
}
