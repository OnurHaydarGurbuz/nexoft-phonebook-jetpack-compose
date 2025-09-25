package com.example.nexoft.navigation


import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.nexoft.feature.contacts.ContactsScreen
import com.example.nexoft.feature.create.CreateContactScreen

object Routes {
    const val CONTACTS = "contacts"
    const val CREATE = "create"
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.CONTACTS) {
        composable(Routes.CONTACTS) {
            ContactsScreen(onCreateNew = { navController.navigate(Routes.CREATE) })
        }
        composable(Routes.CREATE) {
            CreateContactScreen(
                onCancel = { navController.popBackStack() },
                onDone = { first, last, phone ->
                    // TODO: add to ViewModel / show Lottie success
                    navController.popBackStack()
                }
            )
        }
    }
}
