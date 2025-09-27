package com.example.nexoft.navigation

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.nexoft.feature.contacts.ContactsScreen
import com.example.nexoft.feature.contacts.ContactsViewModel
import com.example.nexoft.feature.create.CreateContactScreen
import com.example.nexoft.feature.edit.EditContactScreen
import com.example.nexoft.feature.profile.ContactProfileScreen
import com.example.nexoft.feature.profile.ContactUi
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.ui.platform.LocalContext

object Routes {
    const val CONTACTS = "contacts"
    const val CREATE   = "create"
    const val PROFILE  = "profile"
    const val EDIT     = "edit"

    fun profileOf(id: String): String =
        "$PROFILE/${URLEncoder.encode(id, StandardCharsets.UTF_8.toString())}"

    fun editOf(id: String): String =
        "$EDIT/${URLEncoder.encode(id, StandardCharsets.UTF_8.toString())}"
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.CONTACTS) {

        // CONTACTS
        composable(Routes.CONTACTS) { backStackEntry ->
            // Alt ekranlarla aynı VM’yi paylaş
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.CONTACTS)
            }
            val vm: ContactsViewModel = viewModel(parentEntry)

            ContactsScreen(
                onCreateNew  = { navController.navigate(Routes.CREATE) },
                onOpenProfile = { id -> navController.navigate(Routes.profileOf(id)) },
                vm = vm,
                nav = navController
            )
        }

        // CREATE
        composable(Routes.CREATE) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.CONTACTS)
            }
            val vm: ContactsViewModel = viewModel(parentEntry)
            val ctx = LocalContext.current

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                CreateContactScreen(
                    onCancel = { navController.popBackStack() },
                    onDone = { first, last, phone, photoUri ->

                        vm.addContactRemote(
                            ctx = ctx,
                            first = first,
                            last = last,
                            phone = phone,
                            photoUri = photoUri?.toString()
                        )

                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("globalToast", "User created!")

                        navController.popBackStack()
                    }
                )
            }
        }

        // PROFILE/{id}
        composable(
            route = "${Routes.PROFILE}/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.CONTACTS)
            }
            val vm: ContactsViewModel = viewModel(parentEntry)

            val id = backStackEntry.arguments?.getString("id") ?: run {
                navController.popBackStack(); return@composable
            }

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
                onEdit  = { navController.navigate(Routes.editOf(ui.id)) },
                onDelete = { delId ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("deleteRequestId", delId)
                    navController.popBackStack() // geri: Contacts
                },
                onMarkedDeviceSaved = { savedId -> vm.markAsSavedToDevice(savedId) }
            )
        }

        // EDIT/{id}
        composable(
            route = "${Routes.EDIT}/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.CONTACTS)
            }
            val vm: ContactsViewModel = viewModel(parentEntry)
            val ctx = LocalContext.current

            val id = backStackEntry.arguments?.getString("id") ?: run {
                navController.popBackStack(); return@composable
            }

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
                onDone   = { updated ->

                    vm.updateContactRemote(
                        ctx = ctx,
                        id = updated.id,
                        first = updated.first,
                        last  = updated.last,
                        phone = updated.phone,
                        photoUri = updated.photo?.toString()
                    )
                    val contactsEntry = navController.getBackStackEntry(Routes.CONTACTS)
                    contactsEntry.savedStateHandle["globalToast"] = "User is updated!"
                    navController.popBackStack(Routes.CONTACTS, /* inclusive = */ false)
                },
                onRequestDelete = {
                    val contactsEntry = navController.getBackStackEntry(Routes.CONTACTS)
                    contactsEntry.savedStateHandle["deleteRequestId"] = ui.id
                    navController.popBackStack(Routes.CONTACTS, false)
                }
            )
        }
    }
}
