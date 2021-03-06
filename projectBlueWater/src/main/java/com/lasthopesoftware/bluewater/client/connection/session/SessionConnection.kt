package com.lasthopesoftware.bluewater.client.connection.session

import android.content.Context
import android.content.Intent
import androidx.annotation.IntDef
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ISelectedLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider.Instance.get
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.android.messages.SendMessages
import com.namehillsoftware.handoff.promises.Promise
import org.slf4j.LoggerFactory

class SessionConnection(
	private val localBroadcastManager: SendMessages,
	private val selectedLibraryIdentifierProvider: ISelectedLibraryIdentifierProvider,
	private val libraryConnections: ProvideLibraryConnections) : (BuildingConnectionStatus) -> Unit {

	fun promiseTestedSessionConnection(): Promise<IConnectionProvider?> {
		val newSelectedLibraryId = selectedLibraryIdentifierProvider.selectedLibraryId
			?: return Promise.empty()

		return libraryConnections
			.promiseTestedLibraryConnection(newSelectedLibraryId)
			.updates(this)
	}

	fun isSessionConnectionActive(): Boolean {
		val selectedLibraryId = selectedLibraryIdentifierProvider.selectedLibraryId
			?: return false

		return libraryConnections.isConnectionActive(selectedLibraryId)
	}

	fun promiseSessionConnection(): Promise<IConnectionProvider?> {
		val newSelectedLibraryId = selectedLibraryIdentifierProvider.selectedLibraryId
			?: return Promise.empty()
		return libraryConnections
			.promiseLibraryConnection(newSelectedLibraryId)
			.updates(this)
	}

	override fun invoke(connectionStatus: BuildingConnectionStatus) {
		doStateChange(connectionStatus)
	}

	private fun doStateChange(status: BuildingConnectionStatus) {
		val broadcastIntent = Intent(buildSessionBroadcast)
		broadcastIntent.putExtra(buildSessionBroadcastStatus, BuildingSessionConnectionStatus.getSessionConnectionStatus(status))
		localBroadcastManager.sendBroadcast(broadcastIntent)
		if (status === BuildingConnectionStatus.BuildingConnectionComplete) logger.info("Session started.")
	}

	object BuildingSessionConnectionStatus {
		const val GettingLibrary = 1
		const val GettingLibraryFailed = 2
		const val SendingWakeSignal = 3
		const val BuildingConnection = 4
		const val BuildingConnectionFailed = 5
		const val BuildingSessionComplete = 6

		@SessionConnectionStatus
		fun getSessionConnectionStatus(connectionStatus: BuildingConnectionStatus): Int {
			return when (connectionStatus) {
				BuildingConnectionStatus.GettingLibrary -> GettingLibrary
				BuildingConnectionStatus.SendingWakeSignal -> SendingWakeSignal
				BuildingConnectionStatus.GettingLibraryFailed -> GettingLibraryFailed
				BuildingConnectionStatus.BuildingConnection -> BuildingConnection
				BuildingConnectionStatus.BuildingConnectionFailed -> BuildingConnectionFailed
				BuildingConnectionStatus.BuildingConnectionComplete -> BuildingSessionComplete
			}
		}

		@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
		@IntDef(GettingLibrary, GettingLibraryFailed, SendingWakeSignal, BuildingConnection, BuildingConnectionFailed, BuildingSessionComplete)
		internal annotation class SessionConnectionStatus
	}

	companion object {
		@JvmField
		val buildSessionBroadcast = MagicPropertyBuilder.buildMagicPropertyName(SessionConnection::class.java, "buildSessionBroadcast")
		@JvmField
		val buildSessionBroadcastStatus = MagicPropertyBuilder.buildMagicPropertyName(SessionConnection::class.java, "buildSessionBroadcastStatus")
		private val logger = LoggerFactory.getLogger(SessionConnection::class.java)

		@Volatile
		private lateinit var sessionConnectionInstance: SessionConnection

		@JvmStatic
		@Synchronized
		fun getInstance(context: Context): SessionConnection {
			if (::sessionConnectionInstance.isInitialized) return sessionConnectionInstance

			val applicationContext = context.applicationContext
			return SessionConnection(
				MessageBus(LocalBroadcastManager.getInstance(applicationContext)),
				SelectedBrowserLibraryIdentifierProvider(applicationContext),
				get(applicationContext)).apply { sessionConnectionInstance = this }
		}
	}
}
