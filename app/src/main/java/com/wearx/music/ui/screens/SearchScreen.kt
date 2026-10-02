package com.wearx.music.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.wearx.music.R
import com.wearx.music.data.model.Album
import com.wearx.music.data.model.Track
import com.wearx.music.ui.components.AlbumArtwork
import com.wearx.music.ui.components.MarqueeText
import com.wearx.music.ui.components.NoEdgeFadeScaling
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.CompactButton
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.SolidColor

/**
 * Searches the library by title or artist.
 *
 * Wear Material 3 ships no text field, so the input is the foundation [BasicTextField] — which is
 * also what the platform's own search affordances use. It is focused on entry so the keyboard comes
 * up without a tap, and the query survives leaving and returning because the whole screen is kept
 * alive by the back stack's saved state.
 *
 * Matching is case-insensitive and substring-based, deliberately: a watch keyboard is slow enough
 * that fuzzy matching would mostly hide results rather than find them.
 */
@Composable
fun SearchScreen(
    tracks: List<Track>,
    albums: List<Album>,
    artByTrackId: Map<Long, Uri?>,
    onTrackClick: (Track) -> Unit,
    onAlbumClick: (Album) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScalingLazyListState()

    val trimmed = query.trim()
    val matchingTracks = remember(trimmed, tracks) {
        if (trimmed.isEmpty()) emptyList() else tracks.filter { it.matches(trimmed) }
    }
    val matchingAlbums = remember(trimmed, albums) {
        if (trimmed.isEmpty()) emptyList() else albums.filter { album ->
            album.title.contains(trimmed, ignoreCase = true) ||
                album.artist.contains(trimmed, ignoreCase = true) ||
                album.tracks.any { it.title.contains(trimmed, ignoreCase = true) }
        }
    }

    ScreenScaffold(scrollState = scrollState) { padding ->
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = scrollState,
            scalingParams = NoEdgeFadeScaling,
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            item {
                // A tinted, rounded container behind the whole field. Without it the caret floats on
                // the list with nothing to say where typing ends and the results begin — and on a
                // dial this narrow the field has to read as an object, not as stray text.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (trimmed.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Search,
                            ),
                            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                        )
                    }
                    if (trimmed.isNotEmpty()) {
                        Spacer(Modifier.width(4.dp))
                        CompactButton(
                            onClick = {
                                query = ""
                                keyboard?.hide()
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.search_clear),
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                }
            }

            if (trimmed.isEmpty()) {
                item { Spacer(Modifier.height(8.dp)) }
            } else if (matchingTracks.isEmpty() && matchingAlbums.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.search_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                    )
                }
            }

            itemsIndexed(matchingTracks, key = { _, track -> track.id }) { _, track ->
                SearchResultRow(
                    title = track.title.ifBlank { stringResource(R.string.label_unknown_album) },
                    subtitle = track.artist.ifBlank { stringResource(R.string.label_unknown_artist) },
                    artUri = track.artUri ?: artByTrackId[track.id],
                    onClick = { onTrackClick(track) },
                )
            }

            itemsIndexed(matchingAlbums, key = { _, album -> album.id }) { _, album ->
                SearchResultRow(
                    title = album.title.ifBlank { stringResource(R.string.label_unknown_album) },
                    subtitle = album.artist.ifBlank { stringResource(R.string.label_unknown_artist) },
                    artUri = album.artUri,
                    onClick = { onAlbumClick(album) },
                )
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }

    // Bringing the keyboard up on entry is the whole point of the screen; a tap to focus first is a
    // step the user should not have to take.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
}

/** Case-insensitive substring match on title or artist. */
private fun Track.matches(query: String): Boolean =
    title.contains(query, ignoreCase = true) || artist.contains(query, ignoreCase = true)

@Composable
private fun SearchResultRow(
    title: String,
    subtitle: String,
    artUri: Uri?,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArtwork(
                artworkUri = artUri,
                contentDescription = title,
                size = 32.dp,
                cornerRadius = 8.dp,
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                MarqueeText(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyExtraSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}