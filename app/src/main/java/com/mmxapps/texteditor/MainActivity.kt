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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Delete
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
    private var content = "" // I will handle this in a better way later
    private var editorText by mutableStateOf("")

    // Register document pick launcher
    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            content = readFile(it).toString()

        } // Handle the selected document
    }

    // Register document creation launcher
    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { writeFile(it, content) } // Write to the created document
        content = ""
    }

    private fun readFile(uri: Uri): String? {
        contentResolver.openInputStream(uri)?.bufferedReader().use { reader ->
            val contentR = reader?.readText()
            Log.d("FileContent", contentR ?: "No content")
            return contentR
        }
    }

    private fun writeFile(uri: Uri, content: String) {
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(content.toByteArray(Charsets.UTF_8))
            Log.d("FileWrite", "Content written successfully")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {

            TextEditorTheme {
                TextEditorScreen(
                    editorText = editorText,
                    onTextChange = { editorText = it }, // Update the state
                    onOpenFile = { onFileOpened ->
                        openDocumentLauncher.launch(arrayOf("text/plain"))
                        val contentRF = content
                        content = ""
                        onFileOpened(contentRF) // Pass the content to the callback
                    }
                    , // Trigger the file picker
                    onSaveFile = { filename: String, content: String ->
                        this.content = content
                        createDocumentLauncher.launch(filename)
                    }       // Trigger the file creator
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(
    onOpenFile: (onFileOpened: (String) -> Unit) -> Unit, // Update the callback,
    onSaveFile: (filename: String, content: String) -> Unit,
    editorText: String,
    onTextChange: (String) -> Unit, // Callback to update text
) {

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
                            fontSize = 12.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                        decorationBox = { innerTextField ->
                            Box {
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
                    IconButton(onClick = { onTextChange("") }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                    IconButton(onClick = {
                        onOpenFile { onTextChange(it) }
                    }) {
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
                        (lazyListState.firstVisibleItemIndex * 16 )
            )
        }

        LaunchedEffect(textFieldScrollState.value) {
            if (!lazyListState.isScrollInProgress) {
                val itemHeight = 16 * editorText.split("\n").size
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

            LazyColumn(
                modifier = Modifier
                    .width(30.dp)
                    .fillMaxHeight(),
                state = lazyListState,
                userScrollEnabled = false
            ) {
                items(lines.size) { index ->
                    Box(
                        modifier = Modifier.padding(end = 8.dp) //padding at the rhs of the index
                    ) {
                        // Display the line number only for the first line
                        Text(
                            text = (index + 1).toString(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(18.dp),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )

                    }
                }
            }

            // Main text editor
            BasicTextField(
                value = editorText,
                onValueChange = { onTextChange(it) },
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(textFieldScrollState)
                    .horizontalScroll(state = rememberScrollState(), enabled = true),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp,
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    )
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box{
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
