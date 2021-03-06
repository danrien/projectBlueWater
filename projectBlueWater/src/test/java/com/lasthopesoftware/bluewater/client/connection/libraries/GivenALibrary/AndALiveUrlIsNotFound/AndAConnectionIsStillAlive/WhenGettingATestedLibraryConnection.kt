package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndALiveUrlIsNotFound.AndAConnectionIsStillAlive

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.ValidateConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.client.connection.waking.NoopServerAlarm
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.*

class WhenGettingATestedLibraryConnection {
	@Test
	fun thenTheConnectionIsCorrect() {
		assertThat(connectionProvider?.urlProvider).isEqualTo(firstUrlProvider)
	}

	@Test
	fun thenGettingLibraryIsBroadcast() {
		assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionFailed,
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete
			)
	}

	companion object {
		private val statuses: MutableList<BuildingConnectionStatus> = ArrayList()
		private val firstUrlProvider = mockk<IUrlProvider>()
		private var connectionProvider: IConnectionProvider? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val validateConnectionSettings = mockk<ValidateConnectionSettings>()
			every { validateConnectionSettings.isValid(any()) } returns true

			val connectionSettings = ConnectionSettings(accessCode = "aB5nf")
			val deferredConnectionSettings = DeferredPromise(connectionSettings)
			val secondDeferredConnectionSettings = DeferredPromise(connectionSettings)

			val lookupConnection = mockk<LookupConnectionSettings>()
			every {
				lookupConnection.lookupConnectionSettings(LibraryId(2))
			} returns deferredConnectionSettings andThen secondDeferredConnectionSettings

			val liveUrlProvider = mockk<ProvideLiveUrl>()
			every  {
				liveUrlProvider.promiseLiveUrl(LibraryId(2))
			} returns Promise.empty() andThen firstUrlProvider.toPromise()

			val connectionsTester = mockk<TestConnections>()
			every  { connectionsTester.promiseIsConnectionPossible(any()) } returns false.toPromise()
			val libraryConnectionProvider = LibraryConnectionProvider(
				mockk(),
				validateConnectionSettings,
				lookupConnection,
				NoopServerAlarm(),
				liveUrlProvider,
				connectionsTester,
				OkHttpFactory.getInstance()
			)
			val libraryId = LibraryId(2)
			val futureConnectionProvider = libraryConnectionProvider
				.promiseLibraryConnection(libraryId)
				.updates(statuses::add)
				.eventually(
					{
						libraryConnectionProvider.promiseTestedLibraryConnection(libraryId)
							.updates(statuses::add)
					},
					{
						libraryConnectionProvider.promiseTestedLibraryConnection(libraryId)
							.updates(statuses::add)
					})
				.toFuture()
			deferredConnectionSettings.resolve()
			secondDeferredConnectionSettings.resolve()
			connectionProvider = futureConnectionProvider.get()
		}
	}
}
