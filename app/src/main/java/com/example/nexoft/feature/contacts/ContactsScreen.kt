package com.example.nexoft.feature.contacts


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nexoft.navigation.Routes
import com.example.nexoft.ui.AppToastHost
import com.example.nexoft.ui.DeleteContactSheet
import com.example.nexoft.ui.SwipeActionRow
import com.example.nexoft.ui.rememberToastHostState
import androidx.core.content.edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.ui.unit.Dp


@Composable
fun ContactsScreen(
    onCreateNew: () -> Unit,
    onOpenProfile: (id: String) -> Unit,
    vm: ContactsViewModel = viewModel(),
    nav: NavController
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val toastHost = rememberToastHostState()
    val focus = LocalFocusManager.current

// === PATCH [1] Search history state & persistence ===
    var showHistory by remember { mutableStateOf(false) }
    val searchHistory = remember { mutableStateListOf<String>() }

    val prefs = remember(ctx) { ctx.getSharedPreferences("contacts_prefs", Context.MODE_PRIVATE) }

    // senkron yaz (uygulama kapanırken veri kaybolmasın)
    fun persistHistory() {
        prefs.edit(commit = true) { putString("search_history", searchHistory.joinToString("|")) }
    }

// açılışta tek sefer yükle
    LaunchedEffect(Unit) {
        val stored = prefs.getString("search_history", null)
        if (!stored.isNullOrBlank()) {
            searchHistory.clear()
            searchHistory.addAll(stored.split("|").filter { it.isNotBlank() }.take(5))
        }
    }



    // === PATCH [2] pushHistory (var olan fonksiyonu bununla değiştir)
    fun pushHistory(term: String) {
        val t = term.trim()
        if (t.isBlank()) return
        val idx = searchHistory.indexOfFirst { s -> s.equals(t, ignoreCase = true) }
        if (idx >= 0) searchHistory.removeAt(idx)
        searchHistory.add(0, t)
        if (searchHistory.size > 5) searchHistory.removeAt(searchHistory.lastIndex)
        persistHistory()
    }



    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var openRowId by remember { mutableStateOf<String?>(null) }

    // READ_CONTACTS izni & device rozetleri
    val readPermLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) vm.refreshDeviceBadges(ctx)
    }
    LaunchedEffect(Unit) {
        val hasRead = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED
        if (hasRead) vm.refreshDeviceBadges(ctx) else readPermLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    // Edit/Create dönüş sinyalleri
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val liveDelete = nav.currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>("deleteRequestId")
        val obsDelete = Observer<String> { id ->
            pendingDeleteId = id
            openRowId = null
            nav.currentBackStackEntry?.savedStateHandle?.remove<String>("deleteRequestId")
        }
        liveDelete?.observe(lifecycleOwner, obsDelete)

        val liveToast = nav.currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>("globalToast")
        val obsToast = Observer<String> { msg ->
            toastHost.show(msg)
            nav.currentBackStackEntry?.savedStateHandle?.remove<String>("globalToast")
        }
        liveToast?.observe(lifecycleOwner, obsToast)

        onDispose {
            liveDelete?.removeObserver(obsDelete)
            liveToast?.removeObserver(obsToast)
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) openRowId = null
        showHistory = false
    }

    Scaffold { inner ->
        Box(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .background(Color(0xFFF6F6F6))
                .imePadding()
        ) {
            Column(Modifier.fillMaxSize()) {

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Contacts",
                        color = Color(0xFF202020),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    BlueCircleAddButton(onClick = onCreateNew)
                }

                // Search
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    val doSearch: () -> Unit = {
                        pushHistory(state.searchQuery)
                        showHistory = false
                        focus.clearFocus()
                    }
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = {
                            vm.onEvent(ContactsEvent.OnSearchChanged(it))
                            if (it.isNotBlank()) showHistory = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = "Search",
                                tint = Color(0xFFB0B0B0),
                                modifier = Modifier.clickable {
                                    if (state.searchQuery.isBlank()) {
                                        showHistory = !showHistory
                                    } else {
                                        doSearch()
                                    }
                                }
                            )
                        },
                        placeholder = {     Text(
                            "Search contacts by name",
                            color = Color(0xFFB0B0B0),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .onKeyEvent { e ->
                                if (e.key == androidx.compose.ui.input.key.Key.Enter &&
                                    e.type == androidx.compose.ui.input.key.KeyEventType.KeyUp
                                ) {
                                    doSearch()
                                    true
                                } else false
                            },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color(0xFFE7E7E7),
                            focusedBorderColor = Color(0xFF0075FF),
                            cursorColor = Color(0xFF0075FF)
                        ),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { doSearch() },
                            onDone   = { doSearch() }
                        )
                    )


                    // --- Search History Panel (textfield ALTINDA) ---
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showHistory && searchHistory.isNotEmpty(),
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(150))
                    ) {
                        SearchHistoryPanel(
                            history = searchHistory,
                            onSelect = { term ->
                                vm.onEvent(ContactsEvent.OnSearchChanged(term))
                                pushHistory(term)
                                showHistory = false
                                focus.clearFocus()
                            },
                            onRemove = { term ->
                                searchHistory.remove(term)
                                if (searchHistory.isEmpty()) showHistory = false
                                persistHistory()
                            },
                            onClearAll = {
                                searchHistory.clear()
                                showHistory = false
                                persistHistory()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .zIndex(10f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ---- Filtre / Sonuçlar ----
                val q = state.searchQuery.trim().lowercase()
                val filtered = state.contacts
                    .filter {
                        if (q.isBlank()) true else {
                            val name = (it.firstName + " " + it.lastName).trim().lowercase()
                            name.contains(q) || it.phone.lowercase().contains(q)
                        }
                    }
                    .sortedWith(
                        compareBy<com.example.nexoft.core.model.Contact> {
                            // startsWith olanlar öne
                            val name = (it.firstName + " " + it.lastName).trim().lowercase()
                            val starts = if (q.isNotBlank() && (name.startsWith(q) || it.phone.lowercase().startsWith(q))) 0 else 1
                            starts
                        }.thenBy {
                            (it.firstName + " " + it.lastName).trim().ifBlank { it.phone }.lowercase()
                        }
                    )

                if (filtered.isEmpty()) {
                    if (q.isBlank()) {
                        EmptyState(onCreateNew)
                    } else {
                        NoSearchResults()
                    }
                } else {
                    val groups = filtered.groupBy { displayInitial(it) }.toSortedMap()

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        groups.forEach { (letter, contacts) ->
                            stickyHeader { SectionHeader(letter = letter) }

                            itemsIndexed(contacts, key = { _, c -> c.id }) { index, contact ->
                                val isFirst = index == 0
                                val isLast = index == contacts.lastIndex
                                val shape = when {
                                    isFirst && isLast -> RoundedCornerShape(8.dp)
                                    isFirst -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                    isLast -> RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                                    else -> RoundedCornerShape(0.dp)
                                }

                                SwipeActionRow(
                                    isOpen = openRowId == contact.id,
                                    onOpen = { openRowId = contact.id },
                                    onClose = { if (openRowId == contact.id) openRowId = null },
                                    onEdit = {
                                        openRowId = null
                                        nav.navigate(Routes.editOf(contact.id))
                                    },
                                    onDelete = {
                                        openRowId = null
                                        pendingDeleteId = contact.id
                                    },
                                    actionHeight = 64,
                                    shape = shape                      // <- oval kenarlar ile uyumlu
                                ) {
                                    Surface(
                                        color = Color.White,
                                        shape = shape,
                                        shadowElevation = 0.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 64.dp)
                                            .clickable(enabled = openRowId == null) {
                                                onOpenProfile(contact.id)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Avatar(contact)
                                            Spacer(Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = displayName(contact),
                                                    color = Color(0xFF3D3D3D),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = contact.phone,
                                                    color = Color(0xFF6D6D6D),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isLast) Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }

            // ---- Delete sheet ----
            pendingDeleteId?.let { id ->
                DeleteContactSheet(
                    onDismiss = { pendingDeleteId = null },
                    onConfirm = {
                        vm.deleteContact(id)
                        pendingDeleteId = null
                        toastHost.show("User is deleted!")
                    }
                )
            }

            // ---- Toast host ----
            AppToastHost(hostState = toastHost)




        }
    }
}

@Composable
private fun SearchHistoryPanel(
    history: List<String>,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
                .verticalScroll(rememberScrollState())) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SEARCH HISTORY",
                    color = Color(0xFFB0B0B0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Clear All",
                    color = Color(0xFF0075FF),
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onClearAll() }
                )
            }

            // Items
            history.forEachIndexed { i, term ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .padding(horizontal = 12.dp)
                        .clickable { onSelect(term) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Remove",
                        tint = Color(0xFF202020),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                onRemove(term)
                            }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        term,
                        color = Color(0xFF4F4F4F),
                        fontSize = 14.sp
                    )
                }

                if (i != history.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        thickness = 1.dp,
                        color = Color(0xFFF6F6F6)
                    )
                }
            }
        }
    }
}

