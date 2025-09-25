package com.example.nexoft.navigation


import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.nexoft.feature.contacts.ContactsScreen
import com.example.nexoft.feature.create.CreateContactScreen
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel

object Routes {
    const val CONTACTS = "contacts"
    const val CREATE = "create"
}


@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.CONTACTS) {

        // CONTACTS
        composable(Routes.CONTACTS) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.CONTACTS)
            }
            val vm: com.example.nexoft.feature.contacts.ContactsViewModel = viewModel(parentEntry)

            ContactsScreen(
                onCreateNew = { navController.navigate(Routes.CREATE) }
                // (optional) vm = vm
            )
        }

        // CREATE
        composable(Routes.CREATE) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.CONTACTS)
            }
            val vm: com.example.nexoft.feature.contacts.ContactsViewModel = viewModel(parentEntry)

            CreateContactScreen(
                onCancel = { navController.popBackStack() },
                onDone = { first, last, phone, photoUri ->
                    vm.addContact(first, last, phone, photoUri?.toString())
                    navController.popBackStack()
                }
            )
        }
    }
}
