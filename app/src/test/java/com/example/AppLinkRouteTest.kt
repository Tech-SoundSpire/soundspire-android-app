package com.example

import android.net.Uri
import com.example.ui.navigation.Routes
import com.example.ui.navigation.appLinkRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// App Link URL -> in-app route mapping (must match the manifest intent filter).
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppLinkRouteTest {

  private fun route(url: String) = appLinkRoute(Uri.parse(url))

  @Test
  fun `community links open the right tab`() {
    assertEquals("${Routes.COMMUNITY_DETAIL}/coldplay/about", route("https://app.soundspire.online/community/coldplay"))
    assertEquals("${Routes.COMMUNITY_DETAIL}/coldplay/all-chat", route("https://app.soundspire.online/community/coldplay/all-chat"))
    assertEquals("${Routes.COMMUNITY_DETAIL}/coldplay/suggestions", route("https://app.soundspire.online/community/coldplay/suggestions?x=1"))
    // Unknown tab falls back to About.
    assertEquals("${Routes.COMMUNITY_DETAIL}/coldplay/about", route("https://app.soundspire.online/community/coldplay/whatever"))
  }

  @Test
  fun `review links open song and album screens`() {
    assertEquals("${Routes.REVIEW_DETAIL}/4uLU6hMCjMI75M1A2tKUQC", route("https://app.soundspire.online/reviews/song/4uLU6hMCjMI75M1A2tKUQC"))
    assertEquals("${Routes.ALBUM_DETAIL}/1ATL5GLyefJaxhQzSPVrLX", route("https://app.soundspire.online/reviews/album/1ATL5GLyefJaxhQzSPVrLX"))
  }

  @Test
  fun `other links are ignored`() {
    assertNull(route("https://app.soundspire.online/community"))
    assertNull(route("https://app.soundspire.online/login"))
    assertNull(route("https://app.soundspire.online/reviews/song"))
    assertNull(route("https://evil.example.com/community/coldplay"))
    assertNull(route("http://app.soundspire.online/community/coldplay"))
  }
}