@Composable
private fun NoSearchResults() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp)
            .padding(horizontal = 20.dp),
        //verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NoSearchIcon(size = 96.dp, color = Color(0xFFD1D1D1))
        Spacer(Modifier.height(8.dp))
        Text(
            "No Results",
            color = Color(0xFF202020),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "The user you are looking for could not be found.",
            color = Color(0xFF3D3D3D),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}




@Composable
private fun Avatar(contact: com.example.nexoft.core.model.Contact) {
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(Color(0xFFEDFAFF)),
            contentAlignment = Alignment.Center
        ) {
            val photo = contact.photoUrl
            if (!photo.isNullOrBlank()) {
                AsyncImage(
                    model = photo,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = displayName(contact).firstOrNull()?.uppercaseChar()?.toString() ?: "#",
                    color = Color(0xFF0075FF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (contact.isInDevice) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0075FF))
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = "Saved in phone",
                    tint = Color.White,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                )
            }
        }
    }
}


@Composable
private fun BlueCircleAddButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0xFF0075FF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.White)
    }
}

@Composable
private fun SectionHeader(letter: Char) {
    Surface(color = Color.White, shadowElevation = 0.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = letter.toString(),
                color = Color(0xFFB0B0B0),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}


private fun displayName(c: com.example.nexoft.core.model.Contact): String {
    val name = listOf(c.firstName, c.lastName).filter { it.isNotBlank() }.joinToString(" ").trim()
    return name.ifBlank { c.phone }
}

private fun displayInitial(c: com.example.nexoft.core.model.Contact): Char {
    val name = displayName(c)
    return name.firstOrNull()?.uppercaseChar() ?: '#'
}

@Composable
private fun EmptyState(onCreateNew: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 33.dp)
            .offset(y = (-180).dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        PersonIcon(size = 90.dp, color = Color(0xFFD1D1D1))

        Spacer(Modifier.height(16.dp))
        Text(
            "No Contacts",
            color = Color(0xFF202020),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Contacts you’ve added will appear here.",
            color = Color(0xFF3D3D3D),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Create New Contact",
            color = Color(0xFF0075FF),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onCreateNew() }
        )
    }
}

@Composable
fun PersonIcon(
    modifier: Modifier = Modifier,
    size: Dp = 90.dp,
    color: Color = Color(0xFFD1D1D1)
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "No contacts",
            tint = Color.White,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}


@Composable
fun NoSearchIcon(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    color: Color = Color(0xFFD1D1D1)
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = "No search results",
            tint = Color.White,
            modifier = Modifier.size(size * 0.58f)
        )
    }
}
