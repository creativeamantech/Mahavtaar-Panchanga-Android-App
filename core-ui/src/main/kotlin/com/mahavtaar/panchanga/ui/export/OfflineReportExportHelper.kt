package com.mahavtaar.panchanga.ui.export

import android.content.Context
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object OfflineReportExportHelper {

    fun writeTextToUri(context: Context, uri: Uri, content: String): Result<Unit> = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(content.toByteArray(StandardCharsets.UTF_8))
            stream.flush()
        } ?: error("Unable to open output stream for selected file")
    }

    fun readTextFromUri(context: Context, uri: Uri): Result<String> = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).readText()
        } ?: error("Unable to open input stream for selected file")
    }

    fun printHtmlDocument(context: Context, jobName: String, html: String) {
        val webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true

            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    ?: return
                val adapter = view?.createPrintDocumentAdapter(jobName) ?: return
                val attrs = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                printManager.print(jobName, adapter, attrs)
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    fun sanitizeFileName(raw: String, extension: String): String {
        val base = raw.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9._-]+"), "_")
            .trim('_')
            .ifBlank { "mahavtaar_report" }
        return if (base.endsWith(".$extension")) base else "$base.$extension"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HtmlReportPreviewDialog(
    title: String,
    htmlContent: String,
    suggestedFileName: String,
    onDismiss: () -> Unit,
    onSavedMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val safeName = remember(suggestedFileName) {
        OfflineReportExportHelper.sanitizeFileName(suggestedFileName, "html")
    }

    val saveHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        if (uri != null) {
            OfflineReportExportHelper.writeTextToUri(context, uri, htmlContent)
                .onSuccess { onSavedMessage(ext.htmlSavedMsg) }
                .onFailure { err -> onSavedMessage("${ext.exportActionError}: ${err.message.orEmpty()}") }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("html_preview_close_button")
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = strings.cancel)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    OfflineReportExportHelper.printHtmlDocument(
                                        context = context,
                                        jobName = safeName.removeSuffix(".html"),
                                        html = htmlContent
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("html_preview_print_button")
                            ) {
                                Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(ext.printPdfLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Button(
                                onClick = { saveHtmlLauncher.launch(safeName) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("html_preview_save_button")
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(ext.saveHtmlReport, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("html_preview_webview"),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = false
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                            }
                        },
                        update = { view ->
                            view.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                        }
                    )
                }
            }
        }
    }
}
