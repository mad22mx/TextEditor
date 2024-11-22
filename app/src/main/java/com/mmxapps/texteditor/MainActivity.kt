package com.mmxapps.texteditor

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmxapps.texteditor.ui.theme.TextEditorTheme
import java.time.LocalDateTime


class MainActivity : ComponentActivity() {
    var content = "" // I will handle this in a better way later
    // Register document pick launcher
    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { readFile(it) } // Handle the selected document
    }

    // Register document creation launcher
    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { writeFile(it, content) } // Write to the created document
        content = ""
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TextEditorTheme {
                TextEditorScreen(
                    onOpenFile = { openDocumentLauncher.launch(arrayOf("text/plain")) }, // Trigger the file picker
                    onSaveFile = { filename: String, content: String ->
                        this.content = content
                        createDocumentLauncher.launch(filename)
                    }       // Trigger the file creator
                )
            }
        }
    }

    private fun readFile(uri: Uri) {
        contentResolver.openInputStream(uri)?.bufferedReader().use { reader ->
            val content = reader?.readText()
            Log.d("FileContent", content ?: "No content")
        }
    }

    private fun writeFile(uri: Uri, content: String) {
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(content.toByteArray(Charsets.UTF_8))
            Log.d("FileWrite", "Content written successfully")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(
    onOpenFile: () -> Unit,
    onSaveFile: (filename: String, content: String) -> Unit
) {
    var editorText by remember { mutableStateOf("") }
    var filename by remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    BasicTextField(
                        value = filename,
                        onValueChange = { newText -> filename = newText },
                        maxLines = 1,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 16.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                            ) {
                                if (filename.isEmpty()) {
                                    Text(
                                        "*Untitled.txt",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        }

                    )
                },
                colors = TopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { editorText = "" }) {
                        Icon(Icons.Default.Add, contentDescription = "New")
                    }
                    IconButton(onClick = { onOpenFile() }) { // Call the open file callback
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Open")
                    }
                    IconButton(onClick = {
                        if (filename.isEmpty()) {
                            val date = LocalDateTime.now().toLocalDate()
                            val time = LocalDateTime.now().toLocalTime()
                            val datetime = (date.year.toString() + date.monthValue + date.dayOfMonth.toString() + time.hour.toString()
                                    + time.minute.toString().padStart(2,'0') + time.second.toString().padStart(2,'0'))

                            onSaveFile("Untitled$datetime.txt", editorText) // Call the save file callback ; unnamed
                        } else {
                            onSaveFile(filename, editorText) // Call the save file callback ; named
                        }
                         }) {
                        Icon(Icons.Default.Done, contentDescription = "Save")
                    }
                }
            )
        }
    ) { innerPadding ->
        val lazyListState = rememberLazyListState()
        val textFieldScrollState = rememberScrollState()

        // Create synchronized scroll effect , still needs fixing, the height of the index doesn't match with the btf
        LaunchedEffect(
            remember { derivedStateOf { lazyListState.firstVisibleItemIndex } },
            remember { derivedStateOf { lazyListState.firstVisibleItemScrollOffset } }) {
            textFieldScrollState.scrollTo(
                lazyListState.firstVisibleItemScrollOffset +
                        (lazyListState.firstVisibleItemIndex * 50)
            ) // heights needs adjusting current is set to 50
        }

        LaunchedEffect(textFieldScrollState.value) {
            if (!lazyListState.isScrollInProgress) {
                val itemHeight = 50
                val targetItem = textFieldScrollState.value / itemHeight
                lazyListState.scrollToItem(targetItem, textFieldScrollState.value % itemHeight)
            }
        }
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            val lines = editorText.split("\n")
            val maxCharactersPerLine = 38

            LazyColumn(
                modifier = Modifier
                    .width(30.dp)
                    .fillMaxHeight(),
                state = lazyListState
            ) {
                items(lines.size) { index ->
                    // Calculate number of wrapped lines for the current text line
                    val currentLine = lines[index]
                    val wrappedLines = if (currentLine.isEmpty()) {
                        0
                    } else {
                        (currentLine.length - 1) / maxCharactersPerLine
                    }

                    Box(
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        // Display the line number only for the first line
                        Text(
                            text = (index + 1).toString(),
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .fillMaxWidth()
                                .height(20.dp),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace
                        )

                        // Add spacing for wrapped lines

                        if (wrappedLines > 0) {
                            Spacer(
                                modifier = Modifier
                                    .padding(top = (4 * wrappedLines).dp)
                                    .height((wrappedLines * 20).dp) // Adjust height based on your line height
                            )
                        }
                    }
                }
            }

            // Main text editor
            BasicTextField(
                value = editorText,
                onValueChange = { newText ->
                    editorText = newText
                },
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(textFieldScrollState),
                textStyle = TextStyle(
                    color = Color(0xFFFFFFFF),
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 26.495.sp,
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    )
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                    ) {
                        if (editorText.isEmpty()) {
                            Text(
                                "Start typing...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}
