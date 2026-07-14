                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Role Control", style = MaterialTheme.typography.titleMedium)
                    
                    val isAudioOwner by viewModel.isAudioOwner.collectAsState()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text("Active Audio Owner")
                        Switch(
                            checked = isAudioOwner,
                            onCheckedChange = { viewModel.requestRole(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Connection Logs", style = MaterialTheme.typography.titleMedium)
