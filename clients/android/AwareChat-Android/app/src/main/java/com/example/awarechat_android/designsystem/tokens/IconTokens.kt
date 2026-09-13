package com.example.awarechat_android.designsystem.tokens

import androidx.annotation.DrawableRes
import com.example.awarechat_android.R

object IconTokens {
    /** Server acceptance of an outgoing message. */
    @DrawableRes
    val checkmark = R.drawable.ic_message_sent

    /** Permanent failure of an outgoing message. */
    @DrawableRes
    val failed = R.drawable.ic_message_failed

    /** Conversation-list disclosure indicator. */
    @DrawableRes
    val chevron = R.drawable.ic_chevron_right

    /** Pending outgoing message indicator. */
    @DrawableRes
    val pending = R.drawable.ic_message_pending

    /** Offline connection indicator. */
    @DrawableRes
    val offline = R.drawable.ic_connection_offline
}
