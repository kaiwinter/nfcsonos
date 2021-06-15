package com.github.kaiwinter.nfcsonos.main.model

import android.os.Parcelable
import com.github.kaiwinter.nfcsonos.discover.DiscoverActivity
import com.github.kaiwinter.nfcsonos.main.MainFragment
import com.github.kaiwinter.nfcsonos.main.model.RetryAction.RetryActionType
import kotlinx.parcelize.Parcelize

/**
 * Holds a [RetryActionType] and an optional ID.
 */
@Parcelize
class RetryAction @JvmOverloads constructor(
    val retryActionType: RetryActionType,
    val additionalId: String? = null
) : Parcelable {
    /**
     * If an action which runs on the [MainFragmentViewModel] fails because of a changed household or group
     * the [DiscoverActivity] is started to choose
     * a new one. Afterwards the [MainFragment] executes the previous action with the help of this
     * enum.
     */
    enum class RetryActionType {
        /**
         * Retry loading a favorite.
         */
        RETRY_LOAD_FAVORITE,

        /**
         * Retry loading playback metadata.
         */
        RETRY_LOAD_METADATA;
    }
}

