package com.example.jewelleryapp

import android.net.Uri
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.androidParameters
import com.google.firebase.dynamiclinks.shortLinkAsync

object DynamicLinkHelper {

    private const val DOMAIN_URI_PREFIX = "https://gaganjewellers.page.link"
    private const val PACKAGE_NAME = "com.example.jewelleryapp" // Your app package name

    fun createProductLink(
        productId: String,
        productName: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val deepLink = "https://gaganjewellers.page.link/?productId=$productId"

        val dynamicLink = FirebaseDynamicLinks.getInstance().createDynamicLink()
            .setLink(Uri.parse(deepLink))
            .setDomainUriPrefix(DOMAIN_URI_PREFIX)
            .setAndroidParameters(
                DynamicLink.AndroidParameters.Builder(PACKAGE_NAME)
                    .setMinimumVersion(1) // Minimum app version
                    .build()
            )
            .setSocialMetaTagParameters(
                DynamicLink.SocialMetaTagParameters.Builder()
                    .setTitle("$productName - Gagan Jewellers")
                    .setDescription("Check out this beautiful jewelry piece!")
                    .build()
            )
            .buildDynamicLink()

        // Create short link
        FirebaseDynamicLinks.getInstance().shortLinkAsync {
            longLink = dynamicLink.uri
        }.addOnSuccessListener { result ->
                val shortLink = result.shortLink.toString()
                onSuccess(shortLink)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}