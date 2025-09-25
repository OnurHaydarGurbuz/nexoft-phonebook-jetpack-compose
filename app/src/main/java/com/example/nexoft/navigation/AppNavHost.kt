package com.example.nexoft.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.example.nexoft.feature.contacts.ContactsViewModel
import com.example.nexoft.feature.contacts.ContactsScreen
import com.example.nexoft.feature.create.CreateContactScreen
import com.example.nexoft.feature.profile.ContactProfileScreen
import com.example.nexoft.feature.profile.ContactUi
import com.example.nexoft.feature.edit.EditContactScreen
import androidx.core.net.toUri

object Routes {
    const val CONTACTS = "contacts"
    const val CREATE   = "create"
    const val PROFILE  = "profile"
    const val EDIT     = "edit"
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.CONTACTS) {

        // CONTACTS
        composable(Routes.CONTACTS) { backStackEntry ->
            // Aynı VM'i alt ekranlarda da paylaşacağız
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.CONTACTS)
            }
            val vm: ContactsViewModel = viewModel(parentEntry)

            ContactsScreen(
                onCreateNew = { navController.navigate(Routes.CREATE) },
                onOpenProfile = { id -> navController.navigate("${Routes.PROFILE}/$id") },
                vm = vm                                  // <<< eksik virgül hatası burada çözülüyor
            )
        }

        // CREATE
        composable(Routes.CREATE) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.CONTACTS)
            }
            val vm: ContactsViewModel = viewModel(parentEntry)

            CreateContactScreen(
                onCancel = { navController.popBackStack() },
                onDone = { first, last, phone, photoUri ->
                    vm.addContact(first, last, phone, photoUri?.toString())
                    navController.popBackStack()
                }
            )
        }

        // PROFILE/{id}
        composable("${Routes.PROFILE}/{id}") { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.CONTACTS)
            }
            val vm: ContactsViewModel = viewModel(parentEntry)

            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            val state by vm.state.collectAsStateWithLifecycle()
            val c = state.contacts.firstOrNull { it.id == id } ?: run {
                navController.popBackStack(); return@composable
            }
            val ui = ContactUi(
                id    = c.id,
                first = c.firstName,
                last  = c.lastName,
                phone = c.phone,
                photo = c.photoUrl?.toUri(),
                inDevice = c.isInDevice
            )

            ContactProfileScreen(
                contact = ui,
                onBack  = { navController.popBackStack() },
                onEdit  = { navController.navigate("${Routes.EDIT}/${ui.id}") },
                onDelete = { delId ->
                    vm.deleteContact(delId)
                    navController.popBackStack()
                },
                onMarkedDeviceSaved = { savedId ->
                    vm.markAsSavedToDevice(savedId)
                }
            )
        }

        // EDIT/{id}
        composable("${Routes.EDIT}/{id}") { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.CONTACTS)
            }
            val vm: ContactsViewModel = viewModel(parentEntry)

            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            val state by vm.state.collectAsStateWithLifecycle()
            val c = state.contacts.firstOrNull { it.id == id } ?: run {
                navController.popBackStack(); return@composable
            }
            val ui = ContactUi(
                id    = c.id,
                first = c.firstName,
                last  = c.lastName,
                phone = c.phone,
                photo = c.photoUrl?.toUri(),
                inDevice = c.isInDevice
            )

            EditContactScreen(
                initial = ui,
                onCancel = { navController.popBackStack() },
                onDone = { updated ->
                    vm.updateContact(
                        id    = updated.id,
                        first = updated.first,
                        last  = updated.last,
                        phone = updated.phone,
                        photoUri = updated.photo?.toString()
                    )
                    navController.popBackStack()
                }
            )
        }
    }
}
