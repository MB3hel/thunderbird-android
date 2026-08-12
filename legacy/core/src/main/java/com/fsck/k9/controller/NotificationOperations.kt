package com.fsck.k9.controller

import app.k9mail.legacy.mailstore.ListenableMessageStore
import app.k9mail.legacy.mailstore.MessageStoreManager
import com.fsck.k9.notification.NotificationController
import com.fsck.k9.search.isNewMessages
import com.fsck.k9.search.isSingleFolder
import com.fsck.k9.search.isUnifiedFolders
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.feature.search.legacy.LocalMessageSearch

internal class NotificationOperations(
    private val notificationController: NotificationController,
    private val accountManager: LegacyAccountDtoManager,
    private val messageStoreManager: MessageStoreManager,
) {
    fun clearNotifications(search: LocalMessageSearch, readOnly: Boolean) {
        if (search.isUnifiedFolders) {
            clearUnifiedFoldersNotifications(readOnly)
        } else if (search.isNewMessages) {
            clearAllNotifications(readOnly)
        } else if (search.isSingleFolder) {
            val account = search.firstAccount() ?: return
            val folderId = search.folderIds.first()
            clearNotifications(account, folderId, readOnly)
        } else {
            // TODO: Remove notifications when updating the message list. That way we can easily remove only
            //  notifications for messages that are currently displayed in the list.
        }
    }

    private fun clearUnifiedFoldersNotifications(readOnly: Boolean) {
        for (account in accountManager.getAccounts()) {
            val messageStore = messageStoreManager.getMessageStore(account)

            val folderIds = messageStore.getFolders(excludeLocalOnly = true) { folderDetails ->
                if (folderDetails.isIntegrate) folderDetails.id else null
            }.filterNotNull().toSet()

            if (folderIds.isNotEmpty()) {
                notificationController.clearNewMailNotifications(account) { messageReferences ->
                    messageReferences.filter { messageReference ->
                        messageReference.folderId in folderIds &&
                            // Either supposed to clear all messages regardless of read/unread state or if the message is read
                            (!readOnly || Flag.SEEN in messageStore.getMessageFlags(
                                messageReference.folderId,
                                messageReference.uid,
                            ))
                    }
                }
            }
        }
    }

    private fun clearAllNotifications(readOnly: Boolean) {
        for (account in accountManager.getAccounts()) {
            if (readOnly) {
                val messageStore = messageStoreManager.getMessageStore(account)
                notificationController.clearNewMailNotifications(account) { messageReferences ->
                    messageReferences.filter { messageReference ->
                        Flag.SEEN in messageStore.getMessageFlags(
                            messageReference.folderId,
                            messageReference.uid,
                        )
                    }
                }
            } else {
                notificationController.clearNewMailNotifications(account, clearNewMessageState = false)
            }
        }
    }

    private fun clearNotifications(account: LegacyAccountDto, folderId: Long, readOnly: Boolean) {
        val messageStore = if (readOnly) messageStoreManager.getMessageStore(account) else null
        notificationController.clearNewMailNotifications(account) { messageReferences ->
            messageReferences.filter { messageReference ->
                messageReference.folderId == folderId &&
                    // Either supposed to clear all messages regardless of read/unread state or if the message is read
                    (!readOnly || messageStore?.getMessageFlags(messageReference.folderId, messageReference.uid)
                        ?.contains(Flag.SEEN) == true)
            }
        }
    }

    private fun LocalMessageSearch.firstAccount(): LegacyAccountDto? {
        return accountManager.getAccount(accountUuids.first())
    }
}
