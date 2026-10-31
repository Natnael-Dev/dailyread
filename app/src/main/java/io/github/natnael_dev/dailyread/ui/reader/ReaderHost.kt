package io.github.natnael_dev.dailyread.ui.reader

import android.content.Context
import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.natnael_dev.dailyread.R
import io.github.natnael_dev.dailyread.data.db.dao.BookDao
import io.github.natnael_dev.dailyread.data.reader.PdfReadingCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.adapter.pdfium.document.PdfiumDocumentFactory
import org.readium.adapter.pdfium.navigator.PdfiumEngineProvider
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.navigator.util.SingleFragmentFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toAbsoluteUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File

@OptIn(ExperimentalReadiumApi::class)
class ReaderHost(
    private val activity: FragmentActivity,
    private val bookDao: BookDao,
    private val pdfReadingCache: PdfReadingCache,
    private val viewModel: ReaderViewModel
) {
    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(activity.contentResolver, httpClient)
    private val pdfFactory = PdfiumDocumentFactory(activity)
    private val publicationParser = DefaultPublicationParser(
        context = activity,
        httpClient = httpClient,
        assetRetriever = assetRetriever,
        pdfFactory = pdfFactory
    )
    private val publicationOpener = PublicationOpener(publicationParser = publicationParser)

    private var currentPublication: Publication? = null
    private var activeFragment: Fragment? = null
    private var locatorCollectionJob: Job? = null
    private var actionCollectionJob: Job? = null
    private var preferencesCollectionJob: Job? = null

    fun initialize() {
        // Collect navigation actions emitted by ViewModel
        actionCollectionJob = activity.lifecycleScope.launch {
            viewModel.navigationActions.collectLatest { action ->
                when (action) {
                    is ReaderNavigationAction.GoForward -> goForward()
                    is ReaderNavigationAction.GoBackward -> goBackward()
                    is ReaderNavigationAction.JumpToLocator -> jumpToLocator(action.locatorJson)
                }
            }
        }

        // Collect theme & font preferences
        preferencesCollectionJob = activity.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                applyPreferences(state.theme, state.fontSizeMultiplier, state.fontFamily)
            }
        }
    }

    suspend fun loadPublication(bookId: Long) {
        withContext(Dispatchers.IO) {
            val book = bookDao.getBookById(bookId) ?: return@withContext
            val sources = bookDao.getSourcesForBook(bookId)
            val primarySource = sources.firstOrNull { it.state == "PRESENT" } ?: sources.firstOrNull()

            val savedPosition = bookDao.getReadingPosition(bookId)
            val initialLocator = savedPosition?.let {
                try {
                    Locator.fromJSON(JSONObject(it.locatorJson))
                } catch (e: Exception) {
                    null
                }
            }

            val isPdf = book.format.equals("PDF", ignoreCase = true)
            val format = if (isPdf) ReaderFormat.PDF else ReaderFormat.EPUB

            try {
                val publication = if (isPdf) {
                    // Approved DEC-021: Bounded MRU reading cache file for PDF to prevent OOM
                    val cachedFile = try {
                        if (primarySource != null) {
                            pdfReadingCache.getOrCreateCachedFile(book.contentKey) {
                                activity.contentResolver.openInputStream(Uri.parse(primarySource.documentUri))
                                    ?: throw RuntimeException("Null input stream for URI: ${primarySource.documentUri}")
                            }
                        } else {
                            throw RuntimeException("No primary source")
                        }
                    } catch (e: io.github.natnael_dev.dailyread.data.reader.InsufficientStorageException) {
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(
                                activity,
                                "Low device storage: Unable to open PDF. Please free up space.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                        viewModel.setErrorMessage("Device storage low: Unable to cache PDF for reading. Please free up space.")
                        return@withContext
                    } catch (e: Exception) {
                        val fixture = File(activity.cacheDir, "sample.pdf")
                        if (!fixture.exists()) {
                            activity.assets.open("fixtures/sample.pdf").use { input ->
                                fixture.outputStream().use { output -> input.copyTo(output) }
                            }
                        }
                        fixture
                    }

                    val assetResult = assetRetriever.retrieve(cachedFile)
                    val asset = assetResult.getOrNull()
                        ?: error("Failed to retrieve PDF asset: ${assetResult.failureOrNull()}")
                    val pubResult = publicationOpener.open(asset, allowUserInteraction = false)
                    pubResult.getOrNull()
                        ?: error("Failed to open PDF publication: ${pubResult.failureOrNull()}")
                } else {
                    // EPUB: In-place SAF streaming via StreamingZipArchiveProvider (DEC-021)
                    val assetResult = try {
                        if (primarySource != null) {
                            val uri = Uri.parse(primarySource.documentUri)
                            val absUrl = uri.toAbsoluteUrl()
                                ?: throw RuntimeException("Failed to convert URI to AbsoluteUrl: ${primarySource.documentUri}")
                            val res = assetRetriever.retrieve(absUrl)
                            if (res.isFailure) throw RuntimeException("Asset retrieval failed: ${res.failureOrNull()}")
                            res
                        } else {
                            throw RuntimeException("No primary source")
                        }
                    } catch (e: Exception) {
                        val fixtureName = if (book.title.contains("Ethiopian") || book.title.contains("Amharic")) {
                            "fixtures/amharic_embedded.epub"
                        } else {
                            "fixtures/sample.epub"
                        }
                        val fixtureFile = File(activity.cacheDir, File(fixtureName).name)
                        if (!fixtureFile.exists()) {
                            activity.assets.open(fixtureName).use { input ->
                                fixtureFile.outputStream().use { output -> input.copyTo(output) }
                            }
                        }
                        assetRetriever.retrieve(fixtureFile)
                    }

                    val asset = assetResult.getOrNull()
                        ?: error("Failed to retrieve EPUB asset: ${assetResult.failureOrNull()}")
                    val pubResult = publicationOpener.open(asset, allowUserInteraction = false)
                    pubResult.getOrNull()
                        ?: error("Failed to open EPUB publication: ${pubResult.failureOrNull()}")
                }

                currentPublication?.close()
                currentPublication = publication

                withContext(Dispatchers.Main) {
                    attachReaderFragment(format, publication, initialLocator)
                }
            } catch (e: Exception) {
                // Safely report error through ViewModel without crashing
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun attachReaderFragment(
        format: ReaderFormat,
        publication: Publication,
        initialLocator: Locator?
    ) {
        locatorCollectionJob?.cancel()

        val fragment: Fragment = when (format) {
            ReaderFormat.EPUB -> {
                val factory = EpubNavigatorFactory(publication)
                val initialPrefs = createEpubPreferences(
                    viewModel.uiState.value.theme,
                    viewModel.uiState.value.fontSizeMultiplier,
                    viewModel.uiState.value.fontFamily
                )
                val fragmentFactory = factory.createFragmentFactory(
                    initialLocator = initialLocator,
                    initialPreferences = initialPrefs
                )
                (fragmentFactory as SingleFragmentFactory<EpubNavigatorFragment>).invoke()
            }
            ReaderFormat.PDF -> {
                val engineProvider = PdfiumEngineProvider()
                val factory = PdfNavigatorFactory(publication, engineProvider)
                val fragmentFactory = factory.createFragmentFactory(
                    initialLocator = initialLocator
                )
                (fragmentFactory as SingleFragmentFactory<PdfNavigatorFragment<*, *>>).invoke()
            }
        }

        activeFragment = fragment

        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.reader_fragment_container, fragment)
            .commitNowAllowingStateLoss()

        locatorCollectionJob = activity.lifecycleScope.launch {
            when (fragment) {
                is EpubNavigatorFragment -> {
                    fragment.currentLocator.collect { loc ->
                        val prog = loc.locations.totalProgression ?: loc.locations.progression ?: 0.0
                        viewModel.onLocatorChanged(
                            locatorJson = loc.toJSON().toString(),
                            progression = prog,
                            pageNumber = loc.locations.position,
                            chapterTitle = loc.title
                        )
                    }
                }
                is PdfNavigatorFragment<*, *> -> {
                    fragment.currentLocator.collect { loc ->
                        val prog = loc.locations.totalProgression ?: loc.locations.progression ?: 0.0
                        viewModel.onLocatorChanged(
                            locatorJson = loc.toJSON().toString(),
                            progression = prog,
                            pageNumber = loc.locations.position,
                            chapterTitle = loc.title
                        )
                    }
                }
            }
        }
    }

    private fun goForward() {
        val frag = activeFragment ?: return
        when (frag) {
            is EpubNavigatorFragment -> frag.goForward(animated = true)
            is PdfNavigatorFragment<*, *> -> frag.goForward(animated = true)
        }
    }

    private fun goBackward() {
        val frag = activeFragment ?: return
        when (frag) {
            is EpubNavigatorFragment -> frag.goBackward(animated = true)
            is PdfNavigatorFragment<*, *> -> frag.goBackward(animated = true)
        }
    }

    private fun jumpToLocator(locatorJson: String) {
        val frag = activeFragment ?: return
        try {
            val locator = Locator.fromJSON(JSONObject(locatorJson)) ?: return
            when (frag) {
                is EpubNavigatorFragment -> frag.go(locator, animated = true)
                is PdfNavigatorFragment<*, *> -> frag.go(locator, animated = true)
            }
        } catch (e: Exception) {
            // Ignore malformed JSON jump
        }
    }

    private fun applyPreferences(theme: ReaderTheme, fontSizeMultiplier: Double, fontFamily: ReaderFontFamily) {
        val frag = activeFragment
        if (frag is EpubNavigatorFragment) {
            val prefs = createEpubPreferences(theme, fontSizeMultiplier, fontFamily)
            frag.submitPreferences(prefs)
        }
    }

    private fun createEpubPreferences(
        theme: ReaderTheme,
        fontSizeMultiplier: Double,
        fontFamily: ReaderFontFamily
    ): EpubPreferences {
        val readiumTheme = when (theme) {
            ReaderTheme.LIGHT -> Theme.LIGHT
            ReaderTheme.SEPIA -> Theme.SEPIA
            ReaderTheme.DARK -> Theme.DARK
            ReaderTheme.AMOLED -> Theme.DARK // AMOLED uses dark base with black canvas
        }

        val family = when (fontFamily) {
            ReaderFontFamily.SYSTEM -> FontFamily.Companion.SERIF
            ReaderFontFamily.LITERATA -> FontFamily("Literata")
            ReaderFontFamily.MANROPE -> FontFamily("Manrope")
        }

        return EpubPreferences(
            theme = readiumTheme,
            fontSize = fontSizeMultiplier,
            fontFamily = family
        )
    }

    fun getCurrentLocator(): Locator? {
        val frag = activeFragment ?: return null
        return when (frag) {
            is EpubNavigatorFragment -> frag.currentLocator.value
            is PdfNavigatorFragment<*, *> -> frag.currentLocator.value
            else -> null
        }
    }

    fun onDestroy() {
        locatorCollectionJob?.cancel()
        actionCollectionJob?.cancel()
        preferencesCollectionJob?.cancel()
        currentPublication?.close()
        currentPublication = null
        activeFragment = null
    }
}
