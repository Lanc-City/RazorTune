with open('app/src/main/java/com/example/ui/screens/SearchScreen.kt', 'r') as f:
    code = f.read()

old_sig = '''    thumbnailCornerRadius: Int = 16,
    contentLayoutStyle: ContentLayoutStyle = ContentLayoutStyle.STANDARD,
    downloadStates: Map<String, DownloadStatus> = emptyMap(),'''

new_sig = '''    thumbnailCornerRadius: Int = 16,
    contentLayoutStyle: ContentLayoutStyle = ContentLayoutStyle.STANDARD,
    searchBarStyle: SearchBarStyle = SearchBarStyle.CLASSIC_BOX,
    downloadStates: Map<String, DownloadStatus> = emptyMap(),'''

assert old_sig in code, 'old_sig not found'
code = code.replace(old_sig, new_sig, 1)

old_body_start = '''    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp),
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenStats,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("stats_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BarChart,
                            contentDescription = "Listening Stats",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("settings_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_placeholder),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("search_clear_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search",
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        onSubmitSearch()
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input"),
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
            ) {
                items(SearchFilterOption.entries, key = { it.name }) { option ->
                    val selected = option == selectedFilter
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectFilter(option) },
                        label = {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("filter_chip_${option.name.lowercase()}"),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))'''

new_body_start = '''    val focusManager = LocalFocusManager.current
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            isSearchExpanded = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .then(if (searchBarStyle == SearchBarStyle.FLOATING_BOTTOM) Modifier.padding(bottom = 72.dp) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchBarStyle == SearchBarStyle.HEADER_EXPAND && !isSearchExpanded && query.isBlank()) {
                            IconButton(
                                onClick = { isSearchExpanded = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("header_search_expand_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "Expand Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(
                            onClick = onOpenStats,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("stats_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BarChart,
                                contentDescription = "Listening Stats",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("settings_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                val showSearchInput = searchBarStyle != SearchBarStyle.FLOATING_BOTTOM &&
                    (searchBarStyle != SearchBarStyle.HEADER_EXPAND || isSearchExpanded || query.isNotBlank())

                AnimatedVisibility(visible = showSearchInput) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search_placeholder),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            leadingIcon = {
                                if (searchBarStyle == SearchBarStyle.HEADER_EXPAND && query.isBlank()) {
                                    IconButton(
                                        onClick = { isSearchExpanded = false },
                                        modifier = Modifier.minimumInteractiveComponentSize(),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Close Search",
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onQueryChange("") },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("search_clear_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Clear search",
                                        )
                                    }
                                } else if (searchBarStyle == SearchBarStyle.HEADER_EXPAND) {
                                    IconButton(
                                        onClick = {
                                            isSearchExpanded = false
                                            onQueryChange("")
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize(),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Close search",
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = when (searchBarStyle) {
                                SearchBarStyle.CLASSIC_BOX -> RoundedCornerShape(14.dp)
                                else -> RoundedCornerShape(50.dp)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    onSubmitSearch()
                                },
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_input"),
                        )
                    }
                }

                val showFilterChips = query.isNotBlank() || isSearchExpanded
                AnimatedVisibility(visible = showFilterChips) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp),
                        ) {
                            items(SearchFilterOption.entries, key = { it.name }) { option ->
                                val selected = option == selectedFilter
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectFilter(option) },
                                    label = {
                                        Text(
                                            text = option.label,
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                    modifier = Modifier
                                        .heightIn(min = 40.dp)
                                        .testTag("filter_chip_${option.name.lowercase()}"),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))'''

assert old_body_start in code, 'old_body_start not found'
code = code.replace(old_body_start, new_body_start, 1)

old_end = '''            }
        }
    }
}

@Composable
private fun HomeFeedContent('''

floating_bar_code = '''            }
        }

        if (searchBarStyle == SearchBarStyle.FLOATING_BOTTOM) {
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .widthIn(max = 720.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { onQueryChange("") },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("search_clear_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            onSubmitSearch()
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input_floating"),
                )
            }
        }
    }
}

@Composable
private fun HomeFeedContent('''

assert old_end in code, 'old_end not found'
code = code.replace(old_end, floating_bar_code, 1)

with open('app/src/main/java/com/example/ui/screens/SearchScreen.kt', 'w') as f:
    f.write(code)

print('Successfully updated SearchScreen.kt via script')
